# Inside Popcorn Society

Public engineering documentation for **docs.popcornsociety.app**. This is a static
site hosted separately from the application and its k3s cluster.

## Scope and design

The initial site contains an English overview, an architecture chapter page and
two standalone interactive Archify diagrams. It links to the live application,
GitHub repository and The Coding Lab. Public Grafana dashboards and a collection
of project articles are future additions, explicitly labelled as such. There are
no shared Grafana credentials, embedded operator dashboards or invented metrics.

The presentation follows the app's After Dark branding: midnight/asphalt surfaces,
the red neon popcorn mark, a Bebas Neue wordmark and softer pink reading accents.
Manrope is used for body text; serif headlines retain the notebook's editorial style.
The mark, favicon and self-hosted fonts were copied from the app at `15fa5ec7`;
font licenses are included under `site/assets/fonts/`. Colours and the header lockup
follow `frontend/src/theme/themes/afterDark.ts` at that revision. These are static
copies: refresh them when the app branding changes. Generated diagram palettes
remain independent so their component categories stay distinguishable.
The documentation uses HTML/CSS without an application framework or package install.

## Files

- `site/index.html`: public overview.
- `site/architecture/index.html`: diagram introductions and text explanations.
- `site/architecture/{application,observability}/index.html`: verified generated viewers.
- `site/assets/`: stylesheet, After Dark brand assets, licensed fonts and diagram previews.
- `diagrams/*.json`: editable specifications and portable delivery receipts.
- `../scripts/build-engineering-site.py`: link/integrity validation and public-file packaging.
- `../.github/workflows/engineering-pages.yaml`: PR validation and Pages deployment.

Only the explicitly allowlisted files in `site/` are published. Internal `docs/`,
source evidence specifications, browser captures and local verification receipts
are never included in the deployment artifact.

## Preview and verify

From the repository root, with Python 3.10 or newer:

```sh
python3 scripts/build-engineering-site.py
python3 -m http.server 4371 --bind 127.0.0.1 --directory engineering/dist
```

Open `http://localhost:4371/`. Relative navigation also supports the default
GitHub Pages project path. Canonical and sitemap URLs target the custom domain.
The browser viewers provide theme switching, focus, relationship inspection and
image exports. Their independent tabs leave the documentation page available.

## Maintain the diagrams

Baseline: commit `8fbf5e17`. The diagrams show selected configured relationships,
not a live inventory, health check or claim of high availability. See
[SOURCES.md](SOURCES.md) for the evidence and intentional omissions.

Archify version and source revision are recorded in `diagrams/receipts.json`.
The initial generator is `tt-a1i/archify`, revision
`72c750bb070d95171dbb2244e5b62b1b7da69c12`, skill directory `archify/`.
Generated viewers are committed; CI does not download or run a changing generator.

After editing a specification, use the installed Archify skill, for example:

```sh
ARCHIFY_DIR="${ARCHIFY_DIR:-$HOME/.codex/skills/archify}"
node "$ARCHIFY_DIR/bin/archify.mjs" validate architecture \
  engineering/diagrams/application.json --quality showcase --repo-root . --json
node "$ARCHIFY_DIR/bin/archify.mjs" deliver architecture \
  engineering/diagrams/application.json \
  engineering/site/architecture/application/index.html \
  --quality showcase --repo-root . --json
node "$ARCHIFY_DIR/bin/archify.mjs" visual-check \
  engineering/site/architecture/application/index.html --json
```

Repeat for `observability`. Inspect both themes, interactions and mobile layout;
the automated browser receipt does not substitute for visual review. Refresh the
static previews from the final rendered SVG, then update the portable hashes and
verification results in `diagrams/receipts.json` from the successful delivery.
Never patch generated HTML by hand. The build rejects mismatching specifications
or generated viewers. Recheck source evidence when updating the baseline revision.

The diagram arrows follow calls in the application view and telemetry/result flow
in the observability view. Prometheus initiates scrapes and Grafana initiates queries.

## First publication

These are setup steps, not actions performed by adding this directory:

1. In the repository's **Settings → Pages**, set the source to **GitHub Actions**.
2. Configure **docs.popcornsociety.app** as its custom domain in Pages. The checked-in
   `CNAME` file documents the intended name; a custom Actions workflow does not set
   the domain through that file alone.
3. At Namecheap, add **CNAME**, host **docs**, target **niklastiede.github.io**.
   Keep the existing apex app records. Configure the Pages domain before pointing DNS.
4. Merge this branch into `master`. The `Engineering documentation` workflow validates,
   uploads `engineering/dist` and publishes it. PR runs only validate/package.
   A manual run on `master` can retry the initial publication after setup.
5. Enable **Enforce HTTPS** once GitHub has provisioned the certificate. Check the
   overview, both diagrams, source links, navigation, robots and sitemap on the real host.
6. Add the public documentation link to the main repository README and application
   footer after the address works. Submit this site's sitemap if indexing is desired.

No application version bump, Docker images or Argo CD sync is needed. Later merges
affecting this site automatically publish via the same workflow.

References: [GitHub Pages custom workflows](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages)
and [custom domains](https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/managing-a-custom-domain-for-your-github-pages-site).
