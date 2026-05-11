import { describe, expect, test } from "vitest";
import { pickDailyIndex } from "./pickDailyIndex";

describe("pickDailyIndex", () => {
  test("returns a stable index for a given date and collection length", () => {
    expect(pickDailyIndex(10, new Date("2026-05-10T12:00:00Z"))).toBe(
      pickDailyIndex(10, new Date("2026-05-10T23:59:59Z")),
    );
  });

  test("keeps the index inside the collection bounds", () => {
    expect(pickDailyIndex(3, new Date("2026-05-10T00:00:00Z"))).toBeGreaterThanOrEqual(
      0,
    );
    expect(pickDailyIndex(3, new Date("2026-05-10T00:00:00Z"))).toBeLessThan(3);
  });

  test("returns null for an empty collection", () => {
    expect(pickDailyIndex(0, new Date("2026-05-10T00:00:00Z"))).toBeNull();
  });
});
