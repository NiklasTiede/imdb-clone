import { getMoviePosterToken } from "./movie";

describe("getMoviePosterToken", () => {
  it("uses the canonical poster token when the legacy image token is missing", () => {
    expect(
      getMoviePosterToken({
        posterImageToken: "poster-token",
      }),
    ).toBe("poster-token");
  });

  it("keeps the legacy image token when both poster token fields exist", () => {
    expect(
      getMoviePosterToken({
        imageUrlToken: "image-token",
        posterImageToken: "poster-token",
      }),
    ).toBe("image-token");
  });
});
