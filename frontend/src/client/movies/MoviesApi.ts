import {
  Configuration,
  AuthenticationControllerApi,
  AccountControllerApi,
  CommentControllerApi,
  MovieControllerApi,
  RatingControllerApi,
  SearchControllerApi,
  WatchedMovieControllerApi,
  FileStorageControllerApi,
} from "./generator-output";

/**
 * Configurations for Clients
 * */
const moviesApiClientConfig: Configuration = new Configuration({});

export const authApi = new AuthenticationControllerApi(moviesApiClientConfig);

export const accountApi = new AccountControllerApi(moviesApiClientConfig);

export const commentApi = new CommentControllerApi(moviesApiClientConfig);

export const moviesApi = new MovieControllerApi(moviesApiClientConfig);

export const ratingApi = new RatingControllerApi(moviesApiClientConfig);

export const searchApi = new SearchControllerApi(moviesApiClientConfig);

export const watchlistApi = new WatchedMovieControllerApi(
  moviesApiClientConfig
);

export const fileStorageApi = new FileStorageControllerApi(
    moviesApiClientConfig
);
