import { useCallback, useEffect, useRef, useState } from "react";
import {
  ConciergeClientError,
  createConversation,
  streamMessage,
} from "../api/conciergeClient";
import type {
  ChatTurn,
  ConciergeEvent,
  UsageSummary,
} from "../model/concierge";
import { statusLabels } from "../model/concierge";

const createTurnId = (): string => window.crypto.randomUUID();

export const useConciergeChat = (clientId: string) => {
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
    [clientId],
  );

  return { isStreaming, reset, send, status, turns, usage };
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
