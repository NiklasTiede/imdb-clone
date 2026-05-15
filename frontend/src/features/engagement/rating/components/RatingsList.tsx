import { MovieListRow, MovieListView } from "../../../catalog";
import type { RatedMovie } from "../api/ratingQueries";

type RatingsListProps = {
  items: RatedMovie[];
  onRemove?: (movieId: number) => void;
};

const RatingsList = ({ items, onRemove }: RatingsListProps) => (
  <MovieListView
    ariaLabel="Rated movies"
    columns={{
      genre: "Genre",
      primaryRating: "Your score",
      runtime: "Runtime",
      secondaryRating: "IMDb",
    }}
    hasRowActions={Boolean(onRemove)}
  >
    {items.map((item) => {
      const movie = item.movie;
      const movieId = movie.id;

      return (
        <MovieListRow
          action={
            movieId !== undefined && onRemove
              ? {
                  ariaLabel: "Delete rating",
                  color: "danger",
                  icon: "delete",
                  onClick: () => onRemove(movieId),
                }
              : undefined
          }
          key={movieId ?? movie.primaryTitle}
          movie={movie}
          primaryRating={{ value: item.rating, variant: "user" }}
          secondaryRating={{ value: movie.imdbRating, variant: "imdb" }}
        />
      );
    })}
  </MovieListView>
);

export default RatingsList;
