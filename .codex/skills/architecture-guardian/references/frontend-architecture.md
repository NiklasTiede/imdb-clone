# Frontend Architecture Review

## Scope

Review React feature structure, shared modules, data-fetching ownership, generated API usage, routing, and UI test coverage.

Primary files:

- `frontend/src/app`
- `frontend/src/features`
- `frontend/src/shared`
- `frontend/src/client/movies/MoviesApi.ts`
- `frontend/src/hooks`
- `frontend/src/pages`
- `frontend/package.json`

## Checks

### Feature Ownership

- feature folders own their pages, components, api wrappers, and utilities
- shared code is genuinely shared by multiple features
- old global `components`, `hooks`, `pages`, and `utils` do not become dumping grounds
- feature `index.ts` files expose intentional public APIs
- cross-feature imports are deliberate and do not bypass shared abstractions

### Data Access

- features use API wrappers, not generated Axios internals directly
- React Query keys are stable, scoped, and invalidated by the matching mutations
- auth-sensitive queries and mutations respect route and token behavior
- pagination, filters, and URL state stay consistent across page reloads

### UI and State Boundaries

- forms keep validation close to feature rules
- reusable components are not tied to one feature's backend assumptions
- global providers stay in `app` or `shared`, not feature internals
- components do not duplicate formatting or image URL logic that belongs in shared helpers

### Tests

- feature utilities and API wrappers have focused tests
- page tests cover important user flows and route states
- generated client behavior is mocked through wrapper seams, not copied types

### Metadata Drift

Compare docs and instructions with `frontend/package.json`. Report mismatches such as React or Material UI major versions changing without docs being updated.

## Verification Recommendations

- `cd frontend && yarn run lint`
- `cd frontend && yarn build`
- `cd frontend && CI=true yarn test --watchAll=false` if this project still supports that command
- targeted Vitest tests for changed wrappers/utilities
