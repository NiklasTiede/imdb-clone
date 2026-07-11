# Movie Detail Layout And Backdrop Implementation Plan

**Goal:** Turn the movie detail route from a compact summary card into a broad, cinematic,
responsive destination using data already present in `MovieRecord`, without changing backend API
contracts.

**Architecture:** Keep `MovieDetailPage` responsible for query and mutation orchestration. Rebuild
`MovieHero` as a feature-owned, backdrop-led section and extract focused presentation pieces for
movie facts and rating interaction. Add a shared `BackdropImage` media primitive because stored
backdrops will also be useful on the homepage and future recommendation surfaces. Keep the page as
unframed content bands inside the existing `PageContent` width system instead of one enclosing
surface.

**Tech Stack:** React 19, Material UI 9, TanStack Query 5, React Router 7, Vitest, Testing Library,
and Playwright 1.59.

---

## Current-State Findings

- The route constrains all content to `760px`, so at `1366x768` and `1440x1000` it reads like a
  dialog and leaves most of the viewport unused.
- The page wraps hero and synopsis in one surface. That prevents media, facts, and future sections
  from establishing their own hierarchy.
- `MovieRecord` already includes poster and backdrop tokens, title variants, type, year range,
  runtime, genres, adult classification, ratings, counts, synopsis, and a
  trailer key. The previous page did not render the backdrop or trailer.
- Desktop uses a `150px` poster and low-contrast synthetic background even when a real backdrop is
  available.
- Mobile stacks the title below a centered poster while retaining a wide empty region beside the
  poster. Ratings then become two full-width panels, and the ten-star control creates another dense
  action block.
- The share button has no behavior.
- A missing movie value is treated as an error during initial loading, so loading, invalid ID, and
  request failure are not distinct states.
- Anonymous watchlist and rating attempts use transient errors rather than giving users a durable,
  contextual next step.
- Comments can already be fetched by movie, but `CommentRecord` exposes only `accountId`; author
  name, username, and avatar data require a later backend-contract slice.
- Local seed data currently has stored backdrops but no non-empty trailer keys, so trailer testing
  needs an explicit fixture and remains a separate privacy-focused slice.

## Product Decisions

- Use a cinematic editorial layout consistent with the existing dark catalog theme: real backdrop
  first, inspectable poster second, and restrained brand/rating accents.
- Increase the page width to the wider catalog scale (`1200px`-`1280px`) and align synopsis content
  to the same full-width frame as the backdrop.
- Render the backdrop as decorative media with a dark readability treatment; the title and poster
  remain the accessible movie identity.
- Use a stable hero height/aspect ratio so image loading and fallback states cannot shift the
  layout.
- Provide an intentional theme-based fallback when the backdrop is missing or fails to load.
- On desktop, use a poster/details/ratings grid near the lower part of the hero. On mobile, use a
  shallow backdrop followed by a compact poster-and-identity composition so no empty poster-side
  region remains.
- Keep watchlist and rating actions prominent. Move the ten-point rating control into a focused
  dialog opened by a clear `Rate movie` action, and continue displaying the user's saved score.
- Make `Share` functional through the Web Share API when available and a clipboard fallback
  otherwise. Report success or failure accessibly.
- Keep the detail experience self-contained; source identifiers remain catalog data and are not
  presented as outbound IMDb or TMDB links.
- Preserve existing authentication, watchlist, and rating contracts in this slice.
- Use semantic theme colors and existing layout primitives; do not add a local palette.

## Non-Goals

- No YouTube embed or trailer consent behavior.
- No comments list, composer, editing, deletion, or author enrichment.
- No similar-movie query or recommendation carousel.
- No catalog schema, OpenAPI, generated-client, or backend changes.
- No redesign of the global app bar or search experience.
- No new data enrichment for credits, certification, release dates, or production companies.

## Acceptance Criteria

- At `1440x1000` and `1366x768`, the page uses the wider catalog canvas and the real backdrop is a
  first-viewport signal rather than leaving most of the screen empty.
- At `390x844` and `320x700`, there is no horizontal overflow, poster-side dead space, clipped title,
  or overlapping action control.
- The hero has stable dimensions before images load and a deliberate fallback when backdrop or
  poster media is unavailable.
- Poster, title, year/type/runtime, genres, classification, ratings, rating counts, watchlist state,
  and user rating remain visible and understandable.
- Original title appears only when it differs from the primary title.
- Synopsis and its divider use the same width and horizontal alignment as the backdrop.
- Rating opens in a keyboard-accessible dialog, supports scores `1` through `10`, prevents duplicate
  submission, preserves the current score, and reports mutation failure.
- Share invokes native sharing where supported, otherwise copies the canonical movie URL, and gives
  accessible success/error feedback.
- Initial loading, invalid movie ID, missing movie/request failure, and loaded content are distinct
  states.
- Anonymous watchlist/rating attempts provide a clear sign-in action while preserving the movie URL
  as the return destination.
- Existing catalog, watchlist, and rating tests continue to pass.

---

## Task 1: Lock Down Presentation Rules

**Files:**

- Create: `frontend/src/features/catalog/model/moviePresentation.ts`
- Create: `frontend/src/features/catalog/model/moviePresentation.test.ts`
- Modify: `frontend/src/features/catalog/components/MovieHero.tsx`

- [x] Extract and test movie type/genre labels, year ranges, runtime labels, adult classification,
  and original-title visibility.
- [x] Handle absent and partial movie metadata without rendering separators or empty rows.
- [x] Map TMDB movie types to the appropriate `/movie/` or `/tv/` destination.
- [x] Keep formatting helpers independent of React so all boundary cases are cheap to test.

## Task 2: Add A Reusable Backdrop Primitive

**Files:**

- Create: `frontend/src/shared/media/BackdropImage.tsx`
- Create: `frontend/src/shared/media/BackdropImage.test.tsx`
- Modify: `frontend/src/shared/media/index.ts`
- Modify: `frontend/src/shared/media/imageUrls.ts`
- Modify: `frontend/src/shared/media/imageUrls.test.ts`

- [x] Render the stored large WebP backdrop URL through a stable `aspect-ratio`/height container.
- [x] Treat the image as decorative by default and use `object-fit: cover` with a predictable focal
  position.
- [x] Expose load/error state without showing a broken-image icon.
- [x] Keep the theme-based fallback visible when no token exists or the request fails.
- [x] Avoid adding a network-dependent or catalog-specific fallback asset to the shared primitive.

## Task 3: Rebuild The Backdrop-Led Hero

**Files:**

- Modify: `frontend/src/features/catalog/components/MovieHero.tsx`
- Modify: `frontend/src/features/catalog/components/MovieHero.test.tsx`
- Modify: `frontend/src/features/catalog/components/RatingPill.tsx`
- Modify: `frontend/src/features/catalog/components/RatingPill.test.tsx`

- [x] Replace the synthetic background with `BackdropImage`, a dark readability overlay, and a
  stable fallback treatment.
- [x] Build a wide desktop grid with a substantially larger poster, movie identity/facts, ratings,
  and actions arranged as one coherent hierarchy.
- [x] Build the mobile composition explicitly instead of relying on the desktop stack to collapse;
  keep the shallow backdrop, poster, title, and metadata visually connected without dead space.
- [x] Surface existing user-facing metadata using the tested presentation helpers.
- [x] Keep rating counts readable and reduce panel chrome so ratings support rather than dominate
  the movie identity.
- [x] Preserve watchlist pending/selected states and visible focus styles.
- [x] Test backdrop/fallback variants, long titles, partial metadata, authenticated
  action states, and mobile-safe content structure.

## Task 4: Replace The Inline Ten-Star Control

**Files:**

- Create: `frontend/src/features/catalog/components/MovieRatingDialog.tsx`
- Create: `frontend/src/features/catalog/components/MovieRatingDialog.test.tsx`
- Modify: `frontend/src/features/catalog/components/MovieHero.tsx`
- Modify: `frontend/src/features/catalog/pages/MovieDetailPage.tsx`

- [x] Replace the inline ten-star strip with a compact `Rate movie` action showing the current user
  score when one exists.
- [x] Open a focused dialog with a touch-friendly `1`-`10` choice, clear selected state, and explicit
  cancel/save commands.
- [x] Keep the dialog open while saving, prevent duplicate submissions, and close only after a
  successful mutation.
- [x] Restore focus to the trigger after close and provide an accessible title/instructions.
- [x] Preserve the existing mutation and query invalidation behavior.
- [x] Test initial selection, keyboard interaction, pending state, successful save, cancellation,
  and failure retention.

## Task 5: Recompose The Page And Its States

**Files:**

- Modify: `frontend/src/features/catalog/pages/MovieDetailPage.tsx`
- Create: `frontend/src/features/catalog/pages/MovieDetailPage.test.tsx`
- Modify: `frontend/src/features/catalog/components/Synopsis.tsx`
- Modify: `frontend/src/features/catalog/components/Synopsis.test.tsx`

- [x] Increase `PageContent` width for this route and remove the single enclosing `Surface` and
  divider.
- [x] Compose hero and synopsis as separate unframed bands using one shared content frame.
- [x] Distinguish pending, invalid-ID, and failed/not-found states using existing `StatusState` and a
  movie-detail skeleton where appropriate.
- [x] Add a contextual sign-in action for anonymous watchlist/rating attempts and preserve the
  current movie URL for return navigation.
- [x] Replace plain snackbar messages with severity-aware MUI feedback for mutation and share
  outcomes.
- [x] Test query states, anonymous actions, authenticated mutation orchestration, and failure
  feedback at the page boundary.

## Task 6: Implement Real Sharing

**Files:**

- Create: `frontend/src/features/catalog/utils/shareMovie.ts`
- Create: `frontend/src/features/catalog/utils/shareMovie.test.ts`
- Modify: `frontend/src/features/catalog/components/MovieHero.tsx`
- Modify: `frontend/src/features/catalog/pages/MovieDetailPage.tsx`

- [x] Build a canonical same-origin `/movie?id=<id>` URL instead of sharing incidental query state.
- [x] Prefer `navigator.share` when available and use `navigator.clipboard.writeText` as fallback.
- [x] Treat user cancellation as neutral; distinguish successful copy/share from an actual API
  failure.
- [x] Disable sharing when the movie has no persisted ID.
- [x] Test native share, clipboard fallback, cancellation, and failure paths.

## Task 7: Add Responsive Browser Coverage

**Files:**

- Create: `frontend/e2e/movie-detail.spec.ts`

- [x] Mock auth, movie detail, watchlist, and rating responses so visual tests do not depend on local
  seed data or credentials.
- [x] Cover a movie with backdrop/poster/full metadata and a movie with missing media/partial
  metadata.
- [x] Assert stable hero dimensions, synopsis hierarchy, rating dialog behavior,
  share feedback, and sign-in guidance.
- [x] Assert no horizontal overflow at `1440x1000`, `1366x768`, `390x844`, and `320x700`.
- [x] Capture and inspect desktop/mobile screenshots for long titles, missing media, action wrapping,
  and short-height reachability.
- [x] Verify keyboard order through watchlist, rating dialog, and share action.

## Task 8: Final Verification

- [x] Run focused frontend tests:

  ```bash
  cd frontend
  yarn test -- moviePresentation.test.ts BackdropImage.test.tsx MovieHero.test.tsx \
    RatingPill.test.tsx MovieRatingDialog.test.tsx MovieDetailPage.test.tsx \
    Synopsis.test.tsx shareMovie.test.ts
  ```

- [x] Run full frontend checks:

  ```bash
  cd frontend
  yarn run lint
  yarn test
  yarn build
  ```

- [x] Run responsive browser checks:

  ```bash
  cd frontend
  yarn playwright test e2e/movie-detail.spec.ts --project=desktop-chromium
  yarn playwright test e2e/movie-detail.spec.ts --project=mobile-chromium
  ```

- [x] Run `git diff --check` and confirm no backend API or generated-client files changed.
- [x] Review the final page against `docs/design.md` and the four target viewport sizes.

## Follow-Up Slices

1. Enrich `CommentRecord` with author presentation data and deliver paginated comment workflows.
2. Add the shared recommendation contract and a similar-movies section.
