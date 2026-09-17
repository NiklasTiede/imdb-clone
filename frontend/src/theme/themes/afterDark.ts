import authCinemaBackdrop from "../../assets/img/auth-cinema-backdrop.webp";
import type { AppThemeDefinition } from "../types";

// "After Dark" (docs/brand/after-dark/brand-direction.md): a quiet city at night, neon red
// used sparingly for the moments that matter.
const color = {
  midnight: "#0A0C12",
  asphalt1: "#141925",
  asphalt2: "#1C2230",
  asphalt3: "#2A3142",
  asphalt4: "#626C84",
  paper: "#FFFFFF",
  fog: "#BFC5D1",
  neonRed: "#FF2D3F",
  neonCore: "#FFD6DB",
  sodiumAmber: "#FFB547",
  rainBlue: "#7FA7D9",
  signal: "#FF8A4C",
} as const;

export const afterDarkTheme: AppThemeDefinition = {
  id: "after-dark",
  label: "After Dark",
  tokens: {
    surface: {
      page: color.midnight,
      card: color.asphalt1,
      raised: color.asphalt2,
      inset: "#06070B",
    },
    text: { primary: color.paper, secondary: color.fog },
    line: { divider: color.asphalt3, control: color.asphalt4 },
    accent: {
      main: color.neonRed,
      contrastText: color.midnight,
      core: color.neonCore,
    },
    star: color.sodiumAmber,
    data: {
      user: color.neonRed,
      comparison: color.rainBlue,
      track: color.asphalt3,
    },
    status: { signal: color.signal, success: "#5FC98C" },
    scrim: color.midnight,
    voice: {
      user: { core: "#EEF3FB", mid: color.rainBlue, rim: "#2E4A72" },
      agent: { core: color.neonCore, mid: color.neonRed, rim: "#8C0F1D" },
      flare: color.rainBlue,
    },
  },
  fonts: {
    display: '"Manrope Variable", sans-serif',
    ui: '"Manrope Variable", sans-serif',
  },
  brand: {
    markSrc: "/brand/after-dark/mark.svg",
    faviconSrc: "/brand/after-dark/favicon.svg",
    authBackdropSrc: authCinemaBackdrop,
    // Lockup from brand-direction.md section 6: Bebas Neue, "Society" in neon red.
    wordmark: {
      fontFamily: '"Bebas Neue", sans-serif',
      fontSize: 24,
      fontWeight: 400,
      letterSpacing: "0.06em",
      uppercase: true,
      accentWord: "Society",
    },
    tagline: {
      text: "watch together, after dark",
      align: "center",
      color: "#FF6B78",
      fontSize: 11,
      letterSpacing: "0.14em",
    },
  },
  effects: {
    // Subtle lift and a thin red light edge underneath, no scale bounce.
    posterHover: {
      boxShadow: `0 2px 0 0 ${color.neonRed}, 0 14px 26px -16px rgba(255,45,63,0.85)`,
      transform: "translateY(-3px)",
    },
    posterFocus: { outlineColor: color.neonRed, outlineOffset: 2 },
    cardHover: {
      boxShadow: `0 2px 0 0 ${color.neonRed}, 0 18px 32px -22px rgba(255,45,63,0.8)`,
      transform: "translateY(-2px)",
    },
  },
  browserThemeColor: color.midnight,
};
