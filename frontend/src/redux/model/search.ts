import { createModel } from "@rematch/core";
import { RootModel } from "../models";
import { searchApi } from "../../client/movies/MoviesApi";
import { i18n } from "../../i18n";
import {
  Movie,
  MovieSearchRequest,
  PagedResponseMovie,
} from "../../client/movies/generator-output";
import { AxiosResponse } from "axios";

export type State = {
  movies: Array<Movie>;
};

export const search = createModel<RootModel>()({
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
    async searchMovies(payload: MovieSearchRequest) {
      searchApi
        .search(payload, 0, 10) // requests are yet not working
        .then((response: AxiosResponse<PagedResponseMovie>) => {
          if (
            response.status === 200 &&
            response.data !== null &&
            response.data.content !== null &&
            response.data.content !== undefined
          ) {
            console.log(response.data);
            console.log(response.data.content);
            dispatch.search.setMovies(response.data.content);
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
