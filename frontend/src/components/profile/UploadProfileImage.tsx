import React, { useState } from "react";
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from "@mui/material";
import { CloudUpload as CloudUploadIcon } from "@mui/icons-material";
import ReactCrop, { type Crop, type PixelCrop } from "react-image-crop";
import "react-image-crop/dist/ReactCrop.css";
import { useDispatch } from "react-redux";
import { Dispatch } from "../../redux/store";

const UploadProfileImage: React.FC = () => {
  const dispatch = useDispatch<Dispatch>();

  const [open, setOpen] = useState(false);
  const [src, setSrc] = useState<string | null>(null);
  const [crop, setCrop] = useState<Crop>();
  const [imageRef, setImageRef] = useState<HTMLImageElement | null>(null);
  const [file, setFile] = useState<File | null>(null);

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

  const handleUpload = async () => {
    if (imageRef && crop?.width && crop.height && file) {
      const croppedImage = await getCroppedImage(imageRef, crop, file.name);

      dispatch.fileStorage.storeUserProfilePhoto(croppedImage);
    } else {
      console.log("beep");
    }
  };

  const getCroppedImage = (
    image: HTMLImageElement,
    crop: Partial<Crop>,
    fileName: string
  ): Promise<Blob> => {
    const canvas = document.createElement("canvas");
    const scaleX = image.naturalWidth / image.width;
    const scaleY = image.naturalHeight / image.height;
    canvas.width = crop.width as number;
    canvas.height = crop.height as number;
    const ctx = canvas.getContext("2d")!;

    ctx.drawImage(
      image,
      crop.x! * scaleX,
      crop.y! * scaleY,
      crop.width! * scaleX,
      crop.height! * scaleY,
      0,
      0,
      crop.width!,
      crop.height!
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
          variant="contained"
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
          <DialogContent>
            <ReactCrop
              crop={crop}
              circularCrop
              aspect={1}
              onComplete={handleCropComplete}
              onChange={handleCropComplete}
            >
              <img
                src={src}
                alt="Selected profile"
                onLoad={(event) => setImageRef(event.currentTarget)}
              />
            </ReactCrop>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpen(false)} color="primary">
              Cancel
            </Button>
            <Button onClick={handleUpload} color="primary">
              Upload
            </Button>
          </DialogActions>
        </Dialog>
      )}
    </div>
  );
};

export default UploadProfileImage;
