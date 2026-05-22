# Frontend Performance Observability Baseline

## Goal

Add a lightweight frontend observability layer that measures real browser performance before making
larger architecture decisions such as SSR, Next.js, or React Router server rendering.

The first implementation should answer whether slow page loads are caused by application boot,
route navigation, API calls, image/content loading, layout instability, or JavaScript runtime errors.
It should not attempt to optimize performance yet.

## Scope

Instrument the current Vite React app with a small shared observability module under
`frontend/src/shared/observability`.

Collect these signals:

- Core Web Vitals: `CLS`, `FCP`, `INP`, `LCP`, and `TTFB`.
- App boot timing from frontend entrypoint startup to initial app render scheduling.
- Route navigation timing for React Router path/search changes.
- API request duration and failure metadata at the shared HTTP client boundary.
- Global browser errors and unhandled promise rejections.

Each event should include stable, non-sensitive context:

- app name: `imdb-clone-frontend`
- app version from build-time configuration when available
- environment from Vite mode or build-time configuration
- current route pattern or pathname where practical
- browser metric name, value, rating, and navigation type where available

No user tokens, usernames, request bodies, response bodies, authorization headers, or decrypted
secret values may be recorded.

## Architecture

Create a local observability facade that hides the transport choice:

- `frontend/src/shared/observability/types.ts` defines event types and common context.
- `frontend/src/shared/observability/performanceReporter.ts` exposes a small reporter interface.
- `frontend/src/shared/observability/consolePerformanceReporter.ts` provides a development reporter.
- `frontend/src/shared/observability/webVitals.ts` registers Web Vitals collection.
- `frontend/src/shared/observability/browserErrors.ts` registers global error listeners.
- `frontend/src/shared/observability/routeMetrics.tsx` records client route navigation timing.
- `frontend/src/shared/observability/index.ts` exports the public setup API.

The first transport can log structured events in development and no-op in production unless an
explicit Vite environment flag enables telemetry. This keeps the app safe while the event model is
stabilized.

A later Grafana Faro integration should be able to replace or wrap the reporter without changing
feature code.

## Integration Points

Initialize global observability from `frontend/src/index.tsx` before rendering the app providers.

Add a route metrics component near `BrowserRouter` in `frontend/src/app/AppProviders.tsx` or directly
inside the router tree so it can observe `location.pathname` and `location.search`.

Instrument API timing in the shared API layer, preferably `frontend/src/shared/api/httpClient.ts`,
so generated clients and feature query wrappers do not need local changes.

Keep feature components free of observability-specific code unless a later task defines custom
business events.

## Data Flow

Browser signals are normalized into a small event shape and passed to the active reporter.

In development, the reporter prints structured objects with a clear prefix so they can be inspected
without a collector.

In production, the default behavior is disabled until a collector endpoint and privacy policy are
chosen. The design intentionally avoids adding a backend ingest endpoint in this first slice.

## Error Handling

Observability must never break the app. Reporter failures should be caught and ignored after an
optional development-only console warning.

Global error reporting should capture message, source location when available, and a coarse error
type. It should avoid serializing arbitrary error objects because they can contain sensitive data.

API failure events should include method, sanitized URL path, status code when available, duration,
and failure kind. They should not include request or response payloads.

## Testing

Add focused Vitest coverage for:

- event normalization and context creation
- reporter enablement by environment flag
- Web Vitals callback forwarding with mocked metric callbacks
- API timing success and failure paths at the HTTP client boundary
- route timing behavior with a memory router or test wrapper

Run the relevant frontend tests first, then `yarn run lint` and `yarn build` for the final pass.

## Decision Criteria After Instrumentation

Use collected data to decide later work:

- Bad `TTFB` points to backend, CDN, or hosting latency.
- Good `TTFB` with bad `FCP` points to asset or JavaScript boot cost.
- Bad `LCP` points to hero/media/content loading.
- Bad `INP` points to main-thread or React rendering work.
- Slow route navigation points to route chunk loading or client data fetch waterfalls.
- Slow API spans point to backend endpoint, search, database, or network issues.
- Empty initial HTML with otherwise good backend timing may justify pre-rendering or SSR.

## Out Of Scope

- Migrating to Next.js or React Router SSR.
- Adding Grafana Faro, Sentry, OpenTelemetry, Loki, Tempo, or a backend telemetry ingest endpoint.
- Performance optimization changes.
- Session replay, click tracking, heatmaps, or product analytics.
- Recording user-identifying data.
