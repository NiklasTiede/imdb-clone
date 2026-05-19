export enum ObjectStorageImageSize {
  Small = "120x180",
  Large = "600x900",
  Profile = "800x800",
}

export enum MoviePosterImageSize {
  Small = "120x180",
  Medium = "300x450",
  Large = "600x900",
}

export enum MovieBackdropImageSize {
  Small = "780x439",
  Large = "1280x720",
}

const getObjectStorageHost = () =>
  import.meta.env.VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS ??
  "http://localhost:9000";

export type MovieImageSize =
  | ObjectStorageImageSize.Small
  | ObjectStorageImageSize.Large
  | MoviePosterImageSize;

const buildObjectUrl = (path: string): string =>
  `${getObjectStorageHost()}/imdb-clone/${path}`;

export const getMoviePosterImageUrl = (
  token: string,
  size: MoviePosterImageSize | MovieImageSize,
): string => buildObjectUrl(`movies/posters/${token}_size_${size}.webp`);

export const getMoviePosterFallbackImageUrl = (
  token: string,
  size: MoviePosterImageSize | MovieImageSize,
): string => buildObjectUrl(`movies/posters/${token}_size_${size}.jpg`);

export const getMovieBackdropImageUrl = (
  token: string,
  size: MovieBackdropImageSize,
): string => buildObjectUrl(`movies/backdrops/${token}_size_${size}.webp`);

export const getMovieImageUrl = (
  token: string,
  size: MovieImageSize,
): string => {
  return getMoviePosterImageUrl(token, size);
};

export const getProfileImageUrl = (token: string): string => {
  return buildObjectUrl(
    `profile-photos/${token}_size_${ObjectStorageImageSize.Profile}.jpg`,
  );
};

export const getObjectStorageImageUrl = (
  token: string,
  size: MovieImageSize,
): string => {
  return getMovieImageUrl(token, size);
};
