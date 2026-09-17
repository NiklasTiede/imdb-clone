# Frontend Design System

This document is the design contract for the React frontend. Use it before changing pages,
components, layout, spacing, colors, typography, or interaction states.

## Source Of Truth

The proposed voice interaction is documented in [Movie Concierge voice UI](movie-concierge-voice-design.md),
with a development-only interactive preview using the existing theme and feature components.
The brand direction for the default theme is documented in [After Dark](brand/after-dark/brand-direction.md).

- Themes live in `frontend/src/theme/`:
  - `types.ts` defines `ThemeTokens`, the complete list of what a theme may vary.
  - `themes/afterDark.ts` (default) and `themes/classic.ts` are the theme definitions;
    `themes/index.ts` is the registry.
  - `createAppTheme.ts` turns a definition into the MUI theme, including the component
    overrides shared by every theme.
  - `AppThemeProvider.tsx` selects the active theme and switches it at runtime.
- Theme expectations, including WCAG AA contrast for every registered theme, live in
  `frontend/src/theme/theme.test.ts`.
- Shared layout primitives live in `frontend/src/shared/layout`.
- Shared media primitives live in `frontend/src/shared/media`.
- Feature UI lives under `frontend/src/features/<feature>`.

Do not create a parallel theme, global CSS palette, or one-off design system inside a feature.

## Themes

A theme changes colours, typefaces, brand assets (header mark, sign-in backdrop photo) and a few
interaction effects (poster and card hover). It never changes layout or component structure:
every theme renders the same components.

Rules:

- Components read tokens through the MUI theme: `sx` palette paths such as `"surface.card"`,
  `"accent.main"`, `"star"`, `"data.user"`, `"line.control"`, or `useTheme()` for canvas code.
- Use `accentTint(opacity)` and `scrimTint(opacity)` from `frontend/src/theme` for translucent
  tints and image scrims; use `fontFamilies.display` for headings sized by hand.
- Never hard-code colours (hex, `rgb()`, `rgba()`) outside `frontend/src/theme`, and never branch
  on the active theme id. `frontend/src/frontendArchitecture.test.ts` enforces both.
- Token semantics: `surface.page/card/raised/inset`, `text.primary/secondary`,
  `line.divider/control` (control borders at least 3:1), `accent.main/contrastText/core`,
  `star` (rating stars only), `data.user/comparison/track` (charts), `status.signal/success`
  (errors always with icon and text), `scrim`, `voice.user/agent/flare` (Voice Lens light).
- Sentence case for buttons, pills and labels; no tracked uppercase eyebrows; tabular numerals
  for ratings and stats.

Selection: production uses the default theme (`after-dark`). In development, `?theme=<id>` or
the switcher in the bottom-left corner changes the theme live. To add a theme, add one definition
under `themes/`, register it in `themes/index.ts`, and run the theme tests.

## Layout And Components

Use shared primitives before inventing new containers:

- `PageContent` for page width, horizontal padding, and main content spacing.
- `AppSurface` for app-level panels with optional brand/info accent line.
- `Surface` for framed content panels.
- `PageHeader`, `SectionHeading`, `StatusState`, and `AuthLayout` for common page structure.
- `PosterImage`, `ProfileAvatar`, and image URL helpers for media rendering.

Component rules:

- Keep cards and panels at the existing compact radius scale (`theme.shape.borderRadius` and `borderRadius: 1`).
- Avoid nested cards unless the inner element is a real repeated item or modal surface.
- Use Material UI components and the project theme instead of hand-rolled controls.
- Use `sx` for component-local styling and shared components for reusable layout behavior.
- Keep text readable on dark surfaces; text on the accent uses `accent.contrastText`.
- Keep movie posters/media visually primary on catalog, search, watchlist, ratings, and detail views.
- Preserve responsive behavior for mobile and desktop; text must not overlap or overflow controls.

## Frontend Change Workflow

Before UI work:

1. Inspect `frontend/src/theme/` and nearby shared/feature components.
2. Check whether an existing shared layout/media primitive already solves the layout need.
3. Decide whether a style belongs in the feature component, a shared primitive, or the theme.

During implementation:

- Add theme tokens only when they describe a reusable semantic concept, and give every theme a value.
- Add or update `frontend/src/theme/theme.test.ts` when changing theme tokens or palette behavior.
- Add focused component tests for new visual states when they affect behavior, accessibility, or layout decisions.
- Keep visual changes scoped to the feature or shared primitive being touched.

Verification for design/frontend changes:

```bash
cd frontend
yarn test src/theme
yarn run lint
yarn test
yarn build
```

For route-level visual workflows or responsive behavior, also run relevant Playwright tests:

```bash
cd frontend
yarn playwright test --project=desktop-chromium
yarn playwright test --project=mobile-chromium
```

## Review Checklist

- Does the change use theme tokens instead of hard-coded colours or theme-id checks?
- Are new reusable colors or spacing decisions named semantically?
- Does the UI reuse shared layout/media primitives where appropriate?
- Does the design still work in every registered theme (check with the dev theme switcher)?
- Are accent, star and chart tokens used consistently and sparingly?
- Does text fit on mobile and desktop without overlap?
- Are theme tests updated when theme tokens changed?
- Were lint, tests, build, and any relevant visual checks run or explicitly skipped?
