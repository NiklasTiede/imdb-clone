import { Avatar } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import type { ReactNode } from "react";
import { getProfileImageUrl } from "./imageUrls";

type ProfileAvatarProps = {
  alt?: string;
  fallback?: ReactNode;
  imageUrlToken?: string | undefined;
  sx?: SxProps<Theme>;
};

const ProfileAvatar = ({ alt = "", fallback, imageUrlToken, sx }: ProfileAvatarProps) => (
  <Avatar
    alt={alt}
    src={imageUrlToken ? getProfileImageUrl(imageUrlToken) : undefined}
    sx={sx}
  >
    {fallback}
  </Avatar>
);

export default ProfileAvatar;
