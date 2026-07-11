import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { MemoryRouter, Route, Routes } from "react-router";
import { beforeEach, describe, expect, test, vi } from "vitest";
import {
  MovieRecordMovieGenreEnum,
  MovieRecordMovieTypeEnum,
} from "../../../client/movies/generator-output";
import type { Movie } from "../model/movie";
import MovieDetailPage, { parseMovieId } from "./MovieDetailPage";

const mocks = vi.hoisted(() => ({
  authenticated: false,
  accountEngagementApi: {
    getRatingsByAccount: vi.fn(),
    getWatchedMoviesByAccount: vi.fn(),
  },
  moviesApi: {
    getMovieById: vi.fn(),
  },
  ratingApi: {
    deleteRating: vi.fn(),
    rateMovie: vi.fn(),
  },
  shareMovie: vi.fn(),
  watchlistApi: {
    deleteWatchedMovie: vi.fn(),
    watchMovie: vi.fn(),
  },
}));

vi.mock("../../../shared/auth", () => ({
  authSession: {
    getUsername: () => (mocks.authenticated ? "niklas" : null),
  },
  useAuthSession: () => mocks.authenticated,
}));

vi.mock("../../../shared/api/moviesApi", () => ({
  accountEngagementApi: mocks.accountEngagementApi,
  moviesApi: mocks.moviesApi,
  ratingApi: mocks.ratingApi,
  watchlistApi: mocks.watchlistApi,
}));

vi.mock("../utils/shareMovie", () => ({
  shareMovie: mocks.shareMovie,
}));

vi.mock("../../engagement/comment", () => ({
  MovieCommentsSection: ({ movieId }: { movieId: number }) => (
    <section data-testid="movie-comments">Comments for movie {movieId}</section>
  ),
}));

const movie: Movie = {
  id: 1,
  imdbId: "tt0111161",
  tmdbId: 278,
  movieType: MovieRecordMovieTypeEnum.Movie,
  primaryTitle: "The Shawshank Redemption",
  originalTitle: "The Shawshank Redemption",
  startYear: 1994,
  runtimeMinutes: 142,
  movieGenre: new Set([MovieRecordMovieGenreEnum.Drama]),
  imdbRating: 9.3,
  imdbRatingCount: 3189003,
  description: "Two imprisoned men bond over a number of years.",
  posterImageToken: "poster-token",
  backdropImageToken: "backdrop-token",
  rating: 8.8,
  ratingCount: 1240,
};

const makeQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      mutations: { retry: false },
      queries: { retry: false, staleTime: Infinity },
    },
  });

const renderPage = ({
  initialEntry = "/movie?id=1",
  queryClient = makeQueryClient(),
}: {
  initialEntry?: string;
  queryClient?: QueryClient;
} = {}) => {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/movie" element={<MovieDetailPage />} />
        <Route path="/login" element={<div>Login destination</div>} />
        <Route path="/" element={<div>Movie browser</div>} />
      </Routes>
    </MemoryRouter>,
    { wrapper },
  );
};

const seedMovie = (queryClient: QueryClient) => {
  queryClient.setQueryData(["catalog", "movie", 1], movie);
};

const seedAuthenticatedEngagement = (queryClient: QueryClient) => {
  queryClient.setQueryData(
    ["watchlist", "current-user", "niklas", "movie-ids"],
    new Set<number>(),
  );
  queryClient.setQueryData(["rating", "current-user", "niklas", "movie", 1], 6);
};

describe("MovieDetailPage", () => {
  beforeEach(() => {
    mocks.authenticated = false;
    vi.clearAllMocks();
    mocks.moviesApi.getMovieById.mockResolvedValue({ data: movie });
    mocks.accountEngagementApi.getRatingsByAccount.mockResolvedValue({
      data: { content: [{ movieId: 1, rating: 9 }], last: true },
    });
    mocks.accountEngagementApi.getWatchedMoviesByAccount.mockResolvedValue({
      data: { content: [], last: true },
    });
    mocks.ratingApi.rateMovie.mockResolvedValue({});
    mocks.ratingApi.deleteRating.mockResolvedValue({});
    mocks.shareMovie.mockResolvedValue("copied");
  });

  test("parses only positive integer movie identifiers", () => {
    expect(parseMovieId("42")).toBe(42);
    expect(parseMovieId("42abc")).toBeNull();
    expect(parseMovieId("0")).toBeNull();
    expect(parseMovieId("-1")).toBeNull();
    expect(parseMovieId(null)).toBeNull();
  });

  test("renders a distinct invalid-link state", () => {
    renderPage({ initialEntry: "/movie?id=not-a-number" });

    expect(
      screen.getByRole("heading", { name: "Choose a movie" }),
    ).toBeTruthy();
    expect(screen.getByRole("link", { name: "Browse movies" })).toBeTruthy();
  });

  test("renders a stable loading state", () => {
    mocks.moviesApi.getMovieById.mockReturnValue(new Promise(() => {}));
    renderPage();

    expect(
      screen.getByRole("status", { name: "Loading movie details" }),
    ).toBeTruthy();
  });

  test("renders a retryable request-failure state", async () => {
    mocks.moviesApi.getMovieById.mockRejectedValue(new Error("Unavailable"));
    renderPage();

    expect(
      await screen.findByRole("heading", { name: "Movie unavailable" }),
    ).toBeTruthy();
    expect(screen.getByRole("button", { name: "Try again" })).toBeTruthy();
  });

  test("renders the wide movie content from cached detail data", () => {
    const queryClient = makeQueryClient();
    seedMovie(queryClient);

    renderPage({ queryClient });

    expect(
      screen.getByRole("heading", { name: "The Shawshank Redemption" }),
    ).toBeTruthy();
    expect(screen.getByTestId("movie-backdrop").dataset.hasImage).toBe("true");
    expect(screen.getByRole("heading", { name: "Synopsis" })).toBeTruthy();
    expect(screen.getByTestId("movie-comments").textContent).toContain(
      "Comments for movie 1",
    );
    expect(
      screen.getByText("Two imprisoned men bond over a number of years."),
    ).toBeTruthy();
  });

  test("guides an anonymous rating attempt to sign in", async () => {
    const user = userEvent.setup();
    const queryClient = makeQueryClient();
    seedMovie(queryClient);
    renderPage({ queryClient });

    await user.click(screen.getByRole("button", { name: "Rate movie" }));

    expect(screen.getByText("Sign in to rate this movie.")).toBeTruthy();
    await user.click(screen.getByRole("button", { name: "Sign in" }));
    expect(screen.getByText("Login destination")).toBeTruthy();
  });

  test("saves a rating through the focused dialog", async () => {
    const user = userEvent.setup();
    mocks.authenticated = true;
    const queryClient = makeQueryClient();
    seedMovie(queryClient);
    seedAuthenticatedEngagement(queryClient);
    renderPage({ queryClient });

    await user.click(screen.getByRole("button", { name: "Your rating: 6" }));
    await user.click(screen.getByRole("radio", { name: "9 out of 10" }));
    await user.click(screen.getByRole("button", { name: "Save rating" }));

    await waitFor(() =>
      expect(mocks.ratingApi.rateMovie).toHaveBeenCalledWith(1, 9),
    );
    expect(await screen.findByText("Rating saved.")).toBeTruthy();
  });

  test("adds an authenticated movie to the watchlist", async () => {
    const user = userEvent.setup();
    mocks.authenticated = true;
    const queryClient = makeQueryClient();
    seedMovie(queryClient);
    seedAuthenticatedEngagement(queryClient);
    mocks.watchlistApi.watchMovie.mockResolvedValue({});
    renderPage({ queryClient });

    await user.click(screen.getByRole("button", { name: "Add to watchlist" }));

    await waitFor(() =>
      expect(mocks.watchlistApi.watchMovie).toHaveBeenCalledWith(1),
    );
  });

  test("reports copied share links", async () => {
    const user = userEvent.setup();
    const queryClient = makeQueryClient();
    seedMovie(queryClient);
    renderPage({ queryClient });

    await user.click(screen.getByRole("button", { name: "Share movie" }));

    expect(mocks.shareMovie).toHaveBeenCalledWith({
      movieId: 1,
      title: "The Shawshank Redemption",
    });
    expect(await screen.findByText("Movie link copied.")).toBeTruthy();
  });
});
