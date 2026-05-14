import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import type { MouseEvent } from "react";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router";
import { authSession } from "../../auth/authSession";
import { useAuthSession } from "../../auth/useAuthSession";
import { RoleNameEnum } from "../../auth";
import BrandLogo from "../BrandLogo";
import AdminEditButton from "./AdminEditButton";
import LoginButton from "./LoginButton";
import MovieSearchInput from "./MovieSearchInput";
import UserActions from "./UserActions";
import UserSettingsMenu from "./UserSettingsMenu";

const menuId = "primary-search-account-menu";

function AppBarTop() {
  const navigateTo = useNavigate();
  const location = useLocation();

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const isMenuOpen = Boolean(anchorEl);

  const isLoggedIn = useAuthSession();
  const isAdmin = authSession.hasRole(RoleNameEnum.Admin);
  const username = authSession.getUsername();

  const queryParams = new URLSearchParams(location.search);
  const initialQuery = queryParams.get("query") || "";
  const [query, setQuery] = useState(initialQuery);

  useEffect(() => {
    setQuery(new URLSearchParams(location.search).get("query") || "");
  }, [location.search]);

  const handleProfileMenuOpen = (event: MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    setAnchorEl(null);
    authSession.clear();
    navigateTo("/");
  };

  const handleSearch = (query: string) => {
    const nextQuery = query.trim();
    setQuery(nextQuery);
    navigateTo(
      nextQuery
        ? `/movie-search?query=${encodeURIComponent(nextQuery)}`
        : "/movie-search",
    );
  };

  const handleClear = () => {
    setQuery("");
    navigateTo("/movie-search");
  };

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar
        elevation={0}
        position="sticky"
        sx={{
          backdropFilter: "blur(18px)",
          bgcolor: "rgba(13, 27, 42, 0.92)",
          borderBottom: "1px solid rgba(255,255,255,0.08)",
          color: "common.white",
        }}
      >
        <Toolbar
          sx={{
            display: "grid",
            gap: { xs: 1.25, md: 2 },
            gridTemplateColumns: {
              xs: "1fr auto",
              md: "minmax(176px, auto) minmax(280px, 1fr) auto",
            },
            gridTemplateRows: { xs: "auto auto", md: "auto" },
            minHeight: { xs: 70, md: 68 },
            px: { xs: 1.5, sm: 2.5, md: 3 },
            py: { xs: 1.25, md: 0 },
          }}
        >
          <BrandLogo
            sx={{
              gridColumn: { xs: "1", md: "auto" },
              minWidth: 0,
            }}
          />
          <Box
            sx={{
              gridColumn: { xs: "1 / -1", md: "auto" },
              gridRow: { xs: 2, md: "auto" },
              minWidth: 0,
            }}
          >
            <MovieSearchInput
              query={query}
              onQueryChange={setQuery}
              onSearch={handleSearch}
              onClear={handleClear}
            />
          </Box>
          <Box
            sx={{
              alignItems: "center",
              display: "inline-flex",
              gap: 0.75,
              justifySelf: "end",
            }}
          >
            {isAdmin && <AdminEditButton />}
            {isLoggedIn ? (
              <UserActions
                menuId={menuId}
                onProfileMenuOpen={handleProfileMenuOpen}
                username={username}
              />
            ) : (
              <LoginButton />
            )}
          </Box>
        </Toolbar>
      </AppBar>
      <UserSettingsMenu
        anchorEl={anchorEl}
        menuId={menuId}
        onClose={handleMenuClose}
        onLogout={handleLogout}
        open={isMenuOpen}
        username={username}
      />
    </Box>
  );
}

export default AppBarTop;
