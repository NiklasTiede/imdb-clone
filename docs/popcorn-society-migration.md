# Popcorn Society: branding and domain migration

## Scope and current state

The product name is **Popcorn Society**. The intended primary URL is
**https://popcornsociety.app**, purchased at Namecheap. The current address remains
`https://imdb-clone.the-coding-lab.com` until the explicit domain cutover.

This preparation changes the app's visible branding and provides an SVG popcorn placeholder,
browser icons, Apple touch icon, and installable app icons. Replace the master at
`frontend/public/brand-logo.svg` and regenerate the PNG exports when the final logo is ready.
The header wordmark remains accessible text, so replacing the symbol does not require layout code.

The owner confirmed that the app has practically no usage and that only test accounts use
passkeys. Existing account, rating, watchlist, media, and agent quota data will be retained.
Test passkeys will be registered again on the new domain; there is no cross-domain passkey bridge.

**The migration components are not referenced by the active Argo CD kustomization.**
They live in `infrastructure/clusters/home/apps/components/popcorn-society` so the existing SOPS
plugin includes them when copying the apps directory. Render-only previews and verification live
in `infrastructure/migrations/popcorn-society`.
Merging this preparation does not activate a new hostname or redirect. `VERSION` remains 1.6.1;
application changes need a new version and the normal CD deployment PR before production uses them.
Do not overwrite the existing 1.6.1 images with rebranding changes.

## Technical project rename

The source rename is separate from the domain cutover:

| Area | New name |
| --- | --- |
| Java packages / Gradle group | `app.popcornsociety` |
| Gradle project | `popcorn-society` |
| Python import package | `popcorn_society_agent` |
| Python distribution | `popcorn-society-agent` |
| Agent CLI commands | `popcorn-agent-eval`, `popcorn-agent-voice-probe` |
| Frontend package | `popcorn-society-frontend` |
| Checked-in OpenAPI document | `frontend/src/client/popcorn-society-backend.yaml` |
| Backend configuration prefix | `popcorn-society.*` / `POPCORN_SOCIETY_*` |
| Agent environment prefix | `POPCORN_SOCIETY_AGENT_*` |
| Frontend environment prefix | `VITE_POPCORN_SOCIETY_*` |

Existing backend `IMDB_CLONE_*` / `imdb-clone.*` settings and agent `IMDB_AGENT_*`
settings remain supported during the infrastructure transition. New settings win within the same
source; normal source precedence still applies. The frontend accepts legacy `VITE_IMDB_CLONE_*`
addresses as fallbacks. Checked-in frontend address defaults retain the legacy keys for now so
existing private `.env.local` files can still override them. New explicit keys take precedence.
Do not commit or mechanically rewrite private environment files.

After pulling the source rename, run `./gradlew clean`, `make agent-sync` and
`cd frontend && yarn build:moviesGen`. The Python lockfile includes the renamed distribution.
IDE Java source roots remain `src/main/java` and `src/test/java`.

### Persisted state

- The session cookie changes from `SESSION` to `POPCORN_SESSION`. Existing test accounts must
  log in again; old serialized Java principals are not loaded. Old JDBC sessions expire naturally.
- Scheduled search projection tasks retain their task name and database table. A narrowly scoped
  serializer reads the two old payload class names and writes the new ones. A binary fixture from
  the pre-rename classes verifies this path. No Flyway history or stored catalog data is rewritten.
- A rollback to a pre-rename backend cannot read newly queued projection payloads. Use a forward
  fix or a rollback build that also understands both payload formats; do not blindly roll back the
  Java image after new tasks have been queued. Domain-component rollback alone is unaffected.
- Passkey credentials still use the current RP ID until the separate domain activation below.

### Operational rename and retained external identities

The full inventory and remaining data/resource migration sequence are in
[`popcorn-society-name-audit.md`](popcorn-society-name-audit.md).

Application metrics, frontend/agent telemetry identities, Grafana display folders, local Compose
services and browser storage keys now use Popcorn Society names. Browser IDs/preferences migrate
on read; dashboards and alerts support both old and new metrics during the rollout. Stable Grafana
UIDs/navigation links remain valid. Existing Grafana viewer accounts are not renamed automatically;
the bootstrap display name applies when creating an account.

CD now publishes `popcorn-society-{backend,frontend,agent}` images. Its tested manifest updater
switches image references, backend Spring/Pyroscope identities and runtime environment keys in the
same deployment PR, after publication. Current base manifests still point at the actual published
1.6.1 images and therefore retain their compatible runtime keys. Do not manually replace those
image repository strings before the new images exist. Ensure Docker Hub repository creation/push
permissions are ready before the first renamed release.

Kubernetes resource names, stateful Helm releases, physical volume names, the existing media bucket
and actual hostnames remain explicit migration dependencies. The configurable frontend bucket
setting is `VITE_POPCORN_SOCIETY_OBJECT_STORAGE_BUCKET`; its default still reads existing media.
GitHub URLs still point at the actual existing repository. Genuine IMDb source identifiers and
historical records remain accurate. No persistent data is moved by this preparation.

The dashboard and alert ConfigMaps/PrometheusRules are active GitOps resources: once merged into
the production branch, Argo CD can reconcile them independently of an application release.
Their dual-name queries deliberately work before and after that release. Domain components remain
inactive until separately enabled. See the audit for the local Compose stop-before-recreate step.

Current screenshot and PlantUML source filenames use `popcorn-society-*`; screenshot content is
unchanged. The unreferenced legacy logo and static data-model PNG were removed. The old flow-schema
SVG remains as a historical artifact referenced by an archived implementation plan.

The existing media hostname continues to serve posters. Operator hostnames (Grafana and Argo CD)
and the legacy backend hostname also remain valid. This migration moves the public application,
not the blog at `the-coding-lab.com`.

## 1. Release the preparation on the current domain

- Review and merge the rebranding changes.
- For the release, increment `VERSION` (suggested next feature release: `1.7.0`), run CD, review and
  merge the generated deployment PR with all three immutable image digests. Do not activate the
  `popcorn` Spring profile before those new backend images exist.
- Keep `VITE_SITE_URL=https://imdb-clone.the-coding-lab.com` for this initial release.
- Verify navigation, password login, OAuth, passkeys, image loading, ratings, watchlists and voice.
- Namespace, persistent resource identities and the legacy production RP ID remain unchanged.
  Active dashboard/alert display and query updates can sync on merge.

## 2. Prepare DNS and provider settings

At the authoritative DNS provider (Namecheap if its nameservers are being used):

- Add an apex `A` record (`@`) pointing to the same current public IPv4 address as the existing app.
  Derive the address from current DNS/operations, not a saved IP in a document.
- Add `www` as a CNAME to `popcornsociety.app`. Both names must resolve before requesting the
  prepared certificate, which covers the apex and `www` together.
- Only publish an `AAAA` record if the ingress is actually reachable over that IPv6 address.
- Keep the old app, backend and object-storage DNS records and existing certificates.
- Use a short DNS TTL during cutover. Keep ports 80/443 reachable for cert-manager's HTTP-01
  challenge and certificate renewal. `.app` requires functioning HTTPS in browsers.
- Keep automatic domain renewal enabled.

Prepare the provider applications without removing working legacy callbacks prematurely:

| Provider | New callback |
| --- | --- |
| Google | `https://popcornsociety.app/login/oauth2/code/google` |
| GitHub | `https://popcornsociety.app/login/oauth2/code/github` |

Update provider display name, homepage and applicable authorized-domain/consent settings.
Verify the exact provider settings and required domain ownership checks in their dashboards.
The application uses same-origin `/oauth2/authorization/google` and `/oauth2/authorization/github`.

Mail links switch through `IdentityProperties` when the `popcorn` profile is activated. Keep the
existing authenticated sender for this migration. A new `@popcornsociety.app` sender requires its
own mailbox/provider configuration and SPF/DKIM/DMARC setup; the domain purchase alone does not
create a mail service.

Verify Search Console ownership for the old app subdomain and new domain, preferably through DNS.
Record currently indexed URLs and a baseline of clicks/impressions before changing redirects.

## 3. Serve and test the new domain

Local previews (read-only; the output contains encrypted Secret manifests, so do not publish it):

```bash
kubectl kustomize infrastructure/migrations/popcorn-society/preview > /tmp/popcorn-preview.yaml
kubectl kustomize infrastructure/migrations/popcorn-society/cutover > /tmp/popcorn-cutover.yaml
make verify-popcorn-migration
```

After DNS/provider preparation and deployment of the compatible application images, activate
serving through a separate reviewed GitOps change. Add this to
`infrastructure/clusters/home/apps/kustomization.yaml`:

```yaml
components:
  - components/popcorn-society/serve
```

The previews and `verify-popcorn-migration` describe the pre-activation state. When adding `serve`
to the active tree, remove its duplicate `components` entry from `preview/kustomization.yaml`.
After activating `redirect`, remove the render-only preview folders, `verify.rb` and the temporary
Make target; use the production render and contract checks for ongoing verification.

Do not point Argo CD at the preview folders or manually apply a full preview containing encrypted
Secrets. Its existing SOPS plugin and apps path stay in use.

The `serve` component:

- Adds apex frontend/backend and Concierge ingresses with a **separate** TLS secret, leaving the
  old certificate intact.
- Enables `prod,popcorn` on the backend. That profile selects the new email-link origin and RP ID.
- Allows both domains in agent trusted hosts and voice origins.
- Adds the new HTTPS/WebSocket origins to CSP while retaining the existing media host.
- Adds `X-Robots-Tag: noindex` to the new public app ingress during verification.

This short verification phase is intended for test accounts: legacy passkey login no longer works
once the new RP ID is enabled. Password/OAuth access provides the recovery path. Log in on the new
host, remove obsolete test passkeys using account settings, and enroll new ones. Browser sessions,
microphone permissions and origin-scoped local preferences do not transfer to the new domain.

Verify:

- Certificate readiness, HTTPS and the temporary `X-Robots-Tag: noindex` header.
- Password login, Google login, GitHub login, logout, registration and password-reset email links.
- Passkey enrollment and login on the new RP ID.
- Posters, search, a direct `/movie?id=123` URL, ratings and watchlists with real existing IDs.
- Both voice providers, microphone permission, WebSocket upgrade and returned movie navigation.
- Backend and agent health; no credential, prompt or email token leakage in logs.

## 4. Activate redirects and indexing

Build and deploy a fresh release with `VITE_SITE_URL=https://popcornsociety.app` in
`frontend/.env.production`. This updates the build-generated robots file, homepage sitemap and
social preview image URL. It is a build-time setting, not a frontend container environment setting.
Until redirects are active, the old site remains accessible for verification.

Once the new-domain checks pass, keep `serve` and add the second component in the active apps tree:

```yaml
components:
  - components/popcorn-society/serve
  - components/popcorn-society/redirect
```

This removes temporary noindex from the new app, redirects old **page** URLs and `www` permanently
to the apex, and preserves paths and query strings. It does not blanket-redirect API requests,
OAuth callbacks, WebAuthn routes or Concierge connections. Those more-specific legacy routes stay
available to avoid redirecting credential-bearing requests across origins. The old root route is
removed before adding its replacement, avoiding competing equal-priority routers.

Do not change movie URL paths during this release. Check the actual redirect response and Location:

```bash
curl -I 'https://imdb-clone.the-coding-lab.com/movie?id=123'
curl -I 'https://www.popcornsociety.app/movie?id=123'
curl -I 'https://popcornsociety.app/'
curl -fsS 'https://popcornsociety.app/robots.txt'
curl -fsS 'https://popcornsociety.app/sitemap.xml'
```

The old page responses should be permanent 301/308 redirects to the same path/query on the apex;
the new homepage must return 200 without `X-Robots-Tag: noindex`. Also inspect a rendered movie page
for a self-referencing canonical that retains its `id`, not the homepage canonical.

In Search Console, submit the new sitemap and use Change of Address for the old **app subdomain**.
Do not submit an address change for the whole blog. Inspect representative URLs after rendering.
Update the README live links, CV, portfolio, uptime checks and external profiles after cutover.
Keep the old app's DNS, valid TLS and redirects for at least one year, preferably indefinitely.

## SEO boundary of this change

- Name, description, favicon, manifest and static social metadata use Popcorn Society.
- Client-side route metadata keeps distinct movie canonicals and drops tracking parameters.
- Login, private/account, token-bearing, unknown and search-result routes are marked noindex.
- Homepage WebSite structured data follows the current host.
- `robots.txt` and `sitemap.xml` are generated during production builds. The initial sitemap lists
  only the homepage; it is **not** a complete movie catalog sitemap.
- This remains a client-rendered SPA. Movie-specific server-rendered titles/social cards, a complete
  catalog sitemap and reliable HTTP 404 handling for missing movies are a separate SEO improvement.
  Verify Google's rendered HTML; do not assume all crawlers execute JavaScript.
- Preview noindex is removed only by the redirect component. Never leave it on after launch.

References: [Google's site-move guide](https://developers.google.com/search/docs/crawling-indexing/site-move-with-url-changes),
[site names](https://developers.google.com/search/docs/appearance/site-names),
[Traefik RedirectRegex](https://doc.traefik.io/traefik/reference/routing-configuration/http/middlewares/redirectregex).

## Rollback

Before permanent redirects, remove the serving component to return to the legacy-only configuration;
new-domain test passkeys will then be unusable until its RP ID is restored. User data stays intact.
After permanent redirects, keep the new domain and HTTPS operational: browsers/search engines may
cache the redirects. Roll back application behavior to a compatible rebranding release while
retaining new-domain configuration, rather than redirecting the new domain back to the old one.
Do not roll back to 1.6.1 while enabling a profile that image does not contain.

## Preparation verification (2026-09-16)

- `cd frontend && yarn run lint && yarn test && yarn build`: passed; 441 tests.
- `cd frontend && yarn playwright test e2e/auth-experience.spec.ts e2e/movie-detail.spec.ts --reporter=line`:
  32 desktop/mobile browser tests passed.
- `./gradlew spotlessApply test`: passed.
- `./gradlew integrationTest --tests '*OpenApiContractIntegrationTest' --tests '*WebAuthnSecurityConfigTest'`:
  5 targeted integration tests passed, including checked-in OpenAPI contract verification.
- `cd frontend && yarn run build:moviesGen`: generated client refreshed successfully.
- `make verify-agent`: formatting, lint, typing, architecture, deterministic tests and evals passed.
- `make verify-kubernetes-schema`: passed for the current production tree; custom resource types
  without downloaded schemas are skipped by the existing validator and covered by semantic checks.
- `make verify-popcorn-migration`: both staged renders and routing/redirect contracts passed.
- The cutover render also passed kubeconform (45 valid resources, 26 skipped custom resources,
  zero invalid resources/errors) and the existing Movie Concierge production contracts.
- Activation was rendered from an isolated copy of the apps folder to verify compatibility with
  the SOPS plugin's copy-and-render layout; no secrets were decrypted for this check.

No DNS/provider changes, live voice calls, production OAuth round trips, release, or deployment
were performed. At this initial branding checkpoint, full backend integration and container builds
were not rerun; the later technical rename verification is recorded below. Live new-domain checks
remain mandatory before removing preview noindex and enabling permanent redirects.

## Technical rename verification (2026-09-16)

- `./gradlew test --tests '*MovieSearchProjectionSerializerTest'` reproduced the
  incompatible legacy payload. The retained pre-rename fixture now passes with the compatibility
  serializer.
- `./gradlew spotlessApply build jacocoTestReport`: passed, including 245 fast tests,
  27 architecture tests and 234 integration tests. The two opt-in live search / local llama.cpp
  tests remain skipped. Login/logout tests also verify that the old cookie name is ignored.
- `make verify-agent`: passed, including strict types, import boundaries, 426 deterministic tests
  and the complete deterministic eval set. New/legacy environment names and precedence are covered.
- `cd frontend && yarn lint`: passed.
- `cd frontend && yarn test --maxWorkers=2 && yarn build`: passed, 441 tests and all three TypeScript
  configurations. The initial high-concurrency run hit two 5-second test timeouts under simultaneous
  backend/agent load; the complete lower-concurrency rerun passed without increasing timeouts.
- `cd frontend && yarn e2e auth-experience.spec.ts movie-detail.spec.ts --workers=1 --reporter=line`:
  32 desktop/mobile tests passed. These use mocked API responses, not production accounts.
- `cd frontend && yarn build:moviesGen`: passed. The checked-in spec was moved without content changes.
- `make verify-kubernetes-schema verify-popcorn-migration`: passed; base schema validation reports
  43 valid resources, 24 skipped custom-resource schemas, zero invalid resources/errors.
- `make docker-build-backend && make container-smoke-backend`: passed for the final source state,
  including the session-cookie correction, numeric non-root user and read-only filesystem.
- `make docker-build-agent && make container-smoke-agent`: passed with the renamed Python package.
- `make docker-build-frontend && make container-smoke-frontend`: passed after transient dependency
  download retries.
- Audited all 508 moved Java files: none missing. Third-party Python dependency versions, existing
  Flyway migrations and IMDb dataset import sources are unchanged. `git diff --check HEAD` passed.

Paid live-model/voice calls, production OAuth, DNS/provider changes, new-domain browser checks,
release and deployment were not performed. The remaining operational identifier migration is
listed above; `VERSION` remains `1.6.1`.
