import type { Crop, PixelCrop } from "react-image-crop";
import { i18n } from "../../../i18n";

export const PROFILE_PHOTO_UPLOAD_SIZE = 800;
export const PROFILE_PHOTO_CROP_STAGE_SIZE = 520;

type CanvasCropInput = {
  crop: Pick<PixelCrop, "x" | "y" | "width" | "height">;
  renderedWidth: number;
  renderedHeight: number;
  naturalWidth: number;
  naturalHeight: number;
};

type CanvasCrop = {
  sourceX: number;
  sourceY: number;
  sourceWidth: number;
  sourceHeight: number;
  outputWidth: number;
  outputHeight: number;
};

type ProblemDetailResponse = {
  response?: {
    data?: {
      detail?: unknown;
      message?: unknown;
    };
  };
};

export function createCenteredSquareCrop(width: number, height: number): Crop {
  const size = Math.min(width, height);

  return {
    unit: "px",
    x: Math.round((width - size) / 2),
    y: Math.round((height - size) / 2),
    width: Math.round(size),
    height: Math.round(size),
  };
}

export function buildCanvasCrop({
  crop,
  renderedWidth,
  renderedHeight,
  naturalWidth,
  naturalHeight,
}: CanvasCropInput): CanvasCrop {
  const scaleX = naturalWidth / renderedWidth;
  const scaleY = naturalHeight / renderedHeight;

  return {
    sourceX: Math.round(crop.x * scaleX),
    sourceY: Math.round(crop.y * scaleY),
    sourceWidth: Math.round(crop.width * scaleX),
    sourceHeight: Math.round(crop.height * scaleY),
    outputWidth: PROFILE_PHOTO_UPLOAD_SIZE,
    outputHeight: PROFILE_PHOTO_UPLOAD_SIZE,
  };
}

export function getProfilePhotoUploadErrorMessage(error: unknown): string {
  const data = (error as ProblemDetailResponse).response?.data;

  if (typeof data?.detail === "string" && data.detail.trim()) {
    return data.detail;
  }
  if (typeof data?.message === "string" && data.message.trim()) {
    return data.message;
  }

  return i18n.accountSettings.loadingError("upload profile photo");
}
