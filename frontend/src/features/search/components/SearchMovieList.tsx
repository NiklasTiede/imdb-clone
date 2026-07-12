import { MovieListRow, MovieListView, type Movie } from "../../catalog";

type SearchMovieListProps = {
  movies: Movie[];
};

const SearchMovieList = ({ movies }: SearchMovieListProps) => (
  <MovieListView ariaLabel="Search results">
    {movies.map((movie) => (
      <MovieListRow
        key={movie.id ?? movie.primaryTitle}
        movie={movie}
        primaryRating={{ value: movie.imdbRating, variant: "imdb" }}
      />
    ))}
  </MovieListView>
);

export default SearchMovieList;
