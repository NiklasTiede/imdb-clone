import AccountCircle from "@mui/icons-material/AccountCircle";
import MailIcon from "@mui/icons-material/Mail";
import MoreIcon from "@mui/icons-material/MoreVert";
import Badge from "@mui/material/Badge";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import type React from "react";
import { Link } from "react-router";

type UserActionsProps = {
  menuId: string;
  mobileMenuId: string;
  onMobileMenuOpen: (event: React.MouseEvent<HTMLElement>) => void;
  onProfileMenuOpen: (event: React.MouseEvent<HTMLElement>) => void;
};

const UserActions = ({
  menuId,
  mobileMenuId,
  onMobileMenuOpen,
  onProfileMenuOpen,
}: UserActionsProps) => (
  <div>
    <Box sx={{ display: { xs: "none", md: "flex" } }}>
      <Link style={{ color: "inherit" }} to="your-messages">
        <IconButton size="large" aria-label="show 4 new mails" color="inherit">
          <Badge badgeContent={4} color="error">
            <MailIcon />
          </Badge>
        </IconButton>
      </Link>
      <IconButton
        size="large"
        edge="end"
        aria-label="account of current user"
        aria-controls={menuId}
        aria-haspopup="true"
        onClick={onProfileMenuOpen}
        color="inherit"
      >
        <AccountCircle />
      </IconButton>
    </Box>
    <Box sx={{ display: { xs: "flex", md: "none" } }}>
      <IconButton
        size="large"
        aria-label="show more"
        aria-controls={mobileMenuId}
        aria-haspopup="true"
        onClick={onMobileMenuOpen}
        color="inherit"
      >
        <MoreIcon />
      </IconButton>
    </Box>
  </div>
);

export default UserActions;
