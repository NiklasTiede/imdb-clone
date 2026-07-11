import { describe, expect, test } from "vitest";
import {
  getCarouselScrollPosition,
  getHomeFeedInstanceId,
  getHomeFeedVerticalScrollPosition,
  setCarouselScrollPosition,
  setHomeFeedVerticalScrollPosition,
  startNewHomeFeedSession,
} from "./homeFeedSession";

describe("home feed session", () => {
  test("keeps carousel and vertical restoration state within one feed session", () => {
    setCarouselScrollPosition("top-horror", 264);
    setHomeFeedVerticalScrollPosition(512);

    expect(getCarouselScrollPosition("top-horror")).toBe(264);
    expect(getHomeFeedVerticalScrollPosition()).toBe(512);
  });

  test("starts a genuinely fresh discovery session without old scroll state", () => {
    const previousFeedInstanceId = getHomeFeedInstanceId();
    setCarouselScrollPosition("top-horror", 264);
    setHomeFeedVerticalScrollPosition(512);

    const nextFeedInstanceId = startNewHomeFeedSession();

    expect(nextFeedInstanceId).not.toBe(previousFeedInstanceId);
    expect(getCarouselScrollPosition("top-horror")).toBe(0);
    expect(getHomeFeedVerticalScrollPosition()).toBe(0);
  });
});
