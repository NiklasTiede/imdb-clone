import type { CSSObject } from "@mui/material/styles";

/**
 * Everything a theme is allowed to vary. Themes change colours, typefaces,
 * brand assets and a few interaction effects; they never change layout or
 * component structure. Components read these values through the MUI theme
 * (`sx` palette paths such as "surface.card", or `useTheme()`), never by
 * checking which theme is active.
 */
export type ThemeTokens = {
  surface: {
    /** Page background. */
    page: string;
    /** Cards, header, inputs. */
    card: string;
    /** Raised or hovered surfaces, menus. */
    raised: string;
    /** Inset regions, below the page level. */
    inset: string;
  };
  text: {
    primary: string;
    secondary: string;
  };
  line: {
    /** Decorative dividers and inactive chart tracks. */
    divider: string;
    /** Borders of interactive controls; at least 3:1 against surfaces. */
    control: string;
  };
  accent: {
    /** Primary actions, active states, "your" data. */
    main: string;
    /** Text and icons placed on `main`. */
    contrastText: string;
    /** Highlight on or next to the accent: badge text, focus ring inner edge. */
    core: string;
  };
  /** Rating stars. */
  star: string;
  data: {
    /** The signed-in user's series. */
    user: string;
    /** Neutral or comparison series. */
    comparison: string;
    /** Empty chart tracks. */
    track: string;
  };
  status: {
    /** Errors and destructive warnings; always paired with icon and text. */
    signal: string;
    success: string;
  };
  /** Base colour for functional scrims over images (used with transparency). */
  scrim: string;
  voice: {
    /** Gate light while the user speaks. */
    user: VoiceRamp;
    /** Gate light while the Concierge speaks. */
    agent: VoiceRamp;
    /** Anamorphic flare, independent of who speaks. */
    flare: string;
  };
};

export type VoiceRamp = { core: string; mid: string; rim: string };

export type FontTokens = {
  /** Headings and titles. */
  display: string;
  /** UI and body text. */
  ui: string;
};

export type BrandTokens = {
  /** Header mark, rendered as an image next to the app name. */
  markSrc: string;
  /** SVG favicon; swapped in when the theme changes at runtime. */
  faviconSrc: string;
  /** Background photo on the sign-in and registration pane (shown under the theme scrim). */
  authBackdropSrc: string;
  /** App name next to the mark. */
  wordmark: {
    fontFamily: string;
    fontSize: number;
    fontWeight: number;
    letterSpacing: string;
    uppercase: boolean;
    /** Word of the app name shown in the accent colour, if any. */
    accentWord?: string;
  };
  /** Short line under the app name (hidden on small screens). */
  tagline: {
    text: string;
    /** Alignment under the app name. */
    align: "start" | "center";
    color: string;
    fontSize: number;
    letterSpacing: string;
  };
};

export type EffectTokens = {
  /** Poster cards: applied to the poster on hover and keyboard focus. */
  posterHover: CSSObject;
  /** Poster cards: added on keyboard focus only. */
  posterFocus: CSSObject;
  /** Featured backdrop cards on hover. */
  cardHover: CSSObject;
};

export type AppThemeDefinition = {
  id: string;
  label: string;
  tokens: ThemeTokens;
  fonts: FontTokens;
  brand: BrandTokens;
  effects: EffectTokens;
  /** Browser UI colour (meta theme-color). */
  browserThemeColor: string;
};

declare module "@mui/material/styles" {
  interface Palette {
    surface: ThemeTokens["surface"];
    line: ThemeTokens["line"];
    accent: ThemeTokens["accent"];
    star: string;
    data: ThemeTokens["data"];
    scrim: string;
    voice: ThemeTokens["voice"];
  }

  interface PaletteOptions {
    surface?: ThemeTokens["surface"];
    line?: ThemeTokens["line"];
    accent?: ThemeTokens["accent"];
    star?: string;
    data?: ThemeTokens["data"];
    scrim?: string;
    voice?: ThemeTokens["voice"];
  }

  interface Theme {
    brand: BrandTokens;
    effects: EffectTokens;
    fonts: FontTokens;
  }

  interface ThemeOptions {
    brand?: BrandTokens;
    effects?: EffectTokens;
    fonts?: FontTokens;
  }
}
