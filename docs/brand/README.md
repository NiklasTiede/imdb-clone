# Popcorn Society brand

One folder per theme. The theme id matches `frontend/src/theme/themes/<id>.ts` and the generated
web assets in `frontend/public/brand/<id>/`.

| Path | What it is |
| --- | --- |
| `after-dark/brand-direction.md` | After Dark (default theme): decisions, palette, logo usage, motion and accessibility rules. |
| `after-dark/logo/reflections-mark.svg` | Transparent neon popcorn sign. Source for the header mark and the maskable app icon. |
| `after-dark/logo/reflections-icon.svg` | Sign on a midnight rounded square. Source for favicons, app icons and the Open Graph image. |
| `after-dark/logo/reflections-mark-mono.svg` | Single-colour mark for print and light backgrounds (not used by the web app). |
| `after-dark/scenes/reflections-scene.svg` | Animated scene (not used by the web app; kept for README and future signature surfaces). |
| `after-dark/scenes/reflections-login.html` | Standalone sign-in layout reference from the design exploration. |
| `classic/logo/brand-logo.svg` | Classic theme popcorn logo on a navy rounded square. Source for all Classic web assets. |
| `classic/logo/brand-logo-*.png`, `favicon.ico` | Exports delivered with the Classic logo; the app uses the generated set instead. |
| `first-session-prompt.md`, `CLAUDE.md.example` | Handoff material from the brand exploration. |

## Generated web assets

Run `yarn brand:assets` in `frontend/` after changing a logo source. It renders, per theme, into
`frontend/public/brand/<id>/`:

| File | Use |
| --- | --- |
| `mark.svg` | Header mark next to the app name. |
| `favicon.svg`, `favicon.ico` (16, 32, 48), `favicon-96.png` | Browser tab icons. |
| `apple-touch-icon.png` | iOS home screen, 180 × 180, opaque. |
| `app-icon-192.png`, `app-icon-512.png` | PWA icons. |
| `app-icon-maskable-512.png` | PWA maskable icon, artwork inside the central safe circle. |
| `og-image.png` | Social preview, 1200 × 630. |

`index.html` and `public/manifest.json` reference the default theme's folder; switching themes in
development swaps the SVG favicon at runtime. `frontend/src/theme/theme.test.ts` checks that every
theme has the full set.
