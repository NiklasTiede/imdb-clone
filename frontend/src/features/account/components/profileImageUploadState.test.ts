import { describe, expect, test } from "vitest";
import {
  getProfileImageDialogState,
  initialProfileImageUploadState,
  profileImageUploadReducer,
} from "./profileImageUploadState";

const createImage = (
  dimensions: Pick<
    HTMLImageElement,
    "height" | "naturalHeight" | "naturalWidth" | "width"
  >,
): HTMLImageElement => dimensions as HTMLImageElement;

describe("profileImageUploadState", () => {
  test("keeps the latest file when reads finish out of order", () => {
    const firstFile = new File(["first"], "first.png");
    const latestFile = new File(["latest"], "latest.png");
    const readingFirst = profileImageUploadReducer(
      initialProfileImageUploadState,
      { file: firstFile, type: "file-selected" },
    );
    const readingLatest = profileImageUploadReducer(readingFirst, {
      file: latestFile,
      type: "file-selected",
    });

    const staleCompletion = profileImageUploadReducer(readingLatest, {
      file: firstFile,
      src: "data:first",
      type: "file-read",
    });
    expect(staleCompletion).toBe(readingLatest);

    expect(
      profileImageUploadReducer(staleCompletion, {
        file: latestFile,
        src: "data:latest",
        type: "file-read",
      }),
    ).toEqual({
      file: latestFile,
      src: "data:latest",
      status: "previewing",
    });
  });

  test("makes a preview upload-ready only after the image loads", () => {
    const file = new File(["image"], "profile.png");
    const preview = {
      file,
      src: "data:profile",
      status: "previewing" as const,
    };
    const image = createImage({
      height: 300,
      naturalHeight: 900,
      naturalWidth: 1200,
      width: 400,
    });

    expect(getProfileImageDialogState(preview)).toBe(preview);
    expect(
      profileImageUploadReducer(preview, {
        image,
        type: "image-loaded",
      }),
    ).toEqual({
      crop: { height: 300, unit: "px", width: 300, x: 50, y: 0 },
      file,
      image,
      imageFit: "wide",
      src: "data:profile",
      status: "ready",
    });
  });

  test("closes every active upload state back to idle", () => {
    const file = new File(["image"], "profile.png");
    const reading = { file, status: "reading" as const };

    expect(getProfileImageDialogState(reading)).toBeNull();
    expect(profileImageUploadReducer(reading, { type: "closed" })).toEqual(
      initialProfileImageUploadState,
    );
  });
});
