import { createPerformanceEventContext } from "./config";
import { reportPerformanceEvent } from "./performanceReporter";

type BrowserErrorCleanup = () => void;

let activeCleanup: BrowserErrorCleanup | null = null;

const isError = (value: unknown): value is Error => value instanceof Error;

const errorMessageFromReason = (reason: unknown): string => {
  if (isError(reason)) {
    return reason.message || "Unhandled promise rejection";
  }

  if (typeof reason === "string" && reason.trim() !== "") {
    return reason;
  }

  return "Unhandled promise rejection";
};

const errorTypeFromReason = (reason: unknown): string | undefined =>
  isError(reason) ? reason.name : undefined;

export const registerBrowserErrorReporting = (): BrowserErrorCleanup => {
  if (activeCleanup !== null) {
    return activeCleanup;
  }

  const reportErrorEvent = (event: ErrorEvent): void => {
    reportPerformanceEvent({
      column: event.colno || undefined,
      context: createPerformanceEventContext(window.location.pathname),
      errorType: errorTypeFromReason(event.error),
      line: event.lineno || undefined,
      message: event.message || "Browser error",
      name: "browser_error",
      source: event.filename || undefined,
      timestamp: performance.now(),
      type: "browser_error",
    });
  };

  const reportUnhandledRejection = (event: PromiseRejectionEvent): void => {
    reportPerformanceEvent({
      context: createPerformanceEventContext(window.location.pathname),
      errorType: errorTypeFromReason(event.reason),
      message: errorMessageFromReason(event.reason),
      name: "unhandled_rejection",
      timestamp: performance.now(),
      type: "browser_error",
    });
  };

  window.addEventListener("error", reportErrorEvent);
  window.addEventListener("unhandledrejection", reportUnhandledRejection);

  activeCleanup = () => {
    window.removeEventListener("error", reportErrorEvent);
    window.removeEventListener("unhandledrejection", reportUnhandledRejection);
    activeCleanup = null;
  };

  return activeCleanup;
};
