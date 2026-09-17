import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { describe, expect, test } from "vitest";
import { contrastRatio } from "./contrast";
import { createAppTheme } from "./createAppTheme";
import { resolveThemeId, themeStorageKey } from "./themeSelection";
import {
  appThemes,
  brandAssetFiles,
  defaultThemeId,
  getAppTheme,
} from "./themes";

const memoryStorage = (initial: Record<string, string> = {}) => {
  const values = new Map(Object.entries(initial));
  return {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => void values.set(key, value),
  };
};

describe("theme registry", () => {
  test("uses After Dark as the default theme", () => {
    expect(defaultThemeId).toBe("after-dark");
  });

  test("has unique theme ids", () => {
    const ids = appThemes.map((theme) => theme.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  test("maps tokens onto the MUI palette", () => {
    for (const definition of appThemes) {
      const theme = createAppTheme(definition);
      expect(theme.palette.primary.main).toBe(definition.tokens.accent.main);
      expect(theme.palette.primary.contrastText).toBe(
        definition.tokens.accent.contrastText,
      );
      expect(theme.palette.background.default).toBe(
        definition.tokens.surface.page,
      );
      expect(theme.palette.background.paper).toBe(
        definition.tokens.surface.card,
      );
      expect(theme.palette.star).toBe(definition.tokens.star);
      expect(theme.typography.fontFamily).toBe(definition.fonts.ui);
      expect(theme.brand).toEqual(definition.brand);
    }
  });
});

describe.each(appThemes.map((theme) => [theme.label, theme] as const))(
  "%s contrast (WCAG AA)",
  (_label, { tokens }) => {
    const surfaces = [
      tokens.surface.page,
      tokens.surface.card,
      tokens.surface.raised,
    ];

    test("body and secondary text reach 4.5:1 on every surface", () => {
      for (const surface of surfaces) {
        expect(
          contrastRatio(tokens.text.primary, surface),
        ).toBeGreaterThanOrEqual(4.5);
        expect(
          contrastRatio(tokens.text.secondary, surface),
        ).toBeGreaterThanOrEqual(4.5);
      }
    });

    test("text on the accent reaches 4.5:1", () => {
      expect(
        contrastRatio(tokens.accent.contrastText, tokens.accent.main),
      ).toBeGreaterThanOrEqual(4.5);
    });

    test("accent and highlight used as text reach 4.5:1 on page and card", () => {
      for (const surface of [tokens.surface.page, tokens.surface.card]) {
        expect(
          contrastRatio(tokens.accent.main, surface),
        ).toBeGreaterThanOrEqual(4.5);
        expect(
          contrastRatio(tokens.accent.core, surface),
        ).toBeGreaterThanOrEqual(4.5);
      }
    });

    test("control borders, stars and chart series reach 3:1", () => {
      for (const surface of [tokens.surface.page, tokens.surface.card]) {
        expect(
          contrastRatio(tokens.line.control, surface),
        ).toBeGreaterThanOrEqual(3);
        expect(contrastRatio(tokens.star, surface)).toBeGreaterThanOrEqual(3);
        expect(contrastRatio(tokens.data.user, surface)).toBeGreaterThanOrEqual(
          3,
        );
        expect(
          contrastRatio(tokens.data.comparison, surface),
        ).toBeGreaterThanOrEqual(3);
      }
    });
  },
);

describe("brand assets", () => {
  const frontendRoot = path.resolve(__dirname, "../..");
  const publicFile = (url: string) => path.join(frontendRoot, "public", url);

  test.each(appThemes.map((theme) => [theme.id] as const))(
    "%s has the full web asset set in its own folder",
    (themeId) => {
      const theme = getAppTheme(themeId);
      for (const file of brandAssetFiles) {
        expect(existsSync(publicFile(`brand/${themeId}/${file}`)), file).toBe(
          true,
        );
      }
      expect(theme.brand.markSrc).toBe(`/brand/${themeId}/mark.svg`);
      expect(theme.brand.faviconSrc).toBe(`/brand/${themeId}/favicon.svg`);
    },
  );

  test("index.html and the manifest use the default theme's assets", () => {
    const html = readFileSync(path.join(frontendRoot, "index.html"), "utf8");
    const manifest = readFileSync(publicFile("manifest.json"), "utf8");
    const referenced = [
      ...html.matchAll(/href="(\/brand\/[^"]+)"/g),
      ...manifest.matchAll(/"src": "(\/brand\/[^"]+)"/g),
    ].map((match) => match[1]!);

    expect(referenced.length).toBeGreaterThan(0);
    // The root fallback favicon is the default theme's icon.
    expect(readFileSync(publicFile("favicon.ico"))).toEqual(
      readFileSync(publicFile(`brand/${defaultThemeId}/favicon.ico`)),
    );
    for (const url of referenced) {
      expect(url.startsWith(`/brand/${defaultThemeId}/`), url).toBe(true);
      expect(existsSync(publicFile(url)), url).toBe(true);
    }
  });
});

describe("resolveThemeId", () => {
  test("always uses the default theme when overrides are not allowed", () => {
    const storage = memoryStorage({ [themeStorageKey]: "classic" });
    expect(
      resolveThemeId({
        allowOverride: false,
        search: "?theme=classic",
        storage,
      }),
    ).toBe(defaultThemeId);
  });

  test("applies and remembers the query parameter when overrides are allowed", () => {
    const storage = memoryStorage();
    expect(
      resolveThemeId({
        allowOverride: true,
        search: "?theme=classic",
        storage,
      }),
    ).toBe("classic");
    expect(resolveThemeId({ allowOverride: true, search: "", storage })).toBe(
      "classic",
    );
  });

  test("ignores unknown theme ids", () => {
    const storage = memoryStorage({ [themeStorageKey]: "neon-pink" });
    expect(
      resolveThemeId({ allowOverride: true, search: "?theme=comic", storage }),
    ).toBe(defaultThemeId);
    expect(getAppTheme(defaultThemeId).id).toBe(defaultThemeId);
  });
});
