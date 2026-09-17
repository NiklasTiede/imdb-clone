import authCinemaBackdrop from "../../assets/img/auth-cinema-backdrop.webp";
import type { AppThemeDefinition } from "../types";

// The original navy and gold cinema theme.
const color = {
  navy: "#0b111d",
  surface: "#101827",
  surfaceElevated: "#172132",
  surfaceInset: "#070b12",
  gold: "#f5c518",
  ink: "#101010",
  amber: "#ffb700",
  sky: "#7ab8ff",
} as const;

export const classicTheme: AppThemeDefinition = {
  id: "classic",
  label: "Classic",
  tokens: {
    surface: {
      page: color.navy,
      card: color.surface,
      raised: color.surfaceElevated,
      inset: color.surfaceInset,
    },
    text: { primary: "#ffffff", secondary: "rgba(255,255,255,0.75)" },
    line: {
      divider: "rgba(255,255,255,0.08)",
      control: "rgba(255,255,255,0.36)",
    },
    accent: { main: color.gold, contrastText: color.ink, core: "#fff3c4" },
    star: color.amber,
    data: {
      user: color.sky,
      comparison: color.amber,
      track: "rgba(255,255,255,0.12)",
    },
    status: { signal: "#f87171", success: "#66bb6a" },
    scrim: color.surfaceInset,
    voice: {
      user: { core: "#ffffff", mid: "#8ec8ff", rim: "#1d5ba6" },
      agent: { core: "#fff6e0", mid: color.amber, rim: "#a86500" },
      flare: color.sky,
    },
  },
  fonts: {
    display: '"Manrope Variable", sans-serif',
    ui: '"Manrope Variable", sans-serif',
  },
  brand: {
    markSrc: "/brand/classic/mark.svg",
    faviconSrc: "/brand/classic/favicon.svg",
    authBackdropSrc: authCinemaBackdrop,
    wordmark: {
      fontFamily: '"Manrope Variable", sans-serif',
      fontSize: 15,
      fontWeight: 800,
      letterSpacing: "0",
      uppercase: false,
    },
    tagline: {
      text: "Discover, rate, remember",
      align: "start",
      color: "rgba(255,255,255,0.75)",
      fontSize: 11,
      letterSpacing: "0",
    },
  },
  effects: {
    posterHover: {
      outlineColor: "rgba(255,255,255,0.96)",
      outlineOffset: 2,
      transform: "scale(1.03)",
    },
    posterFocus: {},
    cardHover: {
      borderColor: "rgba(245,197,24,0.5)",
      transform: "translateY(-2px)",
    },
  },
  browserThemeColor: "#0e1626",
};
