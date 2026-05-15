# Frontend Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add measurement-first frontend performance work, then improve image loading, data caching, perceived loading states, movie-detail prefetching, and route chunk shape with before/after verification.

**Architecture:** Add a small dev-only performance module under `frontend/src/shared/performance`, then instrument route/search/movie-detail flows without changing production behavior. Use existing React Query query-object patterns, existing MUI components, and existing route lazy-loading structure; tune direct imports only after comparing production build output.

**Tech Stack:** React 19, TypeScript, Vite 8, Vitest, Testing Library, Material UI 9, TanStack React Query 5, React Router 7.

---

## File Map

- Create `frontend/src/shared/performance/performanceMarks.ts`: constants for app-specific performance mark and measure names.
- Create `frontend/src/shared/performance/performanceLogger.ts`: flag checks, `markPerformance`, `measurePerformance`, and route helper functions.
- Create `frontend/src/shared/performance/performanceLogger.test.ts`: unit tests for disabled/enabled logging behavior.
- Create `frontend/src/shared/performance/index.ts`: public exports.
- Modify `frontend/src/App.tsx`: mark route fallback visible and render a skeleton fallback.
- Modify `frontend/src/app/routes/routeDefinitions.tsx`: wrap lazy route modules so route content visibility is measured.
- Modify `frontend/src/app/AppRoutesArchitecture.test.ts`: keep route architecture expectations aligned with direct page imports in Phase 2.
- Modify `frontend/src/features/search/pages/MovieSearchPage.tsx`: mark search results visible and use placeholder data.
- Modify `frontend/src/features/catalog/pages/MovieDetailPage.tsx`: mark detail route opened and detail content visible.
- Modify `frontend/src/shared/media/PosterImage.tsx`: add native lazy loading, async decoding, `srcSet`, and `sizes`.
- Modify poster callers in `frontend/src/features/catalog/components`, `frontend/src/features/search/components`, `frontend/src/features/home/components`, and engagement card/list components: pass `sizes="(max-width: 600px) 45vw, (max-width: 900px) 30vw, 220px"` for poster-card grids, `sizes="52px"` for list rows, and `loading="eager"` only for above-the-fold hero posters.
- Modify `frontend/src/shared/api/queryClient.ts`: add safe defaults for `staleTime`.
- Modify query modules under `frontend/src/features/**/api`: add specific `staleTime` and `placeholderData` where needed.
- Modify movie card/list components: prefetch detail queries on hover/focus.
- Add or update focused tests beside changed modules.

## Task 1: Phase 0 Performance Logger

**Files:**
- Create: `frontend/src/shared/performance/performanceMarks.ts`
- Create: `frontend/src/shared/performance/performanceLogger.ts`
- Create: `frontend/src/shared/performance/index.ts`
- Test: `frontend/src/shared/performance/performanceLogger.test.ts`

- [ ] **Step 1: Write the failing tests**

Create `frontend/src/shared/performance/performanceLogger.test.ts`:

```ts
import { describe, expect, test, vi } from "vitest";
import {
  isPerformanceLoggingEnabled,
  markPerformance,
  measurePerformance,
} from "./performanceLogger";

const createPerformance = () => ({
  mark: vi.fn(),
  measure: vi.fn(() => ({ duration: 12.4 })),
});

describe("performanceLogger", () => {
  test("enables logging only when the explicit env flag is true", () => {
    expect(
      isPerformanceLoggingEnabled({ VITE_ENABLE_PERF_LOGS: "true" }),
    ).toBe(true);
    expect(
      isPerformanceLoggingEnabled({ VITE_ENABLE_PERF_LOGS: "false" }),
    ).toBe(false);
    expect(isPerformanceLoggingEnabled({})).toBe(false);
  });

  test("does not touch the Performance API when logging is disabled", () => {
    const performanceApi = createPerformance();
    const consoleInfo = vi.fn();

    markPerformance("route:fallback-visible", {
      consoleInfo,
      env: {},
      performanceApi,
    });
    measurePerformance("route:content-visible", "route:start", "route:end", {
      consoleInfo,
      env: {},
      performanceApi,
    });

    expect(performanceApi.mark).not.toHaveBeenCalled();
    expect(performanceApi.measure).not.toHaveBeenCalled();
    expect(consoleInfo).not.toHaveBeenCalled();
  });

  test("marks and logs measures when logging is enabled", () => {
    const performanceApi = createPerformance();
    const consoleInfo = vi.fn();

    markPerformance("route:fallback-visible", {
      consoleInfo,
      env: { VITE_ENABLE_PERF_LOGS: "true" },
      performanceApi,
    });
    measurePerformance("route:content-visible", "route:start", "route:end", {
      consoleInfo,
      env: { VITE_ENABLE_PERF_LOGS: "true" },
      performanceApi,
    });

    expect(performanceApi.mark).toHaveBeenCalledWith("route:fallback-visible");
    expect(performanceApi.measure).toHaveBeenCalledWith(
      "route:content-visible",
      "route:start",
      "route:end",
    );
    expect(consoleInfo).toHaveBeenCalledWith(
      "[perf] route:content-visible: 12.40ms",
    );
  });
});
```

- [ ] **Step 2: Run the focused test to verify it fails**

Run:

```bash
cd frontend && yarn test src/shared/performance/performanceLogger.test.ts
```

Expected: FAIL because `performanceLogger` does not exist.

- [ ] **Step 3: Implement the logger**

Create `frontend/src/shared/performance/performanceMarks.ts`:

```ts
export const performanceMarks = {
  movieDetailContentVisible: "movie-detail:content-visible",
  movieDetailOpened: "movie-detail:opened",
  movieDetailPrefetchComplete: "movie-detail-prefetch:complete",
  movieDetailPrefetchStart: "movie-detail-prefetch:start",
  routeContentVisible: "route:content-visible",
  routeFallbackVisible: "route:fallback-visible",
  searchResultsVisible: "search:results-visible",
  searchSubmitted: "search:submitted",
} as const;
```

Create `frontend/src/shared/performance/performanceLogger.ts`:

```ts
type PerfEnv = {
  VITE_ENABLE_PERF_LOGS?: string;
};

type MeasureResult = {
  duration: number;
};

type PerformanceApi = {
  mark: (name: string) => void;
  measure: (name: string, startMark: string, endMark: string) => MeasureResult;
};

type PerfOptions = {
  consoleInfo?: (message: string) => void;
  env?: PerfEnv;
  performanceApi?: PerformanceApi;
};

const getDefaultPerformance = (): PerformanceApi | undefined =>
  typeof performance === "undefined" ? undefined : performance;

export const isPerformanceLoggingEnabled = (
  env: PerfEnv = import.meta.env,
): boolean => env.VITE_ENABLE_PERF_LOGS === "true";

export const markPerformance = (
  name: string,
  { env = import.meta.env, performanceApi = getDefaultPerformance() }: PerfOptions = {},
) => {
  if (!isPerformanceLoggingEnabled(env) || !performanceApi) {
    return;
  }
  performanceApi.mark(name);
};

export const measurePerformance = (
  name: string,
  startMark: string,
  endMark: string,
  {
    consoleInfo = console.info,
    env = import.meta.env,
    performanceApi = getDefaultPerformance(),
  }: PerfOptions = {},
) => {
  if (!isPerformanceLoggingEnabled(env) || !performanceApi) {
    return;
  }
  const measure = performanceApi.measure(name, startMark, endMark);
  consoleInfo(`[perf] ${name}: ${measure.duration.toFixed(2)}ms`);
};
```

Create `frontend/src/shared/performance/index.ts`:

```ts
export { performanceMarks } from "./performanceMarks";
export {
  isPerformanceLoggingEnabled,
  markPerformance,
  measurePerformance,
} from "./performanceLogger";
```

- [ ] **Step 4: Run the focused test to verify it passes**

Run:

```bash
cd frontend && yarn test src/shared/performance/performanceLogger.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/shared/performance
git commit -m "feat(frontend): add perf logger"
```

## Task 2: Phase 0 Route/Search/Detail Instrumentation

**Files:**
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/app/routes/routeDefinitions.tsx`
- Modify: `frontend/src/features/search/pages/MovieSearchPage.tsx`
- Modify: `frontend/src/features/catalog/pages/MovieDetailPage.tsx`
- Test: `frontend/src/app/AppRoutesArchitecture.test.ts`
- Test: `frontend/src/features/search/pages/MovieSearchPage.test.ts`

- [ ] **Step 1: Write failing tests for route instrumentation**

Update `frontend/src/app/AppRoutesArchitecture.test.ts` to assert that route lazy loading wraps content visibility measurement:

```ts
test("lazy routes measure content visibility", () => {
  const source = readFileSync(routeDefinitionsPath, "utf8");

  expect(source).toContain("markPerformance(performanceMarks.routeContentVisible)");
  expect(source).toContain("measurePerformance(");
});
```

Add a focused search-page test that inspects source-level instrumentation:

```ts
test("marks search results visible when results render", () => {
  const source = readFileSync(
    path.join(projectRoot, "src/features/search/pages/MovieSearchPage.tsx"),
    "utf8",
  );

  expect(source).toContain("performanceMarks.searchResultsVisible");
  expect(source).toContain("performanceMarks.searchSubmitted");
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd frontend && yarn test src/app/AppRoutesArchitecture.test.ts src/features/search/pages/MovieSearchPage.test.ts
```

Expected: FAIL because instrumentation imports and calls do not exist in those files.

- [ ] **Step 3: Instrument route content visibility**

In `frontend/src/app/routes/routeDefinitions.tsx`, import performance helpers:

```ts
import {
  markPerformance,
  measurePerformance,
  performanceMarks,
} from "../../shared/performance";
```

Update `lazyRoute` so the lazy component marks route content when rendered:

```tsx
const lazyRoute = <T extends Record<string, unknown>, K extends keyof T>(
  loadModule: () => Promise<T>,
  exportName: K,
) =>
  lazy(async () => {
    const module = await loadModule();
    const Component = module[exportName] as React.ComponentType;

    return {
      default: () => {
        markPerformance(performanceMarks.routeContentVisible);
        measurePerformance(
          performanceMarks.routeContentVisible,
          performanceMarks.routeFallbackVisible,
          performanceMarks.routeContentVisible,
        );
        return <Component />;
      },
    };
  });
```

- [ ] **Step 4: Mark fallback visibility**

In `frontend/src/App.tsx`, import `useEffect`, `Skeleton`, and performance helpers. Change `RouteFallback` to mark visibility and render skeletons:

```tsx
const RouteFallback = () => {
  useEffect(() => {
    markPerformance(performanceMarks.routeFallbackVisible);
  }, []);

  return (
    <PageContent maxWidth="960px">
      <Surface sx={{ p: 3, mt: 4 }}>
        <Skeleton variant="rounded" height={28} width="36%" />
        <Skeleton variant="text" sx={{ mt: 2 }} width="78%" />
        <Skeleton variant="text" width="64%" />
        <Skeleton variant="rounded" height={280} sx={{ mt: 3 }} />
      </Surface>
    </PageContent>
  );
};
```

- [ ] **Step 5: Instrument search and detail pages**

In `frontend/src/features/search/pages/MovieSearchPage.tsx`, import `useEffect` and performance helpers. Mark submission when search state changes and results when movies render:

```tsx
useEffect(() => {
  if (hasSearchCriteria) {
    markPerformance(performanceMarks.searchSubmitted);
  }
}, [hasSearchCriteria, location.search]);

useEffect(() => {
  if (!isFetching && movies.length > 0) {
    markPerformance(performanceMarks.searchResultsVisible);
    measurePerformance(
      performanceMarks.searchResultsVisible,
      performanceMarks.searchSubmitted,
      performanceMarks.searchResultsVisible,
    );
  }
}, [isFetching, movies.length]);
```

In `frontend/src/features/catalog/pages/MovieDetailPage.tsx`, mark open and visible:

```tsx
useEffect(() => {
  markPerformance(performanceMarks.movieDetailOpened);
}, [movieId]);

useEffect(() => {
  if (movie) {
    markPerformance(performanceMarks.movieDetailContentVisible);
    measurePerformance(
      performanceMarks.movieDetailContentVisible,
      performanceMarks.movieDetailOpened,
      performanceMarks.movieDetailContentVisible,
    );
  }
}, [movie]);
```

- [ ] **Step 6: Run focused tests**

Run:

```bash
cd frontend && yarn test src/app/AppRoutesArchitecture.test.ts src/features/search/pages/MovieSearchPage.test.ts
```

Expected: PASS.

- [ ] **Step 7: Capture baseline build output**

Run:

```bash
cd frontend && yarn build
```

Expected: PASS. Save the chunk output in the task notes before Phase 1 changes.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/App.tsx frontend/src/app/routes/routeDefinitions.tsx frontend/src/features/search/pages/MovieSearchPage.tsx frontend/src/features/catalog/pages/MovieDetailPage.tsx frontend/src/app/AppRoutesArchitecture.test.ts frontend/src/features/search/pages/MovieSearchPage.test.ts
git commit -m "feat(frontend): instrument perf marks"
```

## Task 3: Phase 1 Poster Image Loading

**Files:**
- Modify: `frontend/src/shared/media/PosterImage.tsx`
- Modify: `frontend/src/shared/media/imageUrls.test.ts`
- Test: create or update `frontend/src/shared/media/PosterImage.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `frontend/src/shared/media/PosterImage.test.tsx`:

```tsx
import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import { MinioImageSize } from "./imageUrls";
import PosterImage from "./PosterImage";

describe("PosterImage", () => {
  test("lazy-loads and exposes responsive poster sources by default", () => {
    render(
      <PosterImage
        imageUrlToken="poster-token"
        size={MinioImageSize.Small}
        sizes="80px"
      />,
    );

    const image = screen.getByRole("img", { name: "movie poster" });
    expect(image).toHaveAttribute("loading", "lazy");
    expect(image).toHaveAttribute("decoding", "async");
    expect(image).toHaveAttribute("sizes", "80px");
    expect(image).toHaveAttribute(
      "srcset",
      expect.stringContaining("poster-token_size_120x180.jpg 120w"),
    );
    expect(image).toHaveAttribute(
      "srcset",
      expect.stringContaining("poster-token_size_600x900.jpg 600w"),
    );
  });

  test("allows critical posters to load eagerly", () => {
    render(
      <PosterImage
        imageUrlToken="poster-token"
        loading="eager"
        size={MinioImageSize.Large}
      />,
    );

    expect(screen.getByRole("img", { name: "movie poster" })).toHaveAttribute(
      "loading",
      "eager",
    );
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd frontend && yarn test src/shared/media/PosterImage.test.tsx
```

Expected: FAIL because `PosterImage` does not set the new attributes.

- [ ] **Step 3: Implement PosterImage props**

Update `frontend/src/shared/media/PosterImage.tsx`:

```tsx
type PosterImageProps = {
  imageUrlToken?: string;
  loading?: "eager" | "lazy";
  size: MovieImageSize;
  sizes?: string;
  sx?: SxProps<Theme>;
};

const PosterImage = ({
  imageUrlToken,
  loading = "lazy",
  size,
  sizes,
  sx,
}: PosterImageProps) => {
  const src = imageUrlToken
    ? getMovieImageUrl(imageUrlToken, size)
    : placeholderSearch;
  const srcSet = imageUrlToken
    ? [
        `${getMovieImageUrl(imageUrlToken, MinioImageSize.Small)} 120w`,
        `${getMovieImageUrl(imageUrlToken, MinioImageSize.Large)} 600w`,
      ].join(", ")
    : undefined;

  return (
    <CardMedia
      component="img"
      alt="movie poster"
      decoding="async"
      loading={loading}
      sizes={sizes}
      src={src}
      srcSet={srcSet}
      sx={sx}
    />
  );
};
```

- [ ] **Step 4: Run focused tests**

Run:

```bash
cd frontend && yarn test src/shared/media/PosterImage.test.tsx src/shared/media/imageUrls.test.ts
```

Expected: PASS.

- [ ] **Step 5: Update poster callers with sizes**

Use these sizes:

- Grid/poster cards: `sizes="(max-width: 600px) 45vw, (max-width: 900px) 30vw, 220px"`
- List rows: `sizes="52px"`
- Hero/featured critical poster if rendered above the fold: `loading="eager"`

- [ ] **Step 6: Run component tests that render posters**

Run:

```bash
cd frontend && yarn test src/features/catalog/components src/features/search/components src/features/home/components src/features/engagement
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/shared/media frontend/src/features/catalog frontend/src/features/search frontend/src/features/home frontend/src/features/engagement
git commit -m "perf(frontend): optimize poster loading"
```

## Task 4: Phase 1 React Query Cache Tuning

**Files:**
- Modify: `frontend/src/shared/api/queryClient.ts`
- Modify: `frontend/src/features/search/api/searchQueries.ts`
- Modify: `frontend/src/features/catalog/api/movieQueries.ts`
- Modify: `frontend/src/features/home/api/useFeaturedMovie.ts`
- Modify: `frontend/src/features/home/api/useMoviesByGenre.ts`
- Modify: `frontend/src/features/engagement/watchlist/api/watchlistQueries.ts`
- Modify: `frontend/src/features/engagement/rating/api/ratingQueries.ts`
- Test: existing query tests beside each modified query module.

- [ ] **Step 1: Write failing query tests**

Add assertions to existing query tests:

```ts
expect(searchQueries.movies(params).staleTime).toBe(5 * 60 * 1000);
expect(searchQueries.movies(params).placeholderData).toBeDefined();
```

For user-owned queries:

```ts
expect(
  watchlistQueries.currentUserItems({ page: 0, size: 20, username: "ada" })
    .staleTime,
).toBe(30 * 1000);
```

For catalog detail:

```ts
expect(movieQueries.detail(42).staleTime).toBe(5 * 60 * 1000);
```

- [ ] **Step 2: Run query tests to verify failure**

Run:

```bash
cd frontend && yarn test src/features/search/api src/features/catalog/api src/features/home/api src/features/engagement/watchlist/api src/features/engagement/rating/api
```

Expected: FAIL on missing `staleTime` or `placeholderData`.

- [ ] **Step 3: Implement cache constants**

Create constants in `frontend/src/shared/api/queryClient.ts`:

```ts
export const CATALOG_STALE_TIME_MS = 5 * 60 * 1000;
export const USER_STATE_STALE_TIME_MS = 30 * 1000;

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: CATALOG_STALE_TIME_MS,
    },
  },
});
```

- [ ] **Step 4: Add query-specific options**

Import `keepPreviousData` from `@tanstack/react-query` in paged query modules.

For search:

```ts
placeholderData: keepPreviousData,
staleTime: CATALOG_STALE_TIME_MS,
```

For watchlist and ratings page queries:

```ts
placeholderData: keepPreviousData,
staleTime: USER_STATE_STALE_TIME_MS,
```

For movie detail and home genre/featured data:

```ts
staleTime: CATALOG_STALE_TIME_MS,
```

For user rating and watched movie id queries:

```ts
staleTime: USER_STATE_STALE_TIME_MS,
```

- [ ] **Step 5: Run query tests**

Run:

```bash
cd frontend && yarn test src/features/search/api src/features/catalog/api src/features/home/api src/features/engagement/watchlist/api src/features/engagement/rating/api
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/shared/api/queryClient.ts frontend/src/features
git commit -m "perf(frontend): tune query caching"
```

## Task 5: Phase 1 Movie Detail Prefetch

**Files:**
- Create: `frontend/src/features/catalog/hooks/usePrefetchMovieDetail.ts`
- Modify: `frontend/src/features/catalog/components/PosterMovieCard.tsx`
- Modify: `frontend/src/features/catalog/components/MovieListRow.tsx`
- Modify: other movie-card components only if they own link behavior.
- Test: `frontend/src/features/catalog/hooks/usePrefetchMovieDetail.test.tsx`

- [ ] **Step 1: Write failing hook test**

Create `frontend/src/features/catalog/hooks/usePrefetchMovieDetail.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, test, vi } from "vitest";
import { usePrefetchMovieDetail } from "./usePrefetchMovieDetail";

describe("usePrefetchMovieDetail", () => {
  test("prefetches valid movie ids and ignores missing ids", () => {
    const queryClient = new QueryClient();
    const prefetchQuery = vi
      .spyOn(queryClient, "prefetchQuery")
      .mockResolvedValue();
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => usePrefetchMovieDetail(), { wrapper });

    result.current(42);
    result.current(undefined);

    expect(prefetchQuery).toHaveBeenCalledTimes(1);
    expect(prefetchQuery).toHaveBeenCalledWith(
      expect.objectContaining({ queryKey: ["catalog", "movie", 42] }),
    );
  });
});
```

- [ ] **Step 2: Run hook test to verify failure**

Run:

```bash
cd frontend && yarn test src/features/catalog/hooks/usePrefetchMovieDetail.test.tsx
```

Expected: FAIL because the hook does not exist.

- [ ] **Step 3: Implement prefetch hook**

Create `frontend/src/features/catalog/hooks/usePrefetchMovieDetail.ts`:

```ts
import { useQueryClient } from "@tanstack/react-query";
import { movieQueries } from "../api/movieQueries";
import {
  markPerformance,
  measurePerformance,
  performanceMarks,
} from "../../../shared/performance";

export const usePrefetchMovieDetail = () => {
  const queryClient = useQueryClient();

  return (movieId?: number) => {
    if (movieId === undefined) {
      return;
    }

    markPerformance(performanceMarks.movieDetailPrefetchStart);
    void queryClient
      .prefetchQuery(movieQueries.detail(movieId))
      .then(() => {
        markPerformance(performanceMarks.movieDetailPrefetchComplete);
        measurePerformance(
          performanceMarks.movieDetailPrefetchComplete,
          performanceMarks.movieDetailPrefetchStart,
          performanceMarks.movieDetailPrefetchComplete,
        );
      });
  };
};
```

- [ ] **Step 4: Wire prefetch into movie links**

In `PosterMovieCard.tsx` and `MovieListRow.tsx`, call the hook and add:

```tsx
onFocus={() => prefetchMovieDetail(movie.id)}
onMouseEnter={() => prefetchMovieDetail(movie.id)}
onTouchStart={() => prefetchMovieDetail(movie.id)}
```

Attach handlers to the link/action area, not to bookmark/remove buttons.

- [ ] **Step 5: Run focused tests**

Run:

```bash
cd frontend && yarn test src/features/catalog/hooks/usePrefetchMovieDetail.test.tsx src/features/catalog/components
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/features/catalog
git commit -m "perf(frontend): prefetch movie details"
```

## Task 6: Phase 2 Route Chunk Review

**Files:**
- Modify only if measured bundle output supports it:
  - `frontend/src/app/routes/routeDefinitions.tsx`
  - feature page export files under `frontend/src/features/**/index.ts`
- Test: `frontend/src/app/AppRoutesArchitecture.test.ts`

- [ ] **Step 1: Capture post-Phase-1 build output**

Run:

```bash
cd frontend && yarn build
```

Expected: PASS. Compare emitted chunk names and gzip sizes with the Phase 0 baseline.

- [ ] **Step 2: Inspect route import shape**

Check whether lazy route imports use feature barrels that combine unrelated route code:

```tsx
const MovieDetailPage = lazyRoute(
  () => import("../../features/catalog"),
  "MovieDetailPage",
);
```

If the build output shows route chunks are coupled more than necessary, replace high-traffic route imports with direct page imports:

```tsx
const MovieDetailPage = lazyRoute(
  () => import("../../features/catalog/pages/MovieDetailPage"),
  "default",
);
```

- [ ] **Step 3: Update architecture test**

Keep the test asserting `Suspense`, `lazyRoute`, and private/public route boundaries. If direct imports are used, assert that search, home, catalog, and account pages are still lazy-loaded by route definition.

- [ ] **Step 4: Run route tests and build**

Run:

```bash
cd frontend && yarn test src/app/AppRoutesArchitecture.test.ts
cd frontend && yarn build
```

Expected: PASS. Build output should show equal or smaller main browsing-path chunks.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/routes/routeDefinitions.tsx frontend/src/app/AppRoutesArchitecture.test.ts frontend/src/features
git commit -m "perf(frontend): sharpen route chunks"
```

## Final Verification

- [ ] Run frontend tests:

```bash
cd frontend && yarn test
```

Expected: all tests pass.

- [ ] Run frontend lint:

```bash
cd frontend && yarn run lint
```

Expected: exit code 0.

- [ ] Run frontend build:

```bash
cd frontend && yarn build
```

Expected: exit code 0 with recorded chunk output.

- [ ] Review performance logging manually:

```bash
cd frontend && VITE_ENABLE_PERF_LOGS=true yarn start
```

Expected: route/search/detail interactions emit `[perf] ...` console lines. Without `VITE_ENABLE_PERF_LOGS=true`, there are no performance log lines.

## Self-Review

- Spec coverage: Phase 0 measurement is covered by Tasks 1-2; Phase 1 image/cache/fallback/prefetch behavior is covered by Tasks 2-5; Phase 2 bundle and route tuning is covered by Task 6.
- Placeholders: no deferred placeholders are required for implementation steps; deferred work from the design remains explicitly out of scope.
- Type consistency: performance helper names, mark names, and query constants are defined before use in later tasks.
