import { getConciergeDelegation } from "../api/delegation";
import { useCallback, useEffect, useRef, useState } from "react";
import { BrowserAudio } from "../audio/browserAudio";
import type { GroundedMovie, ApplicationAction } from "../model/concierge";
import { voiceEventSchema, type VoiceStatus } from "../model/voice";

export const useConciergeVoice = (
  onAction: (action: ApplicationAction) => void,
) => {
  const [status, setStatus] = useState<VoiceStatus>("idle");
  const [error, setError] = useState<string | null>(null);
  const [muted, setMuted] = useState(false);
  const [levels, setLevels] = useState({ input: 0, output: 0, playing: false });
  const [userText, setUserText] = useState("");
  const [assistantText, setAssistantText] = useState("");
  const [movies, setMovies] = useState<GroundedMovie[]>([]);
  const generation = useRef(0);
  const audioRef = useRef<BrowserAudio | null>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const frameRef = useRef(0);
  const timerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const mutedRef = useRef(false);
  const blockedAudio = useRef(false);

  const dispose = useCallback(() => {
    generation.current++;
    clearTimeout(timerRef.current);
    cancelAnimationFrame(frameRef.current);
    const socket = socketRef.current;
    socketRef.current = null;
    if (socket?.readyState === WebSocket.OPEN)
      socket.send(JSON.stringify({ type: "end" }));
    socket?.close();
    audioRef.current?.close();
    audioRef.current = null;
  }, []);

  useEffect(() => dispose, [dispose]);

  const end = useCallback(() => {
    dispose();
    setStatus("idle");
    setLevels({ input: 0, output: 0, playing: false });
    setMuted(false);
    mutedRef.current = false;
    blockedAudio.current = false;
  }, [dispose]);

  const start = useCallback(async () => {
    if (audioRef.current) return;
    const current = ++generation.current;
    const isCurrent = () => generation.current === current;
    const fail = (message: string) => {
      if (!isCurrent()) return;
      dispose();
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
    setStatus("connecting");
    setUserText("");
    setAssistantText("");
    setMovies([]);
    setMuted(false);
    mutedRef.current = false;
    blockedAudio.current = false;
    let ready = false;
    let providerReady = false;
    let captureReady = false;
    const becomeReady = () => {
      if (!isCurrent() || !providerReady || !captureReady) return;
      ready = true;
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
        if (isCurrent())
          socket.send(JSON.stringify({ type: "start", delegation }));
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
            fail(event.text ?? "Voice is unavailable. Please use text.");
          } else if (!ready) {
            return;
          } else if (event.type === "interrupt") {
            audio.interrupt();
            if (event.turn > turn) {
              blockedAudio.current = false;
              turn = event.turn;
              setUserText("");
              setAssistantText("");
            }
          } else if (event.turn < turn) {
            return;
          } else if (event.type === "reply-complete") {
            audio.finishReply();
          } else if (
            event.type === "status" &&
            event.status &&
            event.status !== "muted"
          ) {
            setStatus(event.status);
          } else if (event.type === "transcript") {
            if (event.speaker === "user") setUserText(event.text ?? "");
            else setAssistantText(event.text ?? "");
          } else if (event.type === "movie-card" && event.movie) {
            const movie = event.movie;
            const ids = grounded.get(event.turn) ?? new Set<number>();
            ids.add(movie.movieId);
            grounded.set(event.turn, ids);
            setMovies((items) =>
              [
                movie,
                ...items.filter((item) => item.movieId !== movie.movieId),
              ].slice(0, 5),
            );
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
            onAction(event.action);
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
        setLevels(audio.levels());
        frameRef.current = requestAnimationFrame(updateLevels);
      };
      updateLevels();
      await audioStarted;
    } catch (cause) {
      failAudio(cause);
    }
  }, [dispose, onAction]);

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
    blockedAudio.current = true;
    audioRef.current?.interrupt();
    const socket = socketRef.current;
    if (socket?.readyState === WebSocket.OPEN)
      socket.send(JSON.stringify({ type: "interrupt" }));
  }, []);

  return {
    status,
    error,
    muted,
    levels,
    userText,
    assistantText,
    movies,
    start,
    end,
    toggleMute,
    interrupt,
    active: status !== "idle" && status !== "error",
  };
};

export type ConciergeVoice = ReturnType<typeof useConciergeVoice>;
