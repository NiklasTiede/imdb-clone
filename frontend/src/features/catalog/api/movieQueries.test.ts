import { movieQueries } from "./movieQueries";

describe("movieQueries", () => {
  it("builds a stable movie detail query key", () => {
    expect(movieQueries.detail(2872718).queryKey).toEqual([
      "catalog",
      "movie",
      2872718,
    ]);
  });

  it("disables the detail query when no movie id exists", () => {
    expect(movieQueries.detail(null).enabled).toBe(false);
  });
});
