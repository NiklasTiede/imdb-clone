import { expect, it } from "vitest";
import type { ChatTurn, GroundedMovie } from "./concierge";
import {
  chronologicalHistory,
  confirmHistoryNavigation,
  HISTORY_LIMIT,
  updateHistory,
  updateRetrievedMovies,
  updateToolActivity,
} from "./conversationHistory";

const turn = (id: string, timestamp: number): ChatTurn => ({
  id,
  timestamp,
  role: "user",
  text: id,
  movies: [],
});

it("merges text and voice chronologically and limits retained entries", () => {
  const items = Array.from({ length: HISTORY_LIMIT + 5 }, (_, i) =>
    turn(String(i), i),
  );
  const result = chronologicalHistory(
    items.filter((_, i) => i % 2 === 0),
    items.filter((_, i) => i % 2 !== 0),
  );
  expect(result).toHaveLength(HISTORY_LIMIT);
  expect(result[0]?.timestamp).toBe(5);
  expect(result.at(-1)?.timestamp).toBe(HISTORY_LIMIT + 4);
  const updated = updateHistory(result, {
    ...result[0]!,
    text: "Final transcription",
  });
  expect(updated).toHaveLength(HISTORY_LIMIT);
  expect(updated[0]?.text).toBe("Final transcription");
});

it("confirms only requested navigation whose destination was actually observed", () => {
  const items: ChatTurn[] = [
    {
      ...turn("1", 1),
      actions: [
        {
          action: { type: "open_movie", movieId: 42 },
          outcome: "requested",
          destination: "/movie?id=42",
          timestamp: 1,
        },
        {
          action: { type: "open_movie_trailer", movieId: 42 },
          outcome: "rejected",
          destination: "/movie?id=42#trailer",
          timestamp: 1,
        },
      ],
    },
  ];
  expect(confirmHistoryNavigation(items, "/")).toBe(items);
  expect(confirmHistoryNavigation(items, "/movie?id=42#trailer")).toBe(items);
  const confirmed = confirmHistoryNavigation(items, "/movie?id=42");
  expect(confirmed[0]?.actions?.map((action) => action.outcome)).toEqual([
    "opened",
    "rejected",
  ]);
  expect(confirmHistoryNavigation(confirmed, "/movie?id=42")).toBe(confirmed);
});

it("preserves the highest ratings and retrieval order across catalog lookups", () => {
  const movies = Array.from({ length: 8 }, (_, i) => ({
    movieId: i + 1,
    primaryTitle: `Movie ${i}`,
    movieType: "MOVIE",
    genres: [],
    userScore: 10 - i,
    imdbRating: 7,
  }));
  const retrieved = movies.reduce<GroundedMovie[]>(updateRetrievedMovies, []);
  const details = {
    ...movies[0]!,
    userScore: null,
    description: "More details",
  };
  const updated = updateRetrievedMovies(retrieved, details);
  expect(updated.map((movie) => movie.movieId)).toEqual([
    1, 2, 3, 4, 5, 6, 7, 8,
  ]);
  expect(updated[0]).toMatchObject({
    userScore: 10,
    imdbRating: 7,
    description: "More details",
  });
});

it("updates individual tool calls without collapsing repeated lookups", () => {
  const tools = updateToolActivity([], {
    callId: "one",
    tool: "search_movies",
    status: "started",
  });
  const repeated = updateToolActivity(tools, {
    callId: "two",
    tool: "search_movies",
    status: "started",
  });
  expect(
    updateToolActivity(repeated, {
      callId: "one",
      tool: "search_movies",
      status: "completed",
    }),
  ).toEqual([
    { callId: "one", tool: "search_movies", status: "completed" },
    { callId: "two", tool: "search_movies", status: "started" },
  ]);
});
