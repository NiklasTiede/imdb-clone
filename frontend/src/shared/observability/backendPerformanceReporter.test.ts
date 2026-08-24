import type { PerformanceEvent } from "./types";
import {
  createBackendPerformanceReporter,
  toFrontendTelemetryEvent,
} from "./backendPerformanceReporter";

const context = {
  appName: "imdb-clone-frontend" as const,
  appVersion: "test",
  environment: "test",
};

describe("backendPerformanceReporter", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("maps browser signals to a bounded contract without sensitive fields", () => {
    const apiEvent: PerformanceEvent = {
      context: { ...context, route: "/movie-search?q=private" },
      method: "GET",
      name: "api_request",
      status: 200,
      success: true,
      timestamp: 20,
      type: "api_request",
      url: "/api/search/movies?q=private",
      value: 123.4,
    };
    const errorEvent: PerformanceEvent = {
      context,
      errorType: "TypeError",
      message: "private error text",
      name: "browser_error",
      source: "https://example.test/private.js",
      timestamp: 30,
      type: "browser_error",
    };

    const payload = [
      toFrontendTelemetryEvent(apiEvent),
      toFrontendTelemetryEvent(errorEvent),
    ];

    expect(payload).toEqual([
      {
        name: "API_REQUEST",
        operation: "SEARCH",
        outcome: "SUCCESS",
        type: "API_REQUEST",
        value: 123.4,
      },
      { name: "BROWSER_ERROR", type: "BROWSER_ERROR" },
    ]);
    expect(JSON.stringify(payload)).not.toContain("private");
    expect(JSON.stringify(payload)).not.toContain("TypeError");

    expect(
      toFrontendTelemetryEvent({
        ...apiEvent,
        url: "/concierge-api/v1/conversations/private-id/messages",
      }),
    ).toMatchObject({ operation: "CONCIERGE" });
  });

  it("maps concierge actions without movie, route, or conversation identifiers", () => {
    const payload = toFrontendTelemetryEvent({
      context: { ...context, route: "/private/path" },
      name: "open_movie",
      outcome: "executed",
      timestamp: 30,
      type: "concierge_ui_action",
    });

    expect(payload).toEqual({
      name: "OPEN_MOVIE",
      type: "UI_ACTION",
      uiActionOutcome: "EXECUTED",
    });
    expect(JSON.stringify(payload)).not.toContain("private");
    expect(JSON.stringify(payload)).not.toContain("movieId");
  });

  it("batches events and flushes without blocking the caller", async () => {
    vi.useFakeTimers();
    const sender = vi.fn().mockResolvedValue(undefined);
    const delivery = createBackendPerformanceReporter({
      flushIntervalMs: 100,
      maxBatchSize: 2,
      sender,
    });
    const event: PerformanceEvent = {
      context,
      name: "app_boot",
      timestamp: 10,
      type: "app_boot",
      value: 42,
    };

    void delivery.reporter.report(event);
    expect(sender).not.toHaveBeenCalled();
    void delivery.reporter.report(event);

    expect(sender).toHaveBeenCalledWith(
      {
        events: [
          { name: "APP_BOOT", type: "APP_BOOT", value: 42 },
          { name: "APP_BOOT", type: "APP_BOOT", value: 42 },
        ],
      },
      false,
    );

    await vi.runAllTimersAsync();
    delivery.dispose();
  });

  it("drops unknown navigation timings and out-of-range measurements", () => {
    expect(
      toFrontendTelemetryEvent({
        context,
        from: "/",
        name: "route_navigation",
        timestamp: 5,
        to: "/movie-search",
        type: "route_navigation",
        value: 0,
      }),
    ).toBeUndefined();
    expect(
      toFrontendTelemetryEvent({
        context,
        id: "LCP-test",
        name: "LCP",
        navigationType: "navigate",
        rating: "poor",
        timestamp: 5,
        type: "web_vital",
        value: 200_000,
      }),
    ).toBeUndefined();
  });

  it("never lets a synchronous transport failure reach the user path", () => {
    const delivery = createBackendPerformanceReporter({
      maxBatchSize: 1,
      sender: () => {
        throw new Error("collector unavailable");
      },
    });

    expect(() =>
      delivery.reporter.report({
        context,
        name: "app_boot",
        timestamp: 10,
        type: "app_boot",
        value: 42,
      }),
    ).not.toThrow();
  });
});
