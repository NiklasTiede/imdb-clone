import {Configuration, MovieControllerApi, AccountControllerApi, CommentControllerApi, WatchedMovieControllerApi, RatingControllerApi} from './generator-output';

/**
 * Configurations for Clients
 * */
const moviesApiClientConfig: Configuration = new Configuration({
});

export const moviesApi = new MovieControllerApi(moviesApiClientConfig);

export const accountApi = new AccountControllerApi(moviesApiClientConfig);

export const commentApi = new CommentControllerApi(moviesApiClientConfig);

export const watchlistApi = new WatchedMovieControllerApi(moviesApiClientConfig);

export const ratingApi = new RatingControllerApi(moviesApiClientConfig);

/**
 * Clients
 */
// export const api = {
//   moviesApi: new MoviesApi(moviesApiClientConfig)
// };
