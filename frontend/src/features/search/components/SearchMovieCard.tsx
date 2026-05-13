import { MovieRecord } from "../../../client/movies/generator-output";
import { PosterMovieCard } from "../../catalog";

type SearchMovieCardProps = {
  movie: MovieRecord;
  onToggleBookmark?: (movieId: number) => void;
};

const SearchMovieCard = ({ movie, onToggleBookmark }: SearchMovieCardProps) => {
  return <PosterMovieCard movie={movie} onToggleBookmark={onToggleBookmark} />;
};

export default SearchMovieCard;
