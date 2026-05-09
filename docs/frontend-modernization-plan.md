# Frontend Modernization Plan

Goal: move the frontend toward deep feature modules and TanStack Query without breaking current behavior.

## Sequence

1. Fix API client and auth wiring.
   - Centralize Axios setup.
   - Ensure generated OpenAPI clients use the configured Axios instance.
   - Add TanStack Query provider without migrating screens yet.

2. Centralize session handling.
   - Move JWT/localStorage reads and writes behind one small module.
   - Keep route guards and the app bar using the same session source.

3. Introduce deep feature modules.
   - Add `app`, `shared`, and `features` folders.
   - Use feature `index.ts` files as public module boundaries.
   - Avoid cross-feature imports from internal files.

4. Migrate server state to TanStack Query incrementally.
   - Start with movie detail.
   - Then migrate movie search.
   - Then migrate account/profile.
   - Then migrate watchlist, ratings, comments, and media upload mutations.

5. Refactor UI composition.
   - Split the app bar into focused navigation, search, account, and theme controls.
   - Split movie pages into page, data hook, and presentational UI components.

6. Improve responsive layout.
   - Use Playwright screenshots for desktop, tablet, and mobile.
   - Remove fixed-width movie card/detail layouts.

7. Remove Rematch/Redux when it no longer owns meaningful state.
   - Keep only simple client state via React context/hooks where needed.
   - Keep TanStack Query responsible for backend/server state.
