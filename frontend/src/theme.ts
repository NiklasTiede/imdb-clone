import { createTheme } from "@mui/material/styles";

export const movieColors = {
  backdrop: "#0b111d",
  brand: "#f5c518",
  brandInk: "#101010",
  communityBlue: "#7ab8ff",
  gold: "#ffb700",
  info: "#7ab8ff",
  rating: "#ffb700",
  surface: "#101827",
  surfaceElevated: "#172132",
  surfaceInset: "#070b12",
};

export const tokens = () => ({
  grey: {
    100: "#e0e0e0",
    200: "#c2c2c2",
    300: "#a3a3a3",
    400: "#858585",
    500: "#666666",
    600: "#525252",
    700: "#3d3d3d",
    800: "#292929",
    900: "#141414",
  },
  primary: {
    100: "#cdd0d6",
    200: "#9ba1ad",
    300: "#687284",
    400: "#36435b",
    500: movieColors.backdrop,
    600: "#080d17",
    700: "#060a12",
    800: movieColors.surfaceInset,
    900: "#03050a",
  },
  greenAccent: {
    100: "#dbf5ee",
    200: "#b7ebde",
    300: "#94e2cd",
    400: "#70d8bd",
    500: "#4cceac",
    600: "#3da58a",
    700: "#2e7c67",
    800: "#1e5245",
    900: "#0f2922",
  },
  redAccent: {
    100: "#f8dcdb",
    200: "#f1b9b7",
    300: "#e99592",
    400: "#e2726e",
    500: "#db4f4a",
    600: "#af3f3b",
    700: "#832f2c",
    800: "#58201e",
    900: "#2c100f",
  },
  blueAccent: {
    100: "#e1e2fe",
    200: "#c3c6fd",
    300: "#a4a9fc",
    400: "#868dfb",
    500: "#6870fa",
    600: "#535ac8",
    700: "#3e4396",
    800: "#2a2d64",
    900: "#151632",
  },
});

// mui theme settings
export const themeSettings = () => {
  const colors = tokens();

  return {
    palette: {
      mode: "dark" as const,
      primary: {
        main: movieColors.brand,
        contrastText: movieColors.brandInk,
      },
      secondary: {
        main: movieColors.info,
        contrastText: "#ffffff",
      },
      neutral: {
        dark: colors.grey[900],
        main: colors.grey[500],
        light: colors.grey[100],
      },
      background: {
        default: colors.primary[500],
        paper: movieColors.surface,
      },
      text: {
        primary: "#ffffff",
        secondary: "rgba(255,255,255,0.75)",
      },
      divider: "rgba(255,255,255,0.08)",
    },
    shape: {
      borderRadius: 6,
    },
    typography: {
      fontFamily: ["Source Sans Pro", "sans-serif"].join(","),
      fontSize: 12,
      h1: {
        fontFamily: ["Source Sans Pro", "sans-serif"].join(","),
        fontSize: 40,
      },
      h2: {
        fontFamily: ["Source Sans Pro", "sans-serif"].join(","),
        fontSize: 32,
      },
      h3: {
        fontFamily: ["Source Sans Pro", "sans-serif"].join(","),
        fontSize: 24,
      },
      h4: {
        fontFamily: ["Source Sans Pro", "sans-serif"].join(","),
        fontSize: 20,
      },
      h5: {
        fontFamily: ["Source Sans Pro", "sans-serif"].join(","),
        fontSize: 16,
      },
      h6: {
        fontFamily: ["Source Sans Pro", "sans-serif"].join(","),
        fontSize: 14,
      },
    },
  };
};

export const appTheme = createTheme(themeSettings());
