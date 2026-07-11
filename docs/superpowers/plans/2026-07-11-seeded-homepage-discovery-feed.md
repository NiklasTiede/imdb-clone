# Seeded Homepage Discovery Feed Implementation Plan

> **For agentic workers:** Execute tasks sequentially. Keep the feed deterministic for one browser
> document, verify each backend slice before connecting the UI, and use the `frontend-design` skill
> plus `playwright-cli` for the responsive implementation review.

**Goal:** Replace the homepage's three fixed genre rows with an effectively endless, varied stream
of high-quality movie carousels whose topics and movie ordering change on a hard refresh, while
preserving the exact feed and scroll state when a user opens a movie and navigates back.

**Architecture:** The recommendation module owns a versioned section catalog, seeded section
composition, ranking, diversity, global deduplication, and an opaque cursor. The catalog module
continues to own OpenSearch query construction and exposes typed candidate retrieval through its
existing `catalog::recommendation` interface. A bounded `POST /api/recommendations/home-feed`
contract returns a featured movie and pages of fully composed sections. The React homepage uses a
document-scoped feed instance with TanStack Query infinite pages and restores both vertical and
horizontal scroll state. Evergreen semantic topic embeddings are precomputed and versioned by the
catalog module rather than generated during every request.

**Tech Stack:** Java 21, Spring Boot, Spring Modulith, OpenSearch, the existing embedding client,
React 19, TypeScript, Material UI 9, TanStack Query 5, Vitest, Testing Library, and Playwright.

---

## Current-State Findings

- `HomePage.tsx` hard-codes Horror, Thriller, and Sci-Fi in a fixed order.
- `useMoviesByGenre.ts` retrieves one page of 20 OpenSearch hits, sorts only that page by IMDb
  rating in the browser, and keeps 15. It therefore cannot promise the best catalog candidates.
- `useFeaturedMovie.ts` retrieves 30 movies, filters and sorts in the browser, and chooses a daily
  item. It does not change on refresh and can duplicate a carousel movie.
- `MovieCarousel` is already a useful responsive horizontal presentation component, but it has no
  stable section identity, pagination integration, item explanation, or scroll restoration seam.
- The recommendation foundation already provides content-based similar movies through
  `MovieRecommendationCandidateProvider`, `ContentBasedRecommendations`, and
  `ContentRecommendationRanker`. It has no homepage feed contract or section catalog yet.
- `MovieSearchRequest` supports year, runtime, genre, and type filters, but not rating confidence,
  discovery themes, or a bounded recommendation candidate pool.
- The frontend has no homepage infinite query, sentinel, or route/home scroll restoration.
- Existing frontend observability reports technical performance and errors, not discovery product
  events such as section impressions or movie selections.

## Product Decisions

- “Endless” means an infinite-feeling vertical feed of horizontal carousels, with a deliberate
  upper bound of 24 successful sections per feed instance (initially 8 pages of 3). This prevents
  unlimited work and gives the user a meaningful end state.
- A hard browser refresh creates a new feed instance and therefore a new topic mix and movie order.
  Opening a movie and using browser Back restores the same seed, loaded pages, vertical position,
  and each carousel's horizontal position.
- The server owns section definitions and ranking. Do not ship semantic prompts, quality policy, or
  ranking weights as editable frontend inputs.
- Variation must be seeded and quality-preserving. Retrieve a strong candidate pool, rank it, then
  add a small deterministic jitter; never uniformly shuffle the whole catalog.
- The first page always mixes three different families: one broad/high-confidence section, one
  semantic theme, and one structured/editorial section. Later pages use weighted round-robin with
  family cooldowns.
- A movie appears at most once in a feed instance, including the featured hero. A section that
  cannot retain at least 8 unique movies is topped up from a deeper window or replaced by a
  deterministic fallback section.
- Anonymous discovery is the first release. Account personalization and collaborative filtering
  begin only after impression/open/watchlist/rating signals are measured reliably.
- A missing semantic embedding or an unhealthy semantic candidate query skips that definition and
  fills the slot with a deterministic structured fallback; it does not fail the feed page.

## Discovery Strategy

### Section Catalog

Create a backend-owned, versioned registry. Each definition has a stable ID, title, subtitle,
family, strategy kind, optional semantic prompt, typed catalog constraints, quality policy,
selection weight, minimum candidate count, candidate-pool size, display count, cooldown group, and
optional View All destination.

Initial families and representative topics:

| Family | Structured examples | Semantic/editorial examples |
| --- | --- | --- |
| Broad quality | Acclaimed movies, Recent standouts | Crowd pleasers worth revisiting |
| Genre | Great horror, Animated favorites | Sci-fi about identity |
| Genre combination | Crime thrillers, Romantic comedies | Heists gone wrong |
| Era | 1990s thrillers, 1970s classics | Political paranoia |
| Runtime | Great movies under 100 minutes | A gripping short watch |
| Format/type | Acclaimed documentaries, Miniseries | True stories that feel unbelievable |
| Hidden gems | High rating with moderate vote count | Quiet films about grief |
| Theme/mood | — | Found family, Slow-burn mysteries, Small-town secrets |

Start with roughly 40–60 curated definitions so a 24-section feed can vary without becoming thin.
Definitions remain code or validated configuration in recommendation. At reconciliation time,
recommendation passes typed topic definitions through `catalog::recommendation`; catalog owns the
embedding client and projects them into a derived OpenSearch index keyed by section ID, prompt
version, embedding model, and dimensions.

### Seeded Composition

1. The first request accepts no seed or a client-generated random feed instance token. The server
   normalizes it into a feed seed and returns the canonical seed plus `strategyVersion`.
2. Use a deterministic PRNG derived from `hash(feedSeed, strategyVersion)` to construct a weighted
   section order. Guarantee first-page family variety, then apply family/cooldown constraints.
3. Derive an independent section seed from `hash(feedSeed, sectionId, strategyVersion)`. Replaying
   a page therefore returns the same content without storing a server session.
4. The opaque cursor contains only signed/validated continuation facts such as strategy version and
   next section offset. The deterministic order is reproducible; do not serialize all results.
5. The client sends all movie IDs already shown. Enforce a request-size limit and use them for
   cross-page deduplication.

### Candidate Selection And Ranking

- Retrieve a bounded candidate pool of about 80–120 movies per section. Deep catalog pagination is
  not needed for this endpoint; reserve OpenSearch PIT plus `search_after` for future View All pages.
- Apply structured filters inside OpenSearch: type, genre, year/runtime range, minimum IMDb score,
  and a minimum or bounded rating count where relevant.
- Semantic themes use the stored section embedding in a filtered kNN query. Never embed evergreen
  prompts on the synchronous feed request path.
- Compute a shared score from relevance/semantic similarity, IMDb quality with vote confidence,
  freshness where the section calls for it, and novelty/diversity. Add only 8–12% seeded jitter.
- Select greedily while applying intra-section diversity (genre/type/decade) and the feed-wide seen
  set. Record a concise reason and machine-readable tracking context for every selected item.
- Do not use OpenSearch `random_score` with `_seq_no` as the main strategy: updates change sequence
  numbers, so it cannot provide stable replay. Application-level seeded jitter over a bounded,
  sorted candidate pool is predictable and testable.

## API Contract

Use `POST /api/recommendations/home-feed` because the excluded-ID list can grow to hundreds of IDs
and should not be encoded into a URL.

First request:

```json
{
  "feedInstanceId": "browser-document UUID",
  "seed": null,
  "cursor": null,
  "excludedMovieIds": []
}
```

Continuation request:

```json
{
  "feedInstanceId": "browser-document UUID",
  "seed": "canonical server seed",
  "cursor": "opaque cursor",
  "excludedMovieIds": [148, 27, 91]
}
```

Response shape:

```text
HomeFeedResponse
  seed
  strategyVersion
  featuredMovie       # first page only
  sections[]
  nextCursor
  exhausted

HomeFeedSection
  id
  title
  subtitle
  family
  kind
  items[]
  viewAll             # optional typed destination

HomeFeedItem
  movie
  reason
  explanation
  trackingContext
```

If a cursor is incompatible with the active strategy version, return an explicit typed restart
problem instead of silently mixing two strategies. Validate page size, excluded IDs, UUID/token
lengths, and cursor integrity at the web boundary.

## Frontend State Model

- Create a module-level `feedInstanceId = crypto.randomUUID()`. It survives SPA navigation but a
  hard refresh loads a new JavaScript document and creates a new ID.
- Query key: `['home', 'feed', feedInstanceId]`, later extended with a taste-profile version.
- Use `useInfiniteQuery` with `staleTime: Infinity` and a 30–60 minute `gcTime`. Do not use a small
  `maxPages`: browser Back must be able to render all pages already loaded. The server's section
  limit supplies the safety bound.
- `getNextPageParam` passes the returned seed/cursor and the union of movie IDs from all pages.
- Render `data.pages.flatMap(page => page.sections)`. An `IntersectionObserver` sentinel fetches the
  next page, guarded by `hasNextPage && !isFetchingNextPage`; retain an accessible Load More button
  as a keyboard/manual fallback.
- Keep document-scoped restoration maps keyed by feed instance and section ID. Save `window.scrollY`
  and each carousel's `scrollLeft`; restore after cached pages have rendered. Do not persist them
  across a hard reload because refresh is intentionally a new discovery session.

## Non-Goals

- No per-account personalization or collaborative filtering in the first release.
- No user-authored arbitrary semantic prompts on the homepage.
- No unbounded server loop, unlimited section generation, or deep hit pagination.
- No online embedding generation for stable catalog topics.
- No replacement of the existing movie-detail similar-movies endpoint.
- No broad OpenSearch abstraction rewrite; extend the existing catalog recommendation seam.

## Acceptance Criteria

- Two hard refreshes normally produce different section orders and movie orders; replaying the same
  seed/cursor/exclusion input produces byte-equivalent section IDs and movie IDs.
- Page one contains three distinct section families and no repeated movie, including the hero.
- Up to 24 successful sections load in groups of three without repeated section IDs or movie IDs.
- Every displayed section contains 8–15 unique movies; insufficient sections are replaced, not
  rendered as sparse or empty rows.
- Browser Back restores the same feed, all loaded pages, vertical position, and horizontal carousel
  positions. Hard refresh intentionally resets them.
- Later-page failure leaves every loaded section usable and shows a retry only at the feed tail.
- Missing semantic infrastructure degrades to structured sections and does not blank the homepage.
- At 1440, 1024, 390, and 320 pixel widths, the hero and carousels have no document-level horizontal
  overflow, desktop controls remain aligned, and touch scrolling remains native.
- Feed latency, candidate shortages, fallbacks, section impressions, movie impressions, and movie
  opens are observable without logging private data or raw future taste signals.

---

### Task 1: Lock The Feed Contract And Pure Seed Semantics

**Files:**
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/api/HomeFeedRequest.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/api/HomeFeedResponse.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/api/HomeFeedSection.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/api/HomeFeedItem.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/api/HomeFeedService.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/internal/HomeFeedSeed.java`
- Create: `src/test/java/com/thecodinglab/imdbclone/recommendation/internal/HomeFeedSeedTest.java`

- [ ] Model immutable request/response records and keep OpenSearch-specific types out of the API.
- [ ] Define constants for strategy version, sections per page, maximum sections, candidate limits,
  minimum viable section size, and maximum excluded IDs.
- [ ] Implement stable seed derivation and child-section seed derivation with fixed test vectors.
- [ ] Define a cursor codec that validates strategy version and next-section offset. Protect against
  malformed or oversized cursors and test round-trip, tamper, and version mismatch behavior.
- [ ] Keep the API anonymous-compatible; `feedInstanceId` is an opaque correlation token, not an
  authentication or authorization mechanism.

### Task 2: Build The Versioned Section Definition Catalog

**Files:**
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/internal/home/HomeSectionDefinition.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/internal/home/HomeSectionCatalog.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/internal/home/HomeSectionFamily.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/internal/home/HomeSectionKind.java`
- Create: `src/test/java/com/thecodinglab/imdbclone/recommendation/internal/home/HomeSectionCatalogTest.java`

- [ ] Encode 40–60 initial definitions with stable IDs and reviewed user-facing titles/subtitles.
- [ ] Add validation for duplicate IDs, missing prompts, invalid ranges, weak candidate limits,
  unsupported family/kind combinations, and display count outside 8–15.
- [ ] Unit test catalog validity and snapshot the stable IDs so accidental renames are visible.
- [ ] Keep wording concise and genuinely descriptive; avoid fake personalization such as “For you”
  before a taste profile exists.

### Task 3: Extend The Catalog Recommendation Candidate Seam

**Files:**
- Create: `src/main/java/com/thecodinglab/imdbclone/catalog/api/MovieDiscoveryCriteria.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/catalog/api/MovieDiscoveryCandidate.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/catalog/api/MovieDiscoveryCandidateProvider.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/catalog/api/MovieDiscoveryTopic.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/catalog/api/MovieDiscoveryTopicProjector.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/OpenSearchMovieDiscoveryCandidates.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/query/MovieSearchQueryBuilder.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieSearchDocument.java`
- Create: `src/test/java/com/thecodinglab/imdbclone/catalog/internal/search/OpenSearchMovieDiscoveryCandidatesTest.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/catalog/internal/search/query/MovieSearchQueryBuilderTest.java`

- [ ] Define typed discovery criteria for genres, type, year/runtime range, rating/vote confidence,
  semantic section reference, and candidate limit. Do not accept raw OpenSearch JSON or query DSL.
- [ ] Expose a narrow topic-projection contract so recommendation can supply stable topic ID, text,
  and prompt version without gaining access to catalog's internal embedding or OpenSearch types.
- [ ] Ensure the indexed projection contains the rating-count/confidence fields needed by hidden-gem
  and broad-quality policies; add a new index version/reindex path if the mapping changes.
- [ ] Build filtered structured and filtered kNN candidate queries with bounded result windows.
- [ ] Return relevance/semantic rank and the quality fields needed by recommendation ranking.
- [ ] Cover empty filters, exclusions, rating confidence, semantic filters, result caps, and missing
  embeddings in unit/integration tests.

### Task 4: Precompute And Version Semantic Section Embeddings

**Files:**
- Create: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieDiscoveryTopicDocument.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieDiscoveryTopicEmbeddingStore.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieDiscoveryTopicProjector.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/internal/home/HomeSectionEmbeddingCoordinator.java`
- Create: `src/test/java/com/thecodinglab/imdbclone/catalog/internal/search/index/MovieDiscoveryTopicProjectorTest.java`
- Create: `src/test/java/com/thecodinglab/imdbclone/recommendation/internal/home/HomeSectionEmbeddingCoordinatorTest.java`
- Modify: `src/main/resources/config/application.yml`
- Modify: `docs/development.md`

- [ ] Let catalog own a derived OpenSearch index for topic vectors, keyed by section ID + prompt
  version + model name + dimensions. Treat recommendation's definition catalog as source of truth.
- [ ] Have a recommendation coordinator pass its semantic topic definitions through the public
  projector contract; catalog reconciles missing or stale embeddings via an explicit maintenance
  task with a lock/idempotency strategy suitable for multiple app replicas.
- [ ] Do not block application readiness on the external embedding service; expose counts for ready,
  missing, and stale topics and let feed composition fall back deterministically.
- [ ] Test unchanged prompts, prompt version changes, model changes, partial failure, and idempotent
  reruns without calling a live model in normal unit tests.

### Task 5: Implement Seeded Composition, Ranking, And Deduplication

**Files:**
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/internal/home/HomeSectionComposer.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/internal/home/HomeDiscoveryRanker.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/recommendation/internal/home/SeededHomeFeedService.java`
- Create: `src/test/java/com/thecodinglab/imdbclone/recommendation/internal/home/HomeSectionComposerTest.java`
- Create: `src/test/java/com/thecodinglab/imdbclone/recommendation/internal/home/HomeDiscoveryRankerTest.java`
- Create: `src/test/java/com/thecodinglab/imdbclone/recommendation/internal/home/SeededHomeFeedServiceTest.java`

- [ ] Implement weighted family selection, first-page guarantees, adjacent-family cooldowns, stable
  replay, and no repeated section IDs.
- [ ] Rank bounded candidates with relevance, confidence-adjusted quality, policy-specific freshness,
  diversity, and 8–12% seeded jitter. Keep weights centralized and strategy-versioned.
- [ ] Select the hero from a quality pool with the feed seed and place its ID into the seen set.
- [ ] Deduplicate against hero, earlier sections, earlier pages, and the request exclusion set.
- [ ] Top up from a deeper bounded window, then replace an undersized section using the deterministic
  fallback order. Never spin indefinitely looking for a viable section.
- [ ] Test same-seed equality, different-seed variation, family diversity, movie uniqueness, sparse
  catalogs, missing semantic embeddings, candidate-provider failure, and the 24-section cap.

### Task 6: Expose The Feed Endpoint And Regenerate The Client

**Files:**
- Modify: `src/main/java/com/thecodinglab/imdbclone/recommendation/web/RecommendationController.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/recommendation/RecommendationControllerTest.java`
- Modify generated contract via: `frontend/src/client/imdb-clone-backend.yaml`
- Regenerate: `frontend/src/client/movies/generator-output/`
- Verify: `frontend/src/shared/api/moviesApi.ts`

- [ ] Add `POST /api/recommendations/home-feed` with Jakarta validation and the repository's existing
  `ProblemDetail` conventions for malformed cursors, incompatible strategy versions, and limits.
- [ ] Cover first page, continuation, validation, restart condition, anonymous access, and semantic
  fallback in controller tests.
- [ ] Start the backend, update the checked-in OpenAPI specification, and regenerate the Axios client.
  Never edit generated TypeScript manually.
- [ ] Confirm the generated `RecommendationControllerApi` exposes the new method through the existing
  `recommendationApi` shared wrapper.

### Task 7: Add The Infinite Home Feed Query And Document-Scoped State

**Files:**
- Create: `frontend/src/features/home/api/homeFeedQueries.ts`
- Create: `frontend/src/features/home/api/homeFeedQueries.test.tsx`
- Create: `frontend/src/features/home/model/homeFeedSession.ts`
- Create: `frontend/src/features/home/model/homeFeedSession.test.ts`
- Retain temporarily for feature-flag fallback: `frontend/src/features/home/api/useFeaturedMovie.ts`
- Retain temporarily for feature-flag fallback: `frontend/src/features/home/api/useMoviesByGenre.ts`

- [ ] Create one browser-document UUID and query it with `useInfiniteQuery`, `staleTime: Infinity`,
  and a 30–60 minute `gcTime`.
- [ ] Build continuation requests from the server cursor plus the unique movie IDs in all cached
  pages; stop when `exhausted` or `nextCursor` is absent.
- [ ] Keep loaded pages on continuation error and expose separate first-page and next-page states.
- [ ] Store vertical and per-section horizontal positions in a module-scoped map keyed by feed
  instance. Test same-document reuse and new-document reset without local/session storage.
- [ ] Keep the old hooks isolated behind the rollout fallback and mark them for deletion when the
  seeded feed flag is retired; do not continue adding behavior to both implementations.

### Task 8: Migrate The Homepage To Progressive Carousel Pages

**Files:**
- Modify: `frontend/src/features/home/pages/HomePage.tsx`
- Modify: `frontend/src/features/home/pages/HomePage.test.ts`
- Modify: `frontend/src/features/home/components/MovieCarousel.tsx`
- Modify: `frontend/src/features/home/components/MovieCarousel.test.tsx`
- Create: `frontend/src/features/home/components/HomeFeedSentinel.tsx`
- Create: `frontend/src/features/home/components/HomeFeedEndState.tsx`
- Modify: `frontend/src/features/home/components/FeaturedMovieHero.tsx`
- Modify: `frontend/src/features/home/index.ts`

- [ ] Render the hero from the first feed response and flatten section pages in server order. Remove
  `homeGenreRows` and all browser-side ranking.
- [ ] Extend `MovieCarousel` with a stable section ID, optional subtitle/reason/tracking data,
  initial horizontal offset, and a throttled scroll-position callback.
- [ ] Add an IntersectionObserver sentinel guarded against duplicate requests and retain a visible,
  accessible Load More fallback.
- [ ] Show hero + three-row skeletons on the first page, a compact tail loader for continuation, and
  a tail-only retry that leaves loaded rows interactive.
- [ ] At the feed cap, show an intentional end state with “Discover a new mix” that creates a fresh
  feed instance without requiring a full page reload.
- [ ] Preserve the existing watchlist behavior with one shared watchlist query/mutation; do not add
  one mutation hook per carousel.

### Task 9: Restore Navigation State And Add Discovery Events

**Files:**
- Create: `frontend/src/features/home/hooks/useHomeFeedRestoration.ts`
- Create: `frontend/src/features/home/hooks/useHomeFeedRestoration.test.tsx`
- Create: `frontend/src/features/home/observability/homeDiscoveryEvents.ts`
- Create: `frontend/src/features/home/observability/homeDiscoveryEvents.test.ts`
- Modify: `frontend/src/features/home/pages/HomePage.tsx`
- Modify the chosen backend event receiver/storage only after its retention and privacy contract is
  documented.

- [ ] Save state before navigation and restore vertical scroll only after the cached section DOM is
  present; restore each carousel independently when it mounts.
- [ ] Emit deduplicated section and movie impressions using IntersectionObserver thresholds, plus
  movie open and watchlist events with strategy version, section ID, position, movie ID, and an
  opaque tracking token.
- [ ] Keep discovery analytics separate from the current technical-performance reporter unless a
  documented common envelope is introduced. Do not log raw prompts or future private taste signals.
- [ ] Add backend diagnostics for section viability, fallback/skip reason, candidate-pool size,
  composition latency, duplicate rejection count, and semantic readiness.
- [ ] Treat click-through rate as one signal, not the sole optimization objective; record catalog
  coverage, repetition, diversity, short-section rate, and tail failure rate.

### Task 10: Responsive E2E, Failure Testing, And Rollout

**Files:**
- Create: `frontend/e2e/home-feed.spec.ts`
- Modify: `docs/product-roadmap.md` only to mark completed milestones after evidence exists.
- Modify: `docs/development.md`

- [ ] Add deterministic API fixtures for seed replay, changed seed, multiple pages, continuation
  failure, semantic fallback, and exhausted feed.
- [ ] Test automatic continuation and Load More fallback, unique sections/movies, movie navigation
  and Back restoration, hard-refresh reset, retry preservation, and Discover a new mix.
- [ ] Review 1440x1000, 1024x768, 390x844, and 320x700. Verify no document-level horizontal
  overflow, touch-native carousel scrolling, focus visibility, and reduced-motion behavior.
- [ ] Add a configuration flag that can fall back to the current fixed homepage during rollout.
- [ ] Compare latency, successful-section rate, fallback rate, coverage, and engagement before making
  the seeded feed the default. Roll back by flag, not by reverting the API contract.

## Verification Commands

Run targeted tests after every task, then the relevant full suites:

```bash
./gradlew test --tests '*HomeFeed*' --tests '*HomeSection*' --tests '*MovieDiscovery*'
./gradlew test
cd frontend && yarn test
cd frontend && yarn run lint
cd frontend && yarn build
cd frontend && yarn playwright test e2e/home-feed.spec.ts
```

When the OpenSearch mapping or semantic topic index changes, also verify a clean local projection:

```bash
make docker-compose-dev-up
make reindex-local-search
```

Inspect backend logs/metrics during a multi-page run and confirm that no seed, cursor, prompt,
authentication data, or private profile signal is logged in full.

## Recommended Delivery Slices

1. **Structured seeded feed:** Tasks 1–3 plus structured-only parts of Tasks 5–8. This validates the
   contract, variation, deduplication, and UX without depending on semantic topic infrastructure.
2. **Semantic themes:** Task 4 and semantic candidate/ranking branches, protected by deterministic
   fallback.
3. **Restoration and discovery measurement:** Task 9 and the full E2E matrix.
4. **Personalized feed:** A later roadmap slice that adds account taste candidates and exploration /
   exploitation only after trustworthy events and offline evaluation exist.

This ordering gives the homepage useful variety early while preserving a clean path from editorial
and content-based discovery to future personalized recommendations.
