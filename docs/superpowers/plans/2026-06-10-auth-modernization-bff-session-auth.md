# Auth Modernization BFF Session Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace browser-stored JWT auth with BFF-style server sessions, model account credentials explicitly, then add social login, passkeys, and auth hardening in independently shippable phases.

**Architecture:** The Spring Boot monolith acts as the BFF. The browser receives only httpOnly session cookies plus a non-httpOnly CSRF cookie, while Spring Security handles credentials, OAuth2, WebAuthn, session persistence, and logout invalidation. Account is the user-owned profile and authorization subject; credentials are separate login methods attached to that account. Traefik routes frontend-origin auth paths to the backend so production app traffic is same-origin.

**Tech Stack:** Spring Boot 4.0.6, Spring Security 7, Spring Session JDBC, Flyway, PostgreSQL, React 19, Vite, axios, TanStack Query, Material UI, k3s, Traefik, Argo CD, SOPS.

---

## Scope And Sequencing

This migration is split into four shippable phases. Deploy and verify each phase before starting the next:

1. Phase 1: session foundation, same-origin routing, JWT removal, frontend session bootstrap.
2. Phase 2: credential model foundation, token hardening, Google and GitHub OAuth2 login.
3. Phase 3: WebAuthn passkeys.
4. Phase 4: rate limiting, audit logs, and security headers.

Status as of 2026-06-10:

- Phase 1 is implemented and locally verified.
- Phase 2 backend and frontend/ingress wiring are implemented and locally verified.
- Phase 2 production OAuth secrets are still pending because the local workspace does not have `sops` available for encrypted secret edits.
- Phase 3 and Phase 4 are pending and can be adjusted before implementation.
- The local dev database may need a manual Flyway repair/reset if it applied an earlier draft of `V2__create_spring_session_tables.sql`; do not repair it automatically.

## Design Decisions

- The monolith is the BFF. Do not add a gateway service.
- Production app traffic is same-origin on `https://imdb-clone.the-coding-lab.com`.
- `backend.imdb-clone.the-coding-lab.com` remains available for Swagger showcase traffic.
- Session state is stored in PostgreSQL through Spring Session JDBC.
- Account is not credential. An account may have zero or more credentials:
  - `local_credential` for password login.
  - `account_identity_provider` for OAuth2/OIDC provider links.
  - Spring Security `user_credentials` for passkeys.
- Password login, social login, and passkey login coexist against the same account.
- JWT cutover is hard. Existing users re-login once.
- JWT stays completely absent from browser auth. Do not reintroduce JWT for the SPA. A future external API-client feature may add scoped, short-lived API tokens or JWTs, but that must be a separate plan with a distinct threat model, client registration, revocation, and audit trail.
- No Redis is introduced.
- Backend auth code stays in `identity/internal/security/**`.
- Cross-module account operations go through `account.api`.
- Verification and password-reset tokens are credentials-adjacent secrets. Store only hashed token values, set expiry and one-time-use metadata, and never log raw token values.
- Audit events must cover credential lifecycle changes as well as login/logout: social provider link/unlink, passkey add/remove, password credential creation/change/removal, and verification/reset token issuance/consumption.

## Target Identity Model

Use this model as the architecture compass for Phases 2-4:

- `account`: stable user profile, authorization subject, roles, public username/email.
- `local_credential`: optional password credential for an account. Contains password hash and password metadata, not profile data.
- `account_identity_provider`: one row per social provider link, keyed by provider and provider subject.
- `user_entities` / `user_credentials`: Spring Security WebAuthn tables for passkeys, associated back to the same account identity.
- `verification_token`: existing token table must be migrated so persisted token values are hashes, raw tokens appear only in outbound email URLs, and logs never include token values.
- `security_audit_event`: append-only security event trail for login attempts and credential lifecycle changes.

The invariant is: login method changes must not create duplicate accounts when a verified account match exists, and removing one credential must not delete the account if another login method remains.

## Phase 1 Files

- Modify: `build.gradle`
  - Add `org.springframework.session:spring-session-jdbc`.
  - Remove `io.jsonwebtoken:*` dependencies and `jwtVersion` after JWT classes are gone.
- Modify: `gradle.properties`
  - Remove `jwtVersion`.
- Create: `src/main/resources/db/migration/V2__create_spring_session_tables.sql`
  - Add Spring Session JDBC PostgreSQL schema.
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/WebSecurityConfig.java`
  - Replace stateless JWT config with session auth, CSRF, logout, and problem response entry point.
- Create: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/SpaCsrfTokenRequestHandler.java`
  - Use the Spring Security SPA CSRF pattern.
- Create: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/CsrfCookieFilter.java`
  - Force deferred CSRF token loading so the `XSRF-TOKEN` cookie is written.
- Create: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/ProblemDetailAuthenticationEntryPoint.java`
  - Return the existing 401 problem shape from the security filter chain.
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/web/AuthenticationController.java`
  - Perform credential authentication and save the `SecurityContext` into the HTTP session.
  - Add `GET /api/auth/me`.
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/api/AuthenticationService.java`
  - Remove `loginUser`; keep registration, availability, email confirmation, and password reset.
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/IdentityAccess.java`
  - Remove JWT login generation.
- Delete: `src/main/java/com/thecodinglab/imdbclone/identity/api/LoginResponse.java`
  - Replace with `AccountSessionResponse`.
- Create: `src/main/java/com/thecodinglab/imdbclone/identity/api/AccountSessionResponse.java`
  - Return `id`, `username`, `email`, and `roles`.
- Delete: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/JwtAuthenticationFilter.java`
- Delete: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/JwtAuthenticationEntryPoint.java`
- Delete: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/JwtTokenProvider.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/IdentityProperties.java`
  - Remove `jwt` and `cors` nested properties.
- Delete: `src/main/java/com/thecodinglab/imdbclone/shared/error/JwtValidationException.java`
  - Only if no references remain.
- Modify: `src/main/resources/config/application.properties`
  - Add Spring Session and forwarded header defaults.
- Modify: `src/main/resources/config/application-dev.properties`
  - Remove JWT and CORS properties.
  - Set dev cookie secure to false.
- Modify: `src/main/resources/config/application-prod.properties`
  - Remove JWT and CORS properties.
  - Set prod cookie secure to true.
- Modify: `src/main/resources/META-INF/additional-spring-configuration-metadata.json`
  - Remove JWT and CORS metadata entries.
- Modify: backend tests under `src/test/java/com/thecodinglab/imdbclone/**`
  - Replace Bearer setup with `SecurityMockMvcRequestPostProcessors.user(...)` and `csrf()`.
- Modify: `frontend/vite.config.ts`
  - Add proxy entries for `/api`.
- Modify: frontend env files
  - Use same-origin empty `VITE_IMDB_CLONE_BACKEND_ADDRESS`.
- Modify: `frontend/src/shared/api/httpClient.ts`
  - Remove Bearer interceptor.
  - Configure axios XSRF cookie/header names.
  - Clear in-memory auth state on 401.
- Modify: `frontend/src/shared/auth/authSession.ts`
  - Replace localStorage JWT state with in-memory account session state.
  - Add bootstrap support through `/api/auth/me`.
- Modify: `frontend/src/shared/auth/sessionGuards.ts`
  - Keep public API stable while backing it with session state.
- Modify: `frontend/src/shared/auth/useAuthSession.ts`
  - Reflect session bootstrap state.
- Modify: `frontend/src/app/routes/PrivateRoute.tsx`
  - Wait for bootstrap before redirecting.
- Modify: `frontend/src/app/routes/PublicRoute.tsx`
  - Wait for bootstrap before redirecting.
- Modify: `frontend/src/features/identity/api/identityMutations.ts`
  - Remove `jwt-decode`.
  - Return `AccountSessionResponse` directly from login.
  - Add logout mutation.
- Modify: `frontend/package.json`
  - Remove `jwt-decode`.
- Regenerate: `frontend/src/client/imdb-clone-backend.yaml`
- Regenerate: `frontend/src/client/movies/generator-output/**`
- Modify: `frontend/e2e/protected-routes.spec.ts`
  - Replace localStorage seeding with real API login and cookie storage state.
- Modify: `infrastructure/clusters/home/apps/ingress.yaml`
  - Route `/api` on frontend host to the backend service.
- Modify: frontend deployment/build configuration
  - Ensure the production backend address is empty at Vite build time.
- Modify: `infrastructure/clusters/home/apps/backend-runtime.sops.yaml`
  - Remove `jwt_secret_prod` after cutover verification.

## Phase 1 Tasks

### Task 1: Red Tests For Session Auth

**Files:**
- Modify: `src/test/java/com/thecodinglab/imdbclone/identity/AuthenticationControllerTest.java`
- Modify: `build.gradle`

- [x] **Step 1: Add Spring Security test dependency**

```gradle
testImplementation 'org.springframework.security:spring-security-test'
```

- [x] **Step 2: Write failing tests for the new auth contract**

Cover these behaviors:

- `POST /api/auth/login` with CSRF authenticates credentials.
- Login response is `AccountSessionResponse`, not a token response.
- Login creates a `SESSION` cookie and a row in `spring_session`.
- `GET /api/auth/me` returns the current account with the session cookie.
- `POST /api/auth/login` without CSRF returns 403.
- `GET /api/auth/me` without a session returns 401.
- `POST /api/auth/logout` deletes the server session.

- [x] **Step 3: Run the focused red test**

Run:

```bash
./gradlew test --tests AuthenticationControllerTest
```

Observed red result:

```text
BadSqlGrammarException: relation "spring_session" does not exist
```

This is the expected first failure because the Spring Session schema has not been added yet.

### Task 2: Add Spring Session JDBC Schema And Config

**Files:**
- Modify: `build.gradle`
- Modify: `gradle.properties`
- Create: `src/main/resources/db/migration/V2__create_spring_session_tables.sql`
- Modify: `src/main/resources/config/application.properties`
- Modify: `src/main/resources/config/application-dev.properties`
- Modify: `src/main/resources/config/application-prod.properties`

- [ ] **Step 1: Add Spring Session dependency and remove JWT dependencies**

Expected security dependency block:

```gradle
//-- Security
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.session:spring-session-jdbc'
```

- [ ] **Step 2: Remove `jwtVersion` from `gradle.properties`**

Remove:

```properties
jwtVersion=0.13.0
```

- [ ] **Step 3: Add Spring Session PostgreSQL migration**

Create `V2__create_spring_session_tables.sql` using Spring Session JDBC table names:

```sql
CREATE TABLE spring_session (
    primary_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    creation_time BIGINT NOT NULL,
    last_access_time BIGINT NOT NULL,
    max_inactive_interval INT NOT NULL,
    expiry_time BIGINT NOT NULL,
    principal_name VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX spring_session_ix1 ON spring_session (session_id);
CREATE INDEX spring_session_ix2 ON spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON spring_session (principal_name);

CREATE TABLE spring_session_attributes (
    session_primary_id CHAR(36) NOT NULL,
    attribute_name VARCHAR(200) NOT NULL,
    attribute_bytes BYTEA NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES spring_session(primary_id) ON DELETE CASCADE
);
```

- [ ] **Step 4: Add shared session properties**

Add to `src/main/resources/config/application.properties`:

```properties
spring.session.jdbc.initialize-schema=never
spring.session.timeout=14d
server.forward-headers-strategy=framework
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax
```

- [ ] **Step 5: Add environment cookie security**

Add to dev:

```properties
server.servlet.session.cookie.secure=false
```

Add to prod:

```properties
server.servlet.session.cookie.secure=true
```

- [ ] **Step 6: Run the focused test**

Run:

```bash
./gradlew test --tests AuthenticationControllerTest
```

Expected: schema failure is gone; failures move to JWT response, CSRF, or missing `/api/auth/me`.

### Task 3: Replace JWT Security With Session And CSRF

**Files:**
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/WebSecurityConfig.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/SpaCsrfTokenRequestHandler.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/CsrfCookieFilter.java`
- Create: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/ProblemDetailAuthenticationEntryPoint.java`
- Delete: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/JwtAuthenticationFilter.java`
- Delete: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/JwtAuthenticationEntryPoint.java`
- Delete: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/JwtTokenProvider.java`

- [ ] **Step 1: Add problem detail entry point**

`ProblemDetailAuthenticationEntryPoint` writes:

```json
{
  "status": 401,
  "detail": "Sorry, you're not authorized to access this resource.",
  "instance": "/requested/path"
}
```

- [ ] **Step 2: Add SPA CSRF handler**

Use `XorCsrfTokenRequestAttributeHandler` for token rendering and `CsrfTokenRequestAttributeHandler` for header-supplied tokens.

- [ ] **Step 3: Add CSRF cookie filter**

Read `CsrfToken.class.getName()` from the request and call `getToken()` so Spring writes the `XSRF-TOKEN` cookie.

- [ ] **Step 4: Rewrite `WebSecurityConfig`**

Required behavior:

- No CORS bean.
- No JWT filter.
- `SessionCreationPolicy.IF_REQUIRED`.
- `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
- `POST /api/auth/login` and `POST /api/auth/registration` require CSRF.
- `/api/auth/check-*`, `/api/auth/confirm-email-address`, and password reset endpoints remain public.
- `/api/auth/me` requires authentication.
- Logout URL is `/api/auth/logout`.
- Logout clears `SESSION`.
- Use `HttpSessionSecurityContextRepository`.

- [ ] **Step 5: Delete JWT classes**

Delete the three JWT security classes once compilation no longer needs them.

- [ ] **Step 6: Run the focused backend test**

Run:

```bash
./gradlew test --tests AuthenticationControllerTest
```

Expected: login and CSRF behavior failures move to controller/session response work.

### Task 4: Move Login To Server-Side Session

**Files:**
- Create: `src/main/java/com/thecodinglab/imdbclone/identity/api/AccountSessionResponse.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/web/AuthenticationController.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/api/AuthenticationService.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/IdentityAccess.java`
- Delete: `src/main/java/com/thecodinglab/imdbclone/identity/api/LoginResponse.java`

- [ ] **Step 1: Add `AccountSessionResponse`**

Fields:

```java
Long id
String username
String email
List<String> roles
```

- [ ] **Step 2: Remove `loginUser` from `AuthenticationService`**

Keep these methods:

```java
UserIdentityAvailability checkUsernameAvailability(String username);
UserIdentityAvailability checkEmailAvailability(String email);
MessageResponse registerUser(RegistrationRequest request);
MessageResponse confirmEmailAddress(String token);
MessageResponse resetPassword(String email);
MessageResponse saveNewPassword(PasswordResetRequest request);
```

- [ ] **Step 3: Remove JWT login from `IdentityAccess`**

Remove injected dependencies:

```java
AuthenticationManager authenticationManager
JwtTokenProvider jwtTokenProvider
```

- [ ] **Step 4: Implement login in `AuthenticationController`**

Use:

```java
Authentication authentication =
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.usernameOrEmail(), request.password()));
SecurityContext context = securityContextHolderStrategy.createEmptyContext();
context.setAuthentication(authentication);
securityContextHolderStrategy.setContext(context);
request.changeSessionId();
securityContextRepository.saveContext(context, request, response);
```

Return `AccountSessionResponse` derived from `UserPrincipal`.

- [ ] **Step 5: Implement `/api/auth/me`**

Use `@CurrentUser UserPrincipal currentUser` and return `AccountSessionResponse`.

- [ ] **Step 6: Run the focused backend test**

Run:

```bash
./gradlew test --tests AuthenticationControllerTest
```

Expected: `AuthenticationControllerTest` passes.

### Task 5: Migrate Backend Controller Tests From Bearer To Session Test Auth

**Files:**
- Modify: `src/test/java/com/thecodinglab/imdbclone/account/AccountControllerTest.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/catalog/MovieControllerTest.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/catalog/SearchControllerTest.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/engagement/CommentControllerTest.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/engagement/RatingControllerTest.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/engagement/WatchedMovieControllerTest.java`

- [ ] **Step 1: Remove token setup**

Remove `AuthenticationService`, `LoginRequest`, token fields, and `SecurityContextHolder.clearContext()` from affected tests.

- [ ] **Step 2: Add user principals through Spring Security test support**

Use `user(...)` with roles:

```java
.with(user("test_user_two").roles("USER"))
```

Use admin:

```java
.with(user("test_user_one").roles("ADMIN"))
```

- [ ] **Step 3: Add CSRF to mutating requests**

Add:

```java
.with(csrf())
```

to `POST`, `PUT`, `PATCH`, and `DELETE` requests that should pass security.

- [ ] **Step 4: Keep unauthenticated tests without `user(...)`**

For unauthenticated mutating requests, include `csrf()` when the expected result should be 401 instead of 403.

- [ ] **Step 5: Run backend tests by package**

Run:

```bash
./gradlew test --tests '*AuthenticationControllerTest' --tests '*AccountControllerTest' --tests '*MovieControllerTest' --tests '*SearchControllerTest' --tests '*CommentControllerTest' --tests '*RatingControllerTest' --tests '*WatchedMovieControllerTest'
```

Expected: selected backend web tests pass.

### Task 6: Remove Backend JWT And CORS Configuration

**Files:**
- Modify: `gradle.properties`
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/IdentityProperties.java`
- Modify: `src/main/resources/config/application-dev.properties`
- Modify: `src/main/resources/config/application-prod.properties`
- Modify: `src/main/resources/META-INF/additional-spring-configuration-metadata.json`
- Delete: `src/main/java/com/thecodinglab/imdbclone/shared/error/JwtValidationException.java`

- [ ] **Step 1: Remove JWT and CORS records from `IdentityProperties`**

Final constructor fields:

```java
@NotBlank String backendHost,
@NotBlank String frontendHost,
boolean emailVerificationEnabled
```

- [ ] **Step 2: Remove JWT and CORS properties from dev/prod**

Remove keys under:

```properties
imdb-clone.identity.jwt.*
imdb-clone.identity.cors.*
```

- [ ] **Step 3: Remove metadata entries for JWT and CORS**

Delete entries for:

```text
imdb-clone.identity.jwt.secret
imdb-clone.identity.jwt.expiration-in-ms
imdb-clone.identity.cors.allowed-origins
```

- [ ] **Step 4: Verify no backend JWT references remain**

Run:

```bash
rg -n "Jwt|JWT|jjwt|Bearer|Authorization" src/main/java build.gradle gradle.properties src/main/resources/config
```

Expected: no backend runtime JWT references remain. HTTP scratch files may still contain Bearer examples and can be migrated separately.

### Task 7: Frontend Same-Origin Session State

**Files:**
- Modify: `frontend/vite.config.ts`
- Modify: `frontend/src/shared/api/httpClient.ts`
- Modify: `frontend/src/shared/auth/authSession.ts`
- Modify: `frontend/src/shared/auth/sessionGuards.ts`
- Modify: `frontend/src/shared/auth/useAuthSession.ts`
- Modify: `frontend/src/app/routes/PrivateRoute.tsx`
- Modify: `frontend/src/app/routes/PublicRoute.tsx`
- Modify: `frontend/src/features/identity/api/identityMutations.ts`
- Modify: `frontend/src/features/identity/pages/LoginPage.tsx`
- Modify: `frontend/src/shared/layout/AppBarTop/UserSettingsMenu.tsx`
- Modify: `frontend/package.json`

- [ ] **Step 1: Write failing frontend unit tests**

Update tests for:

- no Authorization header is added.
- axios sends XSRF header from `XSRF-TOKEN`.
- 401 clears session state.
- auth state is in memory, not localStorage.
- login stores `AccountSessionResponse`.
- logout calls `/api/auth/logout`.

- [ ] **Step 2: Configure Vite proxy**

Set:

```ts
server: {
  proxy: {
    "/api": {
      target: "http://localhost:8080",
    },
  },
}
```

- [ ] **Step 3: Configure axios for CSRF**

Set:

```ts
apiHttpClient.defaults.xsrfCookieName = "XSRF-TOKEN";
apiHttpClient.defaults.xsrfHeaderName = "X-XSRF-TOKEN";
```

Remove Bearer request interceptor logic.

- [ ] **Step 4: Rewrite `authSession` as in-memory state**

Store:

```ts
{
  id: number;
  username: string;
  email: string;
  roles: string[];
}
```

- [ ] **Step 5: Add bootstrap**

Call `/api/auth/me` once at app startup. Treat 401 as logged out.

- [ ] **Step 6: Update routes**

Private routes wait for bootstrap completion before deciding whether to redirect.

- [ ] **Step 7: Remove `jwt-decode`**

Run:

```bash
cd frontend && yarn remove jwt-decode
```

- [ ] **Step 8: Run frontend tests**

Run:

```bash
cd frontend && yarn test
```

Expected: frontend unit tests pass.

### Task 8: Regenerate OpenAPI Client

**Files:**
- Regenerate: `frontend/src/client/imdb-clone-backend.yaml`
- Regenerate: `frontend/src/client/movies/generator-output/**`

- [ ] **Step 1: Start backend**

Run:

```bash
./gradlew bootRun
```

- [ ] **Step 2: Regenerate spec and client**

Run:

```bash
cd frontend && yarn run updateOpenApiSpec && yarn run build:moviesGen
```

- [ ] **Step 3: Verify generated client includes `AccountSessionResponse`**

Run:

```bash
rg -n "AccountSessionResponse|LoginResponse|accessToken" frontend/src/client
```

Expected: `AccountSessionResponse` exists; `LoginResponse` and `accessToken` no longer appear as auth login output.

### Task 9: Same-Origin Ingress And Deployment Config

**Files:**
- Modify: `infrastructure/clusters/home/apps/ingress.yaml`
- Modify: frontend deployment/build env files under `infrastructure/clusters/home/apps`
- Modify: `infrastructure/clusters/home/apps/backend-runtime.sops.yaml`

- [ ] **Step 1: Route `/api` on frontend host to backend**

Add a Prefix path for `/api` on `imdb-clone.the-coding-lab.com` that targets `imdb-clone-backend`.

- [ ] **Step 2: Preserve backend Swagger host**

Keep `backend.imdb-clone.the-coding-lab.com` routing to backend.

- [ ] **Step 3: Empty frontend backend address for production build**

Ensure Vite sees:

```text
VITE_IMDB_CLONE_BACKEND_ADDRESS=
```

- [ ] **Step 4: Remove JWT production secret after deploy verification**

Remove `jwt_secret_prod` from `backend-runtime.sops.yaml` only after the Phase 1 deployment is proven.

- [ ] **Step 5: Verify rendered Kubernetes resources**

Run:

```bash
kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml
```

Expected: render succeeds and frontend host has `/api` backend routing.

### Task 10: Phase 1 Verification

**Files:**
- No source changes unless verification finds a defect.

- [ ] **Step 1: Backend tests**

Run:

```bash
./gradlew test
```

- [ ] **Step 2: Backend build**

Run:

```bash
./gradlew build jacocoTestReport
```

- [ ] **Step 3: Frontend lint and tests**

Run:

```bash
cd frontend && yarn run lint
cd frontend && yarn test
```

- [ ] **Step 4: Frontend build**

Run:

```bash
cd frontend && yarn build
```

- [ ] **Step 5: Manual local auth smoke**

Verify:

- login sets `SESSION` with HttpOnly and SameSite=Lax.
- login sets `XSRF-TOKEN`.
- `select count(*) from spring_session` increases after login.
- mutating request without CSRF returns 403.
- axios mutating request succeeds.
- backend restart keeps the session valid.
- logout deletes the session row.
- `/api/auth/me` returns 401 after logout.

## Phase 2 Tasks: Credential Model And Social Logins

### Task 11: Credential Model Foundation And Token Hardening

**Files:**
- Create: `src/main/resources/db/migration/V3__credential_model_and_token_hardening.sql`
- Modify: `src/main/java/com/thecodinglab/imdbclone/account/api/AccountIdentityService.java`
- Modify account internal implementation files discovered by `rg "implements AccountIdentityService" src/main/java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/account/internal/persistence/Account.java`
- Create local credential persistence files under `src/main/java/com/thecodinglab/imdbclone/identity/internal/persistence/`
- Modify verification token persistence and services under `src/main/java/com/thecodinglab/imdbclone/identity/internal/`
- Create audit foundation files under `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/audit/`
- Modify tests under `src/test/java/com/thecodinglab/imdbclone/identity/`

- [x] **Step 1: Add local credential schema**

Migration intent:

```sql
CREATE TABLE local_credential (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_password_change_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT local_credential_account_unique UNIQUE (account_id)
);

CREATE INDEX local_credential_account_id_idx
    ON local_credential(account_id);

INSERT INTO local_credential(account_id, password_hash, created_at, updated_at)
SELECT id, password, now(), now()
FROM account
WHERE password IS NOT NULL;

ALTER TABLE account ALTER COLUMN password DROP NOT NULL;
```

Keep the old `account.password` column during the migration if needed for a safe two-step deploy, but all new password reads/writes must move to `local_credential` before later dropping the legacy column in a cleanup migration.

- [x] **Step 2: Harden verification and reset token storage**

Migration intent:

```sql
ALTER TABLE verification_token ADD COLUMN token_hash VARCHAR(128);
ALTER TABLE verification_token ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE verification_token ADD COLUMN consumed_at TIMESTAMP WITH TIME ZONE;
CREATE UNIQUE INDEX verification_token_token_hash_key ON verification_token(token_hash);
```

Implementation rules:

- Generate raw tokens with at least 128 bits of entropy.
- Store only a SHA-256 or stronger keyed hash of the token.
- Compare by hashing the presented token and querying `token_hash`.
- Mark one-time tokens consumed by setting `consumed_at`.
- Reject missing, expired, or consumed tokens with the existing ProblemDetail style.
- Never log raw tokens. Existing logs that include `token=` must be removed or redacted.

- [x] **Step 3: Add security audit foundation**

Create the audit table in the same migration so credential/social/passkey lifecycle events can be recorded as soon as those features are introduced:

```sql
CREATE TABLE security_audit_event (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    event_type VARCHAR(100) NOT NULL,
    principal VARCHAR(255),
    account_id BIGINT REFERENCES account(id) ON DELETE SET NULL,
    ip_address VARCHAR(100),
    details JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX security_audit_event_occurred_at_idx
    ON security_audit_event(occurred_at);

CREATE INDEX security_audit_event_event_type_idx
    ON security_audit_event(event_type);

CREATE INDEX security_audit_event_account_id_idx
    ON security_audit_event(account_id);
```

Add a small internal audit port:

```java
void recordCredentialEvent(SecurityAuditEventType type, AccountId accountId, Map<String, Object> details);
```

The first implementation can be simple JDBC/JPA persistence. Phase 4 will add Spring Security event listeners, retention cleanup, dashboards, and rate-limit event coverage.

- [x] **Step 4: Add account API operations for credentials**

Add account-facing API methods that expose intent, not persistence details:

```java
boolean hasLocalCredential(AccountId accountId);
void createLocalCredential(AccountId accountId, String encodedPassword);
void updateLocalCredential(AccountId accountId, String encodedPassword);
Optional<AccountIdentity> findOptionalByEmail(String email);
```

Password authentication must load credentials through the identity/account boundary without reaching into account internals from `identity/internal/security/**`.

- [x] **Step 5: Preserve password login behavior**

Write tests proving:

- Existing password users can still log in after migration.
- Accounts without `local_credential` cannot password-login and return a clean 401.
- Password reset creates or updates `local_credential` for the account.
- Raw verification/reset tokens are not persisted and not written to logs.
- Token issuance and consumption create audit rows without raw token values.

- [x] **Step 6: Verify credential foundation**

Run:

```bash
./gradlew test --tests '*Authentication*' --tests '*Verification*' --tests '*DatabaseSchemaTest'
./gradlew test
```

Expected: password login, registration, email confirmation, and reset-password flows pass with `local_credential` and hashed one-time tokens.

### Task 12: OAuth2 Backend Foundation

**Files:**
- Modify: `build.gradle`
- Create: `src/main/resources/db/migration/V4__social_login_identity_providers.sql`
- Modify: `src/main/java/com/thecodinglab/imdbclone/account/api/AccountIdentityService.java`
- Modify account internal implementation files discovered by `rg "implements AccountIdentityService" src/main/java`
- Create files under `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/oauth2/`
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/WebSecurityConfig.java`
- Modify: `src/main/resources/config/application-dev.properties`
- Modify: `src/main/resources/config/application-prod.properties`

- [x] **Step 1: Add OAuth2 client dependency**

```gradle
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

- [x] **Step 2: Add social identity schema**

Migration changes:

```sql
CREATE TABLE account_identity_provider (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT account_identity_provider_provider_user_unique
        UNIQUE (provider, provider_user_id)
);

CREATE INDEX account_identity_provider_account_id_idx
    ON account_identity_provider(account_id);
```

- [x] **Step 3: Add account API operations**

Add:

```java
AccountIdentity createSocialAccount(String username, String email);
Optional<AccountIdentityProviderLink> findProviderLink(String provider, String providerUserId);
void linkProvider(AccountId accountId, String provider, String providerUserId, String email);
```

- [x] **Step 4: Implement social provisioning service**

Lookup order:

1. provider and provider user id.
2. verified email link.
3. create enabled account with `ROLE_USER`.

Rules:

- Google may link by email only when `email_verified=true`.
- GitHub may link by email only after fetching a verified primary email from `/user/emails`.
- A provider link must be unique by `(provider, provider_user_id)`.
- Creating or linking a provider must emit a `SOCIAL_PROVIDER_LINKED` security audit event.
- Failed link attempts caused by unverified or conflicting provider email must emit `SOCIAL_PROVIDER_LINK_FAILED` without storing provider access tokens or authorization codes.

- [x] **Step 5: Configure OAuth2 login**

Add custom OIDC user service for Google and OAuth2 user service for GitHub.

- [x] **Step 6: Verify Phase 2 backend**

Run:

```bash
./gradlew test
```

Expected: tests pass and accounts without `local_credential` cannot password-login.

### Task 13: OAuth2 Frontend And Ingress

**Files:**
- Modify: `frontend/src/features/identity/pages/LoginPage.tsx`
- Modify: `frontend/src/features/identity/pages/RegistrationPage.tsx`
- Modify: `frontend/vite.config.ts`
- Modify: `infrastructure/clusters/home/apps/ingress.yaml`
- Modify: `infrastructure/clusters/home/apps/backend-runtime.sops.yaml`

- [x] **Step 1: Add social login buttons**

Buttons navigate with:

```ts
window.location.href = "/oauth2/authorization/google";
window.location.href = "/oauth2/authorization/github";
```

- [x] **Step 2: Proxy OAuth2 routes in dev**

Add Vite proxy paths:

```ts
"/oauth2": { target: "http://localhost:8080" },
"/login/oauth2": { target: "http://localhost:8080" },
```

- [x] **Step 3: Route OAuth2 paths in production**

Add ingress backend paths:

```text
/oauth2
/login/oauth2
```

- [ ] **Step 4: Add OAuth2 secrets**

Pending: add encrypted `google_client_id`, `google_client_secret`, `github_client_id`, and `github_client_secret` entries to `infrastructure/clusters/home/apps/backend-runtime.sops.yaml` with SOPS. Do not store plaintext placeholders in the manifest.

Add Google and GitHub client id and secret placeholders to SOPS-backed runtime secrets.

- [ ] **Step 5: Verify Phase 2 manually**

Verify:

- new Google user creates account and provider link.
- existing password account with same verified Google email links, not duplicates.
- GitHub private email is fetched from `/user/emails`.
- redirect returns to the frontend and `/api/auth/me` sees the session.

## Phase 3 Tasks: Passkeys

### Task 14: WebAuthn Backend

**Files:**
- Modify: `build.gradle`
- Create: `src/main/resources/db/migration/V5__create_webauthn_tables.sql`
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/WebSecurityConfig.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/IdentityProperties.java`
- Create passkey management controller files under `src/main/java/com/thecodinglab/imdbclone/identity/web/`
- Create supporting identity security files under `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/webauthn/`

- [ ] **Step 0: Add Spring Security WebAuthn dependency**

Spring Security 7.0.5 exposes the WebAuthn DSL from config, but the implementation classes and JDBC repositories live in the separate artifact:

```gradle
implementation 'org.springframework.security:spring-security-webauthn'
```

- [ ] **Step 1: Add Spring Security WebAuthn tables**

Use Spring Security JDBC repository table names exactly:

```text
user_entities
user_credentials
```

- [ ] **Step 2: Configure WebAuthn DSL**

Set:

```text
rpName: IMDB Clone
dev rpId: localhost
prod rpId: imdb-clone.the-coding-lab.com
dev origin: http://localhost:3000
prod origin: https://imdb-clone.the-coding-lab.com
```

- [ ] **Step 3: Add JDBC WebAuthn repositories**

Create beans:

```java
JdbcPublicKeyCredentialUserEntityRepository
JdbcUserCredentialRepository
```

Wrap `JdbcUserCredentialRepository` if needed for application audit events; keep the table schema compatible with Spring Security's repository SQL.

- [ ] **Step 4: Normalize WebAuthn login principal**

After `/login/webauthn`, reload `UserPrincipal` through `AccountUserDetails` and save it in the `SecurityContext`.

Also replace Spring Security's default session-backed WebAuthn options repositories with JSON-backed session repositories. `PublicKeyCredentialCreationOptions` is not Java-serializable in Spring Security 7.0.5, so storing it directly breaks Spring Session JDBC.

- [ ] **Step 5: Add passkey management endpoints**

Add:

```text
GET /api/account/passkeys
DELETE /api/account/passkeys/{credentialId}
```

Rules:

- Registering a passkey must attach the credential to the existing account, not create a separate account.
- Deleting a passkey must not delete the account.
- Add and delete operations must emit `PASSKEY_REGISTERED` and `PASSKEY_DELETED` security audit events.

- [ ] **Step 6: Verify Phase 3 backend**

Run:

```bash
./gradlew test
```

Expected: passkey session can call an endpoint using `@CurrentUser UserPrincipal`.

### Task 15: WebAuthn Frontend And Ingress

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/src/features/identity/passkeys/passkeyApi.ts`
- Modify: `frontend/src/features/identity/pages/LoginPage.tsx`
- Modify: `frontend/src/features/account/pages/AccountSettingsPage.tsx`
- Modify: `frontend/vite.config.ts`
- Modify: `infrastructure/clusters/home/apps/ingress.yaml`

- [ ] **Step 1: Add WebAuthn JSON helper**

Run:

```bash
cd frontend && yarn add @github/webauthn-json
```

- [ ] **Step 2: Add passkey login**

Use:

```text
POST /webauthn/authenticate/options
POST /login/webauthn
```

- [ ] **Step 3: Add passkey registration**

Use:

```text
POST /webauthn/register/options
POST /webauthn/register
```

- [ ] **Step 4: Add account settings passkey management**

List and delete passkeys through `/api/account/passkeys`.

- [ ] **Step 5: Route WebAuthn paths**

Proxy and ingress paths:

```text
/webauthn
/login/webauthn
```

- [ ] **Step 6: Verify Phase 3 manually**

Verify:

- logged-in user registers a passkey.
- logout.
- passkey login restores same account.
- rating a movie works after passkey login.
- `signature_count` increments.

## Phase 4 Tasks: Hardening

### Task 16: Spring Rate Limiting

**Files:**
- Modify: `build.gradle`
- Create files under `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/ratelimit/`
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/WebSecurityConfig.java`
- Modify: `src/main/resources/config/application.properties`
- Modify test/e2e properties to disable rate limiting where needed.

- [x] **Step 1: Add dependencies**

```gradle
implementation 'com.bucket4j:bucket4j_jdk17-core:8.19.0'
implementation 'com.github.ben-manes.caffeine:caffeine'
```

- [x] **Step 2: Add rate limit properties**

Prefix:

```properties
imdb-clone.identity.rate-limit.enabled=true
```

- [x] **Step 3: Add auth rate limit filter**

Rules:

```text
/api/auth/login: 10/min per IP
/login/webauthn: 10/min per IP
/api/auth/registration: 5/hour per IP
/api/auth/reset-password: 5/hour per IP
/oauth2/authorization/**: 20/min per IP
login failures: 5/min per username
```

- [x] **Step 4: Expose metrics**

Counter:

```text
imdb_clone.rate_limit.rejections
```

Tags:

```text
rule
```

- [x] **Step 5: Verify rate limiting**

Run a curl loop against login and confirm 429 plus Prometheus counter.

### Task 17: Extend Security Audit Events

**Files:**
- Modify files under `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/audit/`
- Modify or create retention scheduler files under `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/audit/`

- [x] **Step 1: Verify audit table and event taxonomy**

The table is created in Task 11 so social and passkey phases can emit lifecycle events. Confirm it has at least:

```text
occurred_at
event_type
principal
ip_address
details jsonb
```

Recommended event types:

```text
PASSWORD_LOGIN_SUCCESS
PASSWORD_LOGIN_FAILURE
OAUTH2_LOGIN_SUCCESS
OAUTH2_LOGIN_FAILURE
PASSKEY_LOGIN_SUCCESS
PASSKEY_LOGIN_FAILURE
LOGOUT_SUCCESS
LOCAL_CREDENTIAL_CREATED
LOCAL_CREDENTIAL_PASSWORD_CHANGED
SOCIAL_PROVIDER_LINKED
SOCIAL_PROVIDER_LINK_FAILED
PASSKEY_REGISTERED
PASSKEY_DELETED
VERIFICATION_TOKEN_ISSUED
VERIFICATION_TOKEN_CONSUMED
PASSWORD_RESET_TOKEN_ISSUED
PASSWORD_RESET_TOKEN_CONSUMED
RATE_LIMIT_REJECTED
```

- [x] **Step 2: Add event listener**

Listen to:

```text
AuthenticationSuccessEvent
AbstractAuthenticationFailureEvent
LogoutSuccessEvent
```

Skip duplicate `InteractiveAuthenticationSuccessEvent`.

Use the application service/port from Task 11 for explicit lifecycle events that are not reliably represented by Spring authentication events:

```java
void recordCredentialEvent(SecurityAuditEventType type, AccountId accountId, Map<String, Object> details);
```

Use that port from:

- local credential creation/change.
- social provider link and failed link.
- passkey registration and deletion.
- verification token issuance/consumption.
- password-reset token issuance/consumption.

Do not put raw tokens, OAuth access tokens, authorization codes, passkey credential public keys, or credential ids that can be used for replay into `details`.

- [x] **Step 3: Add retention cleanup**

Reuse the scheduler style from `VerificationTokenCleanupScheduler` and delete events older than 90 days.

- [x] **Step 4: Verify audit**

Exercise password, social, passkey, failed login, logout, social linking, passkey add/remove, and verification/reset token consumption. Confirm rows include correct event type and client IP, and confirm no raw token values are present in the table or logs.

### Task 18: Security Headers And Traefik Middleware

**Files:**
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/security/WebSecurityConfig.java`
- Create: `infrastructure/clusters/home/apps/traefik-middlewares.yaml`
- Modify: `infrastructure/clusters/home/apps/kustomization.yaml`
- Modify ingress annotations in `infrastructure/clusters/home/apps/ingress.yaml`
- Modify Grafana dashboard manifests if present under `infrastructure/clusters/home/apps`

- [x] **Step 1: Add backend security headers**

Configure:

```text
Referrer-Policy
Permissions-Policy: publickey-credentials-get=(self)
```

- [x] **Step 2: Add Traefik rate limit middleware**

Coarse rule:

```text
average: 50/s
burst: 100
```

- [x] **Step 3: Add Traefik security headers middleware**

Include:

```text
HSTS
nosniff
referrer policy
CSP
```

- [x] **Step 4: Verify rendered manifests**

Run:

```bash
kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml
```

- [ ] **Step 5: Verify production headers**

Run a securityheaders.com scan and tune CSP so MUI and object-storage images still work.

## Final Verification Matrix

- [x] `./gradlew test`
- [x] `./gradlew build jacocoTestReport`
- [ ] `cd frontend && yarn run lint`
- [ ] `cd frontend && yarn test`
- [ ] `cd frontend && yarn build`
- [x] `kubectl kustomize infrastructure/clusters/home/apps >/tmp/imdb-clone-home-apps.yaml`
- [ ] Manual login, logout, CSRF, restart persistence, social login, passkey login, and rate-limit smoke checks for the relevant deployed phase.

## Open Risks To Recheck Per Phase

- Spring Session serializes `SecurityContext`; changes to `UserPrincipal` can force re-login.
- OAuth2 redirects behind Traefik depend on `server.forward-headers-strategy=framework`.
- Swagger on the backend subdomain uses a separate cookie context and needs CSRF for mutating "try it out" calls.
- WebAuthn principal normalization must be verified against Spring Security 7 source during Phase 3.
- CSP from Traefik may need one tuning pass for Material UI inline styles and object-storage image URLs.
