import { movieCardSx } from "./MovieCard";

describe("movieCardSx", () => {
  it("uses responsive width instead of fixed desktop dimensions", () => {
    expect(movieCardSx.width).toBe("100%");
    expect(movieCardSx.maxWidth).toBe("100%");
    expect("height" in movieCardSx).toBe(false);
  });
});
