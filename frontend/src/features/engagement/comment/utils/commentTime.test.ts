import { describe, expect, test } from "vitest";
import { formatCommentTime, wasCommentEdited } from "./commentTime";

const now = Date.parse("2026-07-11T12:00:00Z");

describe("comment time presentation", () => {
  test("uses readable relative time for recent comments", () => {
    expect(formatCommentTime("2026-07-11T11:58:00Z", now)).toBe(
      "2 minutes ago",
    );
    expect(formatCommentTime("2026-07-10T12:00:00Z", now)).toBe("yesterday");
  });

  test("detects meaningful edits without marking initial persistence", () => {
    expect(
      wasCommentEdited("2026-07-11T10:00:00Z", "2026-07-11T10:05:00Z"),
    ).toBe(true);
    expect(
      wasCommentEdited("2026-07-11T10:00:00Z", "2026-07-11T10:00:00.500Z"),
    ).toBe(false);
  });
});
