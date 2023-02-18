import * as React from "react";
import { styled, alpha } from "@mui/material/styles";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
import InputBase from "@mui/material/InputBase";
import Badge from "@mui/material/Badge";
import MenuItem from "@mui/material/MenuItem";
import Menu from "@mui/material/Menu";
import MenuIcon from "@mui/icons-material/Menu";
import SearchIcon from "@mui/icons-material/Search";
import AccountCircle from "@mui/icons-material/AccountCircle";
import MailIcon from "@mui/icons-material/Mail";
import DarkModeOutlinedIcon from "@mui/icons-material/DarkModeOutlined";
import LightModeOutlinedIcon from "@mui/icons-material/LightModeOutlined";
import MoreIcon from "@mui/icons-material/MoreVert";
import { Link } from "react-router-dom";
import { useTheme } from "@mui/material";
import { ColorModeContext, tokens } from "../theme";
import { useContext } from "react";
import LoginIcon from "@mui/icons-material/Login";
import EditIcon from "@mui/icons-material/Edit";
import { hasUserRole, isJwtNotExpired } from "../utils/jwtHelper";
import { Dispatch } from "../redux/store";
import { useDispatch, useSelector } from "react-redux";
import { i18n } from "../i18n";
import { State as AuthenticationStatus } from "../redux/model/authentication";
import { useNotifier } from "../hooks/useNotifier";
import { RoleNameEnum } from "../client/movies/generator-output";

let settings = [
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

const Search = styled("div")(({ theme }) => ({
  position: "relative",
  borderRadius: theme.shape.borderRadius,
  backgroundColor: alpha(theme.palette.common.white, 0.15),
  "&:hover": {
    backgroundColor: alpha(theme.palette.common.white, 0.25),
  },
  marginRight: theme.spacing(2),
  marginLeft: 0,
  width: "100%",
  [theme.breakpoints.up("sm")]: {
    marginLeft: theme.spacing(3),
    width: "auto",
  },
}));

const SearchIconWrapper = styled("div")(({ theme }) => ({
  padding: theme.spacing(0, 2),
  height: "100%",
  position: "absolute",
  pointerEvents: "none",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
}));

const StyledInputBase = styled(InputBase)(({ theme }) => ({
  color: "inherit",
  "& .MuiInputBase-input": {
    padding: theme.spacing(1, 1, 1, 0),
    paddingLeft: `calc(1em + ${theme.spacing(4)})`,
    transition: theme.transitions.create("width"),
    width: "100%",
    [theme.breakpoints.up("md")]: {
      width: "20ch",
    },
  },
}));

function AppBarTop() {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const colorMode = useContext(ColorModeContext);
  const dispatch = useDispatch<Dispatch>();

  // to use redux-Notifications on all child components
  useNotifier();

  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [mobileMoreAnchorEl, setMobileMoreAnchorEl] =
    React.useState<null | HTMLElement>(null);

  const isMenuOpen = Boolean(anchorEl);
  const isMobileMenuOpen = Boolean(mobileMoreAnchorEl);

  // after isAuthenticated from store is updated -> AppBar comp. is re-rendered
  useSelector(
    (state: { authentication: AuthenticationStatus }) =>
      state.authentication.isAuthenticated
  );
  const isAdmin: boolean = hasUserRole(RoleNameEnum.Admin);
  const isLoggedIn: boolean = isJwtNotExpired();

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMobileMenuClose = () => {
    setMobileMoreAnchorEl(null);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    handleMobileMenuClose();
  };

  const handleLogout = () => {
    setAnchorEl(null);
    handleMobileMenuClose();

    window.localStorage.removeItem("jwtToken");
    window.localStorage.removeItem("rolesFromJwt");
    window.localStorage.removeItem("jwtExpiresAt");
    window.localStorage.removeItem("username");

    dispatch.authentication.setIsAuthenticated(false);
  };

  const handleMobileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setMobileMoreAnchorEl(event.currentTarget);
  };

  const renderMoviesEditing = (
    <Link style={{ color: "inherit" }} to={"editing"}>
      <IconButton size="large" aria-haspopup="true" color="inherit">
        <EditIcon />
      </IconButton>
    </Link>
  );

  const menuId = "primary-search-account-menu";
  const renderMenu = (
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
      open={isMenuOpen}
      onClose={handleMenuClose}
    >
      {settings.map((setting) => (
        <MenuItem key={setting.name} onClick={handleMenuClose}>
          <Typography textAlign="center">
            <Link
              style={{ textDecoration: "none", color: "inherit" }}
              to={`${setting.to}`}
            >
              {setting.text}
            </Link>
          </Typography>
        </MenuItem>
      ))}
      <MenuItem key={"Logout"} onClick={handleLogout}>
        <Typography textAlign="center">
          <Link
            style={{ textDecoration: "none", color: "inherit" }}
            to={"/logout"}
          >
            {i18n.menuOptions.logout}
          </Link>
        </Typography>
      </MenuItem>
    </Menu>
  );

  const mobileMenuId = "primary-search-account-menu-mobile";

  const renderMobileMenu = (
    <Menu
      anchorEl={mobileMoreAnchorEl}
      anchorOrigin={{
        vertical: "top",
        horizontal: "right",
      }}
      id={mobileMenuId}
      keepMounted
      transformOrigin={{
        vertical: "top",
        horizontal: "right",
      }}
      open={isMobileMenuOpen}
      onClose={handleMobileMenuClose}
    >
      <Link
        style={{ textDecoration: "none", color: "inherit" }}
        to={"your-messages"}
      >
        <MenuItem style={{ color: colors.grey["100"] }}>
          <IconButton
            size="large"
            aria-label="show 4 new mails"
            color="inherit"
          >
            <Badge badgeContent={4} color="error">
              <MailIcon />
            </Badge>
          </IconButton>
          <p>{i18n.general.messages}</p>
        </MenuItem>
      </Link>
      <MenuItem
        onClick={handleProfileMenuOpen}
        style={{ color: colors.grey["100"] }}
      >
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

  const renderLogin = (
    <Link style={{ color: "inherit" }} to={"login"}>
      <IconButton size="large" color="inherit">
        <Badge>
          <LoginIcon />
        </Badge>
      </IconButton>
    </Link>
  );

  const renderLoggedInMenu = (
    <div>
      <Box sx={{ display: { xs: "none", md: "flex" } }}>
        <Link style={{ color: "inherit" }} to={"your-messages"}>
          <IconButton
            size="large"
            aria-label="show 4 new mails"
            color="inherit"
          >
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
          onClick={handleProfileMenuOpen}
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
          onClick={handleMobileMenuOpen}
          color="inherit"
        >
          <MoreIcon />
        </IconButton>
      </Box>
    </div>
  );

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static">
        <Toolbar>
          <IconButton
            size="large"
            edge="start"
            color="inherit"
            aria-label="open drawer"
            sx={{ mr: 2 }}
          >
            <MenuIcon />
          </IconButton>
          <Typography
            variant="h6"
            noWrap
            component="a"
            href={"/"}
            sx={{
              mr: 2,
              display: { xs: "none", md: "flex" },
              color: colors.grey[100],
              textDecoration: "none",
              "&:hover": {
                color: colors.grey[200],
              },
            }}
          >
            {i18n.general.appName}
          </Typography>

          <Search>
            <SearchIconWrapper>
              <SearchIcon />
            </SearchIconWrapper>
            <StyledInputBase
              placeholder="Search for Movies…"
              inputProps={{ "aria-label": "search" }}
            />
          </Search>

          <Box sx={{ flexGrow: 1 }} />
          <IconButton
            onClick={colorMode.toggleColorMode}
            size="large"
            aria-label="show 4 new mails"
            color="inherit"
          >
            <Badge>
              {theme.palette.mode === "dark" ? (
                <DarkModeOutlinedIcon />
              ) : (
                <LightModeOutlinedIcon />
              )}
            </Badge>
          </IconButton>
          {isAdmin ? renderMoviesEditing : ""}
          {isLoggedIn ? renderLoggedInMenu : renderLogin}
        </Toolbar>
      </AppBar>
      {renderMobileMenu}
      {renderMenu}
    </Box>
  );
}

export default AppBarTop;
