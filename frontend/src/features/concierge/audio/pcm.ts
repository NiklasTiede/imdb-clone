export const VOICE_SAMPLE_RATE = 24_000;

/** Stateful rate conversion: retain fractional windows across worklet chunks. */
export class PcmEncoder {
  private position = 0;
  private sum = 0;
  private weight = 0;

  constructor(private readonly inputRate: number) {
    if (inputRate < VOICE_SAMPLE_RATE)
      throw new Error("Unsupported microphone sample rate");
  }

  encode(samples: Float32Array): ArrayBuffer {
    const ratio = this.inputRate / VOICE_SAMPLE_RATE;
    const output: number[] = [];
    for (const sample of samples) {
      let remaining = 1;
      while (remaining > 1e-9) {
        const portion = Math.min(remaining, ratio - this.position);
        this.sum += sample * portion;
        this.weight += portion;
        this.position += portion;
        remaining -= portion;
        if (this.position >= ratio - 1e-9) {
          const value = Math.max(-1, Math.min(1, this.sum / this.weight));
          output.push(Math.round(value * (value < 0 ? 32768 : 32767)));
          this.position = this.sum = this.weight = 0;
        }
      }
    }
    const result = new ArrayBuffer(output.length * 2);
    const view = new DataView(result);
    output.forEach((value, index) => view.setInt16(index * 2, value, true));
    return result;
  }
}

export const decodePcm = (buffer: ArrayBuffer): Float32Array<ArrayBuffer> => {
  if (!buffer.byteLength || buffer.byteLength % 2)
    throw new Error("Invalid voice audio");
  const view = new DataView(buffer);
  const samples = new Float32Array(buffer.byteLength / 2);
  for (let i = 0; i < samples.length; i++)
    samples[i] = view.getInt16(i * 2, true) / 32768;
  return samples;
};
