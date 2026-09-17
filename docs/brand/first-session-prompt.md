# First Claude Code session: validate the "After Dark" theme

Copy the prompt below into Claude Code, started in the repository root.

---

We are redesigning the Popcorn Society frontend. The chosen direction, logo and rules are in
`docs/brand/after-dark/brand-direction.md`; logo and scene files are in
`docs/brand/after-dark/logo/` and `docs/brand/after-dark/scenes/`. Read that document fully, then read `AGENTS.md` and `docs/design.md`, and
explore how the MUI 9 theme, typography and shared layout primitives are set up in `frontend/src`.

Goal of this session: make the open decisions (typefaces and the validate-marked colours) on the
real app, not on mockups. Do not roll out the redesign yet.

1. Before changing anything, summarise how the current theme is structured (where tokens live,
   how components are styled, any hard-coded colours such as the gold accents and the light-blue
   rating chart) and propose a plan. Wait for my OK.
2. Add the "After Dark" palette as a theme variant next to the current theme, switchable in
   development only (for example a `?theme=` query parameter or a dev toolbar). The current theme
   must stay the default in production.
3. Create typography variants for the candidate pairs from section 5 (Big Shoulders Display or
   Barlow Condensed with Archivo or Hanken Grotesk), self-hosted via `@fontsource`.
4. Apply the component guidance from section 8 only as far as needed to judge the direction on
   these screens: home feed, a movie detail page, `/your-ratings`, and sign-in.
5. Use Playwright to capture screenshots of those screens for each variant at desktop (1440 px)
   and mobile (390 px) widths, and put them side by side so I can compare. Include the test
   strings from section 5.
6. Check contrast of all text and token pairs against WCAG AA and report failures with suggested
   adjusted values.

Constraints: frontend only, no backend or generated API client changes, no VERSION bump, no
deployment. Run `yarn typecheck`, `yarn lint` and `yarn test` before finishing, and keep changes
on a feature branch.
