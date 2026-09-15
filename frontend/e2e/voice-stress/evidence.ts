import { writeFile } from "node:fs/promises";
import type { Page, TestInfo } from "@playwright/test";
import { voiceEventSchema } from "../../src/features/concierge/model/voice";
import type { BrowserEvidence } from "./browser";

export type WireEvent = {
  at: number;
  type: string;
  turn?: number;
  tool?: string;
  status?: string;
  action?: string;
  matchedMovie?: string;
};
export class WireEvidence {
  events: WireEvent[] = [];
  audio: { at: number; pcm: Buffer }[] = [];
  inputBytes = 0;
  inputSignal: { at: number; peak: number; bytes: number }[] = [];
  connections = 0;
  closes = 0;
  private bytes = 0;
  constructor(
    page: Page | undefined,
    private movies: Map<number, string> = new Map(),
  ) {
    page?.on("websocket", (socket) => {
      if (!socket.url().endsWith("/v1/voice")) return;
      this.connections++;
      socket.on("close", () => this.closes++);
      socket.on("framesent", ({ payload }) => {
        if (typeof payload !== "string") this.recordInput(payload);
        // Never retain start credentials, context or transcript payloads.
      });
      socket.on("framereceived", ({ payload }) => this.receive(payload));
    });
  }
  recordInput(payload: Buffer) {
    this.inputBytes += payload.byteLength;
    let peak = 0;
    for (let i = 0; i + 1 < payload.length; i += 2)
      peak = Math.max(peak, Math.abs(payload.readInt16LE(i)) / 32768);
    if (this.inputSignal.length < 16500)
      this.inputSignal.push({ at: Date.now(), peak, bytes: payload.length });
  }
  receive(payload: string | Buffer) {
    const at = Date.now();
    if (typeof payload !== "string") {
      this.bytes += payload.byteLength;
      if (this.bytes <= 48_000 * 330) this.audio.push({ at, pcm: payload });
      return;
    }
    let decoded: unknown;
    try {
      decoded = JSON.parse(payload);
    } catch {
      this.events.push({ at, type: "invalid-event" });
      return;
    }
    const parsed = voiceEventSchema.safeParse(decoded);
    if (!parsed.success) {
      this.events.push({ at, type: "invalid-event" });
      return;
    }
    const event = parsed.data;
    this.events.push({
      at,
      type: event.type,
      turn: event.turn,
      ...(event.activity
        ? { tool: event.activity.tool, status: event.activity.status }
        : {}),
      ...(event.action
        ? {
            action: event.action.type,
            ...("movieId" in event.action && event.action.movieId
              ? {
                  matchedMovie:
                    this.movies.get(event.action.movieId) ?? "unexpected-movie",
                }
              : {}),
          }
        : {}),
    });
  }
}

export function signalMetrics(samples: readonly number[], rate: number) {
  let clipped = 0,
    energy = 0,
    maxJump = 0,
    prior = 0;
  for (const sample of samples) {
    const value = sample / 32768;
    if (Math.abs(sample) >= 32760) clipped++;
    energy += value * value;
    maxJump = Math.max(maxJump, Math.abs(value - prior));
    prior = value;
  }
  // Near-sinusoidal, loud, persistent audio merits listening review, not an automatic failure.
  let tonalRun = 0,
    longestTonalRun = 0;
  const frame = Math.round(rate * 0.02);
  for (let offset = 0; offset + frame <= samples.length; offset += frame) {
    let sum = 0,
      difference = 0,
      previousDifference = 0,
      samplesEnergy = 0;
    for (let i = offset + 2; i < offset + frame; i++) {
      const a = samples[i]! - samples[i - 1]!;
      const b = samples[i - 1]! - samples[i - 2]!;
      sum += a * b;
      difference += a * a;
      previousDifference += b * b;
      samplesEnergy += (samples[i]! / 32768) ** 2;
    }
    const correlation = sum / Math.sqrt(difference * previousDifference || 1);
    const frequency =
      (Math.acos(Math.max(-1, Math.min(1, correlation))) * rate) /
      (2 * Math.PI);
    // A sine also satisfies x[n]+x[n-2] = 2*cos(w)*x[n-1]. Check residual energy.
    let residual = 0;
    for (let i = offset + 2; i < offset + frame; i++)
      residual +=
        ((samples[i]! + samples[i - 2]! - 2 * correlation * samples[i - 1]!) /
          32768) **
        2;
    const tonal =
      samplesEnergy / frame > 0.0025 &&
      frequency > 700 &&
      frequency < 6500 &&
      residual / (samplesEnergy || 1) < 0.0005;
    tonalRun = tonal ? tonalRun + 20 : 0;
    longestTonalRun = Math.max(longestTonalRun, tonalRun);
  }
  return {
    seconds: samples.length / rate,
    rms: Math.sqrt(energy / (samples.length || 1)),
    clippingRatio: clipped / (samples.length || 1),
    maxSampleJump: maxJump,
    longestTonalCandidateMs: longestTonalRun,
    listeningReviewRequired: longestTonalRun >= 500,
  };
}
export function pcmSamples(buffer: Buffer): number[] {
  const samples: number[] = [];
  for (let i = 0; i + 1 < buffer.length; i += 2)
    samples.push(buffer.readInt16LE(i));
  return samples;
}
export function wav(samples: readonly number[], rate: number): Buffer {
  const output = Buffer.alloc(44 + samples.length * 2);
  output.write("RIFF", 0);
  output.writeUInt32LE(output.length - 8, 4);
  output.write("WAVEfmt ", 8);
  output.writeUInt32LE(16, 16);
  output.writeUInt16LE(1, 20);
  output.writeUInt16LE(1, 22);
  output.writeUInt32LE(rate, 24);
  output.writeUInt32LE(rate * 2, 28);
  output.writeUInt16LE(2, 32);
  output.writeUInt16LE(16, 34);
  output.write("data", 36);
  output.writeUInt32LE(samples.length * 2, 40);
  samples.forEach((sample, i) =>
    output.writeInt16LE(Math.max(-32768, Math.min(32767, sample)), 44 + i * 2),
  );
  return output;
}
export async function saveEvidence(
  info: TestInfo,
  browser: BrowserEvidence,
  wire: WireEvidence,
  scenario: object,
) {
  const received = pcmSamples(
    Buffer.concat(wire.audio.map((frame) => frame.pcm)),
  );
  const rendered = browser.rendered.flatMap((frame) => frame.samples);
  const scheduled = browser.playback.filter(
    (event) => event.type === "scheduled",
  );
  const report = {
    schemaVersion: 2,
    browserTimeOrigin: browser.timeOrigin,
    scenario,
    connection: {
      opened: wire.connections,
      closed: wire.closes,
      inputBytes: wire.inputBytes,
    },
    wireEvents: wire.events,
    audioArrivals: wire.audio.map((frame) => ({
      at: frame.at,
      bytes: frame.pcm.length,
    })),
    playback: browser.playback,
    microphone: browser.microphone,
    inputSignal: wire.inputSignal,
    received: signalMetrics(received, 24000),
    rendered: signalMetrics(rendered, browser.sampleRate || 48000),
    scheduling: {
      chunks: scheduled.length,
      gapsOver40ms: scheduled.filter((event) => (event.gapMs ?? 0) > 40).length,
      maxQueueMs: Math.max(0, ...scheduled.map((event) => event.queueMs ?? 0)),
    },
    limitations: [
      "Audio metrics are diagnostics, not a MOS/intelligibility score.",
      "Scheduling gaps can be natural pauses; received WAV concatenates packets and removes network gaps.",
      "Rendered WAV taps the real Web Audio mix; it does not measure speakers, microphone hardware or OS echo cancellation.",
      "No automatic retry, real microphone, account mutations, raw transcripts or tool arguments.",
      "Provider billing is separate; session wall time is a cap, not a measured dollar amount.",
    ],
  };
  for (const [name, body] of [
    ["report.json", JSON.stringify(report, null, 2)],
    ["received.wav", wav(received, 24000)],
    ["rendered.wav", wav(rendered, browser.sampleRate || 48000)],
  ] as const) {
    const path = info.outputPath(name);
    await writeFile(path, body);
    await info.attach(name, {
      path,
      contentType: name.endsWith("wav") ? "audio/wav" : "application/json",
    });
  }
  return report;
}

export function hasFreshMovieAction(
  events: readonly WireEvent[],
  since: number,
  title: string,
  trailer: boolean,
) {
  return events.some(
    (event) =>
      event.at >= since &&
      event.type === "ui-action" &&
      event.matchedMovie === title &&
      event.action === (trailer ? "open_movie_trailer" : "open_movie"),
  );
}
