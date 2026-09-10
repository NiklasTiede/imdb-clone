import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { BrowserAudio } from "./browserAudio";

class Source {
  buffer: unknown;
  onended?: () => void;
  connect = vi.fn();
  disconnect = vi.fn();
  start = vi.fn();
  stop = vi.fn();
}

class Context {
  static latest: Context;
  currentTime = 1;
  destination = {};
  sources: Source[] = [];
  constructor() {
    Context.latest = this;
  }
  createAnalyser = () => ({
    connect: vi.fn(),
    getFloatTimeDomainData: vi.fn(),
  });
  createBuffer = vi.fn((_channels: number, length: number, rate: number) => ({
    duration: length / rate,
    copyToChannel: vi.fn(),
  }));
  createBufferSource = () => {
    const source = new Source();
    this.sources.push(source);
    return source;
  };
  close = vi.fn().mockResolvedValue(undefined);
}

const pcm = (seconds: number) => new ArrayBuffer(seconds * 24_000 * 2);

beforeEach(() => vi.stubGlobal("AudioContext", Context));
afterEach(() => vi.unstubAllGlobals());

it("queues a normal answer delivered faster than playback without disconnecting", () => {
  const audio = new BrowserAudio();
  for (let i = 0; i < 45; i++) audio.play(pcm(0.5));
  const sources = Context.latest.sources;
  expect(sources).toHaveLength(45);
  expect(sources[0]?.start).toHaveBeenCalledWith(1.3);
  expect(sources[44]?.start).toHaveBeenCalledWith(23.3);
  audio.close();
  expect(sources.every((source) => source.stop.mock.calls.length === 1)).toBe(
    true,
  );
});

it("bounds queued audio including the incoming chunk before allocating playback buffers", () => {
  const audio = new BrowserAudio();
  expect(() => audio.play(pcm(61))).toThrow("Voice playback is falling behind");
  expect(Context.latest.createBuffer).not.toHaveBeenCalled();
  audio.play(pcm(40));
  expect(() => audio.play(pcm(21))).toThrow("Voice playback is falling behind");
  expect(Context.latest.createBuffer).toHaveBeenCalledTimes(1);
  audio.close();
});

it("reclaims queue capacity as audio plays and when interrupted", () => {
  const audio = new BrowserAudio();
  audio.play(pcm(40));
  Context.latest.currentTime += 30;
  audio.play(pcm(30));
  const sources = [...Context.latest.sources];
  audio.interrupt();
  expect(sources.every((source) => source.stop.mock.calls.length === 1)).toBe(
    true,
  );
  audio.play(pcm(40));
  expect(Context.latest.sources[2]?.start).toHaveBeenCalledWith(31.3);
  audio.close();
});

it("rejects malformed PCM and ignores audio after closing", () => {
  const audio = new BrowserAudio();
  expect(() => audio.play(new ArrayBuffer(3))).toThrow("Invalid voice audio");
  expect(() => audio.play(new ArrayBuffer(0))).toThrow("Invalid voice audio");
  audio.close();
  audio.play(pcm(1));
  expect(Context.latest.sources).toHaveLength(0);
});

it("schedules jittery chunks contiguously after a short initial buffer", () => {
  const audio = new BrowserAudio();
  for (const arrival of [1, 1.11, 1.17, 1.32, 1.34, 1.48]) {
    Context.latest.currentTime = arrival;
    audio.play(pcm(0.1));
  }
  const sources = Context.latest.sources;
  for (let i = 1; i < sources.length; i++) {
    const previous = sources[i - 1]?.start.mock.calls[0]?.[0] as number;
    const next = sources[i]?.start.mock.calls[0]?.[0] as number;
    expect(next).toBeCloseTo(previous + 0.1, 6);
  }
  audio.close();
});

it("keeps the reply interruptible during underruns and re-buffers arriving audio", () => {
  const audio = new BrowserAudio();
  audio.play(pcm(0.1));
  Context.latest.currentTime = 2;
  Context.latest.sources[0]?.onended?.();
  expect(audio.levels().playing).toBe(true);
  audio.play(pcm(0.1));
  expect(Context.latest.sources[1]?.start).toHaveBeenCalledWith(2.3);
  audio.finishReply();
  expect(audio.levels().playing).toBe(true);
  Context.latest.sources[1]?.onended?.();
  expect(audio.levels().playing).toBe(false);
  audio.close();
});

it("clears a drained reply on completion or interruption without waiting for more audio", () => {
  const audio = new BrowserAudio();
  audio.play(pcm(0.1));
  Context.latest.sources[0]?.onended?.();
  audio.finishReply();
  expect(audio.levels().playing).toBe(false);
  audio.play(pcm(0.1));
  audio.interrupt();
  expect(audio.levels().playing).toBe(false);
  audio.close();
});

it("starts permission and worklet loading without waiting for the audio device", async () => {
  let resume: (() => void) | undefined;
  const loadModule = vi.fn().mockResolvedValue(undefined);
  const connect = vi.fn();
  class StartupContext extends Context {
    sampleRate = 48000;
    audioWorklet = { addModule: loadModule };
    resume = () =>
      new Promise<void>((resolve) => {
        resume = resolve;
      });
    createMediaStreamSource = () => ({ connect });
  }
  const track = { stop: vi.fn(), addEventListener: vi.fn() };
  const stream = { getTracks: () => [track], getAudioTracks: () => [track] };
  const microphone = vi.fn().mockResolvedValue(stream);
  const capture = {
    port: { close: vi.fn() },
    connect: vi.fn(),
    disconnect: vi.fn(),
  };
  const created = vi.fn();
  vi.stubGlobal("AudioContext", StartupContext);
  vi.stubGlobal("navigator", { mediaDevices: { getUserMedia: microphone } });
  vi.stubGlobal(
    "AudioWorkletNode",
    class {
      constructor() {
        created();
        return capture;
      }
    },
  );
  const audio = new BrowserAudio();
  const granted = vi.fn();
  const starting = audio.start(vi.fn(), vi.fn(), granted);
  expect(microphone).toHaveBeenCalledOnce();
  expect(loadModule).toHaveBeenCalledOnce();
  await Promise.resolve();
  expect(granted).toHaveBeenCalledOnce();
  expect(created).not.toHaveBeenCalled();
  resume?.();
  await starting;
  expect(created).toHaveBeenCalledOnce();
  audio.close();
  expect(track.stop).toHaveBeenCalledOnce();
});

it("stops a late microphone grant after cancellation without opening a connection", async () => {
  let grant: ((stream: unknown) => void) | undefined;
  class StartupContext extends Context {
    audioWorklet = { addModule: vi.fn().mockResolvedValue(undefined) };
    resume = vi.fn().mockResolvedValue(undefined);
  }
  const track = { stop: vi.fn() };
  vi.stubGlobal("AudioContext", StartupContext);
  vi.stubGlobal("navigator", {
    mediaDevices: {
      getUserMedia: () =>
        new Promise((resolve) => {
          grant = resolve;
        }),
    },
  });
  const audio = new BrowserAudio();
  const onGranted = vi.fn();
  const starting = audio.start(vi.fn(), vi.fn(), onGranted);
  audio.close();
  grant?.({ getTracks: () => [track] });
  await starting;
  expect(track.stop).toHaveBeenCalledOnce();
  expect(onGranted).not.toHaveBeenCalled();
});
