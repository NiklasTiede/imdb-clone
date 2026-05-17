import {
  getMovieBackdropImageUrl,
  getMovieImageUrl,
  getObjectStorageImageUrl,
  getMoviePosterFallbackImageUrl,
  getMoviePosterImageUrl,
  getProfileImageUrl,
  MovieBackdropImageSize,
  MoviePosterImageSize,
  ObjectStorageImageSize,
} from "./imageUrls";
import { vi } from "vitest";

describe("movie image URL helpers", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("builds WebP poster URLs from the configured object storage address", () => {
    vi.stubEnv("VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS", "http://localhost:9000");

    expect(getMoviePosterImageUrl("poster-token", MoviePosterImageSize.Small)).toBe(
      "http://localhost:9000/imdb-clone/movies/posters/poster-token_size_120x180.webp",
    );
  });

  it("keeps getMovieImageUrl as a poster compatibility alias", () => {
    expect(getMovieImageUrl("poster-token", ObjectStorageImageSize.Large)).toBe(
      "http://localhost:9000/imdb-clone/movies/posters/poster-token_size_600x900.webp",
    );
    expect(getObjectStorageImageUrl("poster-token", ObjectStorageImageSize.Small)).toBe(
      "http://localhost:9000/imdb-clone/movies/posters/poster-token_size_120x180.webp",
    );
  });

  it("builds JPG poster fallback URLs for manually uploaded movie posters", () => {
    expect(getMoviePosterFallbackImageUrl("poster-token", MoviePosterImageSize.Large)).toBe(
      "http://localhost:9000/imdb-clone/movies/posters/poster-token_size_600x900.jpg",
    );
  });

  it("builds WebP backdrop URLs", () => {
    expect(getMovieBackdropImageUrl("backdrop-token", MovieBackdropImageSize.Large)).toBe(
      "http://localhost:9000/imdb-clone/movies/backdrops/backdrop-token_size_1280x720.webp",
    );
  });

  it("keeps profile photo URLs as JPG", () => {
    vi.stubEnv("VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS", "http://localhost:9000");

    expect(getProfileImageUrl("avatar-token")).toBe(
      "http://localhost:9000/imdb-clone/profile-photos/avatar-token_size_800x800.jpg",
    );
  });
});
