import { createModel } from "@rematch/core";
import { RootModel } from "../models";
import { fileStorageApi } from "../../client/movies/MoviesApi";
import { i18n } from "../../i18n";
import { AxiosRequestConfig, AxiosResponse } from "axios";
import { Movie } from "../../client/movies/generator-output";

export type State = {
  movies: Array<Movie>;
};

export const fileStorage = createModel<RootModel>()({
  state: {
    movies: [],
  } as State,
  reducers: {
    setMovies: (state, payload: Array<Movie>) =>
      reduce(state, {
        movies: payload,
      }),
  },

  effects: (dispatch) => ({
    async storeUserProfilePhoto(image) {
      const options: AxiosRequestConfig = {
        headers: {
          Authorization: "Bearer " + window.localStorage.getItem("jwtToken"),
        },
      };
      fileStorageApi
        .storeUserProfilePhoto(image, options)
        .then((response: AxiosResponse<Array<string>>) => {
          if (response.status === 200 && response.data !== null) {
            console.log(response.data);
            // dispatch.search.setMovies(response.data.content);
          }
        })
        .catch((reason: any) => {
          dispatch.notify.error(
            i18n.accountSettings.loadingError("search for Movies")
          );
          console.log(
            `Error while attempting to search for movies, reason: ${JSON.stringify(
              reason
            )}`
          );
        });
    },
  }),
});

export const reduce = <T>(s: T, v: any) => Object.assign({}, s, v);
