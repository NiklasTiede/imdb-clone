# Frontend Design System

This document is the design contract for the React frontend. Use it before changing pages,
components, layout, spacing, colors, typography, or interaction states.

## Source Of Truth

- Theme tokens live in `frontend/src/theme.ts`.
- Theme expectations live in `frontend/src/theme.test.ts`.
- Shared layout primitives live in `frontend/src/shared/layout`.
- Shared media primitives live in `frontend/src/shared/media`.
- Feature UI lives under `frontend/src/features/<feature>`.

Do not create a parallel theme, global CSS palette, or one-off design system inside a feature. Extend
`theme.ts` and the shared primitives when a value or pattern is genuinely reusable.

## Visual Direction

The app should feel like a polished movie catalog: dark cinematic surfaces, warm cinematic brand gold,
clear poster/media hierarchy, restrained density, and practical browsing controls.

Existing semantic colors:

- `movieColors.backdrop` - app background.
- `movieColors.surface` - default panel/card background.
- `movieColors.surfaceElevated` - higher-emphasis surfaces.
- `movieColors.surfaceInset` - inset/darker regions.
- `movieColors.brand` - primary brand/action color.
- `movieColors.brandInk` - text on brand yellow.
- `movieColors.info` / `movieColors.communityBlue` - informational accent.
- `movieColors.rating` / `movieColors.gold` - rating and score accent.

Prefer semantic tokens over raw hex values. Raw colors are acceptable only for local alpha overlays,
one-off media gradients, or values that are immediately promoted to a token when reused.

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
- Keep text readable on dark surfaces and preserve contrast for brand yellow buttons.
- Keep movie posters/media visually primary on catalog, search, watchlist, ratings, and detail views.
- Preserve responsive behavior for mobile and desktop; text must not overlap or overflow controls.

## Frontend Change Workflow

Before UI work:

1. Inspect `frontend/src/theme.ts` and nearby shared/feature components.
2. Check whether an existing shared layout/media primitive already solves the layout need.
3. Decide whether a style belongs in the feature component, a shared primitive, or `theme.ts`.

During implementation:

- Add theme tokens only when they describe a reusable semantic concept.
- Add or update `frontend/src/theme.test.ts` when changing theme tokens or palette behavior.
- Add focused component tests for new visual states when they affect behavior, accessibility, or layout decisions.
- Keep visual changes scoped to the feature or shared primitive being touched.

Verification for design/frontend changes:

```bash
cd frontend
yarn test src/theme.test.ts
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

- Does the change use `theme.ts` tokens instead of introducing an untracked palette?
- Are new reusable colors or spacing decisions named semantically?
- Does the UI reuse shared layout/media primitives where appropriate?
- Does the design still match the dark cinematic movie-catalog direction?
- Are brand/rating/info accents used consistently and sparingly?
- Does text fit on mobile and desktop without overlap?
- Are theme tests updated when theme tokens changed?
- Were lint, tests, build, and any relevant visual checks run or explicitly skipped?
