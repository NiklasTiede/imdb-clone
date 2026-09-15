import type { Page } from "@playwright/test";

export type PlaybackEvent = {
  at: number;
  type: "scheduled" | "stopped" | "ended";
  id: number;
  start?: number;
  duration?: number;
  queueMs?: number;
  gapMs?: number;
};
export type AudioFrame = { at: number; samples: number[] };
export type BrowserEvidence = {
  timeOrigin: number;
  sampleRate: number;
  playback: PlaybackEvent[];
  rendered: AudioFrame[];
  microphone: { start: number; end: number }[];
};

declare global {
  interface Window {
    voiceStress: BrowserEvidence & {
      speak: (wav: string) => Promise<{ start: number; end: number }>;
      ready: boolean;
    };
  }
}

/** Test-only synthetic microphone and output tap. The application's encoder, WebSocket,
 * AudioWorklet, scheduling, interruption and navigation code run unchanged. */
export async function installAudioLab(page: Page, startupDelayMs = 0) {
  await page.addInitScript(
    ({ startupDelayMs }) => {
      const OriginalContext = window.AudioContext;
      let microphone: AudioContext | undefined;
      let destination: MediaStreamAudioDestinationNode | undefined;
      const evidence: Window["voiceStress"] = {
        timeOrigin: performance.timeOrigin,
        sampleRate: 0,
        playback: [],
        rendered: [],
        microphone: [],
        ready: false,
        async speak(wav) {
          if (!microphone || !destination)
            throw new Error("Synthetic microphone is not ready");
          await microphone.resume();
          const bytes = Uint8Array.from(atob(wav), (char) =>
            char.charCodeAt(0),
          );
          const buffer = await microphone.decodeAudioData(bytes.buffer);
          const source = microphone.createBufferSource();
          source.buffer = buffer;
          source.connect(destination);
          const start = performance.now();
          const interval = { start, end: start + buffer.duration * 1000 };
          evidence.microphone.push(interval);
          source.start();
          await new Promise<void>((resolve, reject) => {
            const timeout = setTimeout(
              () => {
                source.disconnect();
                reject(
                  new Error(
                    "Synthetic utterance did not finish; audio session may have closed",
                  ),
                );
              },
              buffer.duration * 1000 + 2000,
            );
            source.addEventListener(
              "ended",
              () => {
                clearTimeout(timeout);
                source.disconnect();
                resolve();
              },
              { once: true },
            );
          });
          return interval;
        },
      };
      window.voiceStress = evidence;
      navigator.mediaDevices.getUserMedia = async () => {
        microphone = new OriginalContext();
        await microphone.resume();
        destination = microphone.createMediaStreamDestination();
        // A connected silent oscillator keeps the mic clock running between utterances.
        const silence = microphone.createConstantSource();
        silence.offset.value = 0;
        silence.connect(destination);
        silence.start();
        evidence.ready = true;
        return destination.stream;
      };
      const recorderCode = `
      class Recorder extends AudioWorkletProcessor {
        constructor() { super(); this.frames = []; this.count = 0; }
        process(inputs) {
          const samples = inputs[0]?.[0] ?? new Float32Array(128);
          if (samples) {
            for (const value of samples) this.frames.push(Math.max(-32768, Math.min(32767, Math.round(value * 32767))));
            if (this.frames.length >= sampleRate / 10) {
              this.port.postMessage({ time: currentTime + samples.length / sampleRate, samples: this.frames });
              this.frames = [];
            }
          }
          return true;
        }
      }
      registerProcessor('stress-recorder', Recorder);
    `;
      const recorderUrl = URL.createObjectURL(
        new Blob([recorderCode], { type: "text/javascript" }),
      );
      window.AudioContext = class extends OriginalContext {
        private tap?: AudioWorkletNode;
        private tapReady: Promise<void>;
        private previousEnd = 0;
        private id = 0;
        constructor(options?: AudioContextOptions) {
          super(options);
          evidence.sampleRate = this.sampleRate;
          this.tapReady = this.audioWorklet.addModule(recorderUrl).then(() => {
            this.tap = new AudioWorkletNode(this, "stress-recorder");
            this.tap.connect(this.destination); // Worklet output is silent; only observes the mix.
            this.tap.port.onmessage = (
              event: MessageEvent<{ time: number; samples: number[] }>,
            ) => {
              if (evidence.rendered.length < 3300)
                evidence.rendered.push({
                  at:
                    performance.now() +
                    (event.data.time -
                      this.currentTime -
                      event.data.samples.length / this.sampleRate) *
                      1000,
                  samples: event.data.samples,
                });
            };
          });
        }
        override async resume() {
          await super.resume();
          await this.tapReady;
          if (startupDelayMs)
            await new Promise((resolve) => setTimeout(resolve, startupDelayMs));
        }
        override createBufferSource() {
          const source = super.createBufferSource();
          const id = ++this.id;
          const start = source.start.bind(source);
          const stop = source.stop.bind(source);
          source.start = (when = 0, offset = 0, duration?: number) => {
            const at = Math.max(when, this.currentTime);
            if (this.tap) source.connect(this.tap);
            const seconds = duration ?? (source.buffer?.duration ?? 0) - offset;
            evidence.playback.push({
              at: performance.now(),
              type: "scheduled",
              id,
              start: at,
              duration: seconds,
              queueMs: (at - this.currentTime) * 1000,
              gapMs:
                this.previousEnd > 0
                  ? Math.max(0, at - this.previousEnd) * 1000
                  : 0,
            });
            this.previousEnd = at + seconds;
            if (duration === undefined) start(when, offset);
            else start(when, offset, duration);
          };
          source.stop = (when = 0) => {
            evidence.playback.push({
              at: performance.now(),
              type: "stopped",
              id,
            });
            this.previousEnd = 0;
            stop(when);
          };
          source.addEventListener("ended", () => {
            evidence.playback.push({
              at: performance.now(),
              type: "ended",
              id,
            });
          });
          return source;
        }
        override async close() {
          destination?.stream.getTracks().forEach((track) => track.stop());
          if (microphone && microphone.state !== "closed")
            await microphone.close();
          await super.close();
        }
      };
    },
    { startupDelayMs },
  );
}

export async function snapshot(page: Page): Promise<BrowserEvidence> {
  return page.evaluate(() => ({
    timeOrigin: window.voiceStress.timeOrigin,
    sampleRate: window.voiceStress.sampleRate,
    playback: window.voiceStress.playback,
    rendered: window.voiceStress.rendered,
    microphone: window.voiceStress.microphone,
  }));
}
