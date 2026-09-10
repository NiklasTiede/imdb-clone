import { decodePcm, PcmEncoder, VOICE_SAMPLE_RATE } from "./pcm";
import captureUrl from "./capture.worklet.js?no-inline&url";

// Speech can arrive much faster than real time. Keep room for a complete reply
// while bounding queued PCM to about 5.8 MB after decoding to Float32.
const MAX_QUEUED_SECONDS = 60;
const PLAYBACK_LEAD_SECONDS = 0.3;

export class BrowserAudio {
  private readonly context = new AudioContext();
  private readonly output = this.context.createAnalyser();
  private readonly input = this.context.createAnalyser();
  private stream?: MediaStream;
  private capture?: AudioWorkletNode;
  private closed = false;
  private muted = false;
  private nextPlayback = 0;
  private replyPending = false;
  private readonly sources = new Set<AudioBufferSourceNode>();
  private readonly samples = new Float32Array(256);

  constructor() {
    this.input.fftSize = this.output.fftSize = 256;
    this.output.connect(this.context.destination);
  }

  async start(
    onAudio: (audio: ArrayBuffer) => void,
    onEnded: () => void,
    onMicrophoneGranted: () => void = () => {},
  ): Promise<void> {
    // Start all independent setup in the click gesture. On macOS, resuming the
    // audio device can take seconds; it must not delay microphone permission or networking.
    await Promise.all([
      this.context.resume(),
      this.context.audioWorklet.addModule(captureUrl),
      navigator.mediaDevices
        .getUserMedia({
          audio: {
            channelCount: 1,
            echoCancellation: true,
            noiseSuppression: true,
            autoGainControl: true,
          },
        })
        .then((stream) => {
          if (this.closed) {
            stream.getTracks().forEach((track) => track.stop());
            return;
          }
          this.stream = stream;
          stream
            .getAudioTracks()
            .forEach((track) =>
              track.addEventListener("ended", onEnded, { once: true }),
            );
          onMicrophoneGranted();
        }),
    ]);
    if (this.closed || !this.stream) return;
    const encoder = new PcmEncoder(this.context.sampleRate);
    const capture = new AudioWorkletNode(this.context, "concierge-capture");
    this.capture = capture;
    capture.port.onmessage = (event: MessageEvent<Float32Array>) => {
      if (!this.closed && !this.muted) onAudio(encoder.encode(event.data));
    };
    const source = this.context.createMediaStreamSource(this.stream);
    source.connect(this.input);
    source.connect(capture);
    capture.connect(this.context.destination);
  }

  mute(muted: boolean): void {
    this.muted = muted;
    this.stream?.getTracks().forEach((track) => {
      track.enabled = !muted;
    });
  }

  play(data: ArrayBuffer): void {
    if (this.closed) return;
    const queuedSeconds = Math.max(
      0,
      this.nextPlayback - this.context.currentTime,
    );
    const incomingSeconds = data.byteLength / (VOICE_SAMPLE_RATE * 2);
    if (queuedSeconds + incomingSeconds > MAX_QUEUED_SECONDS)
      throw new Error("Voice playback is falling behind");
    const samples = decodePcm(data);
    const buffer = this.context.createBuffer(
      1,
      samples.length,
      VOICE_SAMPLE_RATE,
    );
    buffer.copyToChannel(samples, 0);
    const source = this.context.createBufferSource();
    source.buffer = buffer;
    source.connect(this.output);
    this.sources.add(source);
    source.onended = () => {
      source.disconnect();
      this.sources.delete(source);
    };
    // Buffer briefly at startup/underrun, then join chunks sample-contiguously.
    // Adding a fresh lead to every chunk creates gaps near the playback cursor.
    const at =
      this.nextPlayback > this.context.currentTime
        ? this.nextPlayback
        : this.context.currentTime + PLAYBACK_LEAD_SECONDS;
    source.start(at);
    this.nextPlayback = at + buffer.duration;
    this.replyPending = true;
  }

  finishReply(): void {
    this.replyPending = false;
  }

  levels(): { input: number; output: number; playing: boolean } {
    const rms = (analyser: AnalyserNode): number => {
      analyser.getFloatTimeDomainData(this.samples);
      return Math.min(
        1,
        Math.sqrt(
          this.samples.reduce((sum, x) => sum + x * x, 0) / this.samples.length,
        ) * 6,
      );
    };
    return {
      input: this.muted ? 0 : rms(this.input),
      output: rms(this.output),
      playing: this.replyPending || this.sources.size > 0,
    };
  }

  interrupt(): void {
    this.sources.forEach((source) => {
      source.stop();
      source.disconnect();
    });
    this.sources.clear();
    this.nextPlayback = 0;
    this.replyPending = false;
  }

  close(): void {
    if (this.closed) return;
    this.closed = true;
    this.interrupt();
    this.stream?.getTracks().forEach((track) => track.stop());
    this.capture?.port.close();
    this.capture?.disconnect();
    void this.context.close();
  }
}
