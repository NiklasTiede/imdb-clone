import { describe, expect, test } from "vitest";
import {
  getValidYouTubeVideoKey,
  getYouTubeNoCookieEmbedUrl,
} from "./youtubeTrailer";

describe("YouTube trailer utilities", () => {
  test("accepts and trims a valid video key", () => {
    expect(getValidYouTubeVideoKey(" abcDEF123_- ")).toBe("abcDEF123_-");
  });

  test("rejects missing, malformed, and URL-shaped values", () => {
    expect(getValidYouTubeVideoKey(undefined)).toBeNull();
    expect(getValidYouTubeVideoKey("")).toBeNull();
    expect(getValidYouTubeVideoKey("too-short")).toBeNull();
    expect(
      getValidYouTubeVideoKey("https://youtube.com/watch?v=abcDEF123_-"),
    ).toBeNull();
  });

  test("builds a privacy-enhanced autoplay embed URL", () => {
    expect(getYouTubeNoCookieEmbedUrl("abcDEF123_-")).toBe(
      "https://www.youtube-nocookie.com/embed/abcDEF123_-?autoplay=1&playsinline=1&rel=0",
    );
  });
});
