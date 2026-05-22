import type { PerformanceReporter } from "./types";

export const noopPerformanceReporter: PerformanceReporter = {
  report: () => undefined,
};
