import { createTheme, type Theme } from "@mui/material/styles";
import type { AppThemeDefinition } from "./types";

/** CSS variables for the active theme's typefaces, usable in any `sx` block. */
export const fontFamilies = {
  display: "var(--ps-font-display)",
  ui: "var(--ps-font-ui)",
} as const;

/**
 * Builds the MUI theme for a theme definition. Component overrides here are
 * shared by every theme and only read tokens, so all themes keep the same
 * structure and differ in colour, type and brand alone.
 */
export const createAppTheme = (definition: AppThemeDefinition): Theme => {
  const { tokens, fonts } = definition;
  const heading = (fontSize: number) => ({
    fontFamily: fonts.display,
    fontSize,
  });

  return createTheme({
    brand: definition.brand,
    effects: definition.effects,
    fonts,
    palette: {
      mode: "dark",
      primary: {
        main: tokens.accent.main,
        contrastText: tokens.accent.contrastText,
      },
      secondary: {
        main: tokens.data.comparison,
        contrastText: tokens.surface.page,
      },
      error: { main: tokens.status.signal, contrastText: tokens.surface.page },
      success: {
        main: tokens.status.success,
        contrastText: tokens.surface.page,
      },
      background: {
        default: tokens.surface.page,
        paper: tokens.surface.card,
      },
      text: {
        primary: tokens.text.primary,
        secondary: tokens.text.secondary,
      },
      divider: tokens.line.divider,
      surface: tokens.surface,
      line: tokens.line,
      accent: tokens.accent,
      star: tokens.star,
      data: tokens.data,
      scrim: tokens.scrim,
      voice: tokens.voice,
    },
    shape: {
      borderRadius: 6,
    },
    typography: {
      fontFamily: fonts.ui,
      fontSize: 12,
      h1: heading(40),
      h2: heading(32),
      h3: heading(24),
      h4: heading(20),
      h5: heading(16),
      h6: heading(14),
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          ":root": {
            "--ps-font-display": fonts.display,
            "--ps-font-ui": fonts.ui,
          },
        },
      },
      MuiButtonBase: {
        styleOverrides: {
          root: {
            "&.Mui-focusVisible": {
              outline: `2px solid ${tokens.accent.main}`,
              outlineOffset: 2,
              boxShadow: `0 0 0 1px ${tokens.accent.core} inset`,
            },
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: 4,
            letterSpacing: 0,
            textTransform: "none",
          },
          outlined: {
            borderColor: tokens.line.control,
          },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: { letterSpacing: 0, textTransform: "none" },
        },
      },
      // Floating surfaces (menus, popovers, dialogs, tooltips) use theme surfaces
      // instead of MUI's dark-mode elevation overlay, which tints them grey.
      MuiPaper: {
        styleOverrides: {
          root: { backgroundImage: "none" },
        },
      },
      MuiPopover: {
        styleOverrides: {
          paper: ({ theme }) => ({
            backgroundColor: tokens.surface.raised,
            border: `1px solid ${tokens.line.divider}`,
            borderRadius: theme.shape.borderRadius,
            boxShadow: `0 16px 40px ${theme.alpha(tokens.scrim, 0.55)}`,
          }),
        },
      },
      MuiMenu: {
        styleOverrides: {
          list: { padding: 4 },
        },
      },
      MuiMenuItem: {
        styleOverrides: {
          root: ({ theme }) => ({
            borderRadius: 4,
            minHeight: 40,
            "& .MuiListItemIcon-root": { color: tokens.text.secondary },
            "&:hover": {
              backgroundColor: theme.alpha(tokens.text.primary, 0.06),
            },
            "&.Mui-focusVisible": {
              backgroundColor: theme.alpha(tokens.text.primary, 0.08),
            },
            "&.Mui-selected, &.Mui-selected.Mui-focusVisible": {
              backgroundColor: theme.alpha(tokens.accent.main, 0.12),
              "& .MuiListItemIcon-root": { color: tokens.accent.main },
            },
            "&.Mui-selected:hover": {
              backgroundColor: theme.alpha(tokens.accent.main, 0.18),
            },
          }),
        },
      },
      MuiDialog: {
        styleOverrides: {
          paper: {
            backgroundColor: tokens.surface.raised,
            border: `1px solid ${tokens.line.divider}`,
          },
        },
      },
      MuiTooltip: {
        styleOverrides: {
          tooltip: {
            backgroundColor: tokens.surface.raised,
            border: `1px solid ${tokens.line.divider}`,
            color: tokens.text.primary,
            fontSize: 12,
          },
          arrow: { color: tokens.surface.raised },
        },
      },
      MuiDivider: {
        styleOverrides: {
          root: { borderColor: tokens.line.divider },
        },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          notchedOutline: { borderColor: tokens.line.control },
        },
      },
    },
  });
};
