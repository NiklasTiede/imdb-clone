import AccountCircle from "@mui/icons-material/AccountCircle";
import MailIcon from "@mui/icons-material/Mail";
import IconButton from "@mui/material/IconButton";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import type React from "react";
import { Link } from "react-router";
import { i18n } from "../../../i18n";

type UserMobileMenuProps = {
  anchorEl: HTMLElement | null;
  menuId: string;
  onClose: () => void;
  onProfileMenuOpen: (event: React.MouseEvent<HTMLElement>) => void;
  open: boolean;
  textColor: string;
};

const UserMobileMenu = ({
  anchorEl,
  menuId,
  onClose,
  onProfileMenuOpen,
  open,
  textColor,
}: UserMobileMenuProps) => (
  <Menu
    anchorEl={anchorEl}
    anchorOrigin={{
      vertical: "top",
      horizontal: "right",
    }}
    id={menuId}
    keepMounted
    transformOrigin={{
      vertical: "top",
      horizontal: "right",
    }}
    open={open}
    onClose={onClose}
  >
    <Link
      style={{ textDecoration: "none", color: "inherit" }}
      to="your-messages"
    >
      <MenuItem style={{ color: textColor }}>
        <IconButton size="large" aria-label="messages" color="inherit">
          <MailIcon />
        </IconButton>
        <p>{i18n.general.messages}</p>
      </MenuItem>
    </Link>
    <MenuItem onClick={onProfileMenuOpen} style={{ color: textColor }}>
      <IconButton
        size="large"
        aria-label="account of current user"
        aria-controls="primary-search-account-menu"
        aria-haspopup="true"
        color="inherit"
      >
        <AccountCircle />
      </IconButton>
      <p>{i18n.general.profile}</p>
    </MenuItem>
  </Menu>
);

export default UserMobileMenu;
