# Authentication Experience Polish Implementation Plan

> **For agentic workers:** Execute tasks sequentially and keep every slice testable. Use the
> `frontend-design` skill for visual implementation and `playwright-cli` for responsive review.

**Goal:** Make login and registration feel intentionally composed on desktop and efficient on
mobile, align credential validation across frontend and backend, and give users durable, accurate
feedback throughout password, social, and passkey authentication.

**Architecture:** Keep `AuthLayout` as the route shell and make it own a `100dvh` header/main grid.
Introduce a feature-owned `AuthPageFrame` for the reusable desktop split and mobile form layout.
`AuthVisualPane` becomes a full-height media pane with one centered content group and a locally
bundled cinematic bitmap, so auth rendering does not depend on the catalog API or object storage.
Move registration validation into a tested frontend module that deliberately mirrors the backend
credential contract. Keep mutation state and user-facing feedback in the page/component that owns
the interaction.

**Tech Stack:** React 19, Material UI 9, React Router 7, TanStack Query 5, React Hook Form 7, Zod 4,
Spring Boot/Jakarta Validation, Vitest, Testing Library, and Playwright 1.59.

---

## Product Decisions

- Keep the two-pane cinematic layout at `md` and above; keep mobile focused on a single form column.
- Fill the full area below the auth header. Center one coherent copy/features group inside the media
  pane with consistent insets instead of distributing content against opposite edges.
- Replace the synthetic CSS poster-title grid with an original, locally bundled movie-catalog
  bitmap and a dark readability overlay. Use a semantic color fallback if the image fails.
- Do not fetch a featured movie or hard-code an object-storage token on an authentication route.
- Remove autofocus so mobile keyboards do not open or shift the viewport before user intent.
- Preserve the existing password policy in this slice; make every rule and boundary visible and
  consistent instead of redesigning the security policy.
- Show registration completion as a persistent inline message on the login page after redirect.
  Use the backend `MessageResponse` so verification-enabled and verification-disabled deployments
  can give different accurate instructions.
- Remove placeholder Terms and Privacy links. Writing and approving legal content is separate work.
- Migrate the reset-password page to the shared visual frame, but do not implement its currently
  missing request/token submission workflow in this slice.

## Non-Goals

- No changes to session, CSRF, OAuth2, or WebAuthn protocols.
- No new OAuth providers or passkey-management capabilities.
- No password reset token-flow implementation.
- No legal-policy copy authored by the development team.
- No change to the public catalog or application navigation outside the auth shell/loading state.

## Acceptance Criteria

- At `1440x1000` and `1366x768`, the visual pane extends from the header to the viewport bottom and
  its content has balanced top, right, bottom, and left whitespace.
- At `390x844` and `320x700`, the visual pane is hidden, forms start beneath the header with stable
  padding, the header does not overlap, and the document has no horizontal overflow.
- Short-height desktop content remains reachable by scrolling; vertical centering never clips the
  top of a form.
- Login, registration, and reset-password presentation all use the same responsive frame.
- Username rules consistently enforce 2-20 characters, letters/digits/dot/underscore, no leading or
  trailing dot/underscore, and no consecutive dot/underscore characters.
- Password feedback covers 8-30 characters, uppercase, lowercase, number, and accepted special
  characters. A password cannot show all rules as satisfied while remaining invalid.
- Confirmation reports required/mismatch feedback without repeating the entire complexity policy.
- Pending password/passkey/registration actions visibly identify what is happening and prevent
  duplicate submission.
- Invalid credentials, social-login failure, rate limiting, availability-check failure, and generic
  network failure have distinct, accessible feedback.
- Registration completion remains visible after navigation to login and gives correct next steps.
- Copy describes only implemented product capabilities.

---

### Task 1: Lock Down The Credential Validation Contract

**Files:**
- Create: `src/test/java/com/thecodinglab/imdbclone/shared/validation/CredentialValidationTest.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/shared/validation/ValidUsername.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/shared/validation/ValidUsernameImpl.java`
- Modify: `src/main/java/com/thecodinglab/imdbclone/shared/validation/ValidPasswordImpl.java`
- Create: `frontend/src/features/identity/model/registrationValidation.ts`
- Create: `frontend/src/features/identity/model/registrationValidation.test.ts`
- Modify: `frontend/src/features/identity/pages/RegistrationPage.tsx`
- Modify: `frontend/src/i18n.ts`

- [x] Write backend tests for username length boundaries, allowed separators, invalid leading,
  trailing, or consecutive separators, and password length/composition boundaries.
- [x] Verify `ValidPasswordImpl` treats `null` as the responsibility of `@NotBlank` instead of
  throwing while validation evaluates multiple constraints.
- [x] Correct the `ValidUsername` annotation message from 30 to 20 characters and use wording that
  matches the actual dot/underscore contract.
- [x] Write frontend tests for the same username and password cases, including the special-character
  rule, the 30-character password maximum, and confirmation mismatch.
- [x] Move the registration schema and password-rule descriptors into
  `registrationValidation.ts`; keep display rules and Zod validation driven by the same descriptors.
- [x] Make confirmation required and match the primary password, but do not independently repeat
  every password-complexity error.
- [x] Remove the stale frontend claim that usernames allow hyphens and the conflicting 30-character
  username limit.
- [x] Run the targeted backend and frontend tests before continuing.

### Task 2: Build The Shared Responsive Authentication Frame

**Files:**
- Modify: `frontend/src/shared/layout/AuthLayout.tsx`
- Modify: `frontend/src/shared/layout/AuthLayout.test.tsx`
- Create: `frontend/src/features/identity/components/AuthPageFrame.tsx`
- Create: `frontend/src/features/identity/components/AuthPageFrame.test.tsx`
- Modify: `frontend/src/features/identity/pages/LoginPage.tsx`
- Modify: `frontend/src/features/identity/pages/RegistrationPage.tsx`
- Modify: `frontend/src/features/identity/pages/ResetPasswordPage.tsx`
- Modify: `frontend/src/App.tsx`

- [x] Make `AuthLayout` a `100dvh` grid with an auto-sized header and a `minmax(0, 1fr)` main row;
  remove child dependence on `calc(100vh - 55px)`.
- [x] Render route content inside a semantic `main` region and retain the shared brand/home link.
- [x] On narrow screens, show only the alternate action (`Sign in` or `Sign up`) and hide the longer
  explanatory prefix so it cannot collide with the logo.
- [x] Implement `AuthPageFrame` with the visual pane and a constrained form slot. Use top alignment
  on mobile, centered desktop composition at normal heights, and top alignment/scrolling when the
  viewport is too short for the form plus safe padding.
- [x] Migrate login, registration, and reset-password presentation to `AuthPageFrame` without
  changing their behavior in this task.
- [x] Remove autofocus from auth fields.
- [x] Replace the generic card-shaped lazy-route fallback with a restrained, accessible unframed
  loading indicator that does not look like a broken auth form.
- [x] Test the shell semantics, alternate action behavior, frame variants, and shared-page usage.

### Task 3: Rebuild The Visual Pane Around Real Media Hierarchy

**Files:**
- Create: `frontend/src/assets/img/auth-cinema-backdrop.webp`
- Modify: `frontend/src/features/identity/components/AuthVisualPane.tsx`
- Create: `frontend/src/features/identity/components/AuthVisualPane.test.tsx`

- [x] Generate an original cinematic poster-wall/backdrop bitmap for the app. Avoid copyrighted
  logos or title text, optimize it as WebP, and keep the checked-in asset at a reasonable web size.
- [x] Render the bitmap as full-bleed media with stable dimensions, `object-fit: cover`, an empty alt
  value, and a dark overlay that preserves text contrast across crops.
- [x] Make the pane fill the frame height and center a max-width inner stack containing title,
  subtitle, and feature rows with deliberate spacing.
- [x] Replace fabricated or unsupported claims such as `Join thousands` and `Replies to your
  comments` with concise copy about watchlists, ratings, and catalog discovery that exists today.
- [x] Use theme semantics for foreground, overlay, and fallback colors; remove the local poster color
  palette and synthetic title tiles.
- [x] Test both login/signup copy variants and ensure decorative media is ignored by assistive
  technology.

### Task 4: Improve Login, Social, And Passkey Feedback

**Files:**
- Create: `frontend/src/features/identity/model/authFeedback.ts`
- Create: `frontend/src/features/identity/model/authFeedback.test.ts`
- Modify: `frontend/src/features/identity/pages/LoginPage.tsx`
- Modify: `frontend/src/features/identity/pages/LoginPage.test.tsx`
- Modify: `frontend/src/features/identity/components/PasskeyLoginButton.tsx`
- Create: `frontend/src/features/identity/components/PasskeyLoginButton.test.tsx`
- Modify: `frontend/src/features/identity/components/SocialLoginButtons.tsx`

- [x] Add tested mappings for invalid credentials (`401`), rate limiting (`429`), and generic
  network/server failures without exposing whether an account exists.
- [x] Replace transient password-login error snackbars with an inline MUI `Alert` associated with the
  form; clear stale feedback when a new attempt begins.
- [x] Read `?error=social` and show a neutral social-login failure alert while keeping provider
  details private.
- [x] Show a spinner and `Signing in...` while password login is pending.
- [x] Show `Waiting for passkey...` while the browser ceremony is active. Treat user cancellation or
  no available credential as a neutral cancellation message rather than a red application failure;
  keep transport/server failures as errors.
- [x] Disable duplicate provider starts after a social button is selected and expose a short
  redirecting state until navigation begins.
- [x] Preserve keyboard focus visibility, button accessible names, password-manager autocomplete,
  and password visibility controls.

### Task 5: Improve Registration Availability And Completion Feedback

**Files:**
- Modify: `src/main/java/com/thecodinglab/imdbclone/identity/internal/IdentityAccess.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/identity/AuthenticationControllerTest.java`
- Modify: `src/test/java/com/thecodinglab/imdbclone/identity/AuthenticationTokenFlowTest.java`
- Modify: `frontend/src/features/identity/hooks/useAvailability.test.tsx`
- Modify: `frontend/src/features/identity/pages/RegistrationPage.tsx`
- Modify: `frontend/src/features/identity/pages/RegistrationPage.test.tsx`
- Modify: `frontend/src/features/identity/api/identityMutations.test.ts`
- Modify: `frontend/src/i18n.ts`

- [x] Make registration `MessageResponse` values concise and user-facing for both enabled email
  verification (`Check your email to activate your account`) and disabled verification (`Account
  created. You can sign in now`). Do not expose token or delivery internals.
- [x] Assert the verification-disabled message in `AuthenticationControllerTest` and the
  verification-enabled message in `AuthenticationTokenFlowTest`.
- [x] Pass the successful response through navigation state and render it as a persistent success
  alert on login instead of a snackbar that disappears during redirect.
- [x] Add `Creating account...` and a spinner while submission is pending.
- [x] Keep debounced username/email availability checks, but show a non-blocking helper when a check
  fails and explain that final availability is still validated on submission.
- [x] Continue mapping backend username/email conflicts back to their fields and focus the first
  invalid field after a rejected submission.
- [x] Remove the placeholder Terms/Privacy sentence and links.
- [x] Add tests for availability failure, taken values, backend field errors, pending state, and both
  registration-success messages.

### Task 6: Add Responsive Browser Coverage

**Files:**
- Create: `frontend/e2e/auth-experience.spec.ts`

- [x] Mock `/api/auth/me` as anonymous and mock login/availability/registration responses so the
  tests do not require credentials, OAuth providers, a passkey, or live backend state.
- [x] Assert login and registration load after session bootstrap without horizontal overflow.
- [x] Assert the visual pane is present and fills the auth main region on desktop, and is absent on
  mobile.
- [x] Assert narrow headers retain both the brand link and alternate auth action without overlap.
- [x] Exercise blank-field errors, the complete password checklist, mismatch feedback, availability
  failure, invalid credentials, rate limiting, social failure, pending labels, and registration
  success redirect.
- [x] Capture review screenshots at `1440x1000`, `1366x768`, `390x844`, and `320x700`; inspect text,
  focus, controls, and scroll reachability rather than relying only on DOM assertions.
- [x] Verify keyboard navigation order across header action, fields, password visibility, primary
  action, passkey, and social providers.

### Task 7: Final Verification And Roadmap Update

- [x] Run targeted backend tests:

  ```bash
  ./gradlew test --tests CredentialValidationTest --tests AuthenticationControllerTest --tests AuthenticationTokenFlowTest
  ```

- [x] Run focused frontend tests:

  ```bash
  cd frontend
  yarn test -- registrationValidation.test.ts authFeedback.test.ts AuthLayout.test.tsx AuthPageFrame.test.tsx AuthVisualPane.test.tsx LoginPage.test.tsx RegistrationPage.test.tsx PasskeyLoginButton.test.tsx useAvailability.test.tsx
  ```

- [x] Run full frontend checks:

  ```bash
  cd frontend
  yarn run lint
  yarn test
  yarn build
  yarn playwright test e2e/auth-experience.spec.ts --project=desktop-chromium
  yarn playwright test e2e/auth-experience.spec.ts --project=mobile-chromium
  ```

- [x] Run `./gradlew test` because shared credential validation affects registration and password
  reset contracts.
- [x] Run `git diff --check` and confirm no generated screenshot, local credential, or decrypted
  secret files are staged.
- [x] Update `docs/product-roadmap.md` or release notes after the slice is implemented, keeping only
  unfinished product direction in the roadmap.

## Suggested Commit Boundaries

1. `fix(identity): align credential validation`
2. `feat(identity): polish responsive auth layout`
3. `feat(identity): improve auth flow feedback`
4. `test(identity): cover responsive auth flows`

Each commit should pass its focused tests. The final state must pass the full verification listed
above before deployment.
