import type { PerformanceReporter } from "./types";

export const consolePerformanceReporter: PerformanceReporter = {
  report: (event) => {
    console.info("[frontend-observability]", event);
  },
};
