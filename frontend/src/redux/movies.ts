import { moviesApi } from '../client/movies/MoviesApi';
import {MovieDto} from '../client/movies/generator-output/';

import {createModel} from '@rematch/core';
import type { RootModel } from './models';

// config logger
// const log4js = require('log4js');
// const logger = log4js.getLogger('movies');

export const movies = createModel<RootModel>()({
  state: {
    isLoading: false,
    loaded: false,
    movie: null,
    movieErrorOccurred: false,
  },
  reducers: {
    startLoading: (state) => reduce(state, {
      isLoading: true,
    }),
    stopLoading: (state) => reduce(state, {
      isLoading: false,
      loaded: true,
    }),
    setMovie: (state, payload: MovieDto) => reduce(state, {
      movie: payload
    }),
    movieErrorOccurred: (state) => reduce(state, {
      movieErrorOccurred: true,
    }),
  },
  effects: (dispatch) => ({
    async loadMovieById(movieId) {
      moviesApi.getMovieById(movieId)
               .then(
                 (response: any) => {
                   if (response.status === 200 && response.data !== null) {
                     dispatch.movies.setMovie(response.data);
                   }
                 })
               .catch((reason: any) => {
                 dispatch.movies.movieErrorOccurred();
                 const message = `Error while attempting to load movie for movieId ${movieId}.`;
                 if (reason.message && reason.message === 'Network Error') {
                   console.log(message);
                 } else {
                   console.log(message);
                 }
               })
               .finally(() => {
                 dispatch.movies.stopLoading();
               });
    },


  }),
})

export const reduce = <T>(s: T, v: any) => Object.assign({}, s, v);
