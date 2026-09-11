import { useEffect, useRef } from "react";
import { alpha } from "@mui/material/styles";
import { movieColors } from "../../../theme";

export type OrbState =
  | "ready"
  | "listening"
  | "thinking"
  | "speaking"
  | "muted";

/** Standard lens stops, so the live readout lands on values a lens really has. */
export const STOPS = [1.4, 2, 2.8, 4, 5.6, 8, 11, 16, 22];
/** Aperture-to-f-number constant: f is inversely proportional to the opening. */
const F_CONSTANT = 0.868;
const BLADES = 6;
/** Blade linkage, in units of the housing radius. Each blade is a rigid leaf
 *  hinged just outside the housing: PIVOT is where it is hinged, LINK the arm
 *  from that hinge to the centre of its leading-edge arc, and EDGE that arc's
 *  radius. Opening swings the arc centre around the hinge, so the leaves sweep
 *  tangentially across each other instead of sliding in and out radially. */
const PIVOT = 1.06;
const LINK = 0.62;
const EDGE = 1.17;
const HISTORY = 128;
/** One history slot per 1/32 s, so the ring holds the last four seconds. */
const SLOT_SECONDS = 1 / 32;

const fNumber = (opening: number) =>
  Math.min(22, Math.max(1.4, F_CONSTANT / Math.max(opening, 0.0001)));

const nearestStop = (value: number) =>
  STOPS.reduce((best, stop) =>
    Math.abs(stop - value) < Math.abs(best - value) ? stop : best,
  );

/** Resting opening per state; listening and speaking add live level on top. */
const REST: Record<OrbState, number> = {
  ready: 0.217,
  listening: 0.31,
  thinking: 0.217,
  speaking: 0.31,
  muted: 0.0395,
};
/** The iris never racks fully open. A wide gate is a large lit area, and past
 *  this point the extra brightness costs more than the extra range is worth:
 *  a third of the housing stays blades, so the mechanism always reads. */
const WIDEST = 0.62;

/** The stop a state settles at with no audio, for labelling the state list. */
export const restingStop = (state: OrbState) =>
  nearestStop(fNumber(REST[state]));

/** The fast visual loop stays inside the canvas; it never updates React state. */
export function VoiceLens({
  state,
  live,
  readLevel,
  readoutRef,
  readAudio,
  closed = false,
}: {
  state: OrbState;
  live: boolean;
  closed?: boolean;
  readLevel: () => number;
  readAudio?: () => { input: number; output: number; playing: boolean };
  /** Optional lens index mark. The loop writes its position and value here
   *  directly, only when the stop changes, so React never re-renders. */
  readoutRef?: { current: HTMLElement | null };
}) {
  const canvas = useRef<HTMLCanvasElement>(null);
  const openingRef = useRef(closed ? 0.005 : REST[state]);
  /** Recorded level per slot, plus who was speaking, survives state switches. */
  const track = useRef({
    level: new Float32Array(HISTORY),
    agent: new Uint8Array(HISTORY),
    head: 0,
    carry: 0,
  });

  useEffect(() => {
    if (typeof ResizeObserver === "undefined") return;
    const node = canvas.current;
    const ctx = node?.getContext("2d");
    if (!node || !ctx) return;
    const motion = window.matchMedia("(prefers-reduced-motion: reduce)");
    const tape = track.current;
    let frame = 0;
    let last = 0;
    let elapsed = 0;
    let level = 0;
    let shownStop = 0;
    let size = 360;

    const resize = () => {
      size = node.clientWidth;
      const ratio = Math.min(window.devicePixelRatio || 1, 2);
      node.width = Math.round(size * ratio);
      node.height = Math.round(size * ratio);
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
      wake();
    };

    const draw = (now: number) => {
      const dt = Math.min((now - (last || now)) / 1000, 0.05);
      last = now;
      if (!motion.matches) elapsed += dt;
      const t = elapsed;
      const audio = readAudio?.();
      const speaking = !closed && (audio?.playing ?? state === "speaking");
      const quiet = closed || (state === "muted" && !speaking);

      // Simulated speech envelope for the states without a real audio source.
      const demo = Math.max(
        0,
        Math.sin(t * 3.1) * Math.sin(t * 7.3) * 0.8 + 0.12,
      );
      const input = audio
        ? audio.input
        : live
          ? readLevel()
          : state === "listening"
            ? demo
            : 0;
      const target = quiet
        ? 0
        : audio
          ? speaking
            ? audio.output
            : input
          : Math.max(input, speaking ? demo : 0);
      level +=
        (target - level) *
        (1 - Math.exp(-dt / (target > level ? 0.035 : 0.18)));

      // Print the level onto the ring at a fixed cadence, independent of fps.
      tape.carry += dt;
      while (tape.carry >= SLOT_SECONDS) {
        tape.carry -= SLOT_SECONDS;
        tape.head = (tape.head + 1) % HISTORY;
        tape.level[tape.head] = quiet ? 0 : level;
        tape.agent[tape.head] = speaking ? 1 : 0;
      }

      // Aperture: the one number every other visual follows.
      const rack =
        state === "thinking"
          ? Math.sin(t * 1.15) * 0.05 + Math.sin(t * 0.37) * 0.035
          : 0;
      // Reduced motion holds the iris at its resting stop; the engraved ring
      // keeps printing the level, so the feedback survives without movement.
      const gain = motion.matches
        ? 0
        : speaking
          ? 0.42
          : state === "listening"
            ? 0.46
            : 0;
      const targetOpening = Math.min(
        WIDEST,
        Math.max(
          0.005,
          closed
            ? 0.005
            : REST[speaking ? "speaking" : state] + rack + level * gain,
        ),
      );
      openingRef.current +=
        (targetOpening - openingRef.current) *
        (motion.matches ? 1 : 1 - Math.exp(-dt / 0.1));
      const opening = openingRef.current;
      const stop = nearestStop(fNumber(opening));
      if (readoutRef?.current && stop !== shownStop) {
        shownStop = stop;
        readoutRef.current.dataset.stop = String(stop);
        readoutRef.current.style.setProperty(
          "--stop-index",
          String(STOPS.indexOf(stop)),
        );
      }

      const cx = size / 2;
      const cy = size / 2;
      // Below roughly a hundred pixels the engraving and the blade edges stop
      // resolving, so the lens gives the ring less room, thins its detail and
      // keeps only what still reads: the opening, the light and a coarse ring.
      const compact = size < 96;
      const barrel = size * (compact ? 0.38 : 0.3);
      const ringRadius = barrel * 1.09;
      const gate = opening * barrel;
      // A whole-assembly drift, as if the ring were never quite at rest.
      const drift = motion.matches || closed ? 0 : Math.sin(t * 0.23) * 0.04;

      // Identity colour: blue is the user, gold is the Concierge. It labels the
      // halo, the housing and the engraved ring, and never changes meaning.
      const warm = speaking ? movieColors.brand : movieColors.info;
      // The gate is a light source, so it is separated the way a cinematographer
      // separates two lamps: by colour temperature. The user's voice is daylight
      // arriving at the lens, the reply is tungsten leaving it. Each ramp runs
      // from a near-white core out to a deep rim, which is where the contrast
      // comes from — not from more light, since the area and luminance are the
      // same either way.
      const gate5600 = { core: "#ffffff", mid: "#8ec8ff", rim: "#1d5ba6" };
      const gate3200 = {
        core: "#fff6e0",
        mid: movieColors.gold,
        rim: "#a86500",
      };
      const temperature = speaking ? gate3200 : gate5600;
      const core = temperature.mid;
      const breath = state === "ready" ? Math.sin(t * 0.8) * 0.09 : 0;
      const glow = quiet ? 0.06 : 0.42 + breath + level * 0.6;
      // Opening the iris must not also brighten the source. The gate keeps a
      // near-constant luminance, so total light grows with the opening alone
      // rather than with opening and brightness at once. Loudness reads in the
      // halo, the flare and the ring instead, none of which fill the centre.
      const gateLuma = quiet ? 0.05 : 0.5 + breath + level * 0.12;

      ctx.clearRect(0, 0, size, size);
      ctx.globalCompositeOperation = "source-over";

      // Fade fully before the canvas edge so the glow never exposes its square bounds.
      const halo = ctx.createRadialGradient(
        cx,
        cy,
        barrel * 0.5,
        cx,
        cy,
        size * 0.5,
      );
      halo.addColorStop(0, alpha(warm, 0.16 * glow));
      halo.addColorStop(0.45, alpha(warm, 0.07 * glow));
      halo.addColorStop(1, alpha(warm, 0));
      ctx.fillStyle = halo;
      ctx.fillRect(0, 0, size, size);

      ctx.save();
      ctx.beginPath();
      ctx.arc(cx, cy, barrel, 0, Math.PI * 2);
      ctx.clip();

      // 2. The gate light, painted full width. The blades will mask it to shape.
      ctx.fillStyle = movieColors.surfaceInset;
      ctx.fillRect(cx - barrel, cy - barrel, barrel * 2, barrel * 2);
      const gateLight = ctx.createRadialGradient(
        cx,
        cy,
        0,
        cx,
        cy,
        Math.max(gate * 1.05, 1),
      );
      // Falling off across the whole opening, so a wide gate reads as light
      // down a tube rather than as a flat disc of colour.
      // The core is a hotspot, as a lamp seen head-on always is. It is nearly
      // opaque but covers only the inner fifth, so it costs almost no area.
      gateLight.addColorStop(
        0,
        alpha(temperature.core, Math.min(0.95, gateLuma * 1.5)),
      );
      gateLight.addColorStop(
        0.22,
        alpha(temperature.mid, Math.min(0.95, gateLuma * 1.15)),
      );
      gateLight.addColorStop(0.58, alpha(temperature.mid, gateLuma * 0.8));
      gateLight.addColorStop(0.85, alpha(temperature.rim, gateLuma * 0.58));
      gateLight.addColorStop(1, alpha(temperature.rim, gateLuma * 0.26));
      ctx.fillStyle = gateLight;
      ctx.fillRect(cx - barrel, cy - barrel, barrel * 2, barrel * 2);

      // 3. Blades. Solve the hinge linkage for the opening we want: the arc
      //    centre sits where the circle of possible arm positions around the
      //    hinge meets the circle of centres that produce this gate.
      const hinge = barrel * PIVOT;
      const arm = barrel * LINK;
      const edgeArc = barrel * EDGE;
      const reach = Math.min(
        hinge + arm,
        Math.max(Math.abs(hinge - arm), edgeArc - gate),
      );
      // Along and across the hinge axis, from the standard two-circle solution.
      const along = (reach * reach - arm * arm + hinge * hinge) / (2 * hinge);
      const across = Math.sqrt(Math.max(0, reach * reach - along * along));

      const leaves: { phi: number; x: number; y: number }[] = [];
      for (let blade = 0; blade < BLADES; blade++) {
        const phi = (blade / BLADES) * Math.PI * 2 + drift;
        const cos = Math.cos(phi);
        const sin = Math.sin(phi);
        leaves.push({
          phi,
          // The arc centre, swung off the hinge axis. That offset is the swirl.
          x: cos * along - sin * across,
          y: sin * along + cos * across,
        });
      }

      /** Where a ray from the centre leaves this leaf's edge circle. The centre
       *  always sits inside that circle, so there is exactly one exit. */
      const edgeRadius = (angle: number, leaf: { x: number; y: number }) => {
        const dot = Math.cos(angle) * leaf.x + Math.sin(angle) * leaf.y;
        return Math.min(
          barrel,
          dot +
            Math.sqrt(
              Math.max(0, dot * dot + edgeArc * edgeArc - reach * reach),
            ),
        );
      };

      // The opening is where every leaf agrees: the innermost edge per angle.
      const aperture: { x: number; y: number }[] = [];
      const arc = compact ? 72 : 132;
      for (let i = 0; i < arc; i++) {
        const angle = (i / arc) * Math.PI * 2;
        let r = barrel;
        leaves.forEach((leaf) => {
          r = Math.min(r, edgeRadius(angle, leaf));
        });
        aperture.push({
          x: cx + Math.cos(angle) * r,
          y: cy + Math.sin(angle) * r,
        });
      }

      // Mask the gate light down to the opening, so the leaves can be laid in
      // afterwards without any of them having to cover the whole housing.
      ctx.beginPath();
      ctx.arc(cx, cy, barrel, 0, Math.PI * 2);
      aperture.forEach((point, i) =>
        i === 0 ? ctx.moveTo(point.x, point.y) : ctx.lineTo(point.x, point.y),
      );
      ctx.closePath();
      ctx.fillStyle = "#0a1220";
      ctx.fill("evenodd");

      // Each leaf overlaps only its immediate neighbour, and that neighbour is
      // the one lying on top of it. Clipping to the neighbour's edge circle
      // reproduces that cyclic overlap, which a plain draw order cannot: every
      // leaf then shows the same amount of face, as on a real iris.
      leaves.forEach((leaf, blade) => {
        const over = leaves[(blade + 1) % BLADES];
        if (!over) return;
        ctx.save();
        ctx.beginPath();
        ctx.arc(cx + over.x, cy + over.y, edgeArc, 0, Math.PI * 2);
        ctx.clip();

        ctx.beginPath();
        ctx.arc(cx, cy, barrel, 0, Math.PI * 2);
        ctx.arc(cx + leaf.x, cy + leaf.y, edgeArc, 0, Math.PI * 2, true);
        // Rolled steel lit from the gate: brightest along the leading edge,
        // falling away toward the housing. The leading edge lies opposite this
        // leaf's arc centre, so that direction is where the light comes from.
        const nx = -leaf.x / reach;
        const ny = -leaf.y / reach;
        const face = ctx.createLinearGradient(
          cx + nx * barrel * 0.85,
          cy + ny * barrel * 0.85,
          cx - nx * barrel * 0.95,
          cy - ny * barrel * 0.95,
        );
        // Alternating tones keep the six-fold overlap readable when wide open.
        const lit = blade % 2 === 0;
        face.addColorStop(0, lit ? "#3f444d" : "#31353d");
        face.addColorStop(0.45, lit ? "#1e222a" : "#181b22");
        face.addColorStop(1, lit ? "#0e1116" : "#0a0d11");
        ctx.fillStyle = face;
        ctx.fill();

        // The chamfered leading edge, catching light along its whole sweep.
        ctx.beginPath();
        ctx.arc(cx + leaf.x, cy + leaf.y, edgeArc, 0, Math.PI * 2);
        ctx.strokeStyle = alpha("#c6cad2", compact ? 0.32 : 0.42);
        ctx.lineWidth = compact ? 0.6 : 1;
        ctx.stroke();
        ctx.restore();
      });

      // 4. Light from the gate spilling back onto the blade faces.
      ctx.globalCompositeOperation = "lighter";
      const spill = ctx.createRadialGradient(
        cx,
        cy,
        Math.max(gate * 0.9, 1),
        cx,
        cy,
        Math.max(gate * 3.4, 2),
      );
      spill.addColorStop(0, alpha(core, 0.44 * glow));
      spill.addColorStop(0.5, alpha(warm, 0.16 * glow));
      spill.addColorStop(1, alpha(warm, 0));
      ctx.fillStyle = spill;
      ctx.fillRect(cx - barrel, cy - barrel, barrel * 2, barrel * 2);

      // 5. The aperture's lit edge: the gate light caught on the blade tips.
      ctx.beginPath();
      aperture.forEach((point, i) =>
        i === 0 ? ctx.moveTo(point.x, point.y) : ctx.lineTo(point.x, point.y),
      );
      ctx.closePath();
      ctx.strokeStyle = alpha(core, Math.min(0.26, gateLuma * 0.32));
      ctx.lineWidth = 5 + level * 4;
      ctx.stroke();
      ctx.strokeStyle = alpha("#ffffff", Math.min(0.6, gateLuma * 0.9));
      ctx.lineWidth = 0.8 + level * 0.4;
      ctx.stroke();

      // 6. Housing: an inner shadow that drops the blades into a tube, plus a
      //    soft specular arc so the barrel reads as metal around glass.
      const housing = ctx.createRadialGradient(
        cx,
        cy,
        barrel * 0.72,
        cx,
        cy,
        barrel,
      );
      housing.addColorStop(0, alpha("#000000", 0));
      housing.addColorStop(1, alpha("#000000", 0.72));
      ctx.fillStyle = housing;
      ctx.fillRect(cx - barrel, cy - barrel, barrel * 2, barrel * 2);

      const sheen = ctx.createRadialGradient(
        cx - barrel * 0.55,
        cy - barrel * 0.6,
        0,
        cx - barrel * 0.3,
        cy - barrel * 0.35,
        barrel * 1.1,
      );
      sheen.addColorStop(0, alpha("#ffffff", 0.07));
      sheen.addColorStop(1, alpha("#ffffff", 0));
      ctx.fillStyle = sheen;
      ctx.fillRect(cx - barrel, cy - barrel, barrel * 2, barrel * 2);
      ctx.restore();

      ctx.strokeStyle = alpha("#8f959e", 0.5);
      ctx.lineWidth = compact ? 1 : 1.6;
      ctx.beginPath();
      ctx.arc(cx, cy, barrel - 0.8, 0, Math.PI * 2);
      ctx.stroke();
      ctx.strokeStyle = alpha(warm, 0.22 + glow * 0.2);
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(cx, cy, barrel, 0, Math.PI * 2);
      ctx.stroke();

      // Anamorphic flare. It sits in front of the housing, not inside it, and
      // only fires above a threshold, so it stays an event on loud syllables
      // rather than a permanent bar across the lens. Cool even over a warm
      // source, the way real coated glass throws it.
      const spark = Math.max(0, level - 0.12) / 0.88;
      if (!quiet && spark > 0.004) {
        ctx.globalCompositeOperation = "lighter";
        const reach = barrel * 1.6;
        const bloom = ctx.createLinearGradient(cx - reach, cy, cx + reach, cy);
        bloom.addColorStop(0, alpha(movieColors.communityBlue, 0));
        bloom.addColorStop(0.5, alpha(movieColors.communityBlue, 0.17 * spark));
        bloom.addColorStop(1, alpha(movieColors.communityBlue, 0));
        ctx.fillStyle = bloom;
        const spread = Math.max(2, gate * 0.2);
        ctx.fillRect(cx - reach, cy - spread, reach * 2, spread * 2);

        const filament = ctx.createLinearGradient(
          cx - reach,
          cy,
          cx + reach,
          cy,
        );
        filament.addColorStop(0, alpha("#dfeaff", 0));
        filament.addColorStop(0.5, alpha("#dfeaff", 0.55 * spark));
        filament.addColorStop(1, alpha("#dfeaff", 0));
        ctx.fillStyle = filament;
        ctx.fillRect(cx - reach, cy - 0.9, reach * 2, 1.8);

        // The short vertical companion every anamorphic streak carries. It is
        // one pixel wide, so it only earns its place at the larger sizes.
        if (!compact) {
          const cross = ctx.createLinearGradient(
            cx,
            cy - barrel * 0.55,
            cx,
            cy + barrel * 0.55,
          );
          cross.addColorStop(0, alpha(movieColors.communityBlue, 0));
          cross.addColorStop(
            0.5,
            alpha(movieColors.communityBlue, 0.2 * spark),
          );
          cross.addColorStop(1, alpha(movieColors.communityBlue, 0));
          ctx.fillStyle = cross;
          ctx.fillRect(cx - 0.8, cy - barrel * 0.55, 1.6, barrel * 1.1);
        }
        ctx.globalCompositeOperation = "source-over";
      }

      // 7. The aperture ring. Its knurling is the last four seconds of audio:
      //    newest at twelve o'clock, older trailing clockwise.
      const tickBase = Math.max(1.2, size * 0.005);
      const tickMax = size * (compact ? 0.055 : 0.095);
      const tickStep = compact ? 3 : 1;
      ctx.beginPath();
      ctx.arc(cx, cy, ringRadius, 0, Math.PI * 2);
      ctx.strokeStyle = alpha("#ffffff", 0.07);
      ctx.lineWidth = 1;
      ctx.stroke();
      for (let age = 0; age < HISTORY; age += tickStep) {
        const slot = (tape.head - age + HISTORY) % HISTORY;
        const sample = tape.level[slot] ?? 0;
        const angle = -Math.PI / 2 + (age / HISTORY) * Math.PI * 2;
        const length = tickBase + sample * tickMax;
        const fade = 0.88 - (age / HISTORY) * 0.74;
        const ux = Math.cos(angle);
        const uy = Math.sin(angle);
        ctx.beginPath();
        ctx.moveTo(cx + ux * ringRadius, cy + uy * ringRadius);
        ctx.lineTo(
          cx + ux * (ringRadius + length),
          cy + uy * (ringRadius + length),
        );
        ctx.strokeStyle = alpha(
          tape.agent[slot] ? movieColors.brand : movieColors.info,
          sample > 0.02 ? fade : fade * 0.3,
        );
        ctx.lineWidth = compact ? 1.2 : 1.6;
        ctx.stroke();
      }
      // Index mark: the lens tells you where "now" is.
      ctx.beginPath();
      ctx.moveTo(cx, cy - ringRadius + tickBase * 0.5);
      ctx.lineTo(cx, cy - ringRadius - tickMax - tickBase * 2);
      ctx.strokeStyle = alpha("#ffffff", 0.5);
      ctx.lineWidth = 1;
      ctx.stroke();

      if (audio && speaking && input > 0.02) {
        ctx.beginPath();
        ctx.arc(
          cx,
          cy,
          ringRadius + tickMax + 4,
          -Math.PI / 2,
          -Math.PI / 2 + Math.PI * 2 * Math.min(1, input),
        );
        ctx.strokeStyle = alpha(movieColors.info, 0.8);
        ctx.lineWidth = 2;
        ctx.stroke();
      }
      // Once the iris has closed and the audio history has faded, keep the
      // last frame. An untouched lens must not redraw for every display frame.
      const settled =
        closed &&
        Math.abs(opening - 0.005) < 0.0001 &&
        !tape.level.some((sample) => sample > 0.001);
      if (!settled) frame = requestAnimationFrame(draw);
    };

    const wake = () => {
      cancelAnimationFrame(frame);
      last = 0;
      if (!document.hidden) frame = requestAnimationFrame(draw);
    };
    const observer = new ResizeObserver(resize);
    observer.observe(node);
    resize();
    document.addEventListener("visibilitychange", wake);
    motion.addEventListener("change", wake);
    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
      document.removeEventListener("visibilitychange", wake);
      motion.removeEventListener("change", wake);
    };
  }, [state, live, readLevel, readoutRef, readAudio, closed]);

  return (
    <canvas
      ref={canvas}
      aria-hidden="true"
      style={{ display: "block", width: "100%", aspectRatio: "1" }}
    />
  );
}
