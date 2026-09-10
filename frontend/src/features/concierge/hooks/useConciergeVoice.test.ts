import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useConciergeVoice } from "./useConciergeVoice";

const audio = vi.hoisted(() => ({
  start: vi.fn(),
  close: vi.fn(),
  mute: vi.fn(),
  play: vi.fn(),
  interrupt: vi.fn(),
  finishReply: vi.fn(),
  levels: vi.fn(() => ({ input: 0, output: 0, playing: false })),
}));
vi.mock("../audio/browserAudio", () => ({
  BrowserAudio: class {
    constructor() {
      return audio;
    }
  },
}));

class Socket {
  static OPEN = 1;
  static instances: Socket[] = [];
  readyState = 1;
  bufferedAmount = 0;
  onopen?: () => void;
  onmessage?: (event: { data: unknown }) => void;
  onerror?: () => void;
  onclose?: () => void;
  send = vi.fn();
  close = vi.fn();
  constructor() {
    Socket.instances.push(this);
  }
  emit(value: object) {
    this.onmessage?.({ data: JSON.stringify(value) });
  }
}

beforeEach(() => {
  vi.clearAllMocks();
  audio.start.mockResolvedValue(undefined);
  Socket.instances = [];
  vi.stubGlobal("WebSocket", Socket);
  vi.stubGlobal("AudioContext", class {});
  vi.stubGlobal("navigator", { mediaDevices: { getUserMedia: vi.fn() } });
  vi.stubGlobal(
    "requestAnimationFrame",
    vi.fn(() => 1),
  );
  vi.stubGlobal("cancelAnimationFrame", vi.fn());
});
afterEach(() => vi.unstubAllGlobals());

describe("voice session lifecycle", () => {
  it("finishes playback only for an explicit current reply completion", async () => {
    const { result } = renderHook(() => useConciergeVoice(vi.fn()));
    await act(() => result.current.start());
    const socket = Socket.instances[0];
    if (!socket) throw new Error("Expected voice socket");
    act(() => {
      socket.emit({ type: "ready" });
      socket.emit({ type: "interrupt", turn: 2 });
      socket.emit({ type: "status", status: "listening", turn: 2 });
      socket.emit({ type: "reply-complete", turn: 1 });
    });
    expect(audio.finishReply).not.toHaveBeenCalled();
    act(() => socket.emit({ type: "reply-complete", turn: 2 }));
    expect(audio.finishReply).toHaveBeenCalledTimes(1);
    expect(result.current.active).toBe(true);
  });

  it("identifies playback failures separately from invalid server events", async () => {
    const { result } = renderHook(() => useConciergeVoice(vi.fn()));
    await act(() => result.current.start());
    const socket = Socket.instances[0];
    if (!socket) throw new Error("Expected voice socket");
    act(() => socket.emit({ type: "ready" }));
    audio.play.mockImplementationOnce(() => {
      throw new Error("Voice playback is falling behind");
    });
    act(() => socket.onmessage?.({ data: new ArrayBuffer(4800) }));
    expect(result.current.error).toBe(
      "Voice audio couldn't play. Please reconnect.",
    );
    expect(audio.close).toHaveBeenCalled();
    expect(socket.close).toHaveBeenCalled();
  });

  it("opens navigation once only after same-turn grounding and ignores events after end", async () => {
    const action = vi.fn();
    const { result } = renderHook(() => useConciergeVoice(action));
    await act(() => result.current.start());
    const socket = Socket.instances[0];
    if (!socket) throw new Error("Expected voice socket");
    act(() => socket.emit({ type: "ready" }));
    expect(result.current.status).toBe("listening");
    const command = {
      type: "ui-action",
      turn: 1,
      action: { type: "open_movie", movieId: 42 },
    };
    act(() => socket.emit(command));
    expect(action).not.toHaveBeenCalled();
    act(() =>
      socket.emit({
        type: "movie-card",
        turn: 1,
        movie: {
          movieId: 42,
          primaryTitle: "Forrest Gump",
          movieType: "MOVIE",
          genres: [],
        },
      }),
    );
    act(() => {
      socket.emit(command);
      socket.emit(command);
    });
    expect(action).toHaveBeenCalledTimes(1);
    act(() => result.current.end());
    act(() => socket.emit({ type: "error", text: "stale" }));
    expect(result.current.status).toBe("idle");
    expect(audio.close).toHaveBeenCalled();
    expect(socket.close).toHaveBeenCalled();
  });

  it("stops capture on unmount and never connects after a late permission response", async () => {
    let finish: (() => void) | undefined;
    audio.start.mockImplementationOnce(
      () =>
        new Promise<void>((resolve) => {
          finish = resolve;
        }),
    );
    const { result, unmount } = renderHook(() => useConciergeVoice(vi.fn()));
    let start: Promise<void> | undefined;
    act(() => {
      start = result.current.start();
    });
    unmount();
    await act(async () => {
      finish?.();
      await start;
    });
    expect(audio.close).toHaveBeenCalled();
    expect(Socket.instances).toHaveLength(0);
  });

  it("shows actionable permission errors and releases the audio context", async () => {
    audio.start.mockRejectedValueOnce(
      new DOMException("denied", "NotAllowedError"),
    );
    const { result } = renderHook(() => useConciergeVoice(vi.fn()));
    await act(() => result.current.start());
    expect(result.current.error).toContain("macOS");
    expect(result.current.active).toBe(false);
    expect(audio.close).toHaveBeenCalled();
  });

  it("mutes capture, interrupts playback and closes on connection loss", async () => {
    const { result } = renderHook(() => useConciergeVoice(vi.fn()));
    await act(() => result.current.start());
    const socket = Socket.instances[0];
    if (!socket) throw new Error("Expected voice socket");
    act(() => socket.emit({ type: "ready" }));
    act(() => result.current.toggleMute());
    expect(audio.mute).toHaveBeenCalledWith(true);
    expect(socket.send).toHaveBeenCalledWith('{"type":"mute"}');
    act(() => result.current.interrupt());
    expect(audio.interrupt).toHaveBeenCalled();
    act(() => socket.onclose?.());
    expect(result.current.status).toBe("error");
    expect(audio.close).toHaveBeenCalled();
  });

  it.each(["open_movie", "open_movie_trailer"])(
    "drops audio and %s arriving after a local interruption",
    async (type) => {
      const action = vi.fn();
      const { result } = renderHook(() => useConciergeVoice(action));
      await act(() => result.current.start());
      const socket = Socket.instances[0];
      if (!socket) throw new Error("Expected voice socket");
      const movie = {
        movieId: 42,
        primaryTitle: "Forrest Gump",
        movieType: "MOVIE",
        genres: [],
      };
      act(() => {
        socket.emit({ type: "ready" });
        socket.emit({ type: "interrupt", turn: 1 });
        socket.emit({ type: "movie-card", turn: 1, movie });
      });
      act(() => result.current.interrupt());
      act(() => {
        socket.onmessage?.({ data: new ArrayBuffer(4800) });
        socket.emit({
          type: "ui-action",
          turn: 1,
          action: { type, movieId: 42 },
        });
      });
      expect(audio.play).not.toHaveBeenCalled();
      expect(action).not.toHaveBeenCalled();
      act(() => {
        socket.emit({ type: "interrupt", turn: 2 });
        socket.emit({ type: "movie-card", turn: 2, movie });
        socket.emit({
          type: "ui-action",
          turn: 2,
          action: { type, movieId: 42 },
        });
      });
      expect(action).toHaveBeenCalledTimes(1);
    },
  );
});

it("connects after microphone permission while audio initializes, but waits for both before listening", async () => {
  let finishAudio: (() => void) | undefined;
  let sendAudio: ((chunk: ArrayBuffer) => void) | undefined;
  audio.start.mockImplementationOnce(
    (
      send: (chunk: ArrayBuffer) => void,
      _ended: () => void,
      granted: () => void,
    ) => {
      sendAudio = send;
      granted();
      return new Promise<void>((resolve) => {
        finishAudio = resolve;
      });
    },
  );
  const { result } = renderHook(() => useConciergeVoice(vi.fn()));
  let starting: Promise<void> | undefined;
  await act(async () => {
    starting = result.current.start();
  });
  const socket = Socket.instances[0];
  if (!socket) throw new Error("Expected voice socket");
  act(() => socket.emit({ type: "ready" }));
  act(() => socket.emit({ type: "status", status: "listening" }));
  expect(result.current.status).toBe("connecting");
  sendAudio?.(new ArrayBuffer(4800));
  expect(socket.send).not.toHaveBeenCalled();
  await act(async () => {
    finishAudio?.();
    await starting;
  });
  expect(result.current.status).toBe("listening");
  const chunk = new ArrayBuffer(4800);
  sendAudio?.(chunk);
  expect(socket.send).toHaveBeenCalledWith(chunk);
  act(() => result.current.end());
});

it("closes an overlapping connection on audio failure and ignores a late ready event", async () => {
  let failStartup: ((error: Error) => void) | undefined;
  audio.start.mockImplementationOnce(
    (_send: unknown, _ended: unknown, granted: () => void) => {
      granted();
      return new Promise<void>((_resolve, reject) => {
        failStartup = reject;
      });
    },
  );
  const { result } = renderHook(() => useConciergeVoice(vi.fn()));
  let starting: Promise<void> | undefined;
  await act(async () => {
    starting = result.current.start();
  });
  const socket = Socket.instances[0];
  if (!socket) throw new Error("Expected voice socket");
  await act(async () => {
    failStartup?.(new Error("Audio device unavailable"));
    await starting;
  });
  expect(socket.close).toHaveBeenCalled();
  expect(audio.close).toHaveBeenCalled();
  act(() => socket.emit({ type: "ready" }));
  expect(result.current.status).toBe("error");
});

it("ending during parallel startup cannot restart listening after audio resolves", async () => {
  let finishAudio: (() => void) | undefined;
  audio.start.mockImplementationOnce(
    (_send: unknown, _ended: unknown, granted: () => void) => {
      granted();
      return new Promise<void>((resolve) => {
        finishAudio = resolve;
      });
    },
  );
  const { result } = renderHook(() => useConciergeVoice(vi.fn()));
  let starting: Promise<void> | undefined;
  await act(async () => {
    starting = result.current.start();
  });
  const socket = Socket.instances[0];
  if (!socket) throw new Error("Expected voice socket");
  act(() => {
    socket.emit({ type: "ready" });
    result.current.end();
  });
  await act(async () => {
    finishAudio?.();
    await starting;
  });
  expect(result.current.status).toBe("idle");
  expect(socket.close).toHaveBeenCalled();
});

it("sends the current page after start and updates it without reconnecting", async () => {
  const action = vi.fn();
  const { result, rerender } = renderHook(
    ({ id }) => useConciergeVoice(action, { page: "movie", movieId: id }),
    { initialProps: { id: 6 } },
  );
  await act(() => result.current.start());
  const socket = Socket.instances[0];
  if (!socket) throw new Error("Expected socket");
  rerender({ id: 7 });
  expect(socket.send).not.toHaveBeenCalled();
  act(() => socket.onopen?.());
  expect(
    socket.send.mock.calls.map(
      ([message]) => JSON.parse(message as string) as object,
    ),
  ).toEqual([
    { type: "start", delegation: null },
    { type: "context", context: { page: "movie", movieId: 7 } },
  ]);
  rerender({ id: 8 });
  expect(socket.send).toHaveBeenLastCalledWith(
    JSON.stringify({ type: "context", context: { page: "movie", movieId: 8 } }),
  );
  expect(Socket.instances).toHaveLength(1);
});
