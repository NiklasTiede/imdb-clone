import type { AppThemeDefinition } from "../types";
import { afterDarkTheme } from "./afterDark";
import { classicTheme } from "./classic";

/** All selectable themes. Adding a theme means adding one definition here. */
export const appThemes = [
  afterDarkTheme,
  classicTheme,
] as const satisfies readonly AppThemeDefinition[];

export type AppThemeId = (typeof appThemes)[number]["id"];

export const defaultThemeId: AppThemeId = "after-dark";

/**
 * Brand assets per theme live in public/brand/<theme id>/ and are generated from
 * docs/brand/<theme id>/ with `yarn brand:assets`. index.html and manifest.json
 * reference the default theme's folder.
 */
export const brandAssetFiles = [
  "mark.svg",
  "favicon.svg",
  "favicon.ico",
  "favicon-96.png",
  "apple-touch-icon.png",
  "app-icon-192.png",
  "app-icon-512.png",
  "app-icon-maskable-512.png",
  "og-image.png",
] as const;

export const isAppThemeId = (value: unknown): value is AppThemeId =>
  appThemes.some((theme) => theme.id === value);

export const getAppTheme = (id: AppThemeId): AppThemeDefinition =>
  appThemes.find((theme) => theme.id === id) ?? afterDarkTheme;
