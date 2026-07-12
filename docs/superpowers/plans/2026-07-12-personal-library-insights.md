# Personal Library Insights And Watchlist Decisions

**Status:** Implemented
**Scope:** Ratings page and watchlist page
**Product stage:** Personal library and taste foundation

## Goal

Turn the two personal movie collections into distinct, useful destinations:

- **Ratings answers:** “What does my movie taste look like?”
- **Watchlist answers:** “What should I watch next?”

Keep posters and movie browsing visually primary. Add a restrained cinematic insight layer rather
than turning either page into a generic analytics dashboard.

## Current Baseline And Problems

- Both pages request only page `0` with 30 items.
- Both pages ignore additional result pages.
- Existing statistics are calculated from those first 30 loaded items, so they become inaccurate
  for larger collections.
- Sorting is performed in the frontend and therefore also affects only the first 30 items.
- The watchlist has a random one-movie dialog, but it does not use runtime, mood, genre, age in the
  watchlist, or explain why the movie is a good choice.
- The ratings page exposes a few aggregate tiles, but it does not show rating distribution, taste
  affinity, or differences from IMDb ratings.
- Watchlist page responses already embed `MovieRecord`, but `WatchedMovieMapper` performs one catalog
  lookup per entry. This N+1 lookup should be removed before progressive loading is added.
- Rating page data requires a second batched movie lookup in the frontend. A dedicated library
  response can return rated movies already enriched while leaving the existing rating endpoints
  unchanged.

## Product Decisions

1. Keep ratings and watchlist as separate pages with a common personal-library visual language.
2. Add new library-specific endpoints. Do not break the existing account rating/watchlist endpoints;
   movie-detail and shared state still use them.
3. Compute insights from the complete account collection before pagination.
4. Perform library sorting on the server so later pages cannot reorder already rendered entries.
5. Use an explicit “Load more” action rather than invisible endless scrolling. It is predictable,
   accessible, and appropriate for a personal collection.
6. Replace the four equal-weight statistic tiles with one purposeful insight surface on each page.
7. The first watchlist picker is explainable and deterministic, not embedding-dependent:
   `SAFE_BET`, `FORGOTTEN_GEM`, and `WILD_CARD`.
8. Reuse the Tonight Mode preference controls, using the watchlist as a taste signal while always
   excluding its movies from the returned picks.
9. Do not add a chart dependency. Build the compact distribution chart with Material UI and CSS.
10. Do not introduce diary semantics, custom lists, actors/directors, or annual summaries in this
    slice.
11. Keep the new GET library routes aligned with the existing public profile/watchlist/ratings
    visibility. The Watchlist Tonight POST remains authenticated and always uses the current account.

## Backend Contracts

### Bulk catalog reference

Extend `catalog::reference` with a non-web, unpaged bulk lookup used across modules:

```java
List<MovieRecord> findMoviesByIds(Collection<Long> movieIds);
```

Implementation requirements:

- De-duplicate IDs.
- Return no duplicate movies.
- Preserve no ordering contract; callers build an ID map.
- Chunk repository queries internally to avoid an oversized SQL `IN` clause.
- Keep the existing paginated web-facing method unchanged.

Use this method to remove the watchlist N+1 lookup and to enrich rating-library entries.

### Rating library

Add:

```text
GET /api/account/{username}/library/ratings?page=0&size=30&sort=SCORE_DESC
```

Response:

```text
RatingLibraryResponse
  items: PagedResponse<RatedMovieRecord>
  insights: RatingTasteInsights
```

`RatedMovieRecord`:

- `accountId`
- `movieId`
- `rating`
- `ratedAt` from the rating entity's creation timestamp
- `movie: MovieRecord`

`RatingTasteInsights`:

- `totalRatings`
- `averageUserRating`
- `distribution: List<RatingDistributionBucket>`
- `favoriteGenres: List<TasteFacet>` (maximum five)
- `favoriteDecades: List<TasteFacet>` (maximum five)
- `averageImdbDifference` (`user rating - IMDb rating`)
- `definingMovies: List<RatedMovieInsight>` (maximum five)
- `biggestPositiveDifference: RatedMovieInsight?`
- `biggestNegativeDifference: RatedMovieInsight?`

Use explicit distribution bands so decimal scores remain unambiguous:

- `0–3.9`
- `4–5.9`
- `6–6.9`
- `7–7.9`
- `8–8.9`
- `9–10`

`TasteFacet` contains `label`, `movieCount`, and `averageUserRating`. Rank genres by positive taste
weight, not raw occurrence alone. A deterministic v1 weight is:

```text
sum(max(userRating - 5.5, 0))
```

Tie-break by movie count and then label. A movie may contribute to multiple genre facets.

`RatedMovieInsight` contains `movie`, `userRating`, and nullable `imdbDifference`. Rank defining
movies by user rating descending, IMDb vote count descending, then movie ID ascending.

Supported server-side sorts:

- `SCORE_DESC`, `SCORE_ASC`
- `RATED_AT_DESC`, `RATED_AT_ASC`
- `IMDB_DESC`, `IMDB_ASC`
- `TITLE_ASC`

### Watchlist library

Add:

```text
GET /api/account/{username}/library/watchlist?page=0&size=30&sort=ADDED_AT_DESC
```

Response:

```text
WatchlistLibraryResponse
  items: PagedResponse<WatchedMovieRecord>
  insights: WatchlistInsights
```

`WatchlistInsights`:

- `totalMovies`
- `totalRuntimeMinutes`
- `averageImdbRating`
- `topGenres: List<LibraryFacet>` (maximum five)
- `oldestSavedAt`
- `quickWatchCount` (runtime at most 100 minutes)

Supported server-side sorts:

- `ADDED_AT_DESC`, `ADDED_AT_ASC`
- `IMDB_DESC`, `IMDB_ASC`
- `RUNTIME_DESC`, `RUNTIME_ASC`
- `TITLE_ASC`

### Watchlist Tonight

Expose the watchlist as a recommendation candidate source without allowing the recommendation module
to access engagement persistence:

```java
@NamedInterface("recommendation")
interface WatchlistCandidateProvider {
  List<WatchlistCandidate> findCandidates(Long accountId);
}
```

`WatchlistCandidate` contains `MovieRecord movie` and `Instant addedAt`.

Allow the recommendation module dependency `engagement::recommendation`, then add:

```text
POST /api/recommendations/watchlist-tonight
```

The endpoint must require an authenticated `@CurrentUser`; never accept a username or account ID
from the request body.

`WatchlistTonightRequest`:

- `maxRuntimeMinutes?`
- `movieGenres` (maximum eight)
- `mood?`
- `era?`
- `excludedMovieIds` (maximum 500)
- `seed?`

`WatchlistTonightResponse`:

- `seed`
- `picks: List<WatchlistTonightPick>`

Each pick contains:

- `movie`
- `role`: `SAFE_BET`, `FORGOTTEN_GEM`, or `WILD_CARD`
- `explanation`

Picker rules:

1. Apply runtime, explicit genre, mood-derived genre, era, and excluded-ID constraints.
2. `SAFE_BET`: choose the strongest quality/confidence result.
3. `FORGOTTEN_GEM`: choose the oldest acceptable saved movie not already selected.
4. `WILD_CARD`: choose a seeded result that adds genre/year diversity.
5. If a role has no candidate, fill from the remaining quality-ranked candidates.
6. Return at most three unique movies, excluding all movies currently in the account's watchlist.
7. The same seed and request produce the same order.
8. Excluding previously shown IDs produces a different set where candidates remain.

Extract the shared mood/era filtering helpers from the existing `TonightMode`; do not duplicate the
mapping tables.

## Backend Implementation Tasks

### Task 1: Bulk movie references and N+1 removal

Modify:

- `src/main/java/com/thecodinglab/imdbclone/catalog/api/MovieReferenceService.java`
- `src/main/java/com/thecodinglab/imdbclone/catalog/internal/MovieCatalog.java`
- `src/main/java/com/thecodinglab/imdbclone/engagement/internal/Watchlist.java`
- `src/main/java/com/thecodinglab/imdbclone/engagement/internal/mapper/WatchedMovieMapper.java`

Tests:

- Bulk lookup de-duplicates IDs and handles empty input.
- A watchlist page uses one bulk catalog lookup rather than one lookup per entry.
- Omit a stale engagement entry when its catalog movie no longer exists, log only the structured
  movie ID, and calculate page totals from valid joined entries. Do not fail the entire page.

### Task 2: Personal library snapshot and insight calculators

Add an internal engagement snapshot loader that:

- Loads all account rating or watchlist entries.
- Fetches movie records through the bulk catalog reference interface.
- Joins by movie ID.
- Produces deterministic full-collection insights.
- Sorts the full joined collection before creating the requested `PagedResponse`.

Keep calculations in small pure classes so genre, decade, distribution, IMDb difference, and sort
behavior have direct unit tests.

Add new records and service methods under `engagement/api`, exposed through the existing `profile`
named interface. Wire the two new controller routes through `AccountEngagementController`.

### Task 3: Watchlist candidate interface and picker

Add the narrow engagement candidate interface, update recommendation module dependencies, and build
the seeded three-role ranker. Reuse existing Tonight Mode constraints and explanation style.

Add bounded Micrometer metrics:

- `imdb.library.insights.duration` tagged by `library=ratings|watchlist`
- `imdb.recommendation.watchlist_tonight.requests` tagged by `result=empty|partial|complete`
- `imdb.recommendation.watchlist_tonight.duration`

Never tag metrics or logs with usernames, raw preference values, or the contents of a library.

### Task 4: Contracts and architecture verification

- Add controller tests for authentication, validation, pagination, and stable responses.
- Add service tests with collections larger than 30 entries to prove insights are complete.
- Add deterministic picker tests for all roles, constraints, seed reuse, exclusions, and fewer than
  three candidates.
- Run `ModulithArchitectureTest` after adding `engagement::recommendation`.
- Start the backend, update the checked-in OpenAPI spec, and regenerate the frontend client.
- Do not edit generated Axios files manually.

## Frontend Structure

Create a small shared personal-library area under:

```text
frontend/src/features/engagement/library/
```

Recommended shared components:

- `LibraryMetric.tsx` for compact labeled values inside a larger surface.
- `LibraryLoadMore.tsx` for total/loaded count and accessible pagination action.
- `LibraryMovieGrid.tsx` only if existing ratings/watchlist grids cannot be reused without branching.

Keep page-specific insight components with their owning features:

```text
rating/components/RatingsTasteSnapshot.tsx
rating/components/RatingDistribution.tsx
rating/components/RatingsHighlights.tsx
watchlist/components/WatchlistDecisionPanel.tsx
watchlist/components/WatchlistTonightChoices.tsx
```

Move reusable Tonight controls from the homepage panel to:

```text
frontend/src/features/recommendation/tonight/TonightPreferenceControls.tsx
```

Both homepage Tonight Mode and watchlist decision mode should consume the extracted control. The
control owns selection/toggle behavior only; each surface owns its request and copy.

## Frontend Page Design

### Ratings: Taste Snapshot

Replace `RatingsStats` with one `RatingsTasteSnapshot` surface above the toolbar.

Desktop composition:

- Left two-thirds: accessible distribution bars with count labels.
- Right third: average score, favorite genre, favorite decade, and IMDb difference.
- Bottom poster strip: up to five defining films.
- One compact comparison row for the strongest positive and negative IMDb differences.

Mobile composition:

- Narrative and metrics first.
- Horizontally compact distribution with readable labels.
- Defining posters in a small horizontal strip.
- Difference highlights stack vertically.

Generate deterministic narrative copy from structured insights, for example:

```text
Your strongest signal is science fiction, especially films from the 1990s.
You rate movies 0.4 points higher than IMDb on average.
```

Rules:

- With fewer than three ratings, say that the taste profile is still forming.
- Do not claim a favorite facet without data.
- Keep existing grid/list views and deletion behavior.
- Replace client-side sorting with the server sort query.
- Use the first response's complete insights while flattening item pages with `useInfiniteQuery`.

### Watchlist: Decision Panel

Replace `WatchlistStats` and the random `PickForMeDialog` with one `WatchlistDecisionPanel` above the
toolbar.

The collapsed/default state shows:

- Total movies and total runtime.
- Quick-watch count.
- Top genre.
- A primary “Choose from my watchlist” action.

The expanded state shows extracted preference chips and three role-based results. Each result uses a
poster, title, year/runtime, role label, and explanation. Use a three-column grid on desktop and a
single-column stack on mobile.

Actions:

- `Open movie`
- `Show three others` using the same seed plus shown IDs as exclusions
- `Reset preferences`

Remove the old random picker dialog and `pickRandomWatchlistItem` utility once no callers remain.
Keep watchlist remove/undo behavior unchanged.

### Progressive browsing

Convert both personal-page queries to `useInfiniteQuery`:

- Render the first 30 immediately.
- Flatten and de-duplicate entries by movie ID.
- Use the backend's stable global sort.
- Show `Loaded 30 of 86` next to the `Load more` button.
- Preserve grid/list choice in local storage.
- Reset loaded pages when the sort changes.
- Do not compute statistics from flattened pages; use server insights.

## Frontend Tests

Add or update tests for:

- Query keys include username, page cursor, page size, and server sort.
- Infinite-page flattening does not duplicate movies.
- Ratings profile uses complete backend insights even when the first page contains 30 items.
- Small-history ratings copy is honest and avoids empty claims.
- Distribution bars expose accessible score-band and count labels.
- IMDb difference wording handles positive, negative, zero, and missing values.
- Watchlist decision results display all three roles and explanations.
- Preference toggles can be selected and cleared.
- “Show three others” sends prior movie IDs as exclusions and retains the seed.
- Removing a watchlist movie and deleting a rating still invalidate all relevant library queries.
- Load-more behavior appends results and respects the selected server sort.

## Responsive E2E Coverage

Add `frontend/e2e/personal-library.spec.ts` with fully mocked APIs and authenticated state.

Desktop scenarios:

- Ratings insight layout, histogram labels, defining posters, sort, list/grid switch, and load more.
- Watchlist decision panel, constraints, three picks, refresh, movie navigation, and removal undo.

Mobile scenarios:

- No horizontal page overflow.
- Preference chips wrap or scroll without clipping.
- Poster results remain readable and actions remain reachable.
- Distribution labels do not overlap.
- Load-more and grid/list controls remain keyboard/touch accessible.

## Verification Commands

Backend:

```bash
./gradlew spotlessCheck
./gradlew test
./gradlew integrationTest
```

Contract regeneration after backend starts:

```bash
cd frontend
yarn run updateOpenApiSpec
yarn run build:moviesGen
```

Frontend:

```bash
cd frontend
yarn run lint
yarn test
yarn build
yarn playwright test e2e/personal-library.spec.ts --project=desktop-chromium
yarn playwright test e2e/personal-library.spec.ts --project=mobile-chromium
```

Architecture and patch hygiene:

```bash
./gradlew test --tests ModulithArchitectureTest
git diff --check
```

## Acceptance Criteria

- A collection with more than 30 entries reports complete, accurate insights.
- Sorting remains stable and correct while additional pages are loaded.
- Watchlist page returns only unseen catalog movies from its decision picker.
- The picker returns three unique, explained roles where enough candidates exist.
- Ratings page makes personal taste visible through distribution, facets, defining films, and IMDb
  differences.
- Both pages remain poster-led and visually consistent with the dark cinematic design system.
- Existing rating, watchlist, movie-detail, homepage Tonight Mode, and undo flows remain intact.
- No N+1 catalog lookup remains in watchlist page mapping.
- No usernames, raw library contents, or preferences appear in metrics.
- Backend, frontend, architecture, and responsive E2E checks pass.

## Explicit Follow-Ups, Not Part Of This Slice

- Genre/year/runtime search across the personal library.
- Embedding-based taste vectors and homepage personalization.
- Diary entries, watched dates, rewatches, and annual summaries.
- Named custom lists.
- Actor/director taste statistics, pending person entities.
- Import/export.
- Collaborative filtering.

Update `docs/product-roadmap.md` only after this slice is implemented and verified.
