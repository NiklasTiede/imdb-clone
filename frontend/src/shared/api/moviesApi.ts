import {
  AccountEngagementControllerApi,
  AccountControllerApi,
  AuthenticationControllerApi,
  CommentControllerApi,
  Configuration,
  FileStorageControllerApi,
  MovieControllerApi,
  RatingControllerApi,
  SearchControllerApi,
  WatchedMovieControllerApi,
} from "../../client/movies/generator-output";
import { apiHttpClient } from "./httpClient";

const moviesApiClientConfig = new Configuration({
  basePath: import.meta.env.VITE_IMDB_CLONE_BACKEND_ADDRESS,
});

export const authApi = new AuthenticationControllerApi(
  moviesApiClientConfig,
  undefined,
  apiHttpClient,
);

export const accountApi = new AccountControllerApi(
  moviesApiClientConfig,
  undefined,
  apiHttpClient,
);

export const accountEngagementApi = new AccountEngagementControllerApi(
  moviesApiClientConfig,
  undefined,
  apiHttpClient,
);

export const commentApi = new CommentControllerApi(
  moviesApiClientConfig,
  undefined,
  apiHttpClient,
);

export const moviesApi = new MovieControllerApi(
  moviesApiClientConfig,
  undefined,
  apiHttpClient,
);

export const ratingApi = new RatingControllerApi(
  moviesApiClientConfig,
  undefined,
  apiHttpClient,
);

export const searchApi = new SearchControllerApi(
  moviesApiClientConfig,
  undefined,
  apiHttpClient,
);

export const watchlistApi = new WatchedMovieControllerApi(
  moviesApiClientConfig,
  undefined,
  apiHttpClient,
);

export const fileStorageApi = new FileStorageControllerApi(
  moviesApiClientConfig,
  undefined,
  apiHttpClient,
);
