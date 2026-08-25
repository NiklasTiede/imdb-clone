import { isObservabilityEnabled, shouldUseConsoleReporter } from "./config";
import { configurePerformanceReporter } from "./performanceReporter";
import type { PerformanceEvent, PerformanceReporter } from "./types";

type FrontendTelemetryEvent = {
  type:
    | "API_REQUEST"
    | "APP_BOOT"
    | "BROWSER_ERROR"
    | "UI_ACTION"
    | "ROUTE_NAVIGATION"
    | "WEB_VITAL";
  name:
    | "API_REQUEST"
    | "APP_BOOT"
    | "BROWSER_ERROR"
    | "CLS"
    | "FCP"
    | "INP"
    | "LCP"
    | "OPEN_MOVIE"
    | "ROUTE_NAVIGATION"
    | "TTFB"
    | "UNHANDLED_REJECTION";
  value?: number;
  rating?: "GOOD" | "NEEDS_IMPROVEMENT" | "POOR";
  operation?:
    | "ACCOUNT"
    | "AUTHENTICATION"
    | "CATALOG"
    | "CONCIERGE"
    | "ENGAGEMENT"
    | "MEDIA"
    | "OTHER"
    | "RECOMMENDATIONS"
    | "SEARCH"
    | "WEBAUTHN";
  outcome?:
    | "HTTP_4XX"
    | "HTTP_5XX"
    | "NETWORK_ERROR"
    | "OTHER_ERROR"
    | "SUCCESS";
  uiActionOutcome?: "EXECUTED" | "REJECTED";
};

type FrontendTelemetryBatch = {
  events: FrontendTelemetryEvent[];
};

type BatchSender = (
  batch: FrontendTelemetryBatch,
  preferBeacon: boolean,
) => void | Promise<void>;

type BackendPerformanceReporterOptions = {
  flushIntervalMs?: number;
  maxBatchSize?: number;
  sender?: BatchSender;
};

type BackendPerformanceDelivery = {
  dispose: () => void;
  flush: (preferBeacon?: boolean) => void;
  reporter: PerformanceReporter;
};

const TELEMETRY_ENDPOINT = "/api/observability/frontend";
const DEFAULT_BATCH_SIZE = 12;
const MAX_SERVER_BATCH_SIZE = 20;
const DEFAULT_FLUSH_INTERVAL_MS = 2_000;

const noOpCleanup = (): void => undefined;

const asFiniteValue = (
  value: number | undefined,
  maximum: number,
  requirePositive = false,
): number | undefined => {
  if (
    value === undefined ||
    !Number.isFinite(value) ||
    value < 0 ||
    value > maximum ||
    (requirePositive && value === 0)
  ) {
    return undefined;
  }
  return value;
};

const operationFromUrl = (
  url: string,
): NonNullable<FrontendTelemetryEvent["operation"]> => {
  let pathname: string;
  try {
    pathname = new URL(url, window.location.origin).pathname;
  } catch {
    return "OTHER";
  }

  if (pathname.startsWith("/api/auth/")) return "AUTHENTICATION";
  if (pathname.startsWith("/concierge-api/")) return "CONCIERGE";
  if (pathname.startsWith("/api/account/passkeys")) return "WEBAUTHN";
  if (pathname.startsWith("/api/account/")) return "ACCOUNT";
  if (pathname.startsWith("/api/movie/")) return "CATALOG";
  if (pathname.startsWith("/api/search/")) return "SEARCH";
  if (pathname.startsWith("/api/recommendations/")) return "RECOMMENDATIONS";
  if (
    pathname.startsWith("/api/comment/") ||
    pathname.startsWith("/api/movie-rating/") ||
    pathname.startsWith("/api/watched-movie/")
  ) {
    return "ENGAGEMENT";
  }
  if (pathname.startsWith("/api/file-storage/")) return "MEDIA";
  if (pathname.startsWith("/webauthn/") || pathname === "/login/webauthn") {
    return "WEBAUTHN";
  }
  return "OTHER";
};

const outcomeFromApiEvent = (
  event: Extract<PerformanceEvent, { type: "api_request" }>,
): NonNullable<FrontendTelemetryEvent["outcome"]> => {
  if (event.success) return "SUCCESS";
  if (event.status !== undefined && event.status >= 500) return "HTTP_5XX";
  if (event.status !== undefined && event.status >= 400) return "HTTP_4XX";
  if (event.failureKind === "network") return "NETWORK_ERROR";
  return "OTHER_ERROR";
};

export const toFrontendTelemetryEvent = (
  event: PerformanceEvent,
): FrontendTelemetryEvent | undefined => {
  switch (event.type) {
    case "web_vital": {
      const maximum = event.name === "CLS" ? 10 : 120_000;
      const value = asFiniteValue(event.value, maximum);
      if (value === undefined || event.rating === undefined) return undefined;
      return {
        type: "WEB_VITAL",
        name: event.name,
        rating: event.rating.replace("-", "_").toUpperCase() as NonNullable<
          FrontendTelemetryEvent["rating"]
        >,
        value,
      };
    }
    case "app_boot": {
      const value = asFiniteValue(event.value, 60_000);
      return value === undefined
        ? undefined
        : { type: "APP_BOOT", name: "APP_BOOT", value };
    }
    case "route_navigation": {
      const value = asFiniteValue(event.value, 30_000, true);
      return value === undefined
        ? undefined
        : { type: "ROUTE_NAVIGATION", name: "ROUTE_NAVIGATION", value };
    }
    case "api_request": {
      const value = asFiniteValue(event.value, 120_000);
      return value === undefined
        ? undefined
        : {
            type: "API_REQUEST",
            name: "API_REQUEST",
            operation: operationFromUrl(event.url),
            outcome: outcomeFromApiEvent(event),
            value,
          };
    }
    case "browser_error":
      return {
        type: "BROWSER_ERROR",
        name:
          event.name === "unhandled_rejection"
            ? "UNHANDLED_REJECTION"
            : "BROWSER_ERROR",
      };
    case "concierge_ui_action":
      return {
        type: "UI_ACTION",
        name: "OPEN_MOVIE",
        uiActionOutcome: event.outcome.toUpperCase() as NonNullable<
          FrontendTelemetryEvent["uiActionOutcome"]
        >,
      };
    case "discovery_interaction":
      return undefined;
  }
};

const defaultSender: BatchSender = async (batch, preferBeacon) => {
  const body = JSON.stringify(batch);

  if (
    preferBeacon &&
    typeof navigator.sendBeacon === "function" &&
    navigator.sendBeacon(
      TELEMETRY_ENDPOINT,
      new Blob([body], { type: "application/json" }),
    )
  ) {
    return;
  }

  await fetch(TELEMETRY_ENDPOINT, {
    body,
    credentials: "omit",
    headers: { "Content-Type": "application/json" },
    keepalive: true,
    method: "POST",
  }).then(() => undefined);
};

export const createBackendPerformanceReporter = ({
  flushIntervalMs = DEFAULT_FLUSH_INTERVAL_MS,
  maxBatchSize = DEFAULT_BATCH_SIZE,
  sender = defaultSender,
}: BackendPerformanceReporterOptions = {}): BackendPerformanceDelivery => {
  const boundedBatchSize = Math.min(
    Math.max(1, maxBatchSize),
    MAX_SERVER_BATCH_SIZE,
  );
  const queue: FrontendTelemetryEvent[] = [];
  let timer: ReturnType<typeof setTimeout> | undefined;

  const scheduleFlush = (): void => {
    timer ??= setTimeout(() => flush(false), flushIntervalMs);
  };

  const flush = (preferBeacon = false): void => {
    if (timer !== undefined) {
      clearTimeout(timer);
      timer = undefined;
    }
    if (queue.length === 0) return;

    const events = queue.splice(0, MAX_SERVER_BATCH_SIZE);
    try {
      void Promise.resolve(sender({ events }, preferBeacon)).catch(noOpCleanup);
    } catch {
      // Telemetry delivery must never escape into the user path.
    }

    if (queue.length > 0) scheduleFlush();
  };

  const reporter: PerformanceReporter = {
    report: (event) => {
      const telemetryEvent = toFrontendTelemetryEvent(event);
      if (telemetryEvent === undefined) return;

      queue.push(telemetryEvent);
      if (queue.length >= boundedBatchSize) {
        flush(false);
      } else {
        scheduleFlush();
      }
    },
  };

  return {
    dispose: () => flush(true),
    flush,
    reporter,
  };
};

export const registerBackendPerformanceReporting = (): (() => void) => {
  if (!isObservabilityEnabled() || shouldUseConsoleReporter()) {
    return noOpCleanup;
  }

  const delivery = createBackendPerformanceReporter();
  configurePerformanceReporter(delivery.reporter);

  const flushForPageExit = (): void => delivery.flush(true);
  window.addEventListener("pagehide", flushForPageExit);

  return () => {
    window.removeEventListener("pagehide", flushForPageExit);
    delivery.dispose();
  };
};
