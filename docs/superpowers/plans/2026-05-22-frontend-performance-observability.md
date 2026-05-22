# Frontend Performance Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a lightweight frontend performance observability baseline for Web Vitals, app boot, route navigation, API timings, and global browser errors.

**Architecture:** Create a small `frontend/src/shared/observability` facade with typed events and reporter implementations. Initialize browser-level instrumentation from the Vite entrypoint, observe React Router navigation from the provider tree, and time API calls at the shared Axios client boundary. Keep production reporting disabled unless explicitly enabled by Vite environment flags.

**Tech Stack:** React 19, React Router 7, Vite 8, TypeScript 6, Vitest/jsdom, Axios, TanStack Query, `web-vitals@5.2.0`.

---

## File Structure

- Create `frontend/src/shared/observability/types.ts`: shared event, context, and reporter types.
- Create `frontend/src/shared/observability/config.ts`: reads Vite env flags and creates stable app context.
- Create `frontend/src/shared/observability/performanceReporter.ts`: active reporter registry, safe reporting wrapper, and test reset hook.
- Create `frontend/src/shared/observability/consolePerformanceReporter.ts`: development console reporter.
- Create `frontend/src/shared/observability/noopPerformanceReporter.ts`: production-safe no-op reporter.
- Create `frontend/src/shared/observability/urlSanitizer.ts`: removes query strings and sensitive URL values from API timing events.
- Create `frontend/src/shared/observability/webVitals.ts`: registers `web-vitals` callbacks.
- Create `frontend/src/shared/observability/browserErrors.ts`: registers `error` and `unhandledrejection` listeners.
- Create `frontend/src/shared/observability/appBoot.ts`: records app boot timing from entrypoint startup.
- Create `frontend/src/shared/observability/RouteMetrics.tsx`: React component that records route navigation timing.
- Create `frontend/src/shared/observability/index.ts`: public exports.
- Modify `frontend/src/vite-env.d.ts`: add observability-related Vite environment variables.
- Modify `frontend/src/index.tsx`: initialize global instrumentation and app boot timing before rendering.
- Modify `frontend/src/app/AppProviders.tsx`: mount `RouteMetrics` inside `BrowserRouter`.
- Modify `frontend/src/shared/api/httpClient.ts`: add Axios request/response timing interceptors.
- Modify `frontend/package.json` and `frontend/yarn.lock`: add `web-vitals@5.2.0`.
- Add focused tests next to the new observability files and extend `frontend/src/shared/api/httpClient.test.ts`.

## Environment Contract

Use these Vite env vars:

- `VITE_APP_VERSION`: optional release/version label. Default: `dev`.
- `VITE_OBSERVABILITY_ENABLED`: set to `true` to enable reporting outside development.
- `VITE_OBSERVABILITY_CONSOLE`: set to `true` to force console reporting.

Reporter behavior:

- Development mode: console reporter enabled by default.
- Production mode: no-op reporter by default.
- Any mode with `VITE_OBSERVABILITY_ENABLED=true` and `VITE_OBSERVABILITY_CONSOLE=true`: console reporter enabled.

## Task 1: Add Web Vitals Dependency

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/yarn.lock`

- [ ] **Step 1: Install the dependency**

Run:

```bash
cd frontend
yarn add web-vitals@5.2.0
```

Expected: `package.json` contains `"web-vitals": "5.2.0"` or `"web-vitals": "^5.2.0"` and `yarn.lock` contains a `web-vitals` entry.

- [ ] **Step 2: Verify dependency is listed**

Run:

```bash
cd frontend
node -e "const pkg=require('./package.json'); if (!pkg.dependencies['web-vitals']) process.exit(1); console.log(pkg.dependencies['web-vitals'])"
```

Expected: prints `5.2.0` or `^5.2.0`.

- [ ] **Step 3: Commit dependency change**

Run:

```bash
git add frontend/package.json frontend/yarn.lock
git commit -m "chore: add web vitals dependency"
```

Expected: commit succeeds.

## Task 2: Add Typed Reporter Facade

**Files:**
- Create: `frontend/src/shared/observability/types.ts`
- Create: `frontend/src/shared/observability/config.ts`
- Create: `frontend/src/shared/observability/noopPerformanceReporter.ts`
- Create: `frontend/src/shared/observability/consolePerformanceReporter.ts`
- Create: `frontend/src/shared/observability/performanceReporter.ts`
- Create: `frontend/src/shared/observability/performanceReporter.test.ts`
- Modify: `frontend/src/vite-env.d.ts`

- [ ] **Step 1: Write the failing reporter tests**

Create `frontend/src/shared/observability/performanceReporter.test.ts`:

```ts
import {
  configurePerformanceReporter,
  reportPerformanceEvent,
  resetPerformanceReporterForTests,
} from "./performanceReporter";
import type { PerformanceEvent, PerformanceReporter } from "./types";

const createEvent = (): PerformanceEvent => ({
  context: {
    appName: "imdb-clone-frontend",
    appVersion: "test",
    environment: "test",
  },
  name: "app_boot",
  timestamp: 100,
  type: "app_boot",
  value: 42,
});

describe("performanceReporter", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
  });

  it("forwards events to the configured reporter", () => {
    const reportedEvents: PerformanceEvent[] = [];
    const reporter: PerformanceReporter = {
      report: (event) => reportedEvents.push(event),
    };

    configurePerformanceReporter(reporter);
    reportPerformanceEvent(createEvent());

    expect(reportedEvents).toEqual([createEvent()]);
  });

  it("swallows reporter failures so observability cannot break the app", () => {
    const reporter: PerformanceReporter = {
      report: () => {
        throw new Error("collector unavailable");
      },
    };

    configurePerformanceReporter(reporter);

    expect(() => reportPerformanceEvent(createEvent())).not.toThrow();
  });
});
```

- [ ] **Step 2: Run the failing reporter tests**

Run:

```bash
cd frontend
yarn test src/shared/observability/performanceReporter.test.ts
```

Expected: fails because `performanceReporter` and related types do not exist.

- [ ] **Step 3: Add shared event types**

Create `frontend/src/shared/observability/types.ts`:

```ts
export type PerformanceEventType =
  | "api_request"
  | "app_boot"
  | "browser_error"
  | "route_navigation"
  | "web_vital";

export type PerformanceEventContext = {
  appName: "imdb-clone-frontend";
  appVersion: string;
  environment: string;
  route?: string;
};

export type BasePerformanceEvent = {
  context: PerformanceEventContext;
  name: string;
  timestamp: number;
  type: PerformanceEventType;
  value?: number;
};

export type WebVitalPerformanceEvent = BasePerformanceEvent & {
  type: "web_vital";
  name: "CLS" | "FCP" | "INP" | "LCP" | "TTFB";
  id: string;
  navigationType?: string;
  rating?: "good" | "needs-improvement" | "poor";
  value: number;
};

export type AppBootPerformanceEvent = BasePerformanceEvent & {
  type: "app_boot";
  name: "app_boot";
  value: number;
};

export type RouteNavigationPerformanceEvent = BasePerformanceEvent & {
  type: "route_navigation";
  name: "route_navigation";
  from: string;
  to: string;
  value: number;
};

export type ApiRequestPerformanceEvent = BasePerformanceEvent & {
  type: "api_request";
  name: "api_request";
  failureKind?: "network" | "http" | "unknown";
  method: string;
  status?: number;
  success: boolean;
  url: string;
  value: number;
};

export type BrowserErrorPerformanceEvent = BasePerformanceEvent & {
  type: "browser_error";
  name: "browser_error" | "unhandled_rejection";
  column?: number;
  errorType?: string;
  line?: number;
  message: string;
  source?: string;
};

export type PerformanceEvent =
  | ApiRequestPerformanceEvent
  | AppBootPerformanceEvent
  | BrowserErrorPerformanceEvent
  | RouteNavigationPerformanceEvent
  | WebVitalPerformanceEvent;

export type PerformanceReporter = {
  report: (event: PerformanceEvent) => void;
};
```

- [ ] **Step 4: Add Vite env typings**

Modify `frontend/src/vite-env.d.ts` so `ImportMetaEnv` is:

```ts
interface ImportMetaEnv {
  readonly VITE_APP_VERSION?: string;
  readonly VITE_IMDB_CLONE_BACKEND_ADDRESS?: string;
  readonly VITE_IMDB_CLONE_OBJECT_STORAGE_ADDRESS?: string;
  readonly VITE_OBSERVABILITY_CONSOLE?: string;
  readonly VITE_OBSERVABILITY_ENABLED?: string;
}
```

- [ ] **Step 5: Add config and reporter implementations**

Create `frontend/src/shared/observability/config.ts`:

```ts
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
```

Create `frontend/src/shared/observability/noopPerformanceReporter.ts`:

```ts
import type { PerformanceReporter } from "./types";

export const noopPerformanceReporter: PerformanceReporter = {
  report: () => undefined,
};
```

Create `frontend/src/shared/observability/consolePerformanceReporter.ts`:

```ts
import type { PerformanceReporter } from "./types";

export const consolePerformanceReporter: PerformanceReporter = {
  report: (event) => {
    console.info("[frontend-observability]", event);
  },
};
```

Create `frontend/src/shared/observability/performanceReporter.ts`:

```ts
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
```

- [ ] **Step 6: Run reporter tests**

Run:

```bash
cd frontend
yarn test src/shared/observability/performanceReporter.test.ts
```

Expected: tests pass.

- [ ] **Step 7: Commit reporter facade**

Run:

```bash
git add frontend/src/vite-env.d.ts frontend/src/shared/observability
git commit -m "feat: add frontend observability reporter"
```

Expected: commit succeeds.

## Task 3: Add Web Vitals and Browser Error Instrumentation

**Files:**
- Create: `frontend/src/shared/observability/webVitals.ts`
- Create: `frontend/src/shared/observability/webVitals.test.ts`
- Create: `frontend/src/shared/observability/browserErrors.ts`
- Create: `frontend/src/shared/observability/browserErrors.test.ts`

- [ ] **Step 1: Write failing Web Vitals tests**

Create `frontend/src/shared/observability/webVitals.test.ts`:

```ts
import type { Metric } from "web-vitals";
import {
  configurePerformanceReporter,
  resetPerformanceReporterForTests,
} from "./performanceReporter";
import type { PerformanceEvent } from "./types";
import { registerWebVitals } from "./webVitals";

vi.mock("web-vitals", () => ({
  onCLS: vi.fn(),
  onFCP: vi.fn(),
  onINP: vi.fn(),
  onLCP: vi.fn(),
  onTTFB: vi.fn(),
}));

const metric = (name: Metric["name"]): Metric => ({
  delta: 12,
  entries: [],
  id: `${name}-id`,
  name,
  navigationType: "navigate",
  rating: "good",
  value: 123,
});

describe("registerWebVitals", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
    vi.clearAllMocks();
  });

  it("registers Web Vitals callbacks and reports normalized metric events", async () => {
    const webVitals = await import("web-vitals");
    const events: PerformanceEvent[] = [];
    configurePerformanceReporter({ report: (event) => events.push(event) });

    registerWebVitals();

    expect(webVitals.onCLS).toHaveBeenCalledTimes(1);
    const onCls = vi.mocked(webVitals.onCLS).mock.calls[0][0];
    onCls(metric("CLS"));

    expect(events).toEqual([
      expect.objectContaining({
        id: "CLS-id",
        name: "CLS",
        navigationType: "navigate",
        rating: "good",
        type: "web_vital",
        value: 123,
      }),
    ]);
  });
});
```

- [ ] **Step 2: Write failing browser error tests**

Create `frontend/src/shared/observability/browserErrors.test.ts`:

```ts
import {
  configurePerformanceReporter,
  resetPerformanceReporterForTests,
} from "./performanceReporter";
import type { PerformanceEvent } from "./types";
import { registerBrowserErrorReporting } from "./browserErrors";

describe("registerBrowserErrorReporting", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
  });

  it("reports global error events without serializing the full error object", () => {
    const events: PerformanceEvent[] = [];
    configurePerformanceReporter({ report: (event) => events.push(event) });

    const cleanup = registerBrowserErrorReporting();
    window.dispatchEvent(
      new ErrorEvent("error", {
        colno: 7,
        error: new TypeError("boom"),
        filename: "http://localhost/src/example.ts",
        lineno: 4,
        message: "boom",
      }),
    );
    cleanup();

    expect(events).toEqual([
      expect.objectContaining({
        column: 7,
        errorType: "TypeError",
        line: 4,
        message: "boom",
        name: "browser_error",
        source: "http://localhost/src/example.ts",
        type: "browser_error",
      }),
    ]);
    expect(JSON.stringify(events[0])).not.toContain("stack");
  });

  it("reports unhandled promise rejections with a coarse message", () => {
    const events: PerformanceEvent[] = [];
    configurePerformanceReporter({ report: (event) => events.push(event) });

    const cleanup = registerBrowserErrorReporting();
    const event = new Event("unhandledrejection") as PromiseRejectionEvent;
    Object.defineProperty(event, "reason", {
      value: new Error("rejected"),
    });
    window.dispatchEvent(event);
    cleanup();

    expect(events).toEqual([
      expect.objectContaining({
        errorType: "Error",
        message: "rejected",
        name: "unhandled_rejection",
        type: "browser_error",
      }),
    ]);
  });
});
```

- [ ] **Step 3: Run failing instrumentation tests**

Run:

```bash
cd frontend
yarn test src/shared/observability/webVitals.test.ts src/shared/observability/browserErrors.test.ts
```

Expected: fails because `webVitals.ts` and `browserErrors.ts` do not exist.

- [ ] **Step 4: Implement Web Vitals registration**

Create `frontend/src/shared/observability/webVitals.ts`:

```ts
import { onCLS, onFCP, onINP, onLCP, onTTFB, type Metric } from "web-vitals";
import { createPerformanceEventContext } from "./config";
import { reportPerformanceEvent } from "./performanceReporter";

const reportWebVital = (metric: Metric): void => {
  reportPerformanceEvent({
    context: createPerformanceEventContext(window.location.pathname),
    id: metric.id,
    name: metric.name,
    navigationType: metric.navigationType,
    rating: metric.rating,
    timestamp: performance.now(),
    type: "web_vital",
    value: metric.value,
  });
};

export const registerWebVitals = (): void => {
  onCLS(reportWebVital);
  onFCP(reportWebVital);
  onINP(reportWebVital);
  onLCP(reportWebVital);
  onTTFB(reportWebVital);
};
```

- [ ] **Step 5: Implement browser error reporting**

Create `frontend/src/shared/observability/browserErrors.ts`:

```ts
import { createPerformanceEventContext } from "./config";
import { reportPerformanceEvent } from "./performanceReporter";

const getErrorType = (error: unknown): string | undefined =>
  error instanceof Error ? error.name : undefined;

const getReasonMessage = (reason: unknown): string => {
  if (reason instanceof Error) {
    return reason.message;
  }
  if (typeof reason === "string") {
    return reason;
  }
  return "Unhandled promise rejection";
};

export const registerBrowserErrorReporting = (): (() => void) => {
  const onError = (event: ErrorEvent): void => {
    reportPerformanceEvent({
      column: event.colno || undefined,
      context: createPerformanceEventContext(window.location.pathname),
      errorType: getErrorType(event.error),
      line: event.lineno || undefined,
      message: event.message || "Browser error",
      name: "browser_error",
      source: event.filename || undefined,
      timestamp: performance.now(),
      type: "browser_error",
    });
  };

  const onUnhandledRejection = (event: PromiseRejectionEvent): void => {
    reportPerformanceEvent({
      context: createPerformanceEventContext(window.location.pathname),
      errorType: getErrorType(event.reason),
      message: getReasonMessage(event.reason),
      name: "unhandled_rejection",
      timestamp: performance.now(),
      type: "browser_error",
    });
  };

  window.addEventListener("error", onError);
  window.addEventListener("unhandledrejection", onUnhandledRejection);

  return () => {
    window.removeEventListener("error", onError);
    window.removeEventListener("unhandledrejection", onUnhandledRejection);
  };
};
```

- [ ] **Step 6: Run instrumentation tests**

Run:

```bash
cd frontend
yarn test src/shared/observability/webVitals.test.ts src/shared/observability/browserErrors.test.ts
```

Expected: tests pass.

- [ ] **Step 7: Commit browser signal instrumentation**

Run:

```bash
git add frontend/src/shared/observability
git commit -m "feat: collect frontend browser signals"
```

Expected: commit succeeds.

## Task 4: Add App Boot and Public Setup API

**Files:**
- Create: `frontend/src/shared/observability/appBoot.ts`
- Create: `frontend/src/shared/observability/appBoot.test.ts`
- Create: `frontend/src/shared/observability/index.ts`
- Modify: `frontend/src/index.tsx`

- [ ] **Step 1: Write failing app boot test**

Create `frontend/src/shared/observability/appBoot.test.ts`:

```ts
import {
  configurePerformanceReporter,
  resetPerformanceReporterForTests,
} from "./performanceReporter";
import type { PerformanceEvent } from "./types";
import { reportAppBoot } from "./appBoot";

describe("reportAppBoot", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
  });

  it("reports elapsed time since entrypoint startup", () => {
    const events: PerformanceEvent[] = [];
    configurePerformanceReporter({ report: (event) => events.push(event) });

    reportAppBoot(10, 60);

    expect(events).toEqual([
      expect.objectContaining({
        name: "app_boot",
        timestamp: 60,
        type: "app_boot",
        value: 50,
      }),
    ]);
  });
});
```

- [ ] **Step 2: Run failing app boot test**

Run:

```bash
cd frontend
yarn test src/shared/observability/appBoot.test.ts
```

Expected: fails because `appBoot.ts` does not exist.

- [ ] **Step 3: Implement app boot reporting**

Create `frontend/src/shared/observability/appBoot.ts`:

```ts
import { createPerformanceEventContext } from "./config";
import { reportPerformanceEvent } from "./performanceReporter";

export const reportAppBoot = (
  entrypointStart: number,
  renderScheduledAt = performance.now(),
): void => {
  reportPerformanceEvent({
    context: createPerformanceEventContext(window.location.pathname),
    name: "app_boot",
    timestamp: renderScheduledAt,
    type: "app_boot",
    value: Math.max(0, renderScheduledAt - entrypointStart),
  });
};
```

Create `frontend/src/shared/observability/index.ts`:

```ts
export { reportAppBoot } from "./appBoot";
export { registerBrowserErrorReporting } from "./browserErrors";
export { registerWebVitals } from "./webVitals";
export type { PerformanceEvent, PerformanceReporter } from "./types";
```

- [ ] **Step 4: Wire global observability into the entrypoint**

Modify `frontend/src/index.tsx`:

```tsx
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import AppProviders from "./app/AppProviders";
import {
  registerBrowserErrorReporting,
  registerWebVitals,
  reportAppBoot,
} from "./shared/observability";

const entrypointStart = performance.now();

registerBrowserErrorReporting();
registerWebVitals();

const root = ReactDOM.createRoot(
  document.getElementById("root") as HTMLElement,
);

reportAppBoot(entrypointStart);

root.render(
  // <React.StrictMode>
  <AppProviders>
    <App />
  </AppProviders>,
  // </React.StrictMode>
);
```

- [ ] **Step 5: Run app boot test**

Run:

```bash
cd frontend
yarn test src/shared/observability/appBoot.test.ts
```

Expected: test passes.

- [ ] **Step 6: Commit app boot setup**

Run:

```bash
git add frontend/src/index.tsx frontend/src/shared/observability
git commit -m "feat: initialize frontend observability"
```

Expected: commit succeeds.

## Task 5: Add Route Navigation Metrics

**Files:**
- Create: `frontend/src/shared/observability/RouteMetrics.tsx`
- Create: `frontend/src/shared/observability/RouteMetrics.test.tsx`
- Modify: `frontend/src/shared/observability/index.ts`
- Modify: `frontend/src/app/AppProviders.tsx`

- [ ] **Step 1: Write failing route metrics test**

Create `frontend/src/shared/observability/RouteMetrics.test.tsx`:

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import { Link, Route, Routes } from "react-router";
import { MemoryRouter } from "react-router";
import userEvent from "@testing-library/user-event";
import {
  configurePerformanceReporter,
  resetPerformanceReporterForTests,
} from "./performanceReporter";
import { RouteMetrics } from "./RouteMetrics";
import type { PerformanceEvent } from "./types";

describe("RouteMetrics", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
  });

  it("reports client route navigation duration", async () => {
    const events: PerformanceEvent[] = [];
    configurePerformanceReporter({ report: (event) => events.push(event) });

    render(
      <MemoryRouter initialEntries={["/"]}>
        <RouteMetrics />
        <Link to="/movie-search?q=test">Search</Link>
        <Routes>
          <Route path="/" element={<div>Home</div>} />
          <Route path="/movie-search" element={<div>Search</div>} />
        </Routes>
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByRole("link", { name: "Search" }));

    await waitFor(() => {
      expect(events).toEqual([
        expect.objectContaining({
          from: "/",
          name: "route_navigation",
          to: "/movie-search?q=test",
          type: "route_navigation",
        }),
      ]);
    });
  });
});
```

- [ ] **Step 2: Run failing route metrics test**

Run:

```bash
cd frontend
yarn test src/shared/observability/RouteMetrics.test.tsx
```

Expected: fails because `RouteMetrics.tsx` does not exist.

- [ ] **Step 3: Implement route metrics component**

Create `frontend/src/shared/observability/RouteMetrics.tsx`:

```tsx
import { useEffect, useRef } from "react";
import { useLocation } from "react-router";
import { createPerformanceEventContext } from "./config";
import { reportPerformanceEvent } from "./performanceReporter";

const toRoute = (pathname: string, search: string): string =>
  `${pathname}${search}`;

export const RouteMetrics = () => {
  const location = useLocation();
  const currentRoute = toRoute(location.pathname, location.search);
  const previousRouteRef = useRef(currentRoute);
  const navigationStartRef = useRef(performance.now());

  useEffect(() => {
    const previousRoute = previousRouteRef.current;

    if (previousRoute !== currentRoute) {
      const now = performance.now();
      reportPerformanceEvent({
        context: createPerformanceEventContext(location.pathname),
        from: previousRoute,
        name: "route_navigation",
        timestamp: now,
        to: currentRoute,
        type: "route_navigation",
        value: Math.max(0, now - navigationStartRef.current),
      });
      previousRouteRef.current = currentRoute;
    }

    navigationStartRef.current = performance.now();
  }, [currentRoute, location.pathname]);

  return null;
};
```

Modify `frontend/src/shared/observability/index.ts`:

```ts
export { reportAppBoot } from "./appBoot";
export { registerBrowserErrorReporting } from "./browserErrors";
export { RouteMetrics } from "./RouteMetrics";
export { registerWebVitals } from "./webVitals";
export type { PerformanceEvent, PerformanceReporter } from "./types";
```

- [ ] **Step 4: Mount route metrics in app providers**

Modify `frontend/src/app/AppProviders.tsx`:

```tsx
import { QueryClientProvider } from "@tanstack/react-query";
import { SnackbarProvider } from "notistack";
import type { ReactNode } from "react";
import { BrowserRouter } from "react-router";
import { queryClient } from "../shared/api/queryClient";
import { RouteMetrics } from "../shared/observability";

const AppProviders = ({ children }: { children: ReactNode }) => (
  <QueryClientProvider client={queryClient}>
    <SnackbarProvider maxSnack={3}>
      <BrowserRouter>
        <RouteMetrics />
        {children}
      </BrowserRouter>
    </SnackbarProvider>
  </QueryClientProvider>
);

export default AppProviders;
```

- [ ] **Step 5: Run route metrics test**

Run:

```bash
cd frontend
yarn test src/shared/observability/RouteMetrics.test.tsx
```

Expected: test passes.

- [ ] **Step 6: Commit route metrics**

Run:

```bash
git add frontend/src/app/AppProviders.tsx frontend/src/shared/observability
git commit -m "feat: track frontend route timing"
```

Expected: commit succeeds.

## Task 6: Add API Request Timing

**Files:**
- Create: `frontend/src/shared/observability/urlSanitizer.ts`
- Create: `frontend/src/shared/observability/urlSanitizer.test.ts`
- Modify: `frontend/src/shared/api/httpClient.ts`
- Modify: `frontend/src/shared/api/httpClient.test.ts`

- [ ] **Step 1: Write failing URL sanitizer test**

Create `frontend/src/shared/observability/urlSanitizer.test.ts`:

```ts
import { sanitizeUrlForTelemetry } from "./urlSanitizer";

describe("sanitizeUrlForTelemetry", () => {
  it("keeps only origin and pathname for absolute URLs", () => {
    expect(
      sanitizeUrlForTelemetry(
        "https://api.example.test/movies/42?token=secret&q=matrix",
      ),
    ).toBe("https://api.example.test/movies/42");
  });

  it("keeps relative paths without query strings", () => {
    expect(sanitizeUrlForTelemetry("/movies/search?q=matrix")).toBe(
      "/movies/search",
    );
  });
});
```

- [ ] **Step 2: Extend HTTP client tests for timing**

Append these tests inside `describe("apiHttpClient", () => { ... })` in `frontend/src/shared/api/httpClient.test.ts`:

```ts
  it("reports successful API request timing without query parameters", async () => {
    const { configurePerformanceReporter, resetPerformanceReporterForTests } =
      await import("../observability/performanceReporter");
    const events: unknown[] = [];
    configurePerformanceReporter({ report: (event) => events.push(event) });

    await apiHttpClient.get("/movies/search?q=matrix", {
      adapter: async (config) => ({
        config,
        data: {},
        headers: {},
        status: 200,
        statusText: "OK",
      }),
    });

    expect(events).toEqual([
      expect.objectContaining({
        method: "GET",
        name: "api_request",
        status: 200,
        success: true,
        type: "api_request",
        url: "/movies/search",
      }),
    ]);

    resetPerformanceReporterForTests();
  });

  it("reports failed API request timing without request or response payloads", async () => {
    const { configurePerformanceReporter, resetPerformanceReporterForTests } =
      await import("../observability/performanceReporter");
    const events: unknown[] = [];
    configurePerformanceReporter({ report: (event) => events.push(event) });

    await expect(
      apiHttpClient.get("/movies/42?token=secret", {
        adapter: async (config) =>
          Promise.reject({
            config,
            isAxiosError: true,
            message: "Request failed",
            response: {
              config,
              data: { token: "secret" },
              headers: {},
              status: 500,
              statusText: "Server Error",
            },
            toJSON: () => ({}),
          }),
      }),
    ).rejects.toBeDefined();

    expect(events).toEqual([
      expect.objectContaining({
        failureKind: "http",
        method: "GET",
        status: 500,
        success: false,
        type: "api_request",
        url: "/movies/42",
      }),
    ]);
    expect(JSON.stringify(events[0])).not.toContain("secret");

    resetPerformanceReporterForTests();
  });
```

- [ ] **Step 3: Run failing API tests**

Run:

```bash
cd frontend
yarn test src/shared/observability/urlSanitizer.test.ts src/shared/api/httpClient.test.ts
```

Expected: fails because `urlSanitizer.ts` and HTTP timing interceptors do not exist.

- [ ] **Step 4: Implement URL sanitizer**

Create `frontend/src/shared/observability/urlSanitizer.ts`:

```ts
export const sanitizeUrlForTelemetry = (url: string | undefined): string => {
  if (!url) {
    return "unknown";
  }

  try {
    const parsedUrl = new URL(url, window.location.origin);
    const sanitized = `${parsedUrl.origin}${parsedUrl.pathname}`;
    return url.startsWith("/") ? parsedUrl.pathname : sanitized;
  } catch {
    return url.split("?")[0] || "unknown";
  }
};
```

- [ ] **Step 5: Implement Axios API timing interceptors**

Modify `frontend/src/shared/api/httpClient.ts`:

```ts
import axios, {
  AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { authSession } from "../auth/authSession";
import { reportPerformanceEvent } from "../observability/performanceReporter";
import { sanitizeUrlForTelemetry } from "../observability/urlSanitizer";

type TimedAxiosRequestConfig = InternalAxiosRequestConfig & {
  metadata?: {
    requestStartedAt: number;
  };
};

export const apiHttpClient = axios.create();

const reportApiTiming = ({
  config,
  error,
  response,
}: {
  config?: TimedAxiosRequestConfig;
  error?: AxiosError;
  response?: AxiosResponse;
}): void => {
  const startedAt = config?.metadata?.requestStartedAt ?? performance.now();
  const finishedAt = performance.now();
  const status = response?.status ?? error?.response?.status;
  const success = Boolean(response);

  reportPerformanceEvent({
    context: {
      appName: "imdb-clone-frontend",
      appVersion: import.meta.env.VITE_APP_VERSION ?? "dev",
      environment: import.meta.env.MODE,
      route: window.location.pathname,
    },
    failureKind: success
      ? undefined
      : status !== undefined
        ? "http"
        : error?.request
          ? "network"
          : "unknown",
    method: (config?.method ?? "GET").toUpperCase(),
    name: "api_request",
    status,
    success,
    timestamp: finishedAt,
    type: "api_request",
    url: sanitizeUrlForTelemetry(config?.url),
    value: Math.max(0, finishedAt - startedAt),
  });
};

apiHttpClient.interceptors.request.use(
  (config: TimedAxiosRequestConfig) => {
    config.metadata = { requestStartedAt: performance.now() };
    const token = authSession.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => Promise.reject(error),
);

apiHttpClient.interceptors.response.use(
  (response) => {
    reportApiTiming({
      config: response.config as TimedAxiosRequestConfig,
      response,
    });
    return response;
  },
  (error: AxiosError) => {
    reportApiTiming({
      config: error.config as TimedAxiosRequestConfig | undefined,
      error,
    });
    return Promise.reject(error);
  },
);
```

- [ ] **Step 6: Run API tests**

Run:

```bash
cd frontend
yarn test src/shared/observability/urlSanitizer.test.ts src/shared/api/httpClient.test.ts
```

Expected: tests pass. If TypeScript rejects the custom `metadata` property, keep `TimedAxiosRequestConfig` local and cast `config` at the interceptor boundary instead of changing Axios module declarations.

- [ ] **Step 7: Commit API timing**

Run:

```bash
git add frontend/src/shared/api/httpClient.ts frontend/src/shared/api/httpClient.test.ts frontend/src/shared/observability
git commit -m "feat: track frontend API timing"
```

Expected: commit succeeds.

## Task 7: Run Focused and Full Frontend Verification

**Files:**
- No new files.

- [ ] **Step 1: Run observability-focused tests**

Run:

```bash
cd frontend
yarn test src/shared/observability/performanceReporter.test.ts src/shared/observability/webVitals.test.ts src/shared/observability/browserErrors.test.ts src/shared/observability/appBoot.test.ts src/shared/observability/RouteMetrics.test.tsx src/shared/observability/urlSanitizer.test.ts src/shared/api/httpClient.test.ts
```

Expected: all listed test files pass.

- [ ] **Step 2: Run frontend lint**

Run:

```bash
cd frontend
yarn run lint
```

Expected: exits successfully with no ESLint errors.

- [ ] **Step 3: Run frontend build**

Run:

```bash
cd frontend
yarn build
```

Expected: TypeScript compilation and Vite build complete successfully.

- [ ] **Step 4: Run full frontend test suite**

Run:

```bash
cd frontend
yarn test
```

Expected: all frontend Vitest tests pass.

- [ ] **Step 5: Inspect git status**

Run:

```bash
git status --short
```

Expected: worktree is clean after the task commits.

## Self-Review

- Spec coverage: the plan covers Web Vitals, app boot timing, route navigation timing, API timing/failure metadata, global errors, safe context, no sensitive payloads, development console reporting, production no-op behavior, and verification.
- Placeholder scan: the plan has no unresolved placeholders or unspecified test steps.
- Type consistency: event type names, reporter API, and file names are consistent across tasks.
