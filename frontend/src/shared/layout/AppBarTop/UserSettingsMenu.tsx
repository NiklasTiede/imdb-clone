import LogoutIcon from "@mui/icons-material/Logout";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import StarBorderIcon from "@mui/icons-material/StarBorder";
import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import Avatar from "@mui/material/Avatar";
import Box from "@mui/material/Box";
import Divider from "@mui/material/Divider";
import ListItemIcon from "@mui/material/ListItemIcon";
import Menu from "@mui/material/Menu";
import MenuItem from "@mui/material/MenuItem";
import Typography from "@mui/material/Typography";
import { alpha } from "@mui/material/styles";
import { Link as RouterLink } from "react-router";
import { movieColors } from "../../../theme";

const settings = [
  {
    icon: <StarBorderIcon fontSize="small" />,
    name: "YourRatings",
    to: "/your-ratings",
    text: "Your ratings",
  },
  {
    icon: <BookmarkBorderIcon fontSize="small" />,
    name: "YourWatchlist",
    to: "/your-watchlist",
    text: "Your watchlist",
  },
  {
    icon: <SettingsOutlinedIcon fontSize="small" />,
    name: "AccountSettings",
    to: "/account-settings",
    text: "Account settings",
  },
];

type UserSettingsMenuProps = {
  anchorEl: HTMLElement | null;
  menuId: string;
  onClose: () => void;
  onLogout: () => void;
  open: boolean;
  username?: string | null;
};

const UserSettingsMenu = ({
  anchorEl,
  menuId,
  onClose,
  onLogout,
  open,
  username,
}: UserSettingsMenuProps) => (
  <Menu
    anchorEl={anchorEl}
    anchorOrigin={{
      vertical: "bottom",
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
    slotProps={{
      paper: {
        sx: {
          border: "1px solid rgba(255,255,255,0.1)",
          borderRadius: 2,
          mt: 1,
          minWidth: 280,
          overflow: "hidden",
        },
      },
    }}
  >
    <Box
      sx={{
        alignItems: "center",
        display: "grid",
        gap: 1.5,
        gridTemplateColumns: "40px minmax(0, 1fr)",
        px: 2,
        py: 1.5,
      }}
    >
      <Avatar
        sx={{
          bgcolor: alpha(movieColors.brand, 0.18),
          color: movieColors.brand,
          fontSize: 13,
          fontWeight: 800,
          height: 40,
          width: 40,
        }}
      >
        {(username?.slice(0, 2) || "IM").toUpperCase()}
      </Avatar>
      <Box sx={{ minWidth: 0 }}>
        <Typography sx={{ fontSize: 14, fontWeight: 800 }} noWrap>
          {username || "Your account"}
        </Typography>
        <Typography sx={{ color: "text.secondary", fontSize: 12 }} noWrap>
          Ratings, watchlist, settings
        </Typography>
      </Box>
    </Box>
    <Divider />
    {settings.map((setting) => (
      <MenuItem
        component={RouterLink}
        key={setting.name}
        onClick={onClose}
        to={setting.to}
      >
        <ListItemIcon>{setting.icon}</ListItemIcon>
        <Typography>{setting.text}</Typography>
      </MenuItem>
    ))}
    <Divider />
    <MenuItem key="Logout" onClick={onLogout} sx={{ color: "error.light" }}>
      <ListItemIcon sx={{ color: "inherit" }}>
        <LogoutIcon fontSize="small" />
      </ListItemIcon>
      <Typography>Sign out</Typography>
    </MenuItem>
  </Menu>
);

export default UserSettingsMenu;
