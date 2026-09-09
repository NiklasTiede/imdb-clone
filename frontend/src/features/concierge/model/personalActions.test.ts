import { describe, expect, it } from "vitest";
import { applicationActionSchema } from "./concierge";

const receipt = {
  type: "open_ratings",
  movieId: 6,
  operationId: "1a93141a-7fe4-4210-9e46-82e2827c1db9",
  changed: true,
};

describe("personal action wire contract", () => {
  it("accepts omitted null scores from Python voice serialization", () => {
    expect(applicationActionSchema.parse({ ...receipt, score: 8.5 })).toEqual({
      ...receipt,
      score: 8.5,
      previousScore: null,
    });
    expect(
      applicationActionSchema.parse({ ...receipt, previousScore: 9 }),
    ).toEqual({
      ...receipt,
      score: null,
      previousScore: 9,
    });
  });
  it("preserves zero scores and rejects invalid scores or arbitrary destinations", () => {
    expect(
      applicationActionSchema.parse({ ...receipt, score: 0, previousScore: 0 }),
    ).toEqual({
      ...receipt,
      score: 0,
      previousScore: 0,
    });
    expect(
      applicationActionSchema.safeParse({ ...receipt, score: 11 }).success,
    ).toBe(false);
    expect(
      applicationActionSchema.safeParse({
        ...receipt,
        url: "https://example.invalid",
      }).success,
    ).toBe(false);
  });
});
