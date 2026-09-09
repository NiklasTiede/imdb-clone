import { describe, expect, it } from "vitest";
import { decodePcm, PcmEncoder } from "./pcm";

describe("microphone PCM conversion", () => {
  it.each([24000, 44100, 48000])(
    "preserves duration and amplitude at %i Hz across chunks",
    (rate) => {
      const encoder = new PcmEncoder(rate);
      const input = new Float32Array(rate).fill(0.5);
      const chunks: Float32Array[] = [];
      for (let offset = 0; offset < input.length; offset += 137) {
        const encoded = encoder.encode(input.subarray(offset, offset + 137));
        if (encoded.byteLength) chunks.push(decodePcm(encoded));
      }
      expect(chunks.reduce((sum, c) => sum + c.length, 0)).toBe(24000);
      expect(
        chunks.every((chunk) => chunk.every((x) => Math.abs(x - 0.5) < 0.0001)),
      ).toBe(true);
    },
  );

  it("uses signed little-endian PCM16 and clips overflow", () => {
    const encoded = new PcmEncoder(24000).encode(new Float32Array([-2, 0, 2]));
    expect([...new Uint8Array(encoded)]).toEqual([0, 128, 0, 0, 255, 127]);
    expect(() => decodePcm(new ArrayBuffer(3))).toThrow();
  });
});
