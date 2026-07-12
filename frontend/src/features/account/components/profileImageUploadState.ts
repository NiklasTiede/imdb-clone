import type { Crop, PixelCrop } from "react-image-crop";
import { createCenteredSquareCrop } from "./profilePhotoCropper";

type ImageFit = "tall" | "wide";

type PreviewingProfileImage = {
  file: File;
  src: string;
  status: "previewing";
};

type ReadyProfileImage = {
  crop: Crop;
  file: File;
  image: HTMLImageElement;
  imageFit: ImageFit;
  src: string;
  status: "ready";
};

export type ProfileImageUploadState =
  | { status: "idle" }
  | { file: File; status: "reading" }
  | PreviewingProfileImage
  | ReadyProfileImage;

export type ProfileImageUploadAction =
  | { file: File; type: "file-selected" }
  | { file: File; src: string; type: "file-read" }
  | { file: File; type: "file-read-failed" }
  | { image: HTMLImageElement; type: "image-loaded" }
  | { crop: PixelCrop; type: "crop-changed" }
  | { type: "closed" };

export type ProfileImageDialogState =
  | PreviewingProfileImage
  | ReadyProfileImage;

export const initialProfileImageUploadState: ProfileImageUploadState = {
  status: "idle",
};

const assertNever = (value: never): never => {
  throw new Error(`Unexpected profile image upload value: ${String(value)}`);
};

export const profileImageUploadReducer = (
  state: ProfileImageUploadState,
  action: ProfileImageUploadAction,
): ProfileImageUploadState => {
  switch (action.type) {
    case "file-selected":
      return { file: action.file, status: "reading" };
    case "file-read":
      return state.status === "reading" && state.file === action.file
        ? { file: action.file, src: action.src, status: "previewing" }
        : state;
    case "file-read-failed":
      return state.status === "reading" && state.file === action.file
        ? initialProfileImageUploadState
        : state;
    case "image-loaded": {
      if (state.status !== "previewing") {
        return state;
      }
      const { image } = action;
      return {
        ...state,
        crop: createCenteredSquareCrop(image.width, image.height),
        image,
        imageFit: image.naturalWidth >= image.naturalHeight ? "wide" : "tall",
        status: "ready",
      };
    }
    case "crop-changed":
      return state.status === "ready" ? { ...state, crop: action.crop } : state;
    case "closed":
      return initialProfileImageUploadState;
    default:
      return assertNever(action);
  }
};

export const getProfileImageDialogState = (
  state: ProfileImageUploadState,
): ProfileImageDialogState | null => {
  switch (state.status) {
    case "idle":
    case "reading":
      return null;
    case "previewing":
    case "ready":
      return state;
    default:
      return assertNever(state);
  }
};
