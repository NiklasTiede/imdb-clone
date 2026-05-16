import { describe, expect, test } from "vitest";
import {
  PROFILE_PHOTO_UPLOAD_SIZE,
  buildCanvasCrop,
  createCenteredSquareCrop,
  getProfilePhotoUploadErrorMessage,
} from "./profilePhotoCropper";

describe("profilePhotoCropper", () => {
  test("creates a centered square crop from the rendered image bounds", () => {
    expect(createCenteredSquareCrop(640, 360)).toEqual({
      unit: "px",
      x: 140,
      y: 0,
      width: 360,
      height: 360,
    });
  });

  test("maps rendered crop coordinates back to natural image pixels and fixed upload size", () => {
    expect(
      buildCanvasCrop({
        crop: { x: 50, y: 25, width: 200, height: 200 },
        renderedWidth: 400,
        renderedHeight: 300,
        naturalWidth: 1200,
        naturalHeight: 900,
      }),
    ).toEqual({
      sourceX: 150,
      sourceY: 75,
      sourceWidth: 600,
      sourceHeight: 600,
      outputWidth: PROFILE_PHOTO_UPLOAD_SIZE,
      outputHeight: PROFILE_PHOTO_UPLOAD_SIZE,
    });
  });

  test("extracts backend validation messages for upload failures", () => {
    expect(
      getProfilePhotoUploadErrorMessage({
        response: {
          data: {
            detail: "Profile photo cannot be less than [500] in width.",
          },
        },
      }),
    ).toBe("Profile photo cannot be less than [500] in width.");
  });
});
