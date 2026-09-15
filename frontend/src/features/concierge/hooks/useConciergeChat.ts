import {
  confirmHistoryNavigation,
  updateRetrievedMovies,
  updateToolActivity,
} from "../model/conversationHistory";
import type { PageContext } from "../model/pageContext";
import { getConciergeDelegation } from "../api/delegation";
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
  ApplicationAction,
  UsageSummary,
} from "../model/concierge";
import { statusLabels } from "../model/concierge";

const createTurnId = (): string => window.crypto.randomUUID();

export const useConciergeChat = (
  clientId: string,
  onUiAction: (action: ApplicationAction) => string | void,
  pageContext?: PageContext,
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

  const confirmNavigation = useCallback(
    (path: string) =>
      setTurns((items) => confirmHistoryNavigation(items, path)),
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
      setTurns(
        (current) =>
          [
            ...current,
            {
              id: createTurnId(),
              role: "user",
              text: message,
              movies: [],
              channel: "text",
              timestamp: Date.now(),
              final: true,
            },
            {
              id: assistantTurnId,
              role: "assistant",
              text: "",
              movies: [],
              channel: "text",
              timestamp: Date.now(),
              ...(pageContext
                ? {
                    context: {
                      page: pageContext.page,
                      ...(pageContext.streamingCountry
                        ? { streamingCountry: pageContext.streamingCountry }
                        : {}),
                    },
                  }
                : {}),
            },
          ].slice(-200) as ChatTurn[],
      );
      setStatus("Thinking");
      setUsage(null);
      setIsStreaming(true);

      const abortController = new AbortController();
      const groundedMovieIds = new Set<number>();
      let actionHandled = false;
      abortRef.current = abortController;
      try {
        const delegation = await getConciergeDelegation();
        if (abortController.signal.aborted) return;
        const conversationId =
          conversationIdRef.current ??
          (await createConversation(
            clientId,
            abortController.signal,
            delegation,
          ));
        conversationIdRef.current = conversationId;

        await streamMessage({
          ...(pageContext ? { pageContext } : {}),
          delegation,
          clientId,
          conversationId,
          message,
          signal: abortController.signal,
          onEvent: (event) => {
            if (event.type === "movie-card") {
              groundedMovieIds.add(event.movie.movieId);
            } else if (event.type === "ui-action") {
              const allowed =
                !abortController.signal.aborted &&
                !actionHandled &&
                ((event.action.type !== "open_movie" &&
                  event.action.type !== "open_movie_trailer") ||
                  groundedMovieIds.has(event.action.movieId));
              actionHandled = true;
              if (!allowed) {
                reportUiAction(event.action, "rejected");
                setTurns((items) =>
                  updateAssistantTurn(items, assistantTurnId, (turn) => ({
                    ...turn,
                    actions: [
                      {
                        action: event.action,
                        outcome: "rejected",
                        timestamp: Date.now(),
                      },
                    ],
                  })),
                );
                return;
              }
              let outcome: "requested" | "rejected" = "requested";
              let destination: string | void;
              try {
                destination = onUiAction(event.action);
                reportUiAction(event.action, "executed");
              } catch {
                outcome = "rejected";
                reportUiAction(event.action, "rejected");
              }
              setTurns((items) =>
                updateAssistantTurn(items, assistantTurnId, (turn) => ({
                  ...turn,
                  actions: [
                    {
                      action: event.action,
                      outcome,
                      timestamp: Date.now(),
                      ...(destination ? { destination } : {}),
                    },
                  ],
                })),
              );
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
          setTurns((items) =>
            updateAssistantTurn(items, assistantTurnId, (turn) => ({
              ...turn,
              final: true,
            })),
          );
          setIsStreaming(false);
          setStatus(null);
        }
      }
    },
    [clientId, onUiAction, pageContext],
  );

  return { isStreaming, reset, send, status, turns, usage, confirmNavigation };
};

const reportUiAction = (
  action: ApplicationAction,
  outcome: "executed" | "rejected",
): void => {
  // The Java browser-telemetry contract currently supports movie opens only.
  if (action.type !== "open_movie") return;
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
        movies: updateRetrievedMovies(turn.movies, event.movie),
      })),
    );
  } else if (event.type === "tool-activity") {
    setTurns((current) =>
      updateAssistantTurn(current, assistantTurnId, (turn) => ({
        ...turn,
        tools: updateToolActivity(turn.tools, event.activity),
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
