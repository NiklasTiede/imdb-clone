import type { PerformanceEventContext } from "./types";

export const OBSERVABILITY_APP_NAME = "imdb-clone-frontend" as const;

export const createPerformanceEventContext = (
  route?: string,
): PerformanceEventContext => ({
  appName: OBSERVABILITY_APP_NAME,
  appVersion: import.meta.env.VITE_APP_VERSION ?? "dev",
  environment: import.meta.env.MODE,
  ...(route ? { route } : {}),
});

export const isObservabilityEnabled = (): boolean =>
  import.meta.env.DEV ||
  import.meta.env.VITE_OBSERVABILITY_ENABLED === "true";

export const shouldUseConsoleReporter = (): boolean =>
  import.meta.env.DEV ||
  import.meta.env.VITE_OBSERVABILITY_CONSOLE === "true";
