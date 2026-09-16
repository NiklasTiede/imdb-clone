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

## Deliberately stable identifiers

Keep Java packages, Python imports, configuration prefixes, Docker Hub repositories, Kubernetes
namespaces and resource names, database objects, storage bucket `imdb-clone`, telemetry identifiers,
and browser storage keys unchanged. Renaming these would be separate infrastructure/data migrations.
The GitHub repository and its badges still use the actual existing repository URL.
Historical releases, design records, screenshot filenames and actual IMDb source attribution remain
accurate. Operational dashboards retain their current names until separately updated.

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
- The base GitOps manifests and legacy production RP ID are unchanged by this preparation.

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
were performed. Full backend integration and container builds were not rerun: this preparation
changes branding/configuration and the targeted backend contracts above. Live new-domain checks
remain mandatory before removing preview noindex and enabling permanent redirects.
