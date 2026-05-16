import React, { useLayoutEffect, useState } from "react";
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
  createCenteredSquareCrop,
  getProfilePhotoUploadErrorMessage,
  PROFILE_PHOTO_CROP_STAGE_SIZE,
} from "./profilePhotoCropper";

type ProfileImageUploadProps = {
  buttonVariant?: ButtonProps["variant"];
};

const ProfileImageUpload: React.FC<ProfileImageUploadProps> = ({
  buttonVariant = "contained",
}) => {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();
  const uploadProfilePhoto = useMutation({
    mutationFn: storeUserProfilePhoto,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: mediaMutationKeys.currentProfile,
      });
      setOpen(false);
    },
    onError: (error) => {
      enqueueSnackbar(getProfilePhotoUploadErrorMessage(error), {
        variant: "error",
      });
    },
  });

  const [open, setOpen] = useState(false);
  const [src, setSrc] = useState<string | null>(null);
  const [crop, setCrop] = useState<Crop>();
  const [imageRef, setImageRef] = useState<HTMLImageElement | null>(null);
  const [imageFit, setImageFit] = useState<"wide" | "tall">("wide");
  const [file, setFile] = useState<File | null>(null);

  useLayoutEffect(() => {
    if (imageRef) {
      setCrop(createCenteredSquareCrop(imageRef.width, imageRef.height));
    }
  }, [imageFit, imageRef]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const reader = new FileReader();
      reader.addEventListener("load", () => setSrc(reader.result as string));
      reader.readAsDataURL(e.target.files[0]);
      setFile(e.target.files[0]);
      setOpen(true);
    }
    if (e.target) {
      e.target.value = "";
    }
  };

  const handleCropComplete = (crop: PixelCrop) => {
    setCrop(crop);
  };

  const handleImageLoad = (event: React.SyntheticEvent<HTMLImageElement>) => {
    const image = event.currentTarget;

    setImageFit(image.naturalWidth >= image.naturalHeight ? "wide" : "tall");
    setImageRef(image);
  };

  const handleUpload = async () => {
    if (imageRef && crop?.width && crop.height && file) {
      const croppedImage = await getCroppedImage(imageRef, crop, file.name);

      uploadProfilePhoto.mutate(croppedImage);
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

    return new Promise((resolve) => {
      canvas.toBlob((blob) => {
        if (!blob) return;
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
      {src && (
        <Dialog
          open={open}
          onClose={() => setOpen(false)}
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
                crop={crop}
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
                  src={src}
                  alt="Selected profile"
                  onLoad={handleImageLoad}
                  style={{
                    display: "block",
                    height: imageFit === "tall" ? "100%" : "auto",
                    maxHeight: "100%",
                    maxWidth: "100%",
                    objectFit: "contain",
                    width: imageFit === "wide" ? "100%" : "auto",
                  }}
                />
              </ReactCrop>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpen(false)} color="primary">
              Cancel
            </Button>
            <Button
              onClick={handleUpload}
              color="primary"
              disabled={uploadProfilePhoto.isPending}
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
