import Avatar from "@mui/material/Avatar";
import IconButton from "@mui/material/IconButton";
import Tooltip from "@mui/material/Tooltip";
import { alpha } from "@mui/material/styles";
import type React from "react";
import { movieColors } from "../../../theme";

type UserActionsProps = {
  menuId: string;
  onProfileMenuOpen: (event: React.MouseEvent<HTMLElement>) => void;
  username?: string | null;
};

const UserActions = ({
  menuId,
  onProfileMenuOpen,
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
      aria-haspopup="true"
      color="inherit"
      onClick={onProfileMenuOpen}
      size="large"
      sx={{ p: 0.5 }}
    >
      <Avatar
        sx={{
          bgcolor: alpha(movieColors.brand, 0.18),
          border: `1px solid ${alpha(movieColors.brand, 0.36)}`,
          color: movieColors.brand,
          fontSize: 13,
          fontWeight: 800,
          height: 34,
          width: 34,
        }}
      >
        {(username?.slice(0, 2) || "IM").toUpperCase()}
      </Avatar>
    </IconButton>
  </Tooltip>
);

export default UserActions;
