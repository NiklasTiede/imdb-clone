import { describe, expect, test } from "vitest";
import { formatRatingCount } from "./formatRatingCount";

describe("formatRatingCount", () => {
  test("formats counts under 1000 as the raw number", () => {
    expect(formatRatingCount(0)).toBe("0");
    expect(formatRatingCount(42)).toBe("42");
    expect(formatRatingCount(999)).toBe("999");
  });

  test("formats thousands with one decimal when not a whole multiple", () => {
    expect(formatRatingCount(1200)).toBe("1.2k");
    expect(formatRatingCount(12345)).toBe("12.3k");
  });

  test("drops trailing .0 for whole-thousand values", () => {
    expect(formatRatingCount(1000)).toBe("1k");
    expect(formatRatingCount(245000)).toBe("245k");
  });

  test("formats millions with M suffix", () => {
    expect(formatRatingCount(1_000_000)).toBe("1M");
    expect(formatRatingCount(1_500_000)).toBe("1.5M");
  });

  test("returns dash for nullish values", () => {
    expect(formatRatingCount(undefined)).toBe("—");
    expect(formatRatingCount(null)).toBe("—");
  });
});
