import * as React from "react";
import MenuIcon from "@mui/icons-material/Menu";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { i18n } from "../../../i18n";
import { authSession } from "../../auth/authSession";
import { useAuthSession } from "../../auth/useAuthSession";
import { tokens } from "../../../theme";
import { RoleNameEnum } from "../../../types/roles";
import AdminEditButton from "./AdminEditButton";
import LoginButton from "./LoginButton";
import MovieSearchInput from "./MovieSearchInput";
import UserActions from "./UserActions";
import UserMobileMenu from "./UserMobileMenu";
import UserSettingsMenu from "./UserSettingsMenu";

const menuId = "primary-search-account-menu";
const mobileMenuId = "primary-search-account-menu-mobile";

function AppBarTop() {
  const colors = tokens();
  const navigateTo = useNavigate();

  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [mobileMoreAnchorEl, setMobileMoreAnchorEl] =
    React.useState<null | HTMLElement>(null);

  const isMenuOpen = Boolean(anchorEl);
  const isMobileMenuOpen = Boolean(mobileMoreAnchorEl);

  const isLoggedIn = useAuthSession();
  const isAdmin = authSession.hasRole(RoleNameEnum.Admin);

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
    authSession.clear();
  };

  const handleMobileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setMobileMoreAnchorEl(event.currentTarget);
  };

  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const initialQuery = queryParams.get("query") || "";
  const [query, setQuery] = useState(initialQuery);

  const handleSearch = (query: string) => {
    setQuery(query);
    navigateTo(`/movie-search?query=${query}`);
  };

  const handleClear = () => {
    setQuery("");
    navigateTo("/movie-search");
  };

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
          <Link to="/" style={{ textDecoration: "none" }}>
            <Typography
              variant="h6"
              noWrap
              component="span"
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
          </Link>
          <MovieSearchInput
            query={query}
            onQueryChange={setQuery}
            onSearch={handleSearch}
            onClear={handleClear}
          />

          <Box sx={{ flexGrow: 1 }} />
          {isAdmin ? <AdminEditButton /> : ""}
          {isLoggedIn ? (
            <UserActions
              menuId={menuId}
              mobileMenuId={mobileMenuId}
              onMobileMenuOpen={handleMobileMenuOpen}
              onProfileMenuOpen={handleProfileMenuOpen}
            />
          ) : (
            <LoginButton />
          )}
        </Toolbar>
      </AppBar>
      <UserMobileMenu
        anchorEl={mobileMoreAnchorEl}
        menuId={mobileMenuId}
        onClose={handleMobileMenuClose}
        onProfileMenuOpen={handleProfileMenuOpen}
        open={isMobileMenuOpen}
        textColor={colors.grey[100]}
      />
      <UserSettingsMenu
        anchorEl={anchorEl}
        menuId={menuId}
        onClose={handleMenuClose}
        onLogout={handleLogout}
        open={isMenuOpen}
      />
    </Box>
  );
}

export default AppBarTop;
