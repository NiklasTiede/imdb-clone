import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Container from "@mui/material/Container";
import Snackbar from "@mui/material/Snackbar";
import Stack from "@mui/material/Stack";
import { Fragment, useCallback, useState } from "react";
import { useNavigate } from "react-router";
import { authSession, useAuthSession } from "../../../shared/auth";
import {
  toggleWatchlistMutationOptions,
  watchlistQueries,
} from "../../engagement";
import { useHomeFeed } from "../api/homeFeedQueries";
import HomeFeedEndState from "../components/HomeFeedEndState";
import HomeFeedSentinel from "../components/HomeFeedSentinel";
import FeaturedMovieHero from "../components/FeaturedMovieHero";
import MovieCarousel from "../components/MovieCarousel";
import TonightModePanel from "../components/TonightModePanel";
import { useHomeFeedRestoration } from "../hooks/useHomeFeedRestoration";
import type { HomeFeedMovie, HomeFeedSection } from "../model/homeFeed";
import {
  reportHomeMovieOpen,
  reportHomeSectionImpression,
  reportHomeWatchlistAdded,
} from "../observability/discoveryEvents";
import {
  getCarouselScrollPosition,
  getHomeFeedInstanceId,
  setCarouselScrollPosition,
  startNewHomeFeedSession,
} from "../model/homeFeedSession";

const emptyMovieIds = new Set<number>();

const HomePage = () => {
  const navigate = useNavigate();
  const isAuthenticated = useAuthSession();
  const username = isAuthenticated ? authSession.getUsername() : null;
  const queryClient = useQueryClient();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [feedInstanceId, setFeedInstanceId] = useState(getHomeFeedInstanceId);

  const homeFeed = useHomeFeed(feedInstanceId);
  const pages = homeFeed.data?.pages ?? [];
  const featuredMovie = pages[0]?.featuredMovie ?? null;
  const strategyVersion = pages[0]?.strategyVersion ?? "home-structured-v1";
  const sections: HomeFeedSection[] = pages.flatMap((page) => page.sections ?? []);
  useHomeFeedRestoration(feedInstanceId, sections.length);

  const { data: watchedMovieIds } = useQuery(
    watchlistQueries.movieIds({ username }),
  );

  const toggleWatchlist = useMutation({
    ...toggleWatchlistMutationOptions(queryClient, {
      onAdded: (movieId) =>
        reportHomeWatchlistAdded({
          feedInstanceId,
          movieId,
          sectionId: "featured",
          strategyVersion,
        }),
    }),
    onError: () =>
      setErrorMessage("Could not update your watchlist. Please try again."),
  });

  const isFeaturedBookmarked = Boolean(
    featuredMovie?.id !== undefined && watchedMovieIds?.has(featuredMovie.id),
  );

  const handleViewMovie = () => {
    if (featuredMovie?.id !== undefined) {
      void navigate(`/movie?id=${featuredMovie.id}`);
    }
  };

  const handleToggleFeaturedBookmark = () => {
    if (!username || featuredMovie?.id === undefined) {
      setErrorMessage("Please log in to manage your watchlist.");
      return;
    }

    toggleWatchlist.mutate({
      isBookmarked: isFeaturedBookmarked,
      movieId: featuredMovie.id,
    });
  };

  const fetchNextPage = useCallback(() => {
    if (homeFeed.hasNextPage && !homeFeed.isFetchingNextPage) {
      void homeFeed.fetchNextPage();
    }
  }, [homeFeed]);

  const discoverNewMix = () => {
    const nextFeedInstanceId = startNewHomeFeedSession();
    setFeedInstanceId(nextFeedInstanceId);
    window.scrollTo({ behavior: "auto", top: 0 });
  };

  const isInitialLoading = homeFeed.isPending && pages.length === 0;
  const hasInitialError = homeFeed.isError && pages.length === 0;
  const isExhausted = !homeFeed.hasNextPage && pages.length > 0;
  const tonightModeAfterSectionIndex = Math.min(1, sections.length - 1);

  return (
    <Box sx={{ backgroundColor: "background.default", minHeight: "100vh" }}>
      <Container
        maxWidth="xl"
        sx={{ px: { xs: 0, md: 4 }, py: { xs: 3, md: 4 } }}
      >
        <Box sx={{ px: { xs: 2, md: 0 } }}>
          <FeaturedMovieHero
            error={hasInitialError}
            movie={featuredMovie}
            loading={isInitialLoading}
            isBookmarked={isFeaturedBookmarked}
            isBookmarkLoading={toggleWatchlist.isPending}
            onToggleBookmark={handleToggleFeaturedBookmark}
            onViewMovie={handleViewMovie}
          />
        </Box>
        {isInitialLoading &&
          ["Discovering something great", "A fresh place to start", "More to explore"].map(
            (title) => <MovieCarousel key={title} title={title} movies={[]} loading />,
          )}

        {sections.map((section, index) => {
          const sectionId = section.id ?? section.title ?? "home-section";
          const movies: HomeFeedMovie[] = (section.items ?? [])
            .map((item) => item.movie)
            .filter((movie): movie is HomeFeedMovie => movie !== undefined);
          return (
            <Fragment key={sectionId}>
              <MovieCarousel
                initialScrollLeft={getCarouselScrollPosition(sectionId)}
                movies={movies}
                onScrollPositionChange={(position) =>
                  setCarouselScrollPosition(sectionId, position)
                }
                onMovieOpen={(movieId, position) =>
                  reportHomeMovieOpen({
                    feedInstanceId,
                    movieId,
                    position,
                    sectionId,
                    strategyVersion,
                  })
                }
                onImpression={() =>
                  reportHomeSectionImpression({
                    feedInstanceId,
                    sectionId,
                    strategyVersion,
                  })
                }
                title={section.title ?? "Movie picks"}
                {...(section.subtitle ? { subtitle: section.subtitle } : {})}
              />
              {index === tonightModeAfterSectionIndex && (
                <Box sx={{ px: { xs: 2, md: 0 } }}>
                  <TonightModePanel
                    watchedMovieIds={watchedMovieIds ?? emptyMovieIds}
                  />
                </Box>
              )}
            </Fragment>
          );
        })}

        {homeFeed.isFetchNextPageError && (
          <Box sx={{ px: { xs: 2, md: 0 }, pb: 4 }}>
            <Alert
              action={
                <Button color="inherit" onClick={fetchNextPage} size="small">
                  Retry
                </Button>
              }
              severity="warning"
            >
              We couldn&apos;t load more recommendations. Your current picks are still here.
            </Alert>
          </Box>
        )}

        {homeFeed.hasNextPage && (
          <Stack
            spacing={1.5}
            sx={{ alignItems: "center", pb: 5, pt: 1 }}
          >
            <HomeFeedSentinel
              disabled={homeFeed.isFetchingNextPage}
              onVisible={fetchNextPage}
            />
            {homeFeed.isFetchingNextPage ? (
              <CircularProgress aria-label="Loading more recommendations" size={24} />
            ) : (
              <Button onClick={fetchNextPage} variant="text" sx={{ textTransform: "none" }}>
                Load more recommendations
              </Button>
            )}
          </Stack>
        )}

        {isExhausted && <HomeFeedEndState onDiscoverNewMix={discoverNewMix} />}
      </Container>

      <Snackbar
        open={errorMessage !== null}
        autoHideDuration={4000}
        onClose={() => setErrorMessage(null)}
        message={errorMessage ?? ""}
      />
    </Box>
  );
};

export default HomePage;
