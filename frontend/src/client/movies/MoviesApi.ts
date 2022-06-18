import {Configuration, MoviesApi} from './generator-output';

/**
 * Configurations for Clients
 * */
const moviesApiClientConfig: Configuration = new Configuration({
});

export const moviesApi = new MoviesApi(moviesApiClientConfig)

/**
 * Clients
 */
// export const api = {
//   moviesApi: new MoviesApi(moviesApiClientConfig)
// };
