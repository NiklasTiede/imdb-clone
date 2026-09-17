# Popcorn Society brand direction: "After Dark"

Status: **proposal**. The overall direction and the logo are chosen. Exact palette values and
typefaces are still to be validated in the real app before the redesign is rolled out.

This document is the handoff from a design exploration done in Claude chat. It records the
decisions, the reasoning behind them, and the rules that keep logo, theme and motion consistent.
`docs/design.md` remains the source of truth for implemented tokens; update it once decisions are
final instead of maintaining a second design document.

## 1. Why change

The current UI is solid structurally but has no identity of its own:

- Navy plus gold is a very common look for film and streaming sites and says nothing specific
  about Popcorn Society.
- The UI speaks three colour languages at once: gold (buttons, badges, stars, filter pills,
  SIGN IN), light blue (rating distribution chart) and yellow tracked uppercase labels.
- Template-like details make it feel generic: tracked ALL-CAPS eyebrows ("CURATED DAILY",
  "TASTE SNAPSHOT", "YOUR RATING DISTRIBUTION"), uppercase buttons and pills ("SIGN IN",
  "ALL SCORES", "HIGHEST RATED"), gold left rules next to statistics.
- The product idea of watching and talking about films together is not visible in the design.
- The current popcorn logo does not relate to the rest of the interface.

## 2. Core idea

**The app is a quiet city at night. Neon red is the light that pops up in it.**

The logo "Reflections" (a red neon sign shaped like a striped cinema popcorn carton, hanging on
chains in the rain and mirrored in a puddle) sets the mood. The whole interface is built from the
same world: midnight backgrounds, asphalt surfaces, paper-coloured text, and neon used sparingly
for the moments that matter.

The public brand line is "watch together, after dark" (English, as all brand copy).

## 3. Principles

1. **Neon is scarce.** A neon sign only works because the street around it is dark. Red is
   reserved for the primary action, the active state and brand moments. If everything glows,
   nothing does.
2. **The posters are the colour.** Posters are loud and saturated. The interface stays calm so
   films stand out, not buttons.
3. **Motion has a reason.** Things move when something happens (load, hover, loading, success),
   then come to rest. Nothing loops forever in the reading area.
4. **Together is visible.** Social features (watch parties, friends, shared lists) get first-class
   entry points, not hidden menu items.
5. **Cinema vocabulary, not horror.** The app covers everything from "Ratatouille" to
   "The Conjuring". The mood is a cool night out at the cinema. Horror appears only as an easter
   egg (see section 9).

## 4. Colour

Proposed tokens. Values marked *validate* need checking against real posters and screens.

| Token | Hex | Role |
| --- | --- | --- |
| `midnight` | `#0A0C12` | Page background. Slightly bluer than pure black so it does not read as generic black. |
| `asphalt-1` | `#141925` | Cards, header, inputs. |
| `asphalt-2` | `#1C2230` | Raised or hovered surfaces, menus. |
| `asphalt-3` | `#2A3142` | Borders, dividers, inactive chart tracks. |
| `paper` | `#FFFFFF` | Primary text (was `#E9E6EF`; raised to pure white so text reads as bright as in Classic). |
| `fog` | `#BFC5D1` | Secondary text, metadata (was `#9AA3B5`; brightened to match Classic secondary text, at least 9:1 on every surface). |
| `neon-red` | `#FF2D3F` | Brand accent: primary buttons, active states, focus accents, "your" data. |
| `neon-core` | `#FFD6DB` | Highlights on neon, focus ring inner, badge text on dark. |
| `sodium-amber` | `#FFB547` | Rating stars only (street-lamp light). *validate* |
| `rain-blue` | `#7FA7D9` | Neutral comparison data, e.g. IMDb average in charts. *validate* |
| `signal` | `#FF8A4C` | Errors and destructive warnings, always with icon and text. *validate* |

Rules:

- **Stars stay amber.** Red stars would be confusing; amber fits the night scene as street light.
- **Red is not an error colour.** Because the brand is red, errors must use `signal` plus an icon
  and explicit text, never colour alone.
- **Text on neon red is dark** (`midnight`). White text on `#FF2D3F` fails WCAG AA for normal
  text sizes; `#0A0C12` on `#FF2D3F` passes.
- Replace the light-blue chart: user ratings in `neon-red`, comparison or neutral series in
  `rain-blue`, tracks in `asphalt-3`.
- No gradient washes as decoration. Gradients are allowed only as functional scrims over images
  so text stays legible.
- "Glow" is optional and subtle: at most a soft red shadow on hover or focus of key elements, never
  on body text.

## 5. Typography

Bebas Neue matches the wordmark but is unsuitable as the app's heading face: uppercase only, one
weight, and long titles ("The Lord of the Rings: The Return of the King") become hard to read.

Candidates to compare **in the real app** (all support German umlauts):

| Role | Candidate A | Candidate B |
| --- | --- | --- |
| Display and headings (condensed, sign-like, has lowercase and several weights) | Big Shoulders Display | Barlow Condensed |
| UI and body (clear, tabular figures for ratings and stats) | Archivo | Hanken Grotesk |
| Wordmark only | Bebas Neue (or outlined as paths in logo files) | |

Test strings: "The Lord of the Rings: The Return of the King", "Léon: The Professional",
"Your strongest signal is Drama, especially 2010s films.", ratings "9.3 (3.2M)", German UI text
with umlauts ("Überraschung für heute Abend", "Bewertungen").

Rules:

- Sentence case for buttons, pills and labels ("Sign in", "All scores", "Highest rated").
- Remove tracked ALL-CAPS eyebrows. Where a label carries real information, use a small
  sentence-case label; otherwise drop it.
- Use tabular numerals (`font-variant-numeric: tabular-nums`) for ratings, runtimes and stats.
- Self-host fonts (e.g. `@fontsource/*`) instead of the Google Fonts CDN, for privacy and
  performance.

## 6. Logo: "Reflections"

Anatomy: a red neon tube sign in the shape of a classic striped popcorn carton (three puffs,
tapered carton, three stripes, rim line). Neon is drawn as three stacked strokes: a wide
translucent halo, the red tube, and a thin pale core. In the full scene the sign hangs on chains,
it rains, and the sign is mirrored in a puddle.

Files in `docs/brand/after-dark/logo/`:

| File | Use |
| --- | --- |
| `reflections-mark.svg` | Transparent neon sign. Header (about 40 px), inline brand marks. |
| `reflections-icon.svg` | Sign on a midnight rounded square. Source for favicon, app icons, PWA icons. |
| `reflections-mark-mono.svg` | Single-colour mark for print, merch, light backgrounds. |

Files in `docs/brand/after-dark/scenes/`:

| File | Use |
| --- | --- |
| `reflections-scene.svg` | Self-contained animated scene (CSS inside the SVG, no text, no fonts). Works as `<img>` in the GitHub README. |
| `reflections-login.html` | Standalone reference of the sign-in layout with the animated scene on the left. Not production code. |

Usage rules:

- Below about 64 px use only the mark or icon. Rain, puddle, reflection and people are not
  legible at small sizes.
- Do not recolour the neon. On light backgrounds use the mono mark.
- Keep clear space around the mark of at least 25 % of its width.
- The wordmark "POPCORN SOCIETY" is set in Bebas Neue with "SOCIETY" in `neon-red` in the brand
  lockup. In production logo files convert the wordmark to paths so no font is required.

## 7. Motion

| Moment | Behaviour | Duration |
| --- | --- | --- |
| First load of a session | Header sign switches on with one or two soft flickers, then stays lit. Once per session, not on every route change. | about 1 s |
| Hover or keyboard focus on the logo link | One short flicker. | about 300 ms |
| Real loading state (e.g. "describe a movie" search) | Sign hums: slow, gentle brightness pulse. Stops when results arrive. Can replace a spinner. | while loading |
| Idle | No motion. | |
| Sign-in page, 404, empty watchlist, watch party waiting room | Full animated scene allowed (rain, swing, ripples). | continuous |

Accessibility constraints (WCAG):

- **2.2.2 Pause, Stop, Hide:** anything that starts automatically, lasts longer than 5 seconds and
  sits next to other content needs a way to pause it. Continuous scenes belong only on pages
  where they are the main visual, and should offer a pause control or stop after a while.
- **2.3.1 Three Flashes:** nothing may flash more than three times per second. Flicker is a soft
  dimming (opacity to about 0.3), never a hard on/off, and never repeated rapidly.
- **`prefers-reduced-motion: reduce`:** disable all decorative animation; show the lit sign
  statically.
- Animate only `opacity` and `transform` for performance.

## 8. Component guidance

| Current | Target |
| --- | --- |
| Gold filled primary button "View movie" | `neon-red` fill, `midnight` text, sentence case, square-ish radius (about 4 px). |
| Gold outlined "SIGN IN" | Outlined `neon-red` or paper, sentence case "Sign in". |
| Gold pill badge "ACCLAIMED STANDOUT" | Dark translucent pill, thin `neon-red` border, small red dot, `neon-core` sentence-case text "Acclaimed standout". |
| Gold star rating chips | `sodium-amber` star, paper number, dark translucent chip. |
| Uppercase filter pills, gold active | Sentence case; active pill filled `neon-red` with dark text, inactive outlined `asphalt-3`. |
| Light-blue distribution bars | User series `neon-red`, neutral `rain-blue`, empty tracks `asphalt-3`. |
| Gold left rules next to stats | Remove; use spacing and type hierarchy. |
| Poster card hover | Subtle lift and a thin red light edge underneath, no scale bounce. |
| Section headings with uppercase eyebrow | Display face heading plus a quiet sentence-case subtitle. |

Add visible entry points for togetherness, e.g. "Watch together" as secondary action on the
featured movie and a "Watch parties" navigation item, once the features exist.

## 9. Signature surfaces

- **Sign-in and registration:** split layout. Left: the full animated Reflections scene with
  wordmark and line "watch together, after dark". Right: the forms on `asphalt-1`. On mobile the
  scene becomes a short static or lightly animated band above the form.
- **404 and empty states:** reuse the rainy street without the sign lit, e.g. "Nothing here but
  rain".
- **Horror easter egg:** in the scene, the reflection occasionally shows eyes and a grin. Keep it
  for the Horror genre page or as a rare surprise, controlled by a prop such as `haunted`.

## 10. Voice Lens

The Voice Lens is the app's most prominent animated element. It should adopt the palette (neon
red and neon core for active listening or speaking states, asphalt and fog at rest). Because it
already moves, the header logo must stay still while the lens is active.

## 11. Accessibility checklist

- Text contrast at least 4.5:1 (3:1 for 24 px and larger), including `fog` on `asphalt-2`.
- Visible keyboard focus on every interactive element (red outer ring, `neon-core` inner edge).
- Touch targets at least 44 × 44 px.
- Errors never communicated by colour alone.
- Motion rules from section 7.

## 12. Implementation notes (React 19, MUI 9)

- Implement tokens in the MUI theme (palette, typography, shape, component overrides), ideally
  with MUI CSS variables so the Voice Lens, charts and custom components read the same values.
- Keep one source of truth: theme file plus `docs/design.md`.
- Build the header logo as a React component with inline SVG and CSS keyframes (props such as
  `animated`, `loading`, `size`). An `<img>` SVG cannot react to hover or state.
- Build the scene as a separate lazy-loaded component used only on signature surfaces.
- Keep generated API client code untouched; this is a frontend-only change.

## 13. Assets to generate after the decision

Generated by `yarn brand:assets` (see `docs/brand/README.md`) into `frontend/public/brand/after-dark/`:

- [x] `favicon.ico` (16, 32, 48), `favicon.svg`, `favicon-96.png`
- [x] `apple-touch-icon.png` (180 × 180, no transparency)
- [x] PWA icons 192 and 512, plus a maskable 512 variant with the artwork inside the central safe
  circle (radius 40 % of the icon size)
- [x] Header mark (`mark.svg`, rendered as an image next to the app name)
- [x] Open Graph image 1200 × 630 (icon plus app name in Manrope; a scene-based version is still open)
- [x] README header uses `frontend/public/brand/after-dark/favicon.svg`
- [x] Old `frontend/public/brand-logo.svg` and root icon files removed

Still open: `reflections-mark-mono.svg` has no web use yet (light backgrounds, print).

## 14. Open questions

1. Final display and body typeface (compare candidates in the real app).
2. Final values for `fog`, `sodium-amber`, `rain-blue` and `signal` after contrast checks.
3. Scope of the first rollout: theme and logo only, or also layout changes on home and ratings?

## 15. Exploration history (for context)

Directions explored before choosing Reflections: character-based popcorn mascots, horror cartoon
buckets, an occult gold seal ("Gold Crest"), a candle-lit monster shadow ("Was im Schatten
lauert", slogan "never watch alone."), VHS and pixel styles, a velvet cinema theme
("Velvet Row") and a light horror editorial theme ("Shadow Play"). Reflections was chosen because
it combines the classic popcorn carton, the night-city neon mood the owner likes, a subtle horror
wink, and a strong small-size mark.
