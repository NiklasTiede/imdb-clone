import type { ChatTurn, GroundedMovie, ToolActivity } from "./concierge";

export function updateToolActivity(
  tools: ToolActivity[] = [],
  activity: ToolActivity,
): ToolActivity[] {
  return (
    tools.some((tool) => tool.callId === activity.callId)
      ? tools.map((tool) => (tool.callId === activity.callId ? activity : tool))
      : [...tools, activity]
  ).slice(-24);
}

export function updateRetrievedMovies(
  movies: GroundedMovie[],
  movie: GroundedMovie,
): GroundedMovie[] {
  const existing = movies.find((item) => item.movieId === movie.movieId);
  const updated = {
    ...movie,
    userScore: movie.userScore ?? existing?.userScore,
  };
  // Preserve retrieval order and personal scores across later catalog-detail lookups.
  return (
    existing
      ? movies.map((item) => (item.movieId === movie.movieId ? updated : item))
      : [...movies, updated]
  ).slice(0, 50);
}

export const HISTORY_LIMIT = 200;

export function updateHistory(turns: ChatTurn[], entry: ChatTurn): ChatTurn[] {
  const index = turns.findIndex((turn) => turn.id === entry.id);
  return (
    index < 0
      ? [...turns, entry]
      : turns.map((turn) => (turn.id === entry.id ? entry : turn))
  ).slice(-HISTORY_LIMIT);
}

export function chronologicalHistory(
  text: ChatTurn[],
  voice: ChatTurn[],
): ChatTurn[] {
  return [...text, ...voice]
    .sort((a, b) => (a.timestamp ?? 0) - (b.timestamp ?? 0))
    .slice(-HISTORY_LIMIT);
}

export function confirmHistoryNavigation(
  turns: ChatTurn[],
  destination: string,
): ChatTurn[] {
  let changed = false;
  const next = turns.map((turn) => {
    if (
      !turn.actions?.some(
        (action) =>
          action.outcome === "requested" && action.destination === destination,
      )
    )
      return turn;
    changed = true;
    return {
      ...turn,
      actions: turn.actions.map((action) =>
        action.outcome === "requested" && action.destination === destination
          ? { ...action, outcome: "opened" as const }
          : action,
      ),
    };
  });
  return changed ? next : turns;
}
