import Box from "@mui/material/Box";
import { useQuery } from "@tanstack/react-query";
import { MovieCarousel } from "../../home";
import { recommendationQueries } from "../api/recommendationQueries";

type SimilarMoviesCarouselProps = {
  movieId: number;
};

const SimilarMoviesCarousel = ({ movieId }: SimilarMoviesCarouselProps) => {
  const query = useQuery(recommendationQueries.similarMovies(movieId));

  if (query.isError || (!query.isPending && (query.data?.length ?? 0) === 0)) {
    return null;
  }

  return (
    <Box
      component="section"
      aria-label="Similar movies"
      sx={{ borderTop: "1px solid", borderColor: "divider", pt: { xs: 3, md: 4 } }}
    >
      <MovieCarousel
        getMovieCaption={(movie) =>
          query.data?.find((item) => item.movie.id === movie.id)?.explanation
        }
        loading={query.isPending}
        movies={(query.data ?? []).map((item) => item.movie)}
        subtitle="More movies chosen for their shared themes, era, and genre"
        title="Similar movies"
      />
    </Box>
  );
};

export default SimilarMoviesCarousel;
