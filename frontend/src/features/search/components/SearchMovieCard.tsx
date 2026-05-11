import { MovieRecord } from "../../../client/movies/generator-output";
import MovieCard from "../../../components/common/MovieCard";

type SearchMovieCardProps = {
  movie: MovieRecord;
  onToggleBookmark?: (movieId: number) => void;
};

const SearchMovieCard = ({ movie, onToggleBookmark }: SearchMovieCardProps) => {
  return <MovieCard movie={movie} onToggleBookmark={onToggleBookmark} />;
};

export default SearchMovieCard;
