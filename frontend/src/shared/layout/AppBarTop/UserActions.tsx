import IconButton from "@mui/material/IconButton";
import Tooltip from "@mui/material/Tooltip";
import type React from "react";
import { ProfileAvatar } from "../../media";

type UserActionsProps = {
  isMenuOpen?: boolean;
  menuId: string;
  onProfileMenuOpen: (event: React.MouseEvent<HTMLElement>) => void;
  imageUrlToken?: string | undefined;
  username?: string | null;
};

const UserActions = ({
  isMenuOpen = false,
  menuId,
  onProfileMenuOpen,
  imageUrlToken,
  username,
}: UserActionsProps) => (
  <Tooltip
    placement="bottom-end"
    slotProps={{
      popper: {
        modifiers: [
          {
            name: "preventOverflow",
            options: {
              boundary: "viewport",
              padding: 8,
            },
          },
        ],
      },
    }}
    title="Account menu"
  >
    <IconButton
      aria-label="account of current user"
      aria-controls={menuId}
      aria-expanded={isMenuOpen}
      aria-haspopup="true"
      color="inherit"
      onClick={onProfileMenuOpen}
      size="large"
      sx={{ p: 0.5 }}
    >
      <ProfileAvatar
        alt={username ? `${username} profile` : "Account profile"}
        fallback={(username?.slice(0, 2) || "IM").toUpperCase()}
        imageUrlToken={imageUrlToken}
        sx={{
          bgcolor: (theme) => theme.alpha(theme.palette.accent.main, 0.18),
          border: "1px solid",
          borderColor: (theme) =>
            isMenuOpen
              ? theme.palette.accent.main
              : theme.alpha(theme.palette.accent.main, 0.36),
          boxShadow: (theme) =>
            isMenuOpen
              ? `0 0 0 2px ${theme.alpha(theme.palette.accent.main, 0.35)}`
              : "none",
          transition: "border-color 150ms ease, box-shadow 150ms ease",
          // Accent tints can be too dark for accent-coloured text; the core stays readable.
          color: "accent.core",
          fontSize: 13,
          fontWeight: 800,
          height: 38,
          width: 38,
        }}
      />
    </IconButton>
  </Tooltip>
);

export default UserActions;
