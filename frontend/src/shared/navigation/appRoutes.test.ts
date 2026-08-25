import { movieDetailPath } from "./appRoutes";

describe("app-owned routes", () => {
  it("builds a movie details route from a catalog ID", () => {
    expect(movieDetailPath(42)).toBe("/movie?id=42");
  });

  it.each([0, -1, 1.5, Number.NaN, Number.POSITIVE_INFINITY])(
    "rejects unsafe movie ID %s",
    (movieId) => {
      expect(() => movieDetailPath(movieId)).toThrow();
    },
  );
});
