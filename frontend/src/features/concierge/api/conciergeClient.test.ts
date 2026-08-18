import { ConciergeClientError, consumeEventStream } from "./conciergeClient";

const event = (value: object): string =>
  `event: message\r\nid: 1\r\ndata: ${JSON.stringify(value)}\r\n\r\n`;

const streamChunks = (...chunks: string[]): ReadableStream<Uint8Array> =>
  new ReadableStream({
    start(controller) {
      const encoder = new TextEncoder();
      chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)));
      controller.close();
    },
  });

describe("concierge event stream", () => {
  it("parses typed events split across CRLF chunk boundaries", async () => {
    const events: unknown[] = [];
    const payload =
      event({ type: "status", sequence: 1, status: "searching_catalog" }) +
      event({
        type: "movie-card",
        sequence: 2,
        movie: {
          movieId: 42,
          primaryTitle: "Arrival",
          movieType: "MOVIE",
          genres: ["DRAMA", "SCI_FI"],
        },
      });
    const splitAt = payload.indexOf("\r\n") + 1;

    await consumeEventStream(
      streamChunks(payload.slice(0, splitAt), payload.slice(splitAt)),
      (received) => events.push(received),
    );

    expect(events).toHaveLength(2);
    expect(events[1]).toMatchObject({
      type: "movie-card",
      movie: { movieId: 42, primaryTitle: "Arrival" },
    });
  });

  it("rejects malformed or out-of-order events", async () => {
    const payload =
      event({ type: "text", sequence: 2, delta: "First" }) +
      event({ type: "text", sequence: 2, delta: "Duplicate" });

    await expect(
      consumeEventStream(streamChunks(payload), () => undefined),
    ).rejects.toEqual(
      expect.objectContaining<Partial<ConciergeClientError>>({
        code: "protocol",
      }),
    );
  });
});
