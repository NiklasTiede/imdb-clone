import { describe, expect, test } from "vitest";
import { createAppTheme } from "../../../theme";
import { afterDarkTheme } from "../../../theme/themes/afterDark";
import { classicTheme } from "../../../theme/themes/classic";
import {
  posterHoverContainerSx,
  posterHoverTargetClassName,
  posterHoverTargetSx,
} from "./posterHover";

const hoverSelector = `&:is(:hover, :focus-visible) .${posterHoverTargetClassName}`;

const resolve = (definition: typeof classicTheme) =>
  posterHoverContainerSx(createAppTheme(definition)) as Record<string, unknown>;

describe("posterHover", () => {
  test("classic scales poster cards and adds a white outline", () => {
    expect(resolve(classicTheme)[hoverSelector]).toEqual({
      outlineColor: "rgba(255,255,255,0.96)",
      outlineOffset: 2,
      transform: "scale(1.03)",
    });
  });

  test("after dark lifts poster cards with a light edge instead of scaling", () => {
    const hover = resolve(afterDarkTheme)[hoverSelector] as Record<
      string,
      string
    >;
    expect(hover.transform).toBe("translateY(-3px)");
    expect(hover.boxShadow).toContain(afterDarkTheme.tokens.accent.main);
  });

  test("keeps a transparent outline ready for the transition", () => {
    expect(posterHoverTargetSx.outline).toBe("2px solid transparent");
    expect(posterHoverTargetSx.transition).toContain("outline-color");
    expect(posterHoverTargetSx.transition).toContain("transform");
  });
});
