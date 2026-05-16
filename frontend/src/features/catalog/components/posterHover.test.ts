import { describe, expect, test } from "vitest";
import {
  posterHoverContainerSx,
  posterHoverTargetClassName,
  posterHoverTargetSx,
} from "./posterHover";

describe("posterHover", () => {
  test("scales poster cards and adds a white outline on hover and keyboard focus", () => {
    const hoverSelector = `&:is(:hover, :focus-visible) .${posterHoverTargetClassName}`;

    expect(posterHoverContainerSx.overflow).toBe("visible");
    expect(posterHoverContainerSx[hoverSelector]).toEqual({
      outlineColor: "rgba(255,255,255,0.96)",
      outlineOffset: 2,
      transform: "scale(1.03)",
    });
    expect(posterHoverTargetSx.outline).toBe("2px solid transparent");
    expect(posterHoverTargetSx.transition).toContain("outline-color");
    expect(posterHoverTargetSx.transition).toContain("transform");
  });
});
