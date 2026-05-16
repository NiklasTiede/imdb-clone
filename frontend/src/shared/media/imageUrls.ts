export enum ObjectStorageImageSize {
  Small = "120x180",
  Large = "600x900",
  Profile = "800x800",
}

const getObjectStorageHost = () =>
  import.meta.env.VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS ??
  import.meta.env.VITE_IMDB_CLONE_MINIO_ADDRESS ??
  "http://localhost:9000";

export type MovieImageSize =
  | ObjectStorageImageSize.Small
  | ObjectStorageImageSize.Large;

export const getMovieImageUrl = (
  token: string,
  size: MovieImageSize,
): string => {
  const objectStorageHost = getObjectStorageHost();
  const basePath = "/imdb-clone/movies/";

  return `${objectStorageHost}${basePath}${token}_size_${size}.jpg`;
};

export const getProfileImageUrl = (token: string): string => {
  const objectStorageHost = getObjectStorageHost();
  const basePath = "/imdb-clone/profile-photos/";

  return `${objectStorageHost}${basePath}${token}_size_${ObjectStorageImageSize.Profile}.jpg`;
};

export const getObjectStorageImageUrl = (
  token: string,
  size: MovieImageSize,
): string => {
  return getMovieImageUrl(token, size);
};

export const MinioImageSize = ObjectStorageImageSize;
export const getMinioImageUrl = getObjectStorageImageUrl;
