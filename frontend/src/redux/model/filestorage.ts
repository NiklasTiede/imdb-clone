import { createModel } from "@rematch/core";
import { RootModel } from "../models";
import { fileStorageApi } from "../../client/movies/MoviesApi";
import { i18n } from "../../i18n";
import { AxiosRequestConfig, AxiosResponse } from "axios";
import { Movie } from "../../client/movies/generator-output";

export type State = {
  profilePhotoSwitch: boolean;
  movies: Array<Movie>;
};

export const fileStorage = createModel<RootModel>()({
  state: {
    profilePhotoSwitch: false,
    movies: [],
  } as State,
  reducers: {
    setUpdateProfilePhoto: (state) => {
      reduce(state, {
        profilePhotoSwitch: !state.profilePhotoSwitch,
      });
    },
    setMovies: (state, payload: Array<Movie>) =>
      reduce(state, {
        movies: payload,
      }),
  },

  effects: (dispatch) => ({
    async storeUserProfilePhoto(image) {
      fileStorageApi
        .storeUserProfilePhoto(image)
        .then((response: AxiosResponse<Array<string>>) => {
          if (response.status === 200 && response.data !== null) {
            console.log(response.data);

            // turn switch for rerendering
            dispatch.fileStorage.setUpdateProfilePhoto();

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
