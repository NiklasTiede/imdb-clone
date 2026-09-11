# Movie Concierge voice UI

## Integrated application

The normal app is voice-only. `/voice-orb.html` remains a separate development-only study.

- A single 115 px Voice Lens at 93% opacity stays at the bottom center on desktop and mobile.
  Its iris is closed before starting. There is no header voice entry, transcript launcher, or
  visible status/control bar in the normal app. The canvas halo fades to transparent at its edges.
  Once the closed iris and audio history have settled, painting stops until a resize, visibility,
  motion-preference or state change wakes it. Active voice keeps the live animation.
- Clicking the lens requests microphone access and starts the identity-scoped voice session.
  The iris opens during connection and responds to microphone and actual playback levels.
  Clicking again ends the session or cancels a pending connection. Keyboard activation, focus
  indication, a tooltip, and screen-reader status announcements expose the same control.
- After 45 seconds without input the server sends `standby`, closes the session, and the browser
  releases the microphone and closes the iris. No conversation panel or error appears. Clicking
  the closed lens starts again; automatic wake-up from local speech detection is not implemented.
- Connection errors, permission failures, recoverable notices, and the five-minute limit use a
  temporary message. Confirmed personal changes keep their existing receipts and Undo action.
- Open `/?conciergeDebug=1` to enable the conversation companion. This initial-URL setting survives
  route navigation until reload. Reload a URL without the parameter to leave it. This is a UI
  switch for the current user's session, not an authorization boundary or access to other users' logs.
- The debug Module is loaded only for the URL opt-in. Its text controller stays mounted when the
  drawer closes so history survives navigation; normal voice does not initialize text chat.
- Debug mode retains the combined text/voice timeline, capability examples, country preferences,
  source credits, and compact microphone/end/conversation controls. The lens remains visible when
  its drawer is open and centers beside the drawer on desktop. Navigation minimizes the drawer.
- Transcripts update in place per session/turn/speaker. Interrupted replies are labelled because
  their transcript may contain unplayed words. Tool activity and all retrieved cards are collapsed
  under their turn, separate from the spoken answer. Personal ratings are labelled separately from IMDb.
- The combined view retains the latest 200 entries in memory. Nothing is persisted to browser
  storage or a new backend transcript store. Reload, identity changes, and explicit conversation
  reset clear history. This UI does not record audio.
- Debug text input and capability clicks join an active voice session and receive spoken replies.
  They use the same grounding, delegation, budgets, and navigation rules. With voice off they use
  text chat; starting voice does not import that earlier chat's model memory.
- Speech uses xAI `audio.output.speed=1.15`. The persistent dock owns the only live lens renderer;
  amplitude samples update its frame loop without causing a React update per audio sample.

## Conversation companion study

The `/voice-orb.html` study now opens the conversation companion by default. The lens stays
unchanged and appears compactly in its header; the large voice panel is hidden while the companion
is open. Closing the companion brings back the larger voice presentation.

- Six capability cards introduce discovery, trailers, watchlists, ratings, streaming, and navigation.
  Choosing one fills and focuses the draft without submitting it. Personal examples carry a
  sign-in label in guest preview mode.
- The explicit **View example conversation** control loads an illustrative voice/text timeline:
  a film answer with a TMDB source, a confirmed watchlist update, and a movie navigation followed
  by a failed trailer load. Expand **Action details** for sample outcomes, timings, and context.
- Capabilities collapse behind **Explore what I can do** once there is a conversation.
- **Preferences** contains the country and guest/signed-in design switch. Existing example actions
  keep their original country context when the current preference changes.
- The draft and timeline survive closing/reopening the companion, but not reloading the page.
  Submitting a draft only shows it locally with a preview explanation; no agent request is made.
- The input and microphone controls stay at the bottom while the conversation scrolls. This is a
  visual study, not the delivered production conversation store or action acknowledgement contract.

## Aperture study (September 2026)

Open `http://localhost:3000/voice-orb.html` with the frontend development server running.
This standalone study replaces the earlier luminous sphere with a **camera aperture**: six iris
blades over a lit gate, inside a barrel whose ring is engraved with the standard stop scale. It is
drawn on the same 2D canvas, in the existing theme colours, with no new packages. The composition
is exploratory; the production Concierge launcher, drawer, and voice transport are unchanged. The
app's existing personal watchlist/rating and TMDB tools are not connected here.

Why an aperture. The signal has to prove, at a glance, that speech is arriving. The blades give a
large, unmistakable change — the opening moves through roughly a 6:1 range rather than the few
percent a pulsing sphere could carry — and the mechanism belongs to the subject: a movie app
answers in the language of lenses.

The blades are the real linkage, not a decorative polygon. Each is a rigid leaf hinged just outside
the housing, with a fixed arm from that hinge to the centre of its circular leading edge. Setting an
opening solves for where that arm has to point, which swings the edge arc around the hinge, so the
leaves sweep tangentially across one another and the assembly appears to swirl as it opens and
closes. Each leaf is drawn clipped to its neighbour's edge circle, because on a real iris the leaves
overlap cyclically — every one lies under exactly one other — which a plain draw order cannot
reproduce and which is what keeps all six showing the same amount of face.

| Part | What it encodes |
| --- | --- |
| Iris opening | Live level on top of a resting opening per state; blades swing, never scale |
| Gate luminance | Near constant. Opening the iris passes more light, it does not brighten the source |
| Engraved ring | The last four seconds of level, newest at twelve o'clock, older trailing clockwise |
| Ring colour | `info` blue for the user's audio, `brand` gold for the reply, on the same ring |
| Gate light | Who is speaking, as colour temperature: ~5600 K daylight for the user, ~3200 K tungsten for the reply |
| Index mark + ƒ number | The stop the iris is currently at, snapped to real lens stops |
| Anamorphic streak | Level peaks only; cool even over a warm source, as on real glass |

The blades are neutral graphite, not blue steel, which is both what a real iris looks like and what
makes the gate legible. Gold used to carry the speaking state on its own because gold is
complementary to a blue body; blue sat in the same family as the blades and disappeared into them.
Against neutral metal both temperatures read.

Colour temperature is what separates the two voices at the gate, the way a cinematographer separates
two lamps. The user's voice is daylight arriving at the lens: a white core through saturated sky
blue to a deep rim. The reply is tungsten leaving it: a warm-white core through `brand` gold to
amber. Identity is unchanged and still carried by hue — `info` blue and `brand` gold label the halo,
the housing ring and the engraved marks, so the app's existing "blue is the user" convention holds.
Only the gate, which is a light source rather than a label, is graded by temperature. The contrast
lives inside the opening, between core and rim, so it costs no extra lit area.

Brightness is deliberately not tied to the opening. If a wide gate were also a bright one, loud
speech would light a large area twice over and the overlay would glare on a dark page. The gate
holds a near-constant luminance and falls off across its own width, the iris stops at `f/1.4` with a
third of the housing still blades, and loudness is carried instead by the halo, the engraved ring
and the anamorphic flare — none of which fill the centre.

The ƒ number is derived, not decorative: `f = 0.868 / opening`, clamped to `f/1.4 … f/22` and
snapped to the standard scale. That makes the state list a stop scale — ready `ƒ/4`, listening
`ƒ/2.8` opening to `ƒ/1.4`, thinking racking around `ƒ/4`, speaking `ƒ/2.8`, mic off `ƒ/22`.

- State buttons simulate ready, listening, thinking, speaking, and muted appearances.
- **Test my microphone** / **Start voice** explicitly request local microphone access. Only a
  browser analyser consumes the audio: no recording, audible loopback, upload, or provider session.
- In the speaking preview, a live microphone still prints blue marks on the ring next to the gold
  ones. Gold speech motion and the transcript are simulated; there is no speech recognition here.
- Mute disables audio tracks; stop, end, leaving the page, and component cleanup release resources.
  Late permission responses after cancellation also stop their newly returned tracks.
- The lens carries two levels of detail. Under about 96 px the engraving and the blade edges stop
  resolving, so it gives the ring less room, thins the edges, drops the flare's vertical companion
  and draws every third mark. **At real size** on the study page shows it live at dock, compact and
  drawer footprints; the panel above draws it at 340 px, the largest it should ever appear.
- The canvas owns its animation loop without per-frame React updates, and writes the live ƒ value
  to the index mark as a CSS variable and `data-stop` attribute rather than through React state.
  It caps device pixel ratio at 2 and pauses when hidden.
- Reduced motion holds the iris at its state's resting stop and freezes the racking, breathing and
  blade drift. The engraved ring keeps printing level, so the feedback survives without movement.
- Streaming country changes stay in preview state and do not modify the app's saved preference.

Implementation: `frontend/src/features/concierge/preview/orb/`. No new packages or production entry
imports. Review the aperture with real MacBook microphone input before deciding how to integrate
the header entry, existing drawer, transcript, and compact session controls. The dock question is
settled: at 42 px the mechanism is mush, which is why the compact level of detail exists and why the
dock row is drawn at 56 px — treat that as the floor. What is left to judge is whether the
four-second ring earns its space once a live transcript sits beside it.

## Original drawer study

The following documents the original design proposal and its development-only simulation.

The normal application now implements local voice using this design: real capture/playback levels,
Start voice, mute/interrupt/end, and a persistent navigation dock. Activate with `make run-agent-voice`.
This document's `/voice-design.html` entry remains a simulation; test the real feature in the normal
app at `http://localhost:3000`. See `movie-concierge.md` for the current delivered capabilities.

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
