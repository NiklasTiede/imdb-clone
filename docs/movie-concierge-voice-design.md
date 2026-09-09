# Movie Concierge voice UI

Status: Design proposal with an interactive, development-only preview. No browser voice transport
or authenticated watchlist action is implemented by this design study.

## Fit with the current application

The design uses `frontend/src/theme.ts` directly: Manrope Variable, the dark cinematic surfaces,
warm brand gold and the existing informational blue. `ConciergeExperience` already has a fixed
bottom-right launcher; `ConciergeDrawer` is 440 px wide on desktop and full width below `sm`.
User messages already use the blue accent. These are the starting points, not a new visual system.

The preview reuses `AppSurface` and `ConciergeMovieCard`. It shows the checked-in catalog screenshot
as a static background reference. Its Forrest Gump result and navigation/watchlist interactions are
explicitly simulated; no production catalog, account, microphone or provider is contacted.

## Visual decisions

| Element | Design |
| --- | --- |
| Main surface | `surfaceInset`, with existing `surface` footer and `surfaceElevated` result cards |
| Typography | Existing Manrope; 14 px panel title, 19 px voice status, 12–13 px conversation text |
| Voice signal | Quiet circular outline, subtle inset light, seven waveform bars; 116 px desktop, 92 px mobile |
| User audio | `info` blue, matching the existing user-message accent; always labelled as the user |
| Agent audio | `brand` gold, matching the Concierge identity; always labelled as the Concierge |
| Searching | Slow ring movement and a concrete status; no simulated speech waveform |
| Compact dock | 42 px signal inside a dark framed panel; compact app radius, not a second oversized bubble |
| Actions | Existing MUI buttons, 44 px icon targets, visible labels/tooltips and focus indicators |

Gold remains an accent. Movie cards remain visually primary; the voice signal supports status
rather than competing with posters. No new global palette, font, 3D runtime or animation library
is needed. The circular signal is intentional; other surfaces retain the compact radius scale.

## Placement and transitions

1. Keep the existing Concierge entry point. Opening it shows an explicit **Start voice** action and
   a **Prefer typing?** alternative. Merely opening the panel must not acquire the microphone.
2. On desktop, expand into the existing 440 px drawer footprint. On mobile, use the available full
   viewport with a scrolling transcript area and pinned controls above the device safe area.
3. The downward chevron **minimizes** the conversation. It leaves a dock at the bottom right on
   desktop (24 px inset), and nearly full width on mobile (12 px side insets plus safe-area bottom).
   There is currently no mobile bottom-navigation bar to position it above.
4. The dock replaces the idle launcher while a voice session is active. It exposes microphone
   mute/resume, expand and end. While the agent is speaking, its signal is an interrupt button.
5. Explicit movie navigation minimizes the drawer, moves to the movie page and preserves the
   session. Watchlist success refreshes the query, opens the watchlist and shows a receipt/undo.
   Failed writes must never display success. In this preview these actions only change local state.
6. **End voice session** stops microphone tracks, playback, pending work and the provider session.
   It is different from minimizing and from muting. Logout/account switch also ends the session.

Keep the session controller above route pages and outside the drawer's lifecycle, within the
existing identity-scoped Concierge owner. Transcripts/cards can remain visible after voice ends.
The active dock needs a shared layout offset for bottom-right snackbars so receipts never obscure
the end/mute controls. Respect keyboard/safe-area changes and reflow at narrow widths.

## User-visible states

| State | Signal | Copy / control |
| --- | --- | --- |
| Ready | Static gold microphone | “Let's find your next movie” / Start voice |
| Connecting | Restrained progress ring | “Connecting…” / Cancel |
| Listening | Blue waveform from local microphone level | “Listening to you” / Mute |
| Searching | Progress ring | “Searching movies…” / End |
| Speaking | Gold waveform from actual playback level | “Concierge is speaking” / Interrupt |
| Muted | Static muted microphone | “Microphone is off” / Resume |
| Permission failure | Static unavailable microphone | Permission help and text alternative |
| Connection lost | No live waveform | “Connection lost” / Reconnect or text |

The preview covers ready, listening, searching, speaking, muted and permission failure. Connecting
and network loss require the real session lifecycle; never label a disconnected session as listening.
Muting affects microphone input, not already playing speech. Interrupting stops playback/generation
and cannot roll back an already committed watchlist write.

The first release uses English copy and English catalog titles. “Find Forrest Gump and open it”
is the representative command. German command interpretation is deferred.

## Motion and accessibility

The preview uses clearly labelled simulated CSS waveforms. The implemented app must drive the
blue bars from microphone samples and gold bars from the playback stream, not arbitrary animation
or incoming network chunk counts. Only one active speaker needs emphasis; interruptions switch
attention to the user and immediately flush queued assistant playback.

Honour `prefers-reduced-motion`: static signal and speaker labels preserve all information. Audio
levels and partial transcripts must not spam screen-reader live regions. Announce final utterances,
meaningful state transitions and action receipts. Use colour together with labels/icons, never alone.
Preserve Drawer focus handling; minimizing restores focus to the dock, ending to the launcher.

## Reviewing the proposal

From the repository root:

```bash
cd frontend
yarn start
```

Open `http://localhost:3000/voice-design.html`. The top toolbar switches simulated states; try
minimizing, interrupting, opening Forrest Gump, adding it to the preview watchlist, undoing, and
switching to text. The toolbar belongs only to this review page.

The entry is `frontend/voice-design.html`, with feature-owned code under
`frontend/src/features/concierge/preview/conciergeVoicePreview.tsx`. It is not imported by the app
and is not an entry in the normal Vite production build. Production implementation should split
the signal and dock into focused components backed by the real voice-session state.

Checked in the browser at 1440, 390 and 320 px: controls, simulated navigation/undo, text fallback,
no horizontal overflow and reduced-motion behaviour. These are preview checks, not voice/MCP E2E.
