import * as zod from "zod";
import {
  MovieSearchRequestMovieGenreEnum,
  MovieSearchRequestMovieTypeEnum,
} from "../../../client/movies/generator-output";

const optionalNullableString = zod.string().nullable().optional();

export const groundedMovieSchema = zod.object({
  movieId: zod.number().int().positive(),
  primaryTitle: zod.string().min(1),
  originalTitle: optionalNullableString,
  movieType: zod.string().min(1),
  startYear: zod.number().int().nullable().optional(),
  runtimeMinutes: zod.number().int().nonnegative().nullable().optional(),
  genres: zod.array(zod.string()),
  imdbRating: zod.number().min(0).max(10).nullable().optional(),
  userScore: zod.number().min(0).max(10).nullable().optional(),
  imdbRatingCount: zod.number().int().nonnegative().nullable().optional(),
  description: optionalNullableString,
  posterImageToken: optionalNullableString,
  explanation: optionalNullableString,
});

const statusEventSchema = zod.object({
  type: zod.literal("status"),
  sequence: zod.number().int().nonnegative(),
  status: zod.enum([
    "thinking",
    "searching_catalog",
    "fetching_details",
    "finding_similar",
    "choosing_tonight",
  ]),
});

const textEventSchema = zod.object({
  type: zod.literal("text"),
  sequence: zod.number().int().nonnegative(),
  delta: zod.string().min(1),
});

const movieCardEventSchema = zod.object({
  type: zod.literal("movie-card"),
  sequence: zod.number().int().nonnegative(),
  movie: groundedMovieSchema,
});

export const toolLabels = {
  search_movies: "Search movies",
  get_movie_details: "Read movie details",
  get_movie_enrichment: "Look up additional movie facts",
  get_movie_watch_providers: "Check streaming availability",
  get_similar_movies: "Find similar movies",
  get_tonight_picks: "Find tonight's picks",
  get_my_ratings: "Read your ratings",
  get_my_recommendations: "Find personal recommendations",
  get_my_watchlist: "Read your watchlist",
  add_movie_to_my_watchlist: "Add to your watchlist",
  remove_movie_from_my_watchlist: "Remove from your watchlist",
  set_my_movie_rating: "Save your rating",
  remove_my_movie_rating: "Remove your rating",
} as const;

export const toolActivitySchema = zod
  .object({
    callId: zod.string().min(1).max(200),
    tool: zod.enum(
      Object.keys(toolLabels) as [
        keyof typeof toolLabels,
        ...Array<keyof typeof toolLabels>,
      ],
    ),
    status: zod.enum(["started", "completed", "failed"]),
  })
  .strict();

const toolActivityEventSchema = zod.object({
  type: zod.literal("tool-activity"),
  sequence: zod.number().int().nonnegative(),
  activity: toolActivitySchema,
});

export type ToolActivity = zod.infer<typeof toolActivitySchema>;

const openMovieActionSchema = zod
  .object({
    type: zod.literal("open_movie"),
    movieId: zod.number().int().positive(),
  })
  .strict();

export const applicationActionSchema = zod.discriminatedUnion("type", [
  zod
    .object({
      type: zod.literal("open_page"),
      destination: zod.enum(["home", "settings", "watchlist", "ratings"]),
    })
    .strict(),
  zod
    .object({
      type: zod.literal("show_search_results"),
      query: zod.string().max(200),
      genres: zod
        .array(zod.enum(MovieSearchRequestMovieGenreEnum))
        .max(30)
        .default([]),
      movieType: zod.enum(MovieSearchRequestMovieTypeEnum).nullish(),
      minStartYear: zod.number().int().min(1850).max(2030).nullish(),
      maxStartYear: zod.number().int().min(1850).max(2030).nullish(),
      minRuntimeMinutes: zod.number().int().min(0).max(5000).nullish(),
      maxRuntimeMinutes: zod.number().int().min(0).max(5000).nullish(),
    })
    .strict(),
  openMovieActionSchema,
  zod
    .object({
      type: zod.literal("open_movie_trailer"),
      movieId: zod.number().int().positive(),
    })
    .strict(),
  zod
    .object({
      type: zod.literal("open_watchlist"),
      operationId: zod.string().uuid().nullable().optional(),
      movieId: zod.number().int().positive().nullable().optional(),
      created: zod.boolean().nullable().optional(),
      removed: zod.boolean().nullable().optional(),
    })
    .strict(),
  zod
    .object({
      type: zod.literal("open_ratings"),
      operationId: zod.string().uuid(),
      movieId: zod.number().int().positive(),
      changed: zod.boolean(),
      score: zod.number().min(0).max(10).nullable().default(null),
      previousScore: zod.number().min(0).max(10).nullable().default(null),
    })
    .strict(),
  zod.object({ type: zod.literal("open_login") }).strict(),
]);
export type ApplicationAction = zod.infer<typeof applicationActionSchema>;

const uiActionEventSchema = zod
  .object({
    type: zod.literal("ui-action"),
    sequence: zod.number().int().nonnegative(),
    action: applicationActionSchema,
  })
  .strict();

const errorEventSchema = zod.object({
  type: zod.literal("error"),
  sequence: zod.number().int().nonnegative(),
  code: zod.string().min(1),
  message: zod.string().min(1),
  retryable: zod.boolean(),
});

const usageEventSchema = zod.object({
  type: zod.literal("usage"),
  sequence: zod.number().int().nonnegative(),
  usage: zod.object({
    model: zod.string().min(1),
    requests: zod.number().int().nonnegative(),
    toolCalls: zod.number().int().nonnegative(),
    inputTokens: zod.number().int().nonnegative(),
    cacheReadTokens: zod.number().int().nonnegative().default(0),
    cacheWriteTokens: zod.number().int().nonnegative().default(0),
    outputTokens: zod.number().int().nonnegative(),
    totalTokens: zod.number().int().nonnegative(),
    estimatedCostUsd: zod.string().regex(/^\d+(?:\.\d+)?$/),
    costAvailable: zod.boolean(),
    costBasis: zod.string().nullable().optional(),
  }),
});

const completionEventSchema = zod.object({
  type: zod.literal("completion"),
  sequence: zod.number().int().nonnegative(),
  conversationId: zod.string().min(1),
  outcome: zod.enum(["success", "error", "cancelled"]),
});

export const conciergeEventSchema = zod.discriminatedUnion("type", [
  statusEventSchema,
  textEventSchema,
  movieCardEventSchema,
  toolActivityEventSchema,
  uiActionEventSchema,
  errorEventSchema,
  usageEventSchema,
  completionEventSchema,
]);

export type ConciergeEvent = zod.infer<typeof conciergeEventSchema>;
export type GroundedMovie = zod.infer<typeof groundedMovieSchema>;
export type OpenMovieAction = zod.infer<typeof openMovieActionSchema>;
export type UsageSummary = zod.infer<typeof usageEventSchema>["usage"];

export type ChatTurn = {
  id: string;
  role: "user" | "assistant";
  text: string;
  movies: GroundedMovie[];
  tools?: ToolActivity[];
  error?: { message: string; retryable: boolean };
  channel?: "voice" | "text";
  timestamp?: number;
  final?: boolean;
  interrupted?: boolean;
  context?: { page: string; streamingCountry?: string };
  actions?: {
    action: ApplicationAction;
    outcome: "requested" | "rejected" | "opened";
    timestamp: number;
    destination?: string;
  }[];
};

export const statusLabels: Record<
  zod.infer<typeof statusEventSchema>["status"],
  string
> = {
  thinking: "Thinking",
  searching_catalog: "Searching the catalog",
  fetching_details: "Loading movie details",
  finding_similar: "Finding similar movies",
  choosing_tonight: "Choosing tonight's lineup",
};
