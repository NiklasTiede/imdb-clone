import { describe, expect, test } from "vitest";
import { appTheme, movieColors } from "./theme";

describe("movie theme", () => {
  test("exposes semantic cinema color tokens", () => {
    expect(movieColors.brand).toBe("#f5c518");
    expect(movieColors.brandInk).toBe("#101010");
    expect(movieColors.info).toBe("#4dabf7");
    expect(movieColors.rating).toBe("#ffb700");
  });

  test("uses the brand color for primary actions", () => {
    expect(appTheme.palette.primary.main).toBe(movieColors.brand);
    expect(appTheme.palette.primary.contrastText).toBe(movieColors.brandInk);
    expect(appTheme.palette.background.paper).toBe(movieColors.surface);
  });
});
