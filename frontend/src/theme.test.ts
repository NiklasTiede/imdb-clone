import { describe, expect, test } from "vitest";
import { appTheme, movieColors } from "./theme";

describe("movie theme", () => {
  test("exposes semantic cinema color tokens", () => {
    expect(movieColors.backdrop).toBe("#0b111d");
    expect(movieColors.brand).toBe("#f5c518");
    expect(movieColors.brandInk).toBe("#101010");
    expect(movieColors.info).toBe("#7ab8ff");
    expect(movieColors.rating).toBe("#ffb700");
    expect(movieColors.surface).toBe("#101827");
    expect(movieColors.surfaceElevated).toBe("#172132");
    expect(movieColors.surfaceInset).toBe("#070b12");
  });

  test("uses the brand color for primary actions", () => {
    expect(appTheme.palette.primary.main).toBe(movieColors.brand);
    expect(appTheme.palette.primary.contrastText).toBe(movieColors.brandInk);
    expect(appTheme.palette.background.default).toBe(movieColors.backdrop);
    expect(appTheme.palette.background.paper).toBe(movieColors.surface);
  });
});
