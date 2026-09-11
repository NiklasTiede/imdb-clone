import { useCallback, useEffect, useRef, useState } from "react";

/** Local analyser only: no recording, playback, or connection to the Concierge. */
export function usePreviewMicrophone() {
  const [status, setStatus] = useState<"off" | "starting" | "on" | "muted">(
    "off",
  );
  const [error, setError] = useState("");
  const resources = useRef<{
    context: AudioContext;
    stream: MediaStream;
    analyser: AnalyserNode;
    samples: Float32Array<ArrayBuffer>;
  } | null>(null);
  const generation = useRef(0);
  const activeContext = useRef<AudioContext | null>(null);

  const release = useCallback(() => {
    generation.current += 1;
    const current = resources.current;
    resources.current = null;
    current?.stream.getTracks().forEach((track) => track.stop());
    const context = activeContext.current;
    activeContext.current = null;
    if (context && context.state !== "closed")
      void context.close().catch(() => {});
  }, []);

  const stop = useCallback(() => {
    release();
    setStatus("off");
  }, [release]);

  useEffect(() => {
    const onPageHide = () => stop();
    window.addEventListener("pagehide", onPageHide);
    return () => {
      window.removeEventListener("pagehide", onPageHide);
      release();
    };
  }, [release, stop]);

  const start = async () => {
    release();
    const request = generation.current;
    setError("");
    setStatus("starting");
    let context: AudioContext | undefined;
    let stream: MediaStream | undefined;
    try {
      context = new AudioContext();
      activeContext.current = context;
      // Resume in the click gesture, with a rejection handler attached immediately.
      const resumed = context.resume().then(
        () => null,
        () => new Error("Audio device unavailable"),
      );
      stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });
      if (generation.current !== request) {
        stream.getTracks().forEach((track) => track.stop());
        void context.close().catch(() => {});
        return;
      }
      const analyser = context.createAnalyser();
      analyser.fftSize = 512;
      context.createMediaStreamSource(stream).connect(analyser);
      resources.current = {
        context,
        stream,
        analyser,
        samples: new Float32Array(512),
      };
      const resumeError = await resumed;
      if (generation.current !== request) return;
      if (resumeError) throw resumeError;
      stream.getAudioTracks().forEach((track) =>
        track.addEventListener(
          "ended",
          () => {
            if (generation.current === request) {
              stop();
              setError("Microphone disconnected. Connect it and try again.");
            }
          },
          { once: true },
        ),
      );
      setStatus("on");
    } catch {
      stream?.getTracks().forEach((track) => track.stop());
      if (context && context.state !== "closed")
        void context.close().catch(() => {});
      if (generation.current === request) {
        resources.current = null;
        activeContext.current = null;
        setStatus("off");
        setError(
          "Microphone unavailable. Allow access in your browser and try again. You can still explore the simulated states.",
        );
      }
    }
  };

  const toggleMute = () => {
    const current = resources.current;
    if (!current) return;
    const enabled = !current.stream.getAudioTracks()[0]?.enabled;
    current.stream.getAudioTracks().forEach((track) => {
      track.enabled = enabled;
    });
    setStatus(enabled ? "on" : "muted");
  };

  const readLevel = useCallback(() => {
    const current = resources.current;
    if (!current || !current.stream.getAudioTracks()[0]?.enabled) return 0;
    current.analyser.getFloatTimeDomainData(current.samples);
    const rms = Math.sqrt(
      current.samples.reduce((sum, value) => sum + value * value, 0) /
        current.samples.length,
    );
    // Small noise floor and compressed response keep quiet speech visible.
    return Math.min(1, Math.pow(Math.max(0, rms - 0.008) * 8, 0.7));
  }, []);

  return { status, error, start, stop, toggleMute, readLevel };
}
