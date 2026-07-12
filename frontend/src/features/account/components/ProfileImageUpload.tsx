import React, { useReducer } from "react";
import {
  Box,
  Button,
  ButtonProps,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from "@mui/material";
import { CloudUpload as CloudUploadIcon } from "@mui/icons-material";
import ReactCrop, { type Crop, type PixelCrop } from "react-image-crop";
import "react-image-crop/dist/ReactCrop.css";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { mediaMutationKeys, storeUserProfilePhoto } from "../../media";
import { useSnackbar } from "notistack";
import {
  buildCanvasCrop,
  getProfilePhotoUploadErrorMessage,
  PROFILE_PHOTO_CROP_STAGE_SIZE,
} from "./profilePhotoCropper";
import {
  getProfileImageDialogState,
  initialProfileImageUploadState,
  profileImageUploadReducer,
} from "./profileImageUploadState";

type ProfileImageUploadProps = {
  buttonVariant?: ButtonProps["variant"];
};

const ProfileImageUpload: React.FC<ProfileImageUploadProps> = ({
  buttonVariant = "contained",
}) => {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const [uploadState, dispatch] = useReducer(
    profileImageUploadReducer,
    initialProfileImageUploadState,
  );
  const dialogState = getProfileImageDialogState(uploadState);
  const uploadProfilePhoto = useMutation({
    mutationFn: storeUserProfilePhoto,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: mediaMutationKeys.currentProfile,
      });
      dispatch({ type: "closed" });
    },
    onError: (error) => {
      enqueueSnackbar(getProfilePhotoUploadErrorMessage(error), {
        variant: "error",
      });
    },
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.currentTarget.files?.item(0);

    if (selectedFile) {
      dispatch({ file: selectedFile, type: "file-selected" });
      const reader = new FileReader();
      reader.addEventListener("load", () => {
        if (typeof reader.result === "string") {
          dispatch({
            file: selectedFile,
            src: reader.result,
            type: "file-read",
          });
        } else {
          dispatch({ file: selectedFile, type: "file-read-failed" });
        }
      });
      reader.addEventListener("error", () => {
        dispatch({ file: selectedFile, type: "file-read-failed" });
      });
      reader.readAsDataURL(selectedFile);
    }
    e.currentTarget.value = "";
  };

  const handleCropComplete = (crop: PixelCrop) => {
    dispatch({ crop, type: "crop-changed" });
  };

  const handleImageLoad = (event: React.SyntheticEvent<HTMLImageElement>) => {
    dispatch({ image: event.currentTarget, type: "image-loaded" });
  };

  const handleUpload = async () => {
    if (uploadState.status !== "ready") {
      return;
    }
    try {
      const croppedImage = await getCroppedImage(
        uploadState.image,
        uploadState.crop,
        uploadState.file.name,
      );
      uploadProfilePhoto.mutate(croppedImage);
    } catch (error) {
      enqueueSnackbar(getProfilePhotoUploadErrorMessage(error), {
        variant: "error",
      });
    }
  };

  const getCroppedImage = (
    image: HTMLImageElement,
    crop: Partial<Crop>,
    fileName: string,
  ): Promise<File> => {
    const canvas = document.createElement("canvas");
    const canvasCrop = buildCanvasCrop({
      crop: crop as PixelCrop,
      renderedWidth: image.width,
      renderedHeight: image.height,
      naturalWidth: image.naturalWidth,
      naturalHeight: image.naturalHeight,
    });

    canvas.width = canvasCrop.outputWidth;
    canvas.height = canvasCrop.outputHeight;
    const ctx = canvas.getContext("2d")!;

    ctx.drawImage(
      image,
      canvasCrop.sourceX,
      canvasCrop.sourceY,
      canvasCrop.sourceWidth,
      canvasCrop.sourceHeight,
      0,
      0,
      canvasCrop.outputWidth,
      canvasCrop.outputHeight,
    );

    return new Promise((resolve, reject) => {
      canvas.toBlob((blob) => {
        if (!blob) {
          reject(new Error("Could not create the cropped profile image."));
          return;
        }
        const newBlob = new File([blob], fileName, { type: "image/jpeg" });
        resolve(newBlob);
      }, "image/jpeg");
    });
  };

  return (
    <div>
      <input
        accept="image/*"
        style={{ display: "none" }}
        id="raised-button-file"
        type="file"
        onChange={handleFileChange}
      />
      <label htmlFor="raised-button-file">
        <Button
          variant={buttonVariant}
          color="primary"
          component="span"
          startIcon={<CloudUploadIcon />}
        >
          Upload
        </Button>
      </label>
      {dialogState && (
        <Dialog
          open
          onClose={() => dispatch({ type: "closed" })}
          maxWidth="md"
          fullWidth
        >
          <DialogTitle>Crop your new profile picture</DialogTitle>
          <DialogContent
            sx={{
              display: "flex",
              justifyContent: "center",
              pb: 2,
            }}
          >
            <Box
              sx={{
                alignItems: "center",
                bgcolor: "grey.950",
                border: "1px solid",
                borderColor: "divider",
                display: "flex",
                height: {
                  xs: "min(72vw, 420px)",
                  sm: PROFILE_PHOTO_CROP_STAGE_SIZE,
                },
                justifyContent: "center",
                maxWidth: "100%",
                overflow: "hidden",
                width: {
                  xs: "min(72vw, 420px)",
                  sm: PROFILE_PHOTO_CROP_STAGE_SIZE,
                },
              }}
            >
              <ReactCrop
                {...(dialogState.status === "ready"
                  ? { crop: dialogState.crop }
                  : {})}
                circularCrop
                aspect={1}
                onComplete={handleCropComplete}
                onChange={handleCropComplete}
                style={{
                  maxHeight: "100%",
                  maxWidth: "100%",
                }}
              >
                <img
                  src={dialogState.src}
                  alt="Selected profile"
                  onLoad={handleImageLoad}
                  style={{
                    display: "block",
                    height:
                      dialogState.status === "ready" &&
                      dialogState.imageFit === "tall"
                        ? "100%"
                        : "auto",
                    maxHeight: "100%",
                    maxWidth: "100%",
                    objectFit: "contain",
                    width:
                      dialogState.status !== "ready" ||
                      dialogState.imageFit === "wide"
                        ? "100%"
                        : "auto",
                  }}
                />
              </ReactCrop>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => dispatch({ type: "closed" })}
              color="primary"
            >
              Cancel
            </Button>
            <Button
              onClick={() => {
                void handleUpload();
              }}
              color="primary"
              disabled={
                uploadProfilePhoto.isPending || uploadState.status !== "ready"
              }
            >
              Upload
            </Button>
          </DialogActions>
        </Dialog>
      )}
    </div>
  );
};

export default ProfileImageUpload;
