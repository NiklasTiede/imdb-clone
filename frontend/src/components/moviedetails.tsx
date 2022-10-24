import React, {useEffect} from 'react';
import { useDispatch, useSelector} from 'react-redux';
import {Dispatch, RootState } from '../redux/store';
import {MovieRecord} from '../client/movies/generator-output';


export default function MovieDetails() {

  const dispatch = useDispatch<Dispatch>()
  const isLoading = useSelector((state: {movies: RootState}) => state.movies.isLoading);
  const loaded = useSelector((state: {movies: RootState}) => state.movies.loaded);
  const movieOne: MovieRecord = useSelector((state: {movies: RootState}) => state.movies.movie);

  useEffect(() => {
    if (!loaded && !isLoading) {
      dispatch.movies.loadMovieById(2872718);
    }
  }, []);

  return (
    <div>
      <h1>Notes</h1>
      <h1>title: { movieOne?.primaryTitle }</h1>
      <h1>startYear: { movieOne?.startYear }</h1>
      <h1>imdbRating: { movieOne?.imdbRating }</h1>
    </div>
  );
}
