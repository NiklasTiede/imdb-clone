# Frontend Performance Design

## Goal

Improve perceived and measured frontend performance in three phases:

1. Add measurement first, so each optimization has before/after evidence.
2. Apply low-risk runtime and perceived-speed improvements.
3. Tune route chunks and bundle shape using measured build output.

The scope is the React frontend under `frontend/src`. Backend API contract
changes, image conversion pipelines, and infrastructure observability are out of
scope for this pass.

## Current Context

The app already uses route-level `React.lazy` through
`frontend/src/app/routes/routeDefinitions.tsx` and wraps routes in `Suspense`.
The current fallback is a generic text surface.

React Query is centralized in `frontend/src/shared/api/queryClient.ts` with
`refetchOnWindowFocus: false` and `retry: 1`, but no default `staleTime`.
Feature query definitions generally do not use `placeholderData` or tuned
staleness.

Poster rendering is centralized in `frontend/src/shared/media/PosterImage.tsx`.
It currently chooses a single MinIO image size and does not set native lazy
loading, async decoding, `srcSet`, or responsive `sizes`.

Search, watchlist, and ratings are paged. Current visible result counts are
small enough that virtualization is not part of the first implementation pass.

## Phase 0: Measurement First

Add a dev-only performance module under `frontend/src/shared/performance`.

The module will provide:

- A runtime flag based on `import.meta.env.VITE_ENABLE_PERF_LOGS === "true"`.
- A small `mark` and `measure` wrapper around the browser Performance API.
- Console reporting for app-specific measures in development builds.
- No new measurement dependency in the first pass. Web Vitals reporting can be
  added later if the app-specific measures are not enough.

Initial app-specific marks:

- Route fallback visible and route content visible.
- Search criteria submitted and search results visible.
- Movie detail route opened and detail content visible.
- Movie detail prefetch started and completed.

Build-level baseline:

- Capture `yarn build` chunk and gzip output before and after changes.
- Use the existing Vite output as the baseline source of truth.
- Add a short docs note or package script only if it makes repeated collection
  simpler.

Phase 0 must not change user-facing behavior except for optional console logs
when `VITE_ENABLE_PERF_LOGS=true`.

## Phase 1: Runtime And Perceived-Speed Wins

Apply low-risk improvements backed by Phase 0 measurements.

Image behavior:

- Update `PosterImage` to default to `loading="lazy"` and `decoding="async"`.
- Add `srcSet` using the existing MinIO small and large poster sizes.
- Add a `sizes` prop so grid and list callers describe rendered poster width.
- Allow critical images to opt into `loading="eager"` when needed.

Route fallback:

- Replace the text-only route fallback with a MUI skeleton layout that resembles
  the app surface.
- Keep the fallback generic enough for all lazy routes.

React Query cache behavior:

- Add a global `staleTime` suitable for mostly-static catalog browsing.
- Use five minutes for catalog and search browsing data by default.
- Override with shorter stale windows for account, watchlist, and rating data,
  starting at 30 seconds for user-owned state.
- Use `placeholderData: keepPreviousData` for paged search, watchlist, and
  ratings queries where the previous page should remain visible while fetching.

Prefetch behavior:

- Prefetch movie details from movie cards and list rows on hover and keyboard
  focus when a valid movie id is available.
- Reuse `movieQueries.detail(movieId)` so cache keys remain consistent.
- Avoid prefetching for placeholder links or missing ids.

Loading states:

- Prefer skeletons where the shape is known.
- Keep small inline progress indicators for form submissions and tiny status
  checks where skeletons are not appropriate.

## Phase 2: Bundle And Route Tuning

Measure before changing route chunk boundaries.

Bundle review:

- Compare `yarn build` output before and after Phase 1.
- Identify large route chunks and shared chunks that affect cold navigation.

Route import review:

- Check whether feature barrel imports such as `import("../../features/catalog")`
  pull unrelated pages into the same chunk.
- If the bundle output shows avoidable coupling, switch selected lazy routes to
  direct page imports.

Admin and account isolation:

- Confirm account-only code such as `react-image-crop` stays out of the primary
  browsing path.
- Keep admin/editing code isolated from public browsing routes.

No dependency should be added for bundle analysis unless the built-in Vite
output is not enough to make the next decision.

## Deferred Work

Virtualization is deferred. The current UI uses pagination with small page
sizes, so `@tanstack/react-virtual` is only justified if the app introduces
large visible lists, infinite scrolling, or measured scroll/render jank.

Image format conversion to WebP or AVIF is deferred because it likely belongs
in the backend/media pipeline rather than this frontend-only pass.

Production telemetry upload is deferred. Phase 0 logs locally only; sending
metrics to a backend endpoint can be designed separately once the local metrics
are useful.

## Testing And Verification

Phase 0:

- Unit test the performance helpers with logging enabled and disabled.
- Verify `VITE_ENABLE_PERF_LOGS=true yarn start` emits useful local logs.
- Run `yarn build` and save the baseline chunk output in the task notes.

Phase 1:

- Add or update focused component tests for `PosterImage` attributes and route
  fallback rendering.
- Add query tests for `staleTime` and `placeholderData` where query objects are
  already tested.
- Run `yarn test`, `yarn run lint`, and `yarn build`.
- Compare app-specific performance logs before and after changes.

Phase 2:

- Compare production build chunk output before and after route import changes.
- Keep route architecture tests passing.
- Run `yarn test`, `yarn run lint`, and `yarn build`.

## Rollback Criteria

Revert or skip an optimization when it:

- Adds a dependency without measurable value.
- Increases the main browsing-path bundle without a compensating UX win.
- Makes loading states less stable or less accessible.
- Adds noisy performance logs when the explicit env flag is disabled.
