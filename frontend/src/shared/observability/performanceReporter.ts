import { isObservabilityEnabled, shouldUseConsoleReporter } from "./config";
import type { PerformanceEvent, PerformanceReporter } from "./types";

const consolePerformanceReporter: PerformanceReporter = {
  report: (event) => {
    console.info("[frontend-observability]", event);
  },
};

const noopPerformanceReporter: PerformanceReporter = {
  report: () => undefined,
};

let activeReporter: PerformanceReporter =
  isObservabilityEnabled() && shouldUseConsoleReporter()
    ? consolePerformanceReporter
    : noopPerformanceReporter;

export const configurePerformanceReporter = (
  reporter: PerformanceReporter,
): void => {
  activeReporter = reporter;
};

const warnReporterFailure = (error: unknown): void => {
  if (import.meta.env.DEV) {
    console.warn("[frontend-observability] reporter failed", error);
  }
};

const isPromiseLike = (value: unknown): value is PromiseLike<void> =>
  value !== null &&
  (typeof value === "object" || typeof value === "function") &&
  typeof (value as { then?: unknown }).then === "function";

export const reportPerformanceEvent = (event: PerformanceEvent): void => {
  try {
    const result = activeReporter.report(event);

    if (isPromiseLike(result)) {
      void Promise.resolve(result).catch(warnReporterFailure);
    }
  } catch (error) {
    warnReporterFailure(error);
  }
};

export const resetPerformanceReporterForTests = (): void => {
  activeReporter = noopPerformanceReporter;
};
