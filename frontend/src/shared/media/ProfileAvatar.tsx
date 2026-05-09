import { Avatar } from "@mui/material";
import type { SxProps, Theme } from "@mui/material/styles";
import { getProfileImageUrl } from "./imageUrls";

type ProfileAvatarProps = {
  imageUrlToken?: string;
  sx?: SxProps<Theme>;
};

const ProfileAvatar = ({ imageUrlToken, sx }: ProfileAvatarProps) => (
  <Avatar
    src={imageUrlToken ? getProfileImageUrl(imageUrlToken) : undefined}
    sx={sx}
  />
);

export default ProfileAvatar;
