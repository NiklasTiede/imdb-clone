import { getConciergeBrowserId } from "../model/browserIdentity";
import type { PageContext } from "../model/pageContext";
import { getConciergeIdentity } from "../api/delegation";
import type { VoiceModel } from "../api/voiceModels";
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
  accountId: number | null = null,
  model: VoiceModel = "grok",
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
  const actionRef = useRef(onAction);
  const accountIdRef = useRef(accountId);
  const socketAccountRef = useRef<number | null>(null);
  const authenticationPending = useRef(false);
  useEffect(() => {
    actionRef.current = onAction;
  }, [onAction]);

  const synchronizeLogin = useCallback(async () => {
    const socket = socketRef.current;
    const current = generation.current;
    if (
      !readyRef.current ||
      accountIdRef.current === null ||
      socketAccountRef.current !== null ||
      authenticationPending.current ||
      socket?.readyState !== WebSocket.OPEN
    )
      return;
    authenticationPending.current = true;
    try {
      const identity = await getConciergeIdentity();
      if (generation.current !== current || socketRef.current !== socket)
        return;
      if (!identity.delegation || identity.accountId !== accountIdRef.current)
        throw new Error("Login changed");
      socket.send(
        JSON.stringify({
          type: "authenticate",
          delegation: identity.delegation,
        }),
      );
    } catch {
      if (generation.current === current) {
        authenticationPending.current = false;
        setNotice(
          "Your sign-in could not be verified for voice. Restart voice to try again.",
        );
      }
    }
  }, []);
  useEffect(() => {
    accountIdRef.current = accountId;
    void synchronizeLogin();
  }, [accountId, synchronizeLogin]);
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
    socketAccountRef.current = null;
    authenticationPending.current = false;
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
    const startedAt = performance.now();
    const startupTimes: Record<string, number> = {};
    const startupStage = (stage: string) => {
      startupTimes[stage] ??= Math.round(performance.now() - startedAt);
    };
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
          ? "Voice audio access was blocked. Allow microphone access for this site in your browser's site permissions and for your browser app in your device settings, then try again."
          : name === "NotFoundError"
            ? "No microphone found. Connect an input device and try again."
            : name === "NotReadableError"
              ? "Your microphone couldn't be opened. Check whether another app is using it and whether your input device is working, then try again."
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
    const startupMessages: (string | ArrayBuffer)[] = [];
    let startupBytes = 0;
    let processMessage: ((data: unknown) => void) | undefined;
    const bufferStartup = (data: string | ArrayBuffer) => {
      // A hosted agent may greet us while macOS is still opening the audio device.
      // Preserve PCM and control-event order, with a bounded startup backlog.
      startupBytes +=
        typeof data === "string" ? data.length * 2 : data.byteLength;
      if (startupBytes > 512_000 || startupMessages.length >= 1024) {
        startupMessages.length = 0;
        fail("Voice audio couldn't start in time. Please reconnect.");
        return;
      }
      startupMessages.push(data);
    };
    const becomeReady = () => {
      if (!isCurrent() || ready || !providerReady || !captureReady) return;
      ready = true;
      readyRef.current = true;
      clearTimeout(timerRef.current);
      setStatus("listening");
      void synchronizeLogin();
      if (mutedRef.current)
        socketRef.current?.send(JSON.stringify({ type: "mute" }));
      const pending = startupMessages.splice(0);
      startupBytes = 0;
      for (const data of pending) {
        if (!isCurrent()) break;
        processMessage?.(data);
      }
    };
    try {
      if (!navigator.mediaDevices?.getUserMedia || !window.AudioContext) {
        fail(
          "Voice requires a browser with microphone support and a secure connection (HTTPS or localhost). If you opened this page inside another app, open it in your browser and try again.",
        );
        return;
      }
      const audio = new BrowserAudio(model === "gpt-live-1");
      audioRef.current = audio;
      // Fetch delegation while the microphone starts, handling rejection immediately
      // even if permission stays pending or the user ends the session meanwhile.
      const identityReady = getConciergeIdentity().then(
        (identity) => {
          startupStage("identity_ready_ms");
          return { identity };
        },
        (error: unknown) => ({ error }),
      );
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
        () => {
          startupStage("microphone_granted_ms");
          microphoneGranted();
        },
      );
      // Connect as soon as permission is granted while the audio device/worklet still starts.
      // Failure/end closes both branches; no socket is opened while permission is pending.
      void audioStarted.then(() => {
        startupStage("audio_device_ready_ms");
        captureReady = true;
        becomeReady();
      }, failAudio);
      await Promise.race([permission, audioStarted]);
      if (!isCurrent()) return;
      const base =
        import.meta.env.VITE_POPCORN_SOCIETY_CONCIERGE_ADDRESS ??
        import.meta.env.VITE_IMDB_CLONE_CONCIERGE_ADDRESS ??
        "/concierge-api";
      const url = new URL(`${base}/v1/voice`, window.location.href);
      url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
      const browserId = getConciergeBrowserId();
      const identityResult = await identityReady;
      if ("error" in identityResult) throw identityResult.error;
      const { identity } = identityResult;
      if (!isCurrent()) return;
      socketAccountRef.current = identity.accountId;
      const socket = new WebSocket(url);
      socket.onopen = () => {
        if (!isCurrent()) return;
        startupStage("browser_socket_open_ms");
        socket.send(
          JSON.stringify({
            type: "start",
            browser_id: browserId,
            delegation: identity.delegation,
            model,
          }),
        );
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
      const movieActions = new Map<number, Set<string>>();
      const acceptsAction = (turn: number, action: ApplicationAction) => {
        if (!actions.has(turn)) return true;
        return (
          (action.type === "open_movie" ||
            action.type === "open_movie_trailer") &&
          movieActions.get(turn)?.has(`${action.type}:${action.movieId}`) ===
            false
        );
      };
      const grounded = new Map<number, Set<number>>();
      let turn = 0;
      processMessage = (data: unknown) => {
        if (!isCurrent()) return;
        if (data instanceof ArrayBuffer) {
          startupStage("first_audio_packet_ms");
          if (!ready) {
            bufferStartup(data);
            return;
          }
          try {
            if (!blockedAudio.current) {
              audio.play(data);
              if (startupTimes.first_audio_queued_ms === undefined) {
                startupStage("first_audio_queued_ms");
                // GPT-Live also streams silence: queued PCM is not proof of audible speech.
                console.debug("[Voice startup]", { model, ...startupTimes });
              }
            }
          } catch {
            fail("Voice audio couldn't play. Please reconnect.");
          }
          return;
        }
        try {
          if (typeof data !== "string" || data.length > 32_000)
            throw new Error("Invalid event");
          const event = voiceEventSchema.parse(JSON.parse(data));
          if (event.type === "ready") {
            startupStage("provider_ready_ms");
            providerReady = true;
            becomeReady();
          } else if (event.type === "authenticated") {
            socketAccountRef.current = accountIdRef.current;
            authenticationPending.current = false;
          } else if (event.type === "authentication-failed") {
            authenticationPending.current = false;
            setNotice(event.text ?? "Voice could not verify your sign-in.");
          } else if (event.type === "error") {
            fail(event.text ?? "Voice is unavailable. Please reconnect.");
          } else if (event.type === "quota-warning") {
            setNotice(
              event.text ?? "Your voice time allowance is almost used up.",
            );
          } else if (event.type === "standby") {
            markInterrupted();
            dispose();
            setError(null);
            setNotice(null);
            setStatus("standby");
            setLevels({ input: 0, output: 0, playing: false });
          } else if (!ready) {
            bufferStartup(data);
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
            acceptsAction(event.turn, event.action) &&
            ((event.action.type !== "open_movie" &&
              event.action.type !== "open_movie_trailer") ||
              grounded.get(event.turn)?.has(event.action.movieId))
          ) {
            actions.add(event.turn);
            if (
              event.action.type === "open_movie" ||
              event.action.type === "open_movie_trailer"
            ) {
              const keys = movieActions.get(event.turn) ?? new Set<string>();
              keys.add(`${event.action.type}:${event.action.movieId}`);
              movieActions.set(event.turn, keys);
            }
            let outcome: "requested" | "rejected" = "requested";
            let destination: string | void;
            try {
              destination = actionRef.current(event.action);
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
            acceptsAction(event.turn, event.action)
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
      socket.onmessage = (message: MessageEvent<unknown>) =>
        processMessage?.(message.data);
      socket.onerror = () =>
        fail(
          "Voice connection failed. Check that the voice service is running.",
        );
      socket.onclose = () =>
        fail("Voice connection closed. Reconnect to continue.");
      const updateLevels = () => {
        if (!isCurrent()) return;
        const next = audio.levels();
        if (
          next.output > 0.015 &&
          startupTimes.first_output_signal_ms === undefined
        ) {
          startupStage("first_output_signal_ms");
          console.debug("[Voice startup signal]", { model, ...startupTimes });
        }
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
  }, [dispose, markInterrupted, synchronizeLogin, model]);

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
    blockedAudio.current = model !== "gpt-live-1";
    audioRef.current?.interrupt();
    const socket = socketRef.current;
    if (socket?.readyState === WebSocket.OPEN)
      socket.send(JSON.stringify({ type: "interrupt" }));
  }, [markInterrupted, model]);

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
      blockedAudio.current = model !== "gpt-live-1";
      audioRef.current?.interrupt();
      socket.send(JSON.stringify({ type: "text", text }));
      textPendingRef.current = true;
      setTextPending(true);
      setNotice(null);
      setStatus("thinking");
      return true;
    },
    [markInterrupted, model],
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
