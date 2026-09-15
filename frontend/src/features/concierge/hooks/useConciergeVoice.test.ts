import { installLocalStorageMock } from "../../../test/installLocalStorageMock";
import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useConciergeVoice } from "./useConciergeVoice";
import { getConciergeIdentity } from "../api/delegation";

vi.mock("../api/delegation", () => ({ getConciergeIdentity: vi.fn() }));

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
  installLocalStorageMock();
  vi.clearAllMocks();
  vi.mocked(getConciergeIdentity).mockResolvedValue({
    accountId: null,
    delegation: null,
  });
  audio.start.mockResolvedValue(undefined);
  audio.levels.mockReturnValue({ input: 0, output: 0, playing: false });
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
  it("accepts a grounded movie correction in one turn without replaying duplicates or receipts", async () => {
    const action = vi.fn();
    const { result } = renderHook(() => useConciergeVoice(action));
    await act(() => result.current.start());
    const socket = Socket.instances[0]!;
    const open = (movieId: number, turn = 1) =>
      socket.emit({
        type: "ui-action",
        turn,
        action: { type: "open_movie", movieId },
      });
    const ground = (movieId: number, turn = 1) =>
      socket.emit({
        type: "movie-card",
        turn,
        movie: {
          movieId,
          primaryTitle: "Fixture",
          movieType: "MOVIE",
          genres: [],
        },
      });
    act(() => {
      socket.emit({ type: "ready" });
      ground(6);
      open(6);
      open(6);
      open(7); // Missing same-turn evidence still rejects a correction.
    });
    expect(action).toHaveBeenCalledTimes(1);
    act(() => {
      ground(7);
      open(7);
      open(7);
      open(6); // A delayed duplicate must not restore the old destination.
      for (let i = 0; i < 2; i++)
        socket.emit({
          type: "ui-action",
          turn: 2,
          action: { type: "open_watchlist" },
        });
      ground(6, 2);
      open(6, 2); // Personal receipt navigation stays terminal for its turn.
    });
    expect(action.mock.calls).toEqual([
      [{ type: "open_movie", movieId: 6 }],
      [{ type: "open_movie", movieId: 7 }],
      [{ type: "open_watchlist" }],
    ]);
    act(() => result.current.end());
  });

  it("opens a movie and its trailer for separate Live tasks within one caption row", async () => {
    const action = vi.fn();
    const { result } = renderHook(() =>
      useConciergeVoice(action, undefined, null, "gpt-live-1"),
    );
    await act(() => result.current.start());
    const socket = Socket.instances[0]!;
    const movie = {
      movieId: 42,
      primaryTitle: "Forrest Gump",
      movieType: "MOVIE",
      genres: [],
    };
    act(() => socket.emit({ type: "ready" }));
    for (const [index, type] of [
      "open_movie",
      "open_movie_trailer",
    ].entries()) {
      const turn = index + 2;
      act(() => {
        socket.emit({
          type: "transcript",
          turn: 1,
          speaker: "user",
          text:
            index === 0
              ? "Open Forrest Gump."
              : "Open Forrest Gump. Now play its trailer.",
        });
        socket.emit({ type: "movie-card", turn, movie });
        socket.emit({ type: "ui-action", turn, action: { type, movieId: 42 } });
        socket.emit({ type: "ui-action", turn, action: { type, movieId: 42 } });
      });
    }
    expect(action.mock.calls).toEqual([
      [{ type: "open_movie", movieId: 42 }],
      [{ type: "open_movie_trailer", movieId: 42 }],
    ]);
    act(() => result.current.end());
  });

  it("selects GPT-Live explicitly and accepts audio after repeated manual interruptions", async () => {
    const { result } = renderHook(() =>
      useConciergeVoice(vi.fn(), undefined, null, "gpt-live-1"),
    );
    await act(() => result.current.start());
    const socket = Socket.instances[0]!;
    act(() => {
      socket.onopen?.();
      socket.emit({ type: "ready" });
    });
    expect(socket.send).toHaveBeenCalledWith(
      expect.stringContaining('"model":"gpt-live-1"'),
    );
    for (let i = 0; i < 3; i++) {
      act(() => {
        result.current.interrupt();
        socket.onmessage?.({ data: new ArrayBuffer(960) });
      });
    }
    expect(audio.play).toHaveBeenCalledTimes(3);
    expect(result.current.active).toBe(true);
    act(() => result.current.end());
  });
  it("keeps audio, history and the socket through login and uses the latest navigation handler", async () => {
    const beforeLogin = vi.fn();
    const afterLogin = vi.fn();
    const { result, rerender } = renderHook(
      ({
        accountId,
        onAction,
      }: {
        accountId: number | null;
        onAction: typeof beforeLogin;
      }) => useConciergeVoice(onAction, undefined, accountId),
      {
        initialProps: {
          accountId: null as number | null,
          onAction: beforeLogin,
        },
      },
    );
    await act(() => result.current.start());
    const socket = Socket.instances[0]!;
    act(() => {
      socket.onopen?.();
      socket.emit({ type: "ready" });
      socket.emit({
        type: "transcript",
        speaker: "user",
        text: "Find Forrest Gump",
        final: true,
        turn: 1,
      });
    });
    vi.mocked(getConciergeIdentity).mockResolvedValue({
      accountId: 17,
      delegation: "verified-login",
    });
    rerender({ accountId: 17, onAction: afterLogin });
    await waitFor(() =>
      expect(socket.send).toHaveBeenCalledWith(
        JSON.stringify({ type: "authenticate", delegation: "verified-login" }),
      ),
    );
    act(() => {
      socket.emit({ type: "authenticated" });
      socket.emit({
        type: "ui-action",
        turn: 2,
        action: { type: "open_page", destination: "watchlist" },
      });
    });
    expect(Socket.instances).toHaveLength(1);
    expect(audio.start).toHaveBeenCalledTimes(1);
    expect(audio.close).not.toHaveBeenCalled();
    expect(socket.close).not.toHaveBeenCalled();
    expect(
      result.current.turns.some((turn) => turn.text === "Find Forrest Gump"),
    ).toBe(true);
    expect(afterLogin).toHaveBeenCalledWith({
      type: "open_page",
      destination: "watchlist",
    });
    expect(beforeLogin).not.toHaveBeenCalled();
  });

  it("synchronizes a login that happens while the provider is still connecting", async () => {
    const { result, rerender } = renderHook(
      ({ accountId }: { accountId: number | null }) =>
        useConciergeVoice(vi.fn(), undefined, accountId),
      { initialProps: { accountId: null as number | null } },
    );
    await act(() => result.current.start());
    const socket = Socket.instances[0]!;
    act(() => socket.onopen?.());
    vi.mocked(getConciergeIdentity).mockResolvedValue({
      accountId: 17,
      delegation: "verified-login",
    });
    rerender({ accountId: 17 });
    expect(getConciergeIdentity).toHaveBeenCalledTimes(1);
    await act(async () => socket.emit({ type: "ready" }));
    expect(socket.send).toHaveBeenLastCalledWith(
      JSON.stringify({ type: "authenticate", delegation: "verified-login" }),
    );
    act(() =>
      socket.emit({
        type: "authentication-failed",
        text: "Please restart voice to verify sign-in.",
      }),
    );
    expect(result.current.active).toBe(true);
    expect(result.current.notice).toContain("verify sign-in");
    expect(socket.close).not.toHaveBeenCalled();
  });

  it("discards a login credential that arrives after voice was stopped", async () => {
    const { result, rerender } = renderHook(
      ({ accountId }: { accountId: number | null }) =>
        useConciergeVoice(vi.fn(), undefined, accountId),
      { initialProps: { accountId: null as number | null } },
    );
    await act(() => result.current.start());
    const socket = Socket.instances[0]!;
    act(() => {
      socket.onopen?.();
      socket.emit({ type: "ready" });
    });
    let resolveLogin!: (identity: {
      accountId: number;
      delegation: string;
    }) => void;
    vi.mocked(getConciergeIdentity).mockReturnValue(
      new Promise((resolve) => {
        resolveLogin = resolve;
      }),
    );
    rerender({ accountId: 17 });
    act(() => result.current.end());
    await act(async () =>
      resolveLogin({ accountId: 17, delegation: "late-login" }),
    );
    expect(
      socket.send.mock.calls.some(([value]) =>
        String(value).includes("late-login"),
      ),
    ).toBe(false);
  });

  it("releases the microphone and permits restart when the final socket write fails", async () => {
    const { result } = renderHook(() => useConciergeVoice(vi.fn()));
    await act(() => result.current.start());
    const socket = Socket.instances[0]!;
    act(() => socket.emit({ type: "ready" }));
    socket.send.mockImplementationOnce(() => {
      throw new Error("Connection closed during shutdown");
    });
    act(() => result.current.end());
    expect(audio.close).toHaveBeenCalledTimes(1);
    expect(socket.close).toHaveBeenCalledTimes(1);
    expect(result.current.active).toBe(false);
    expect(result.current.error).toBeNull();
    await act(() => result.current.start());
    act(() => Socket.instances[1]!.emit({ type: "ready" }));
    expect(result.current.active).toBe(true);
    expect(audio.start).toHaveBeenCalledTimes(2);
  });

  it("sends text on the ready voice socket and resumes audio after server acknowledgement", async () => {
    const { result } = renderHook(() => useConciergeVoice(vi.fn()));
    expect(result.current.sendText("Hello")).toBe(false);
    await act(() => result.current.start());
    expect(result.current.sendText("Hello")).toBe(false);
    const socket = Socket.instances[0]!;
    act(() => socket.emit({ type: "ready" }));
    act(() => {
      expect(result.current.sendText("   ")).toBe(false);
      expect(result.current.sendText("x".repeat(601))).toBe(false);
      expect(result.current.sendText("  Open it  ")).toBe(true);
      expect(result.current.sendText("Open it")).toBe(false);
    });
    expect(socket.send).toHaveBeenCalledWith(
      JSON.stringify({ type: "text", text: "Open it" }),
    );
    expect(result.current.canSendText).toBe(false);
    act(() => socket.onmessage?.({ data: new ArrayBuffer(4800) }));
    expect(audio.play).not.toHaveBeenCalled();
    act(() => {
      socket.emit({ type: "interrupt", turn: 1 });
      socket.emit({
        type: "transcript",
        turn: 1,
        speaker: "user",
        text: "Open it",
        final: true,
      });
      socket.onmessage?.({ data: new ArrayBuffer(4800) });
    });
    expect(
      result.current.turns
        .filter((turn) => turn.role === "user")
        .map((turn) => turn.text),
    ).toEqual(["Open it"]);
    expect(audio.play).toHaveBeenCalledTimes(1);
    expect(result.current.canSendText).toBe(true);
    expect(Socket.instances).toHaveLength(1);
    act(() => result.current.end());
    expect(result.current.sendText("Hello")).toBe(false);
  });

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
    expect(result.current.error).toContain("browser's site permissions");
    expect(result.current.error).toContain("device settings");
    expect(result.current.error).not.toContain("macOS");
    expect(result.current.active).toBe(false);
    expect(audio.close).toHaveBeenCalled();
    expect(Socket.instances).toHaveLength(0);
  });

  it("distinguishes an unavailable microphone from denied permission", async () => {
    audio.start.mockRejectedValueOnce(
      new DOMException("device unavailable", "NotReadableError"),
    );
    const { result } = renderHook(() => useConciergeVoice(vi.fn()));
    await act(() => result.current.start());
    expect(result.current.error).toContain("microphone couldn't be opened");
    expect(result.current.active).toBe(false);
    expect(audio.close).toHaveBeenCalled();
    expect(Socket.instances).toHaveLength(0);
  });

  it("explains missing browser audio support without requesting a session", async () => {
    vi.stubGlobal("navigator", {});
    const { result } = renderHook(() => useConciergeVoice(vi.fn()));
    await act(() => result.current.start());
    expect(result.current.error).toContain("HTTPS or localhost");
    expect(result.current.error).toContain("inside another app");
    expect(result.current.active).toBe(false);
    expect(audio.start).not.toHaveBeenCalled();
    expect(Socket.instances).toHaveLength(0);
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
  const greeting = [new ArrayBuffer(960), new ArrayBuffer(1920)];
  act(() => {
    greeting.forEach((data) => socket.onmessage?.({ data }));
    socket.emit({
      type: "transcript",
      speaker: "assistant",
      text: "Welcome!",
      turn: 0,
    });
    socket.emit({ type: "reply-complete", turn: 0 });
  });
  expect(audio.play).not.toHaveBeenCalled();
  expect(audio.finishReply).not.toHaveBeenCalled();
  expect(result.current.status).toBe("connecting");
  sendAudio?.(new ArrayBuffer(4800));
  expect(socket.send).not.toHaveBeenCalled();
  await act(async () => {
    finishAudio?.();
    await starting;
  });
  expect(result.current.status).toBe("listening");
  expect(audio.play.mock.calls).toEqual(greeting.map((chunk) => [chunk]));
  expect(audio.finishReply).toHaveBeenCalledOnce();
  expect(result.current.turns.some((turn) => turn.text === "Welcome!")).toBe(
    true,
  );
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
  act(() => {
    socket.emit({ type: "ready" });
    socket.onmessage?.({ data: new ArrayBuffer(960) });
  });
  await act(async () => {
    failStartup?.(new Error("Audio device unavailable"));
    await starting;
  });
  expect(socket.close).toHaveBeenCalled();
  expect(audio.close).toHaveBeenCalled();
  expect(audio.play).not.toHaveBeenCalled();
  act(() => socket.emit({ type: "ready" }));
  expect(result.current.status).toBe("error");
});

it.each(["end", "provider-error", "overflow"])(
  "does not replay a startup greeting after %s",
  async (termination) => {
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
    const socket = Socket.instances[0]!;
    act(() => {
      socket.emit({ type: "ready" });
      socket.onmessage?.({ data: new ArrayBuffer(960) });
      if (termination === "end") result.current.end();
      else if (termination === "provider-error")
        socket.emit({ type: "error", text: "Provider unavailable" });
      else socket.onmessage?.({ data: new ArrayBuffer(512_000) });
    });
    await act(async () => {
      finishAudio?.();
      await starting;
    });
    expect(audio.play).not.toHaveBeenCalled();
    expect(socket.close).toHaveBeenCalled();
    expect(result.current.status).toBe(
      termination === "end" ? "idle" : "error",
    );
  },
);

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
    expect.objectContaining({
      type: "start",
      delegation: null,
      model: "grok",
      browser_id: expect.stringMatching(/^browser-/),
    }),
    { type: "context", context: { page: "movie", movieId: 7 } },
  ]);
  rerender({ id: 8 });
  expect(socket.send).toHaveBeenLastCalledWith(
    JSON.stringify({ type: "context", context: { page: "movie", movieId: 8 } }),
  );
  expect(Socket.instances).toHaveLength(1);
});

it("retains and updates the conversation across turns, end and reconnect", async () => {
  const { result } = renderHook(() =>
    useConciergeVoice(vi.fn(), {
      page: "movie",
      movieId: 42,
      streamingCountry: "CH",
    }),
  );
  await act(() => result.current.start());
  const socket = Socket.instances[0]!;
  act(() => {
    socket.emit({ type: "ready" });
    socket.emit({ type: "interrupt", turn: 1 });
    socket.emit({
      type: "transcript",
      turn: 1,
      speaker: "user",
      text: "Find Forrest",
    });
    socket.emit({
      type: "transcript",
      turn: 1,
      speaker: "user",
      text: "Find Forrest Gump",
      final: true,
    });
    socket.emit({
      type: "transcript",
      turn: 1,
      speaker: "assistant",
      text: "Here it is",
      final: true,
    });
    socket.emit({ type: "reply-complete", turn: 1 });
    socket.emit({ type: "interrupt", turn: 2 });
    socket.emit({
      type: "transcript",
      turn: 2,
      speaker: "user",
      text: "What is it about?",
      final: true,
    });
  });
  expect(result.current.turns.map((turn) => turn.text)).toEqual([
    "Find Forrest Gump",
    "Here it is",
    "What is it about?",
  ]);
  expect(result.current.turns[1]?.context).toEqual({
    page: "movie",
    streamingCountry: "CH",
  });
  act(() => result.current.end());
  await act(() => result.current.start());
  expect(result.current.turns).toHaveLength(3);
  act(() => result.current.clearHistory());
  expect(result.current.turns).toEqual([]);
});

it("marks an interrupted reply and ignores stale interruption events", async () => {
  const { result } = renderHook(() => useConciergeVoice(vi.fn()));
  await act(() => result.current.start());
  const socket = Socket.instances[0]!;
  act(() => {
    socket.emit({ type: "ready" });
    socket.emit({ type: "interrupt", turn: 2 });
    socket.emit({
      type: "transcript",
      turn: 2,
      speaker: "assistant",
      text: "Forrest Gump is…",
    });
  });
  audio.levels.mockReturnValue({ input: 0, output: 0.4, playing: true });
  audio.interrupt.mockClear();
  act(() => socket.emit({ type: "interrupt", turn: 1 }));
  expect(audio.interrupt).not.toHaveBeenCalled();
  act(() => result.current.interrupt());
  expect(result.current.turns[0]?.interrupted).toBe(true);
  act(() =>
    socket.emit({
      type: "transcript",
      turn: 2,
      speaker: "assistant",
      text: "Late words",
    }),
  );
  expect(result.current.turns[0]?.text).toBe("Forrest Gump is…");
  audio.levels.mockReturnValue({ input: 0, output: 0, playing: false });
});

it("records an action handler failure without killing voice", async () => {
  const { result } = renderHook(() =>
    useConciergeVoice(() => {
      throw new Error("Navigation failed");
    }),
  );
  await act(() => result.current.start());
  act(() => {
    Socket.instances[0]!.emit({ type: "ready" });
    Socket.instances[0]!.emit({
      type: "ui-action",
      turn: 1,
      action: { type: "open_page", destination: "home" },
    });
  });
  expect(result.current.turns[0]?.actions?.[0]?.outcome).toBe("rejected");
  expect(result.current.active).toBe(true);
  expect(result.current.error).toBeNull();
});

it("reads live amplitude without rendering the session on every audio frame", async () => {
  let renders = 0;
  const { result } = renderHook(() => {
    renders++;
    return useConciergeVoice(vi.fn());
  });
  await act(() => result.current.start());
  const baseline = { count: renders };
  audio.levels.mockReturnValue({ input: 0.7, output: 0, playing: false });
  const callback = vi.mocked(requestAnimationFrame).mock.calls.at(-1)?.[0];
  act(() => callback?.(100));
  expect(result.current.readLevels().input).toBe(0.7);
  expect(renders).toBe(baseline.count);
  audio.levels.mockReturnValue({ input: 0, output: 0, playing: false });
});

it("keeps listening after a rejected-write notice and clears it on the next user turn", async () => {
  const { result } = renderHook(() => useConciergeVoice(vi.fn()));
  await act(() => result.current.start());
  const socket = Socket.instances[0];
  if (!socket) throw new Error("Expected voice socket");
  act(() => {
    socket.emit({ type: "ready" });
    socket.emit({ type: "interrupt", turn: 1 });
    socket.emit({
      type: "status",
      status: "listening",
      turn: 1,
      text: "That change wasn't confirmed.",
    });
  });
  expect(result.current.active).toBe(true);
  expect(result.current.status).toBe("listening");
  expect(result.current.notice).toBe("That change wasn't confirmed.");
  expect(result.current.turns[0]?.error?.message).toBe(
    "That change wasn't confirmed.",
  );
  expect(socket.close).not.toHaveBeenCalled();
  act(() => socket.emit({ type: "interrupt", turn: 2 }));
  expect(result.current.notice).toBeNull();
  expect(result.current.active).toBe(true);
});

it("retains tool outcomes, personal scores and all seven retrieved movies", async () => {
  const { result } = renderHook(() => useConciergeVoice(vi.fn()));
  await act(() => result.current.start());
  const socket = Socket.instances[0]!;
  act(() => {
    socket.emit({ type: "ready" });
    socket.emit({ type: "interrupt", turn: 1 });
    socket.emit({
      type: "tool-activity",
      turn: 1,
      activity: { callId: "read", tool: "get_my_ratings", status: "started" },
    });
    for (let id = 1; id <= 7; id++)
      socket.emit({
        type: "movie-card",
        turn: 1,
        movie: {
          movieId: id,
          primaryTitle: `Movie ${id}`,
          movieType: "MOVIE",
          genres: [],
          userScore: 11 - id,
          imdbRating: 7,
        },
      });
    socket.emit({
      type: "tool-activity",
      turn: 1,
      activity: { callId: "read", tool: "get_my_ratings", status: "completed" },
    });
    socket.emit({
      type: "transcript",
      turn: 1,
      speaker: "assistant",
      text: "Movie 1 is your highest-rated movie.",
      final: true,
    });
  });
  expect(result.current.turns[0]?.tools).toEqual([
    { callId: "read", tool: "get_my_ratings", status: "completed" },
  ]);
  expect(result.current.turns[0]?.movies).toHaveLength(7);
  expect(result.current.turns[0]?.movies[0]).toMatchObject({
    movieId: 1,
    userScore: 10,
    imdbRating: 7,
  });
  expect(result.current.error).toBeNull();
});

it("handles inactivity as a closed standby state without an error and permits a fresh start", async () => {
  const { result } = renderHook(() => useConciergeVoice(vi.fn()));
  await act(() => result.current.start());
  const socket = Socket.instances[0]!;
  act(() => {
    socket.emit({ type: "ready" });
    socket.emit({
      type: "transcript",
      speaker: "user",
      turn: 1,
      text: "Play the trailer",
      final: true,
    });
    socket.emit({ type: "standby" });
    socket.onclose?.();
  });
  expect(result.current.status).toBe("standby");
  expect(result.current.active).toBe(false);
  expect(result.current.canSendText).toBe(false);
  expect(result.current.error).toBeNull();
  expect(result.current.notice).toBeNull();
  expect(result.current.turns).toHaveLength(1);
  expect(audio.close).toHaveBeenCalledOnce();
  expect(socket.close).toHaveBeenCalledOnce();
  await act(() => result.current.start());
  act(() => Socket.instances[1]!.emit({ type: "ready" }));
  expect(result.current.active).toBe(true);
  expect(result.current.status).toBe("listening");
});

it("reuses browser quota identity across models and displays quota warnings without ending voice", async () => {
  const { result, rerender } = renderHook(
    ({ model }: { model: "grok" | "gpt-live-1" }) =>
      useConciergeVoice(vi.fn(), undefined, null, model),
    { initialProps: { model: "grok" as "grok" | "gpt-live-1" } },
  );
  await act(() => result.current.start());
  const first = Socket.instances[0]!;
  act(() => first.onopen?.());
  const initial = JSON.parse(first.send.mock.calls[0]![0] as string) as {
    browser_id: string;
  };
  act(() => first.emit({ type: "ready" }));
  act(() =>
    first.emit({ type: "quota-warning", text: "Less than a minute remains." }),
  );
  expect(result.current.notice).toBe("Less than a minute remains.");
  expect(result.current.active).toBe(true);
  act(() => result.current.end());
  rerender({ model: "gpt-live-1" });
  await act(() => result.current.start());
  const second = Socket.instances[1]!;
  act(() => second.onopen?.());
  const next = JSON.parse(second.send.mock.calls[0]![0] as string) as {
    browser_id: string;
  };
  expect(next.browser_id).toBe(initial.browser_id);
});
