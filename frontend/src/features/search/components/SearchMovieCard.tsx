import { PosterMovieCard, type Movie } from "../../catalog";

type SearchMovieCardProps = {
  movie: Movie;
  onToggleBookmark?: (movieId: number) => void;
};

const SearchMovieCard = ({ movie, onToggleBookmark }: SearchMovieCardProps) => {
  return <PosterMovieCard movie={movie} onToggleBookmark={onToggleBookmark} />;
};

export default SearchMovieCard;
