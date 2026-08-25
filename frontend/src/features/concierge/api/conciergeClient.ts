import * as zod from "zod";
import { createPerformanceEventContext } from "../../../shared/observability/config";
import { reportPerformanceEvent } from "../../../shared/observability/performanceReporter";
import { conciergeEventSchema, type ConciergeEvent } from "../model/concierge";

const conversationResponseSchema = zod.object({
  conversationId: zod.string().regex(/^[a-f0-9]{32}$/),
});

const getBaseAddress = (): string =>
  import.meta.env.VITE_IMDB_CLONE_CONCIERGE_ADDRESS ?? "/concierge-api";

export class ConciergeClientError extends Error {
  constructor(
    public readonly code: "network" | "protocol" | "unavailable",
    message: string,
  ) {
    super(message);
    this.name = "ConciergeClientError";
  }
}

export const createConversation = async (
  clientId: string,
  signal: AbortSignal,
): Promise<string> => {
  const response = await safeFetch(`${getBaseAddress()}/v1/conversations`, {
    method: "POST",
    headers: clientHeaders(clientId),
    signal,
  });
  if (!response.ok) {
    throw unavailableError();
  }
  const parsed = conversationResponseSchema.safeParse(await response.json());
  if (!parsed.success) {
    throw protocolError();
  }
  return parsed.data.conversationId;
};

export const streamMessage = async ({
  clientId,
  conversationId,
  message,
  onEvent,
  signal,
}: {
  clientId: string;
  conversationId: string;
  message: string;
  onEvent: (event: ConciergeEvent) => void;
  signal: AbortSignal;
}): Promise<void> => {
  const response = await safeFetch(
    `${getBaseAddress()}/v1/conversations/${conversationId}/messages`,
    {
      method: "POST",
      headers: {
        ...clientHeaders(clientId),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ message }),
      signal,
    },
  );
  if (!response.ok || !response.body) {
    throw unavailableError();
  }
  let completed = false;
  await consumeEventStream(response.body, (event) => {
    if (completed) {
      throw protocolError();
    }
    if (event.type === "completion") {
      if (event.conversationId !== conversationId) {
        throw protocolError();
      }
      completed = true;
    }
    onEvent(event);
  });
  if (!completed) {
    throw protocolError();
  }
};

export const consumeEventStream = async (
  stream: ReadableStream<Uint8Array>,
  onEvent: (event: ConciergeEvent) => void,
): Promise<void> => {
  const reader = stream.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let previousSequence = 0;

  const consumeFrame = (frame: string) => {
    if (!frame.trim() || frame.startsWith(":")) {
      return;
    }
    const data = frame
      .split("\n")
      .filter((line) => line.startsWith("data:"))
      .map((line) => line.slice(5).trimStart())
      .join("\n");
    if (!data) {
      return;
    }

    let decoded: unknown;
    try {
      decoded = JSON.parse(data);
    } catch {
      throw protocolError();
    }
    const parsed = conciergeEventSchema.safeParse(decoded);
    if (!parsed.success || parsed.data.sequence <= previousSequence) {
      throw protocolError();
    }
    previousSequence = parsed.data.sequence;
    onEvent(parsed.data);
  };

  try {
    while (true) {
      const { done, value } = await reader.read();
      buffer =
        `${buffer}${decoder.decode(value, { stream: !done })}`.replaceAll(
          "\r\n",
          "\n",
        );
      let boundary = buffer.indexOf("\n\n");
      while (boundary >= 0) {
        consumeFrame(buffer.slice(0, boundary));
        buffer = buffer.slice(boundary + 2);
        boundary = buffer.indexOf("\n\n");
      }
      if (done) {
        if (buffer.trim()) {
          consumeFrame(buffer);
        }
        return;
      }
    }
  } finally {
    reader.releaseLock();
  }
};

const clientHeaders = (clientId: string): Record<string, string> => ({
  Accept: "text/event-stream, application/json",
  "X-Concierge-Client-ID": clientId,
});

const safeFetch = async (
  input: RequestInfo | URL,
  init: RequestInit,
): Promise<Response> => {
  const startedAt = performance.now();
  const telemetryUrl =
    typeof input === "string"
      ? input
      : input instanceof URL
        ? input.href
        : input.url;
  try {
    const response = await fetch(input, init);
    const finishedAt = performance.now();
    reportPerformanceEvent({
      context: createPerformanceEventContext(window.location.pathname),
      ...(response.ok ? {} : { failureKind: "http" as const }),
      method: init.method ?? "GET",
      name: "api_request",
      status: response.status,
      success: response.ok,
      timestamp: finishedAt,
      type: "api_request",
      url: telemetryUrl,
      value: Math.max(0, finishedAt - startedAt),
    });
    return response;
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw error;
    }
    const finishedAt = performance.now();
    reportPerformanceEvent({
      context: createPerformanceEventContext(window.location.pathname),
      failureKind: "network",
      method: init.method ?? "GET",
      name: "api_request",
      success: false,
      timestamp: finishedAt,
      type: "api_request",
      url: telemetryUrl,
      value: Math.max(0, finishedAt - startedAt),
    });
    throw new ConciergeClientError(
      "network",
      "The Movie Concierge could not be reached.",
    );
  }
};

const protocolError = () =>
  new ConciergeClientError(
    "protocol",
    "The Movie Concierge returned an invalid response.",
  );

const unavailableError = () =>
  new ConciergeClientError(
    "unavailable",
    "The Movie Concierge is temporarily unavailable.",
  );
