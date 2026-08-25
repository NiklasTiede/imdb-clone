import * as zod from "zod";

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

const openMovieActionSchema = zod
  .object({
    type: zod.literal("open_movie"),
    movieId: zod.number().int().positive(),
  })
  .strict();

const uiActionEventSchema = zod
  .object({
    type: zod.literal("ui-action"),
    sequence: zod.number().int().nonnegative(),
    action: openMovieActionSchema,
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
  error?: { message: string; retryable: boolean };
};

export const statusLabels: Record<
  zod.infer<typeof statusEventSchema>["status"],
  string
> = {
  thinking: "Thinking",
  searching_catalog: "Searching the catalog",
  fetching_details: "Opening movie details",
  finding_similar: "Finding similar movies",
  choosing_tonight: "Choosing tonight's lineup",
};
