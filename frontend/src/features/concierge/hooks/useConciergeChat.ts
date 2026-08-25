import { useCallback, useEffect, useRef, useState } from "react";
import { createPerformanceEventContext } from "../../../shared/observability/config";
import { reportPerformanceEvent } from "../../../shared/observability/performanceReporter";
import {
  ConciergeClientError,
  createConversation,
  streamMessage,
} from "../api/conciergeClient";
import type {
  ChatTurn,
  ConciergeEvent,
  OpenMovieAction,
  UsageSummary,
} from "../model/concierge";
import { statusLabels } from "../model/concierge";

const createTurnId = (): string => window.crypto.randomUUID();

export const useConciergeChat = (
  clientId: string,
  onUiAction: (action: OpenMovieAction) => void,
) => {
  const [turns, setTurns] = useState<ChatTurn[]>([]);
  const [status, setStatus] = useState<string | null>(null);
  const [usage, setUsage] = useState<UsageSummary | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const conversationIdRef = useRef<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(
    () => () => {
      abortRef.current?.abort();
    },
    [],
  );

  const reset = useCallback(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    conversationIdRef.current = null;
    setTurns([]);
    setStatus(null);
    setUsage(null);
    setIsStreaming(false);
  }, []);

  const send = useCallback(
    async (rawMessage: string) => {
      const message = rawMessage.trim();
      if (!message || message.length > 600 || abortRef.current) {
        return;
      }

      const assistantTurnId = createTurnId();
      setTurns((current) => [
        ...current,
        { id: createTurnId(), role: "user", text: message, movies: [] },
        { id: assistantTurnId, role: "assistant", text: "", movies: [] },
      ]);
      setStatus("Thinking");
      setUsage(null);
      setIsStreaming(true);

      const abortController = new AbortController();
      const groundedMovieIds = new Set<number>();
      let actionHandled = false;
      abortRef.current = abortController;
      try {
        const conversationId =
          conversationIdRef.current ??
          (await createConversation(clientId, abortController.signal));
        conversationIdRef.current = conversationId;

        await streamMessage({
          clientId,
          conversationId,
          message,
          signal: abortController.signal,
          onEvent: (event) => {
            if (event.type === "movie-card") {
              groundedMovieIds.add(event.movie.movieId);
            } else if (event.type === "ui-action") {
              const allowed =
                !actionHandled && groundedMovieIds.has(event.action.movieId);
              actionHandled = true;
              if (!allowed) {
                reportUiAction("rejected");
                return;
              }
              try {
                onUiAction(event.action);
                reportUiAction("executed");
              } catch {
                reportUiAction("rejected");
              }
              return;
            }
            applyEvent({
              event,
              assistantTurnId,
              setTurns,
              setStatus,
              setUsage,
            });
          },
        });
      } catch (error) {
        if (!(error instanceof DOMException && error.name === "AbortError")) {
          const message =
            error instanceof ConciergeClientError
              ? error.message
              : "The Movie Concierge is temporarily unavailable.";
          setTurns((current) =>
            updateAssistantTurn(current, assistantTurnId, (turn) => ({
              ...turn,
              error: { message, retryable: true },
            })),
          );
        }
      } finally {
        if (abortRef.current === abortController) {
          abortRef.current = null;
          setIsStreaming(false);
          setStatus(null);
        }
      }
    },
    [clientId, onUiAction],
  );

  return { isStreaming, reset, send, status, turns, usage };
};

const reportUiAction = (outcome: "executed" | "rejected"): void => {
  reportPerformanceEvent({
    context: createPerformanceEventContext(window.location.pathname),
    name: "open_movie",
    outcome,
    timestamp: performance.now(),
    type: "concierge_ui_action",
  });
};

type ApplyEventArguments = {
  event: ConciergeEvent;
  assistantTurnId: string;
  setTurns: React.Dispatch<React.SetStateAction<ChatTurn[]>>;
  setStatus: React.Dispatch<React.SetStateAction<string | null>>;
  setUsage: React.Dispatch<React.SetStateAction<UsageSummary | null>>;
};

const applyEvent = ({
  event,
  assistantTurnId,
  setTurns,
  setStatus,
  setUsage,
}: ApplyEventArguments) => {
  if (event.type === "status") {
    setStatus(statusLabels[event.status]);
  } else if (event.type === "text") {
    setTurns((current) =>
      updateAssistantTurn(current, assistantTurnId, (turn) => ({
        ...turn,
        text: `${turn.text}${event.delta}`,
      })),
    );
  } else if (event.type === "movie-card") {
    setTurns((current) =>
      updateAssistantTurn(current, assistantTurnId, (turn) => ({
        ...turn,
        movies: turn.movies.some(
          (movie) => movie.movieId === event.movie.movieId,
        )
          ? turn.movies
          : [...turn.movies, event.movie],
      })),
    );
  } else if (event.type === "error") {
    setTurns((current) =>
      updateAssistantTurn(current, assistantTurnId, (turn) => ({
        ...turn,
        error: { message: event.message, retryable: event.retryable },
      })),
    );
  } else if (event.type === "usage") {
    setUsage(event.usage);
  }
};

const updateAssistantTurn = (
  turns: ChatTurn[],
  id: string,
  update: (turn: ChatTurn) => ChatTurn,
): ChatTurn[] => turns.map((turn) => (turn.id === id ? update(turn) : turn));
