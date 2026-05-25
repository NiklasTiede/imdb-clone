import { getMoviePosterToken } from "./movie";

describe("getMoviePosterToken", () => {
  it("uses the canonical poster token", () => {
    expect(
      getMoviePosterToken({
        posterImageToken: "poster-token",
      }),
    ).toBe("poster-token");
  });
});
