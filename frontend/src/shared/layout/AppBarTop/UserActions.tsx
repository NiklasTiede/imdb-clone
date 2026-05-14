import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import Avatar from "@mui/material/Avatar";
import Badge from "@mui/material/Badge";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Tooltip from "@mui/material/Tooltip";
import type React from "react";
import { Link as RouterLink } from "react-router";

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
  <Box sx={{ alignItems: "center", display: "inline-flex", gap: 0.75 }}>
    <Tooltip title="Messages">
      <IconButton
        aria-label="messages and notifications"
        color="inherit"
        component={RouterLink}
        size="large"
        to="/your-messages"
      >
        <Badge badgeContent={4} color="error">
          <NotificationsOutlinedIcon />
        </Badge>
      </IconButton>
    </Tooltip>
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
            bgcolor: "rgba(245,197,24,0.18)",
            border: "1px solid rgba(245,197,24,0.36)",
            color: "#f5c518",
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
  </Box>
);

export default UserActions;
