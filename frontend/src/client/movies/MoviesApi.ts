import axios, { AxiosInstance, AxiosRequestConfig, AxiosError } from "axios";
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
const customAxiosInstance: AxiosInstance = axios.create();
customAxiosInstance.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    const token = window.localStorage.getItem("jwtToken");
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

const moviesApiClientConfig: Configuration = new Configuration({
    basePath: process.env.REACT_APP_IMDB_CLONE_BACKEND_APP_ADDRESS,
  baseOptions: customAxiosInstance,
});

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
