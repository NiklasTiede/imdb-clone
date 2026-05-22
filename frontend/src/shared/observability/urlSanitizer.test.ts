import { sanitizeUrlForTelemetry } from "./urlSanitizer";

describe("sanitizeUrlForTelemetry", () => {
  it("keeps only origin and pathname for absolute URLs", () => {
    expect(
      sanitizeUrlForTelemetry(
        "https://api.example.test/movies/42?token=secret&q=matrix",
      ),
    ).toBe("https://api.example.test/movies/42");
  });

  it("keeps relative paths without query strings", () => {
    expect(sanitizeUrlForTelemetry("/movies/search?q=matrix")).toBe(
      "/movies/search",
    );
  });
});
