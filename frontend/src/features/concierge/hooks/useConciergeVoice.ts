import type { PageContext } from "../model/pageContext";
import { getConciergeDelegation } from "../api/delegation";
import { useCallback, useEffect, useRef, useState } from "react";
import { BrowserAudio } from "../audio/browserAudio";
import {
  updateHistory,
  confirmHistoryNavigation,
  updateRetrievedMovies,
  updateToolActivity,
} from "../model/conversationHistory";
import type { ChatTurn, ApplicationAction } from "../model/concierge";
import { voiceEventSchema, type VoiceStatus } from "../model/voice";

export const useConciergeVoice = (
  onAction: (action: ApplicationAction) => string | void,
  pageContext?: PageContext,
) => {
  const [turns, setTurns] = useState<ChatTurn[]>([]);
  const assistantId = useRef<string | null>(null);
  const [status, setStatus] = useState<VoiceStatus>("idle");
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [muted, setMuted] = useState(false);
  const [textPending, setTextPending] = useState(false);
  const textPendingRef = useRef(false);
  const readyRef = useRef(false);
  const [levels, setLevels] = useState({ input: 0, output: 0, playing: false });
  const generation = useRef(0);
  const audioRef = useRef<BrowserAudio | null>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const frameRef = useRef(0);
  const playingRef = useRef(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const mutedRef = useRef(false);
  const blockedAudio = useRef(false);
  const pageContextRef = useRef(pageContext);
  const contextSocketRef = useRef<WebSocket | null>(null);
  useEffect(() => {
    pageContextRef.current = pageContext;
    const socket = socketRef.current;
    if (
      pageContext &&
      socket === contextSocketRef.current &&
      socket?.readyState === WebSocket.OPEN
    ) {
      socket.send(JSON.stringify({ type: "context", context: pageContext }));
    }
  }, [pageContext]);

  const dispose = useCallback(() => {
    readyRef.current = false;
    textPendingRef.current = false;
    generation.current++;
    clearTimeout(timerRef.current);
    cancelAnimationFrame(frameRef.current);
    const socket = socketRef.current;
    socketRef.current = null;
    contextSocketRef.current = null;
    try {
      if (socket?.readyState === WebSocket.OPEN)
        socket.send(JSON.stringify({ type: "end" }));
    } catch {
      // The peer may disconnect during shutdown; still release the microphone.
    }
    socket?.close();
    audioRef.current?.close();
    audioRef.current = null;
    playingRef.current = false;
  }, []);

  useEffect(() => dispose, [dispose]);

  const readLevels = useCallback(
    () => audioRef.current?.levels() ?? { input: 0, output: 0, playing: false },
    [],
  );
  const markInterrupted = useCallback(() => {
    if (!audioRef.current?.levels().playing) return;
    const id = assistantId.current;
    setTurns((items) =>
      items.map((item) =>
        item.id === id ? { ...item, interrupted: true } : item,
      ),
    );
  }, []);
  const confirmNavigation = useCallback(
    (path: string) =>
      setTurns((items) => confirmHistoryNavigation(items, path)),
    [],
  );
  const clearHistory = useCallback(() => {
    setTurns([]);
    assistantId.current = null;
  }, []);

  const end = useCallback(() => {
    markInterrupted();
    dispose();
    setStatus("idle");
    setError(null);
    setNotice(null);
    setLevels({ input: 0, output: 0, playing: false });
    setMuted(false);
    mutedRef.current = false;
    blockedAudio.current = false;
  }, [dispose, markInterrupted]);

  const start = useCallback(async () => {
    if (audioRef.current) return;
    const current = ++generation.current;
    const isCurrent = () => generation.current === current;
    const turnMetadata = new Map<
      number,
      { timestamp: number; context: PageContext | undefined }
    >();
    const record = (
      turn: number,
      role: "user" | "assistant",
      update: (entry: ChatTurn) => ChatTurn,
    ) => {
      const id = `voice-${current}-${turn}-${role}`;
      if (role === "assistant") assistantId.current = id;
      const metadata = turnMetadata.get(turn) ?? {
        timestamp: Date.now(),
        context: pageContextRef.current,
      };
      turnMetadata.set(turn, metadata);
      const timestamp = metadata.timestamp + (role === "assistant" ? 0.01 : 0);
      const context = metadata.context;
      setTurns((items) =>
        updateHistory(
          items,
          update(
            items.find((item) => item.id === id) ?? {
              id,
              role,
              text: "",
              movies: [],
              channel: "voice",
              timestamp,
              ...(context
                ? {
                    context: {
                      page: context.page,
                      ...(context.streamingCountry
                        ? { streamingCountry: context.streamingCountry }
                        : {}),
                    },
                  }
                : {}),
            },
          ),
        ),
      );
    };
    const fail = (message: string) => {
      if (!isCurrent()) return;
      markInterrupted();
      dispose();
      const timestamp = Date.now();
      setTurns((items) =>
        updateHistory(items, {
          id: `voice-${current}-error`,
          role: "assistant",
          channel: "voice",
          timestamp,
          text: "",
          movies: [],
          error: { message, retryable: true },
        }),
      );
      setError(message);
      setStatus("error");
      setLevels({ input: 0, output: 0, playing: false });
    };
    const failAudio = (cause: unknown) => {
      const name =
        cause instanceof Error || cause instanceof DOMException
          ? cause.name
          : "";
      fail(
        name === "NotAllowedError"
          ? "Microphone access was denied. Allow it in your browser and macOS System Settings → Privacy & Security → Microphone, then try again."
          : name === "NotFoundError"
            ? "No microphone found. Connect an input device and try again."
            : "Microphone audio couldn't start. Check your browser permissions and input device.",
      );
    };
    setError(null);
    setNotice(null);
    setStatus("connecting");
    setTextPending(false);
    setMuted(false);
    mutedRef.current = false;
    blockedAudio.current = false;
    let ready = false;
    let providerReady = false;
    let captureReady = false;
    const becomeReady = () => {
      if (!isCurrent() || !providerReady || !captureReady) return;
      ready = true;
      readyRef.current = true;
      clearTimeout(timerRef.current);
      setStatus("listening");
      if (mutedRef.current)
        socketRef.current?.send(JSON.stringify({ type: "mute" }));
    };
    try {
      if (!navigator.mediaDevices?.getUserMedia || !window.AudioContext) {
        throw new Error(
          "Microphone audio requires a supported browser on localhost or HTTPS.",
        );
      }
      const audio = new BrowserAudio();
      audioRef.current = audio;
      let microphoneGranted: () => void = () => {};
      const permission = new Promise<void>((resolve) => {
        microphoneGranted = resolve;
      });
      const audioStarted = audio.start(
        (chunk) => {
          const socket = socketRef.current;
          if (
            !isCurrent() ||
            !ready ||
            !chunk.byteLength ||
            socket?.readyState !== WebSocket.OPEN
          )
            return;
          if (socket.bufferedAmount > 240_000) {
            fail("Audio connection is too slow. Please reconnect.");
            return;
          }
          socket.send(chunk);
        },
        () =>
          fail(
            "The microphone disconnected. Check your input device and reconnect.",
          ),
        microphoneGranted,
      );
      // Connect as soon as permission is granted while the audio device/worklet still starts.
      // Failure/end closes both branches; no socket is opened while permission is pending.
      void audioStarted.then(() => {
        captureReady = true;
        becomeReady();
      }, failAudio);
      await Promise.race([permission, audioStarted]);
      if (!isCurrent()) return;
      const base =
        import.meta.env.VITE_IMDB_CLONE_CONCIERGE_ADDRESS ?? "/concierge-api";
      const url = new URL(`${base}/v1/voice`, window.location.href);
      url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
      const delegation = await getConciergeDelegation();
      if (!isCurrent()) return;
      const socket = new WebSocket(url);
      socket.onopen = () => {
        if (!isCurrent()) return;
        socket.send(JSON.stringify({ type: "start", delegation }));
        contextSocketRef.current = socket;
        if (pageContextRef.current)
          socket.send(
            JSON.stringify({
              type: "context",
              context: pageContextRef.current,
            }),
          );
      };
      socket.binaryType = "arraybuffer";
      socketRef.current = socket;
      timerRef.current = setTimeout(
        () =>
          fail(
            "Voice connection timed out. Check that the voice service is running.",
          ),
        20_000,
      );
      const actions = new Set<number>();
      const grounded = new Map<number, Set<number>>();
      let turn = 0;
      socket.onmessage = (message: MessageEvent<unknown>) => {
        if (!isCurrent()) return;
        if (message.data instanceof ArrayBuffer) {
          try {
            if (ready && !blockedAudio.current) audio.play(message.data);
          } catch {
            fail("Voice audio couldn't play. Please reconnect.");
          }
          return;
        }
        try {
          if (typeof message.data !== "string" || message.data.length > 32_000)
            throw new Error("Invalid event");
          const event = voiceEventSchema.parse(JSON.parse(message.data));
          if (event.type === "ready") {
            providerReady = true;
            becomeReady();
          } else if (event.type === "error") {
            fail(event.text ?? "Voice is unavailable. Please reconnect.");
          } else if (event.type === "standby") {
            markInterrupted();
            dispose();
            setError(null);
            setNotice(null);
            setStatus("standby");
            setLevels({ input: 0, output: 0, playing: false });
          } else if (!ready) {
            return;
          } else if (event.type === "interrupt") {
            if (event.turn < turn) return;
            markInterrupted();
            audio.interrupt();
            if (event.turn > turn) {
              setNotice(null);
              blockedAudio.current = false;
              turn = event.turn;
            }
          } else if (event.turn < turn) {
            return;
          } else if (event.type === "reply-complete") {
            audio.finishReply();
            record(event.turn, "assistant", (entry) => ({
              ...entry,
              final: true,
            }));
          } else if (
            event.type === "status" &&
            event.status &&
            event.status !== "muted"
          ) {
            setStatus(event.status);
            if (event.text) {
              const message = event.text;
              setNotice(message);
              record(event.turn, "assistant", (entry) => ({
                ...entry,
                final: true,
                error: { message, retryable: false },
              }));
            }
          } else if (event.type === "transcript") {
            if (event.speaker === "user" && event.final) {
              textPendingRef.current = false;
              setTextPending(false);
            }
            if (
              event.speaker &&
              !(event.speaker === "assistant" && blockedAudio.current)
            )
              record(event.turn, event.speaker, (entry) => ({
                ...entry,
                text: event.text ?? "",
                final: event.final,
              }));
          } else if (event.type === "movie-card" && event.movie) {
            const movie = event.movie;
            record(event.turn, "assistant", (entry) => ({
              ...entry,
              movies: updateRetrievedMovies(entry.movies, movie),
            }));
            const ids = grounded.get(event.turn) ?? new Set<number>();
            ids.add(movie.movieId);
            grounded.set(event.turn, ids);
          } else if (event.type === "tool-activity" && event.activity) {
            const activity = event.activity;
            record(event.turn, "assistant", (entry) => ({
              ...entry,
              tools: updateToolActivity(entry.tools, activity),
              ...(activity.status === "started" ? { final: false } : {}),
            }));
          } else if (
            event.type === "ui-action" &&
            !blockedAudio.current &&
            event.action &&
            !actions.has(event.turn) &&
            ((event.action.type !== "open_movie" &&
              event.action.type !== "open_movie_trailer") ||
              grounded.get(event.turn)?.has(event.action.movieId))
          ) {
            actions.add(event.turn);
            let outcome: "requested" | "rejected" = "requested";
            let destination: string | void;
            try {
              destination = onAction(event.action);
            } catch {
              outcome = "rejected";
            }
            const action = event.action;
            record(event.turn, "assistant", (entry) => ({
              ...entry,
              actions: [
                ...(entry.actions ?? []),
                {
                  action,
                  outcome,
                  timestamp: Date.now(),
                  ...(destination ? { destination } : {}),
                },
              ],
            }));
          } else if (
            event.type === "ui-action" &&
            event.action &&
            !actions.has(event.turn)
          ) {
            const action = event.action;
            record(event.turn, "assistant", (entry) => ({
              ...entry,
              actions: [
                ...(entry.actions ?? []),
                { action, outcome: "rejected" as const, timestamp: Date.now() },
              ].slice(-10),
            }));
          }
        } catch {
          fail("Voice sent an invalid response. Please reconnect.");
        }
      };
      socket.onerror = () =>
        fail(
          "Voice connection failed. Check that the voice service is running.",
        );
      socket.onclose = () =>
        fail("Voice connection closed. Reconnect to continue.");
      const updateLevels = () => {
        if (!isCurrent()) return;
        const next = audio.levels();
        if (playingRef.current !== next.playing) {
          playingRef.current = next.playing;
          setLevels(next);
        }
        frameRef.current = requestAnimationFrame(updateLevels);
      };
      updateLevels();
      await audioStarted;
    } catch (cause) {
      failAudio(cause);
    }
  }, [dispose, onAction, markInterrupted]);

  const toggleMute = useCallback(() => {
    const value = !mutedRef.current;
    mutedRef.current = value;
    setMuted(value);
    audioRef.current?.mute(value);
    const socket = socketRef.current;
    if (socket?.readyState === WebSocket.OPEN)
      socket.send(JSON.stringify({ type: value ? "mute" : "resume" }));
  }, []);

  const interrupt = useCallback(() => {
    markInterrupted();
    blockedAudio.current = true;
    audioRef.current?.interrupt();
    const socket = socketRef.current;
    if (socket?.readyState === WebSocket.OPEN)
      socket.send(JSON.stringify({ type: "interrupt" }));
  }, [markInterrupted]);

  const sendText = useCallback(
    (message: string) => {
      const text = message.trim();
      const socket = socketRef.current;
      if (
        !text ||
        text.length > 600 ||
        !readyRef.current ||
        textPendingRef.current ||
        socket?.readyState !== WebSocket.OPEN
      )
        return false;
      markInterrupted();
      blockedAudio.current = true;
      audioRef.current?.interrupt();
      socket.send(JSON.stringify({ type: "text", text }));
      textPendingRef.current = true;
      setTextPending(true);
      setNotice(null);
      setStatus("thinking");
      return true;
    },
    [markInterrupted],
  );

  return {
    turns,
    clearHistory,
    readLevels,
    confirmNavigation,
    status,
    error,
    notice,
    muted,
    levels,
    start,
    end,
    toggleMute,
    interrupt,
    sendText,
    canSendText:
      status !== "idle" &&
      status !== "standby" &&
      status !== "error" &&
      status !== "connecting" &&
      !textPending,
    active: status !== "idle" && status !== "error" && status !== "standby",
  };
};

export type ConciergeVoice = ReturnType<typeof useConciergeVoice>;
