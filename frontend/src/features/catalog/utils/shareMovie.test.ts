import { describe, expect, test, vi } from "vitest";
import { buildMovieShareUrl, shareMovie } from "./shareMovie";

describe("shareMovie", () => {
  test("builds a canonical movie URL", () => {
    expect(buildMovieShareUrl(42, "https://movies.example")).toBe(
      "https://movies.example/movie?id=42",
    );
  });

  test("prefers native sharing", async () => {
    const share = vi.fn().mockResolvedValue(undefined);

    await expect(
      shareMovie(
        { movieId: 42, title: "Arrival" },
        { origin: "https://movies.example", share },
      ),
    ).resolves.toBe("shared");
    expect(share).toHaveBeenCalledWith({
      title: "Arrival",
      url: "https://movies.example/movie?id=42",
    });
  });

  test("copies the URL when native sharing is unavailable", async () => {
    const copyText = vi.fn().mockResolvedValue(undefined);

    await expect(
      shareMovie(
        { movieId: 42, title: "Arrival" },
        { copyText, origin: "https://movies.example" },
      ),
    ).resolves.toBe("copied");
    expect(copyText).toHaveBeenCalledWith(
      "https://movies.example/movie?id=42",
    );
  });

  test("treats native share cancellation as neutral", async () => {
    const share = vi
      .fn()
      .mockRejectedValue(new DOMException("Cancelled", "AbortError"));

    await expect(
      shareMovie({ movieId: 42, title: "Arrival" }, { share }),
    ).resolves.toBe("cancelled");
  });

  test("rejects an actual sharing failure", async () => {
    await expect(
      shareMovie(
        { movieId: 42, title: "Arrival" },
        { share: vi.fn().mockRejectedValue(new Error("Share failed")) },
      ),
    ).rejects.toThrow("Share failed");
  });
});
