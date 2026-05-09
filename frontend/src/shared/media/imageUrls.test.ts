import {
  getMinioImageUrl,
  getMovieImageUrl,
  getProfileImageUrl,
  MinioImageSize,
} from "./imageUrls";
import { vi } from "vitest";

describe("getMinioImageUrl", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("builds a movie image URL from the configured MinIO address", () => {
    vi.stubEnv("VITE_IMDB_CLONE_MINIO_ADDRESS", "http://localhost:9000");

    expect(getMinioImageUrl("poster-token", MinioImageSize.Small)).toBe(
      "http://localhost:9000/imdb-clone/movies/poster-token_size_120x180.jpg",
    );
  });

  it("falls back to localhost when no MinIO address is configured", () => {
    expect(getMovieImageUrl("poster-token", MinioImageSize.Large)).toBe(
      "http://localhost:9000/imdb-clone/movies/poster-token_size_600x900.jpg",
    );
  });

  it("builds a profile image URL from the configured MinIO address", () => {
    vi.stubEnv("VITE_IMDB_CLONE_MINIO_ADDRESS", "http://localhost:9000");

    expect(getProfileImageUrl("avatar-token")).toBe(
      "http://localhost:9000/imdb-clone/profile-photos/avatar-token_size_800x800.jpg",
    );
  });
});
