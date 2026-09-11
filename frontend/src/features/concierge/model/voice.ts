import { z } from "zod";
import {
  applicationActionSchema,
  groundedMovieSchema,
  toolActivitySchema,
} from "./concierge";

export const voiceEventSchema = z
  .object({
    type: z.enum([
      "ready",
      "status",
      "transcript",
      "movie-card",
      "tool-activity",
      "ui-action",
      "interrupt",
      "reply-complete",
      "error",
      "standby",
    ]),
    status: z.enum(["listening", "thinking", "searching", "muted"]).optional(),
    speaker: z.enum(["user", "assistant"]).optional(),
    text: z.string().max(6000).optional(),
    final: z.boolean().default(false),
    turn: z.number().int().nonnegative().default(0),
    movie: groundedMovieSchema.optional(),
    action: applicationActionSchema.optional(),
    activity: toolActivitySchema.optional(),
  })
  .strict();

export type VoiceStatus =
  | "idle"
  | "standby"
  | "connecting"
  | "listening"
  | "thinking"
  | "searching"
  | "error";
export const voiceLabels: Record<VoiceStatus, string> = {
  idle: "Let's find your next movie",
  standby: "Voice paused after inactivity. Click to start again.",
  connecting: "Connecting…",
  listening: "Listening to you",
  thinking: "Thinking…",
  searching: "Searching movies…",
  error: "Voice couldn't continue",
};
