import {
  getMovieImageUrl,
  getObjectStorageImageUrl,
  getProfileImageUrl,
  ObjectStorageImageSize,
} from "./imageUrls";
import { vi } from "vitest";

describe("getObjectStorageImageUrl", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("builds a movie image URL from the configured object storage address", () => {
    vi.stubEnv("VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS", "http://localhost:9000");

    expect(getObjectStorageImageUrl("poster-token", ObjectStorageImageSize.Small)).toBe(
      "http://localhost:9000/imdb-clone/movies/poster-token_size_120x180.jpg",
    );
  });

  it("falls back to the legacy MinIO address during migration", () => {
    vi.stubEnv("VITE_IMDB_CLONE_MINIO_ADDRESS", "http://legacy-storage:9000");

    expect(getMovieImageUrl("poster-token", ObjectStorageImageSize.Large)).toBe(
      "http://legacy-storage:9000/imdb-clone/movies/poster-token_size_600x900.jpg",
    );
  });

  it("falls back to localhost when no object storage address is configured", () => {
    expect(getMovieImageUrl("poster-token", ObjectStorageImageSize.Large)).toBe(
      "http://localhost:9000/imdb-clone/movies/poster-token_size_600x900.jpg",
    );
  });

  it("builds a profile image URL from the configured object storage address", () => {
    vi.stubEnv("VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS", "http://localhost:9000");

    expect(getProfileImageUrl("avatar-token")).toBe(
      "http://localhost:9000/imdb-clone/profile-photos/avatar-token_size_800x800.jpg",
    );
  });
});
