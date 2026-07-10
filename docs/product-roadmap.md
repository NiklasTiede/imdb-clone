# Product Roadmap

This document captures the long-term product direction for IMDb Clone. It is a prioritized backlog,
not a release commitment. Refine each slice into an implementation plan or issue before starting
work, and keep completed items in the release notes rather than growing this document indefinitely.

## Product Direction

The application should become a place where users can both inspect a movie in depth and continue
discovering movies without repeatedly seeing the same small set of results.

Guiding principles:

- Make the movie detail page the primary destination for watching, evaluating, and discussing a
  movie.
- Make the homepage feel fresh across visits while keeping each individual visit stable and
  predictable.
- Treat search relevance as a measured product capability, not a one-time ranking implementation.
- Use PostgreSQL as the source of truth and OpenSearch as the derived discovery index.
- Prefer explainable editorial queries before introducing opaque personalization.
- Preserve fast loading, responsive layouts, keyboard access, and useful empty states as content
  density increases.

## Strategic Long-Term Goals

The product should evolve as a personal movie discovery and journal application with a deep,
inspectable catalog. The first five strategic stages are:

1. **Search relevance and recommendation foundation** - measure retrieval quality and establish a
   reusable, explainable recommendation capability.
2. **Movie destination** - deliver the wider detail layout, trailer, comments, richer metadata, and
   similar movies.
3. **Fresh discovery** - build the dynamic homepage, progressive carousel feed, and a focused
   `What should I watch tonight?` experience.
4. **Personal movie journal** - support diary entries, watched dates, rewatches, notes, tags, and
   authored activity.
5. **Personal library and taste** - add custom lists, taste statistics, annual summaries, and
   portable import/export.

These stages reinforce one another. Recommendation is a cross-cutting platform used by search,
homepage sections, movie details, watchlist prioritization, Tonight Mode, and user profiles rather
than a separate carousel implementation in each feature.

## Current Baseline

### Movie detail

- The page is constrained to `760px` and contains one surface with the movie hero and synopsis.
- `MovieRecord` already exposes the primary and original titles, movie type, year range, runtime,
  genres, IMDb rating and count, community rating and count, description, poster and backdrop
  tokens, IMDb/TMDB IDs, and `trailerYoutubeKey`.
- Rating and watchlist actions are already available.
- The backend already exposes paginated comments by movie, plus authenticated create, update, and
  delete operations.
- Comment responses currently expose only `accountId`; displaying a useful author identity will
  require an enriched comment contract or a separate account lookup.
- There is no dedicated similar-movies contract. The existing semantic and filtered OpenSearch
  capabilities can provide its foundation.

### Homepage

- The homepage contains one featured movie followed by three fixed genre rows: horror, thriller,
  and science fiction.
- Each genre query retrieves 20 results, sorts by IMDb rating in the frontend, and displays at most
  15 movies.
- The same categories and ordering are requested on every visit.
- The current carousel component already supports horizontal scrolling, loading states, and a
  `View all` action.

### Search relevance

- Non-empty searches retrieve up to 100 lexical and 100 semantic candidates, then combine them
  with equal-weight reciprocal rank fusion using a fixed rank constant of 60.
- Lexical ranking strongly boosts titles and multiplies scores by IMDb rating-count popularity;
  semantic ranking uses one embedding built from title, type, year, runtime, genres, and synopsis.
- Hybrid results are capped at four pages. A user-selected rating sort is currently applied in the
  frontend to one returned page rather than being part of the backend ranking contract.
- There is no representative relevance benchmark, query analytics, score explanation, or tuning
  process, so improvements cannot yet be measured reliably.

### Unfinished topbar destinations

- The edit icon is visible only to administrators and `/editing` is role-protected, but the page is
  currently a placeholder.
- The notification icon links authenticated users to `/your-messages`, which is also a placeholder.
- The backend already exposes paginated comments by username, making an authored-comments page
  possible without a new persistence model.
- Authored comments are user activity, not private messages. The route label and icon should match
  the first delivered behavior; actual notifications need their own event and unread-state model.

## Roadmap

### 1. Richer movie detail page

Goal: turn the page into a broad, media-led movie destination instead of a compact summary panel.

- Increase the responsive content width to the application's wider catalog scale while preserving
  readable synopsis and comment line lengths.
- Recompose the page into unframed content bands for hero media, facts, trailer, community, and
  recommendations instead of nesting everything inside one card.
- Use the backdrop image as a strong first-viewport signal and retain the poster as the primary
  inspectable asset.
- Surface more of the existing data:
  - Original title when different from the display title
  - Movie or series type
  - Start and end years
  - Runtime
  - Genres and adult classification
  - IMDb and community rating counts
  - Links to the corresponding IMDb and TMDB records when IDs are available
- Keep rating and watchlist actions prominent and usable on mobile.

#### Trailer

- Embed the saved YouTube trailer when `trailerYoutubeKey` is present.
- Lazy-load the player so it does not delay the initial movie content.
- Prefer YouTube's privacy-enhanced embed domain and define the required consent/privacy behavior
  before loading third-party content.
- Provide an intentional no-trailer state instead of leaving an empty media frame.
- Validate the stored value as a YouTube video key; do not accept arbitrary embed URLs.

#### Comments

- Show paginated comments below the movie content for anonymous and authenticated users.
- Add an authenticated comment composer with clear validation and mutation feedback.
- Allow authors to edit and delete their own comments; retain the existing administrator behavior.
- Enrich comment responses with display name, username, and profile-image token so the frontend does
  not issue one account request per comment.
- Start with newest-first or oldest-first sorting. Popular sorting should wait until reactions or
  another meaningful ranking signal exist.
- Use a `Load more` interaction initially; consider comment-level infinite loading only after the
  basic workflow is stable.

#### Similar movies

- Add a dedicated similar-movies query that excludes the current movie.
- Combine semantic embedding similarity with shared genres, movie type, release period, and rating
  quality.
- Avoid a row filled with near-duplicates by diversifying genres, years, or franchises where data
  permits.
- Return a compact explanation such as `Similar themes` or `More science-fiction thrillers` when
  the matching strategy can support it.
- Reuse the existing poster carousel and movie-card contracts.

### 2. Expand homepage discovery

Goal: make repeat visits expose different high-quality parts of the catalog.

- Increase each carousel's useful result pool so users can scroll beyond the first viewport.
- Replace hard-coded React queries with a catalog of homepage section definitions. Each definition
  should contain a stable identifier, title, subtitle, query strategy, and optional `View all`
  destination.
- Build a mixture of section types:
  - Highly rated movies by genre
  - Recent releases with sufficient rating confidence
  - Underseen or underrated movies
  - Movies from a decade or era
  - Movie-type rows such as miniseries, documentaries, or TV movies
  - Semantic themes such as space exploration, courtroom tension, found family, or slow-burn
    mysteries
  - Community favorites once enough local ratings exist
- Shuffle eligible sections once per page visit using a feed seed. Keep the selected order stable
  while that page remains open so queries and layout do not jump during rerenders.
- Avoid showing the same movie in adjacent rows and limit repetition across the whole loaded feed.
- Preserve a small set of high-confidence fallback rows when a specialized query has too few
  results.

#### Progressive section loading

- Implement an infinite vertical feed of carousel sections, not an endless single-column list of
  movie cards.
- Load an initial group of sections, then request another group as a sentinel approaches the
  viewport.
- Put an explicit upper bound on sections per visit and provide a retry state when a later group
  fails.
- Keep already loaded sections in place when fetching the next group.
- Make browser back navigation restore the feed seed, loaded sections, and carousel positions.
- Track section impressions and movie opens through the existing observability pipeline before
  using engagement signals for ranking.

#### What should I watch tonight?

- Turn the existing pick-for-me concept into a focused decision flow rather than another large
  result grid.
- Let users constrain runtime, mood, genres, era, movie type, and whether watched titles are
  allowed.
- Return three strong, diverse choices with a short explanation for each recommendation.
- Support `Show me three others` without repeating rejected candidates during the session.
- Allow anonymous use with content-based recommendations and improve results for signed-in users
  from their explicit preferences and history.
- Record lightweight feedback such as selected, rejected, already watched, or not interested so
  the recommendation platform can learn without forcing a rating.

### 3. Improve search relevance

Goal: make exact-title, partial-title, filtered, and conceptual searches consistently useful and
make ranking changes objectively testable.

#### Establish a relevance baseline

- Build a versioned evaluation set containing representative queries and expected relevant movies:
  - Exact and original titles
  - Prefixes and incomplete titles
  - Common misspellings
  - Genre, era, mood, plot, and theme descriptions
  - Queries combined with year, runtime, type, and genre filters
  - Ambiguous and no-result queries
- Track ranking metrics such as reciprocal rank, precision at 5/10, and normalized discounted
  cumulative gain, plus zero-result rate and latency.
- Add regression tests for a small deterministic fixture and an offline evaluation command for a
  production-like index.
- Record anonymized query shape, result count, latency, and selected result position through the
  existing observability pipeline. Do not log raw sensitive search text by default.

#### Tune retrieval and fusion

- Make lexical and semantic RRF weights, rank constant, and candidate depth configurable and tune
  them against the evaluation set.
- Detect title-like queries and favor lexical results; give semantic retrieval more influence for
  descriptive natural-language queries.
- Revisit the IMDb popularity multiplier so popular movies improve weak ties without displacing a
  strong title match.
- Verify that filters are applied identically to lexical and semantic candidate sets.
- Move explicit sorting into the backend contract. A `rating` or `year` sort should intentionally
  replace relevance ordering across the full result set, not reorder only one frontend page.
- Evaluate fuzzy title matching, analyzers, synonyms, and original-title handling before adding
  complexity to semantic ranking.
- Version embedding text and benchmark whether synopsis quality, keywords, cast, or crew improve
  retrieval as richer catalog data becomes available.
- Consider a second-stage reranker only after candidate retrieval and RRF are measured and tuned.

#### Make relevance diagnosable

- Add an administrator-only search diagnostics view or endpoint showing lexical rank, semantic
  rank, fused score, matched fields, and active ranking configuration.
- Keep previous relevance configurations reproducible so regressions can be compared before a
  reindex or model change is deployed.
- Add dashboards for search latency, embedding failures, zero-result rate, and result selection.

### 4. Administrator catalog management

Goal: turn the existing admin edit action into a focused catalog-management workspace.

- Keep both frontend routing and backend mutation endpoints protected by `ROLE_ADMIN`.
- Start with catalog search and movie selection, then provide explicit create and edit workflows.
- Support all useful existing movie fields, including titles, type, year range, runtime, genres,
  description, ratings, external IDs, media tokens, and YouTube trailer key.
- Add poster/backdrop upload or replacement through the existing media service rather than asking
  administrators to edit storage tokens manually.
- Validate YouTube keys and external IDs and show a preview before saving.
- Warn about unsaved changes and display field-level server validation errors.
- Make deletion a separate, strongly confirmed action with clear consequences for ratings,
  comments, watchlists, media, and search projection.
- Show whether the OpenSearch projection succeeded and provide an administrator retry/reindex path
  when catalog persistence succeeds but projection fails.
- Record administrator changes in an audit trail before adding bulk editing.
- Later additions may include bulk imports, data-quality queues, missing-media reports, and
  duplicate detection.

### 5. User comments and activity

Goal: replace the empty messages destination with an honest, useful view of the signed-in user's
community activity.

- Rename the initial destination to `Your comments` or `Activity` and use a comment/activity icon.
- Load the existing paginated `/api/account/{username}/comments` contract.
- Enrich each item with movie title and poster information in one backend response or a bounded
  batch lookup; do not issue one movie request per row.
- Link each comment back to its movie and, when supported, directly to the comment anchor.
- Let users edit and delete their comments with the same authorization rules as the movie detail
  page.
- Provide pagination or `Load more`, loading and retry states, and a useful empty state.
- Consider an activity page with tabs for comments, ratings, and watchlist only if it improves
  navigation over the existing dedicated ratings and watchlist pages.

Actual notifications should be a later, separate capability:

- Add notifications only for meaningful events such as replies, moderation actions, or account
  security events.
- Persist read/unread state and show a badge only when unread notifications exist.
- Do not present authored comments as conversations or imply private messaging.

### 6. Discovery API

Goal: move homepage composition and similarity rules out of individual React hooks.

- Introduce a backend homepage-feed contract that accepts or returns a feed seed and pages through
  section descriptors.
- Keep editorial section definitions versioned in backend code or configuration rather than
  scattering query text across components.
- Add a dedicated similar-movies endpoint or catalog query boundary.
- Extend OpenSearch filters only when a planned section requires them; do not expose unrestricted
  query-building details to the frontend.
- Cache expensive semantic section queries for a bounded period while retaining variation between
  visits.
- Define minimum rating-count thresholds so tiny samples do not dominate `top-rated` sections.

### 7. Catalog enrichment

Goal: support richer detail pages and better recommendation signals with additional source data.

Existing data should be fully used before expanding the schema. Later enrichment candidates:

- Cast, directors, writers, characters, and crew through the existing `Person` and `CastMember`
  domain direction.
- Release dates, countries, languages, certifications, production companies, and keywords.
- Trailer availability and media-quality indicators during seed generation.
- Stable franchise or collection identifiers where a reliable source exists.

Any new imported data needs provenance, repeatable seed behavior, API ownership, search-index
mapping, and a migration/reindex plan.

## Recommendation Platform

Goal: provide one recommendation capability that can serve multiple product surfaces and evolve
from content-based discovery into personalization when enough trustworthy signals exist.

### Candidate generation

- Generate content-based candidates from semantic embeddings, genres, type, year, runtime, and
  later cast, crew, keywords, and collections.
- Add editorial candidates from versioned semantic themes and homepage section definitions.
- Add quality and freshness candidates using rating confidence, recent catalog additions, and
  time-decayed activity.
- Add personal candidates from ratings, watchlist, watched history, diary entries, and explicit
  `not interested` feedback.
- Defer collaborative filtering until the application has enough active users and interactions to
  avoid sparse, misleading results.

### Ranking and diversity

- Rank with configurable contributions for user affinity, content similarity, quality, freshness,
  novelty, and popularity.
- Apply hard constraints such as adult-content policy, movie type, runtime, and already-watched
  behavior before ranking.
- Diversify the final set across genres, years, creators, and franchises so one preference does not
  produce a row of near-duplicates.
- Keep anonymous recommendations strong through editorial, semantic, and popularity candidates.
- Use recent explicit behavior more strongly than old implicit clicks, and document retention and
  privacy rules for interaction data.

### Explanations and surfaces

- Return a reason code and user-facing explanation with each recommendation, such as `Similar
  themes`, `Because you rated Arrival highly`, or `A highly rated film from your favorite genres`.
- Serve recommendations through stable contracts for:
  - Personalized and anonymous homepage rows
  - Similar movies on a detail page
  - Tonight Mode
  - Watchlist prioritization
  - Post-rating or post-watch suggestions
  - Search fallback and empty states
  - Taste profiles and annual summaries
- Avoid exposing raw ranking internals to the frontend; return the result, explanation, and tracking
  context needed by the surface.

### Evaluation

- Evaluate relevance, catalog coverage, diversity, novelty, repetition, and latency instead of
  optimizing only click-through rate.
- Keep recommendation impressions, selections, dismissals, watchlist additions, ratings, and
  watched events distinguishable.
- Add an administrator diagnostics view for candidate sources, rank contributions, exclusions, and
  explanation generation.
- Version recommendation configurations so a model, embedding, or weighting change can be compared
  and rolled back.

## Personal Movie Journal

Goal: give users durable personal value even when they are not interacting with other users.

- Distinguish `watched` from a dated diary entry. A user may mark a movie as seen without knowing
  when, or log one or more specific viewings.
- Store watched date, optional rating, short note or review, and user-defined tags for each diary
  entry.
- Support rewatches without overwriting historical diary entries or past ratings.
- Provide chronological diary, calendar, and yearly views.
- Let users add or edit a review after logging a movie.
- Define how the existing single current rating relates to historical diary-entry ratings before
  changing the persistence model.
- Use journal events as explicit recommendation signals while allowing users to exclude private
  notes and tags from personalization.

## Personal Library And Taste

### Custom lists

- Allow users to create named lists in addition to the watchlist.
- Support ordered entries, optional notes, public/private visibility, and shareable links.
- Provide fast add/remove actions from movie cards and detail pages.
- Add list cloning or collaboration only after ownership, permissions, and moderation are clear.

### Taste statistics

- Show rating distribution, favorite genres, decades, movie types, runtimes, actors, and directors.
- Highlight community-rating differences and changing preferences over time.
- Produce a yearly summary from diary data with top-rated movies, viewing totals, rewatches, and
  discoveries.
- Keep statistics useful with small histories and explain minimum-data requirements.

### Import and export

- Export ratings, watched history, diary entries, comments/reviews, watchlist, and custom lists in a
  documented CSV or JSON format.
- Support a previewable, idempotent import flow with row-level errors and external-ID matching.
- Prioritize IMDb-compatible rating/watchlist CSV import, then consider a documented generic format.
- Never silently overwrite existing ratings, diary entries, or private content during import.

## Additional Product Ideas

These are candidates rather than committed scope. Promote an idea into the ordered roadmap only
after its user outcome and data requirements are clear.

- **Comment replies, reactions, and spoiler controls** - deepen community interaction and provide
  the signals needed for meaningful comment ranking and notifications.
- **People and credits pages** - make actors, directors, writers, characters, and filmographies
  navigable once `Person` and `CastMember` data is populated.
- **Recently viewed** - help users return to movie pages without requiring an account, with a clear
  local-history control.
- **Real share actions** - make the existing movie share button copy or invoke a stable deep link.
- **Data-quality administration** - surface missing trailers, descriptions, images, embeddings, and
  suspicious duplicates as an actionable queue.
- **Moderation tools** - add report, review, and moderation workflows before community volume makes
  ad hoc database intervention necessary.

## Suggested Delivery Slices

1. **Search relevance baseline** - add the query benchmark, metrics, and diagnostics before tuning
   RRF or changing the embedding model.
2. **Recommendation foundation** - define candidate, ranking, explanation, interaction-event, and
   evaluation contracts with anonymous content-based results first.
3. **Detail layout and existing metadata** - widen and restructure the page without changing API
   contracts.
4. **Trailer** - add the lazy, privacy-aware player and fallback behavior.
5. **Movie comments** - expose author metadata and build list/create/edit/delete workflows.
6. **Your comments** - make authored comments discoverable and manageable from the user area.
7. **Search relevance tuning** - tune weighted fusion, lexical popularity, candidate retrieval, and
   server-side sorting against the benchmark.
8. **Similar movies** - deliver the first recommendation surface using the shared platform.
9. **Homepage section catalog and Tonight Mode** - add stable per-visit mixing and constrained,
   explainable movie picks before infinite loading.
10. **Progressive homepage feed** - add section pagination, deduplication, restoration, and metrics.
11. **Movie journal and rewatches** - add dated diary entries, notes, tags, and historical viewings.
12. **Custom lists, taste statistics, and portability** - build the personal library, yearly
   summaries, and safe import/export.
13. **Administrator movie editing** - deliver search/select and safe single-movie editing before
   delete or bulk operations.
14. **Catalog enrichment and advanced personalization** - add people and richer metadata, then use account
   activity only when enough meaningful signals exist.

Each slice should be independently deployable and include backend tests where contracts change,
frontend behavior tests, responsive Playwright coverage, and production observability checks.

## Open Product Decisions

- Should third-party YouTube content require an explicit consent click?
- Should homepage variation change on every reload, once per browser session, or once per day?
- Which section definitions are editorially curated and which may be generated from semantic text?
- What minimum IMDb/community rating count makes a `top-rated` or `hidden gem` row credible?
- Should comments default to newest first or preserve conversation order oldest first?
- How much repetition is acceptable between sections and across consecutive visits?
- When should authenticated recommendations become personalized rather than generally editorial?
- Which search queries form the initial relevance benchmark, and who decides what is relevant?
- Should title-like and descriptive queries use different fusion weights or an explicit search mode?
- Should `/your-messages` become `/your-comments` or a broader `/activity` destination?
- Which movie mutations belong in the first admin release: edit only, create and edit, or deletion
  as well?
- Which user interactions may influence recommendations, and how can users inspect or reset that
  history?
- Should watchlist titles be excluded, boosted, or shown in a dedicated recommendation surface?
- How should the current rating behave when diary entries preserve historical ratings?
- Which custom-list and diary fields are public by default?
- Which external import formats should be officially supported and tested?

## Idea Template

Add future ideas with enough structure to evaluate them:

```markdown
### Idea name

- User outcome:
- Current limitation:
- Existing data or capability:
- Required backend/API work:
- Required frontend work:
- Success signal:
- Open questions:
```
