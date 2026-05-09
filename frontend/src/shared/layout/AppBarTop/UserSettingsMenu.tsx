import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import Typography from "@mui/material/Typography";
import { Link } from "react-router";
import { i18n } from "../../../i18n";

const settings = [
  {
    name: "YourRatings",
    to: "/your-ratings",
    text: "Your Ratings",
  },
  {
    name: "YourWatchlist",
    to: "/your-watchlist",
    text: "Your Watchlist",
  },
  {
    name: "AccountSettings",
    to: "/account-settings",
    text: "Account Settings",
  },
];

type UserSettingsMenuProps = {
  anchorEl: HTMLElement | null;
  menuId: string;
  onClose: () => void;
  onLogout: () => void;
  open: boolean;
};

const UserSettingsMenu = ({
  anchorEl,
  menuId,
  onClose,
  onLogout,
  open,
}: UserSettingsMenuProps) => (
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
    {settings.map((setting) => (
      <MenuItem key={setting.name} onClick={onClose}>
        <Typography sx={{ textAlign: "center" }}>
          <Link
            style={{ textDecoration: "none", color: "inherit" }}
            to={setting.to}
          >
            {setting.text}
          </Link>
        </Typography>
      </MenuItem>
    ))}
    <MenuItem key="Logout" onClick={onLogout}>
      <Typography sx={{ textAlign: "center" }}>
        <Link style={{ textDecoration: "none", color: "inherit" }} to="/logout">
          {i18n.menuOptions.logout}
        </Link>
      </Typography>
    </MenuItem>
  </Menu>
);

export default UserSettingsMenu;
