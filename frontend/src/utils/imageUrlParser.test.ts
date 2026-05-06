import { getMinioImageUrl, MinioImageSize } from "./imageUrlParser";

const originalMinioAddress = process.env.REACT_APP_IMDB_CLONE_MINIO_ADDRESS;

describe("getMinioImageUrl", () => {
  afterEach(() => {
    process.env.REACT_APP_IMDB_CLONE_MINIO_ADDRESS = originalMinioAddress;
  });

  it("builds a movie image URL from the configured MinIO address", () => {
    process.env.REACT_APP_IMDB_CLONE_MINIO_ADDRESS = "http://localhost:9000";

    expect(getMinioImageUrl("poster-token", MinioImageSize.Small)).toBe(
      "http://localhost:9000/imdb-clone/movies/poster-token_size_120x180.jpg"
    );
  });

  it("falls back to localhost when no MinIO address is configured", () => {
    delete process.env.REACT_APP_IMDB_CLONE_MINIO_ADDRESS;

    expect(getMinioImageUrl("poster-token", MinioImageSize.Large)).toBe(
      "http://localhost:9000/imdb-clone/movies/poster-token_size_600x900.jpg"
    );
  });
});
