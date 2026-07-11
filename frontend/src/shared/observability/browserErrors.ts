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
    const errorType = errorTypeFromReason(event.error);
    reportPerformanceEvent({
      ...(event.colno ? { column: event.colno } : {}),
      context: createPerformanceEventContext(window.location.pathname),
      ...(errorType === undefined ? {} : { errorType }),
      ...(event.lineno ? { line: event.lineno } : {}),
      message: event.message || "Browser error",
      name: "browser_error",
      ...(event.filename ? { source: event.filename } : {}),
      timestamp: performance.now(),
      type: "browser_error",
    });
  };

  const reportUnhandledRejection = (event: PromiseRejectionEvent): void => {
    const errorType = errorTypeFromReason(event.reason);
    reportPerformanceEvent({
      context: createPerformanceEventContext(window.location.pathname),
      ...(errorType === undefined ? {} : { errorType }),
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
