import { consolePerformanceReporter } from "./consolePerformanceReporter";
import { isObservabilityEnabled, shouldUseConsoleReporter } from "./config";
import { noopPerformanceReporter } from "./noopPerformanceReporter";
import type { PerformanceEvent, PerformanceReporter } from "./types";

let activeReporter: PerformanceReporter =
  isObservabilityEnabled() && shouldUseConsoleReporter()
    ? consolePerformanceReporter
    : noopPerformanceReporter;

export const configurePerformanceReporter = (
  reporter: PerformanceReporter,
): void => {
  activeReporter = reporter;
};

export const reportPerformanceEvent = (event: PerformanceEvent): void => {
  try {
    activeReporter.report(event);
  } catch (error) {
    if (import.meta.env.DEV) {
      console.warn("[frontend-observability] reporter failed", error);
    }
  }
};

export const resetPerformanceReporterForTests = (): void => {
  activeReporter = noopPerformanceReporter;
};
