import { MovieListRow, MovieListView, type Movie } from "../../../catalog";
import type { WatchlistItem } from "../model/watchlist";
import { formatRelativeDate } from "../utils/watchlistFormat";

type WatchlistListProps = {
  items: WatchlistItem[];
  onRemove: (movieId: number) => void;
};

const WatchlistList = ({ items, onRemove }: WatchlistListProps) => (
  <MovieListView
    ariaLabel="Watchlist movies"
    columns={{
      genre: "Genre",
      primaryRating: "IMDb",
      runtime: "Runtime",
      timestamp: "Added",
    }}
  >
    {items.map((item) => {
      const movie: Movie = item.movie ?? {
        id: item.movieId,
        primaryTitle: "Unknown title",
      };
      const movieId = item.movieId ?? movie?.id;

      return (
        <MovieListRow
          action={
            movieId !== undefined
              ? {
                  ariaLabel: "Remove from watchlist",
                  color: "danger",
                  icon: "delete",
                  onClick: () => onRemove(movieId),
                }
              : undefined
          }
          key={movieId ?? movie.primaryTitle}
          movie={movie}
          primaryRating={{ value: movie.imdbRating, variant: "imdb" }}
          timestamp={formatRelativeDate(item.addedAt)}
        />
      );
    })}
  </MovieListView>
);

export default WatchlistList;
