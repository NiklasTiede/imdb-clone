export enum MinioImageSize {
  Small = "120x180",
  Large = "600x900",
}

export const getMinioImageUrl = (
  token: string,
  size: MinioImageSize
): string => {
  const minioHost =
    process.env.REACT_APP_IMDB_CLONE_MINIO_ADDRESS ?? "http://localhost:9000";
  const basePath = "/imdb-clone/movies/";

  return `${minioHost}${basePath}${token}_size_${size}.jpg`;
};
