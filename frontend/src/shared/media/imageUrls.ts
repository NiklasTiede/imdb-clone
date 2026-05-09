export enum MinioImageSize {
  Small = "120x180",
  Large = "600x900",
  Profile = "800x800",
}

const getMinioHost = () =>
  import.meta.env.VITE_IMDB_CLONE_MINIO_ADDRESS ?? "http://localhost:9000";

export type MovieImageSize = MinioImageSize.Small | MinioImageSize.Large;

export const getMovieImageUrl = (
  token: string,
  size: MovieImageSize,
): string => {
  const minioHost = getMinioHost();
  const basePath = "/imdb-clone/movies/";

  return `${minioHost}${basePath}${token}_size_${size}.jpg`;
};

export const getProfileImageUrl = (token: string): string => {
  const minioHost = getMinioHost();
  const basePath = "/imdb-clone/profile-photos/";

  return `${minioHost}${basePath}${token}_size_${MinioImageSize.Profile}.jpg`;
};

export const getMinioImageUrl = (
  token: string,
  size: MovieImageSize,
): string => {
  return getMovieImageUrl(token, size);
};
