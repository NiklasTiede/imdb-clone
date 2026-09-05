# REST API contract

Application REST endpoints use `/api/v1`. Spring MVC's native API version conditions
(`version = "1"`) and the `v{version}` path segment select the implementation.
Only application paths under `/api/` participate in version resolution. OAuth callbacks,
WebAuthn, MCP, OpenAPI and Actuator retain their protocol-specific paths.
Springdoc expands the versioned mappings to concrete `/api/v1` paths. The generated
TypeScript client is built from that document; never hand-edit generated files.

This migration replaces the unversioned application API. Ship backend and frontend together;
old browser tabs and external callers must reload/update. No compatibility aliases are retained.
A new major version is reserved for incompatible contract changes, not every release or
additional optional response field.

## Resources and HTTP semantics

- Movies: `GET /api/v1/movies/{id}`, `GET /api/v1/movies?ids=1,2`,
  `POST /api/v1/movies`, `PUT`/`DELETE /api/v1/movies/{id}`.
- Comments: `GET`/`POST /api/v1/movies/{id}/comments` and
  `GET`/`PUT`/`DELETE /api/v1/comments/{id}`.
- Current-user ratings: `PUT /api/v1/accounts/me/ratings/{movieId}` with
  `{ "score": 8.5 }`; `DELETE` on the same URI removes the rating.
- Current-user watchlist: `PUT`/`DELETE /api/v1/accounts/me/watchlist/{movieId}`.
- Admin role assignment: `PUT`/`DELETE /api/v1/accounts/{username}/roles/admin`.
- Complex search retains POST with a structured filter body.

Creation returns `201` with a `Location` header for addressable resources. Rating and
watchlist PUT requests return `201` when creating and `200` when replacing an existing
resource; repeated watchlist PUTs preserve the original added timestamp.
DELETE responses with status `204` have no body and are typed as `void` in the client.
Bulk movie lookup accepts 1–30 positive IDs. Pagination requires `page >= 0` and `1 <= size <= 30`.

## Authentication, authorization and errors

The request filter distinguishes public routes from authenticated routes. Method security
restricts privileged controller operations. The services enforce ownership using account IDs;
only owners or admins may edit/delete an account or comment. Role-management service methods
also require ADMIN, protecting calls that do not originate in the REST controller.

Use `hasRole('ADMIN')` for administrative operations and `hasAnyRole('USER', 'ADMIN')`
for member operations. This explicitly supports admins without requiring a second stored role.
Do not replace object ownership checks with a role check.

- `401`: no valid authentication; the frontend clears its local authentication state.
- `403`: authenticated caller lacks permission; preserve the frontend session.
- `400`: malformed or invalid request; never reflect rejected values or raw exception messages.

Errors use `application/problem+json`. Validation errors include `code: validation_failed`
and an `errors` map from field names to safe validation messages. Security-filter denials
also use Problem Details. Tests cover anonymous, owner, other-user and admin access, as well
as the absence of rejected passwords in validation responses and application logs.

## Recovery actions

GET requests never activate accounts or send password-reset emails.

- `POST /api/v1/auth/email-confirmations` with `{ "token": "..." }`.
- `POST /api/v1/auth/password-reset-requests` with `{ "email": "..." }`.
- `POST /api/v1/auth/password-resets` with `{ "token": "...", "newPassword": "..." }`.

Email-confirmation links open `/confirm-email?token=...` in the frontend; confirmation
requires a button click. Password-reset links open `/reset-password?token=...`.
All mutation requests retain CSRF protection and password-reset requests retain rate limiting.

## Verification

Run `./gradlew test integrationTest`, including the OpenAPI contract drift assertion.
With the backend running, refresh via `yarn run updateOpenApiSpec` and regenerate via
`yarn run build:moviesGen` in `frontend/`. Run frontend lint, tests and build, plus the
movie-detail browser tests for comments and ratings, plus desktop/mobile recovery flows.
Movie Concierge uses the separate MCP contract at `/mcp`, which this migration does not change.
