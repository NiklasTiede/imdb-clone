# Initial site verification

Verified 2026-09-17. Both diagrams are `architecture` diagrams generated from
the specifications in `diagrams/`. Their specification/artifact SHA-256 values,
generator revision and review results are recorded in [receipts.json](diagrams/receipts.json).

## Results

- Archify `validate` and `deliver`: both passed 9/9 showcase checks with zero
  composition errors and warnings.
- Archify `visual-check`: both passed browser containment and readability checks
  at 1440×900, 1600×1000, 1920×1080 and 2048×1320.
- Perceptual review: light and dark screenshots inspected. An initially clipped
  profile label was moved, followed by a new delivery and browser check.
  Visual correction rounds: application 0; observability 1.
- Playwright: overview and architecture pages checked at widths 375, 768 and
  1440; no horizontal overflow, broken images or page JavaScript exceptions.
- Both diagram viewers also fit the 375px mobile viewport horizontally.
- Application Java-node focus and observability Grafana-node focus open their
  component details. Overview-to-architecture navigation works.
- Both PNG exports downloaded successfully. Clean exports are the public
  thumbnails: application 4170×2400, observability 4170×2610.
- `python3 scripts/build-engineering-site.py`: links, diagram byte identity and
  the explicit 18-file public package validated.
- All 18 public files returned HTTP 200 from the local preview server.
- Sitemap XML parsed successfully. Published HTML contains no local filesystem URLs.
- `actionlint .github/workflows/engineering-pages.yaml`: passed.

## After Dark branding refresh

- Compared the local React app with its After Dark theme and copied its mark,
  favicon, Bebas Neue wordmark font and Manrope body font (with licenses).
- Rechecked overview and architecture pages at widths 375, 768 and 1440:
  no horizontal overflow, broken images or page JavaScript exceptions; both fonts load.
- Desktop and mobile screenshots visually reviewed; logo, lockup and pink accents
  match the app's brand direction. Editorial serif headlines remain intentional.
- Pink text on page/card backgrounds and red wordmark text on the page all exceed
  4.5:1 contrast. Buttons use dark text on pink.
- Rebuilt the 18-file public package and checked all assets return HTTP 200.
  Diagram receipt checks still pass; generated viewers were not modified.

## Boundaries

No backend, frontend application or agent runtime code changed; their test suites
were not run. No Kubernetes, DNS, Grafana or repository Pages settings were changed.
The Pages workflow has been statically checked but not executed on GitHub. Domain,
certificate and public-host checks remain part of the first publication procedure.
No assertion is made about the current live health of the services in the diagrams.
