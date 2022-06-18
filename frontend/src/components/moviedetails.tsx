import React, {useEffect} from 'react';
import { useDispatch, useSelector} from 'react-redux';
import {Dispatch, RootState } from '../redux/store';
import {MovieDto} from '../client/movies/generator-output';


export default function MovieDetails() {

  const dispatch = useDispatch<Dispatch>()
  const isLoading = useSelector((state: {movies: RootState}) => state.movies.isLoading);
  const loaded = useSelector((state: {movies: RootState}) => state.movies.loaded);
  const movieOne: MovieDto = useSelector((state: {movies: RootState}) => state.movies.movie);

  useEffect(() => {
    if (!loaded && !isLoading) {
      dispatch.movies.loadMovieById(5);
    }
  }, []);

  return (
    <div>
      <h1>Notes</h1>
      <h1>title: { movieOne?.title }</h1>
      <h1>year: { movieOne?.year }</h1>
      <h1>movieId: { movieOne?.movieId }</h1>
    </div>
  );
}
