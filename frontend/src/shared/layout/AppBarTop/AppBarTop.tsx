import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import { useQuery } from "@tanstack/react-query";
import type { MouseEvent } from "react";
import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router";
import { movieColors } from "../../../theme";
import { accountQueries } from "../../api/accountProfileQueries";
import { logoutSession, RoleNameEnum } from "../../auth";
import { authSession } from "../../auth/authSession";
import { useAuthSession } from "../../auth/useAuthSession";
import BrandLogo from "../BrandLogo";
import AdminEditButton from "./AdminEditButton";
import LoginButton from "./LoginButton";
import MovieSearchInput from "./MovieSearchInput";
import UserActions from "./UserActions";
import UserSettingsMenu from "./UserSettingsMenu";

const menuId = "primary-search-account-menu";
const SEARCH_DEBOUNCE_MS = 300;

function AppBarTop() {
  const navigateTo = useNavigate();
  const location = useLocation();

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const isMenuOpen = Boolean(anchorEl);

  const isLoggedIn = useAuthSession();
  const isAdmin = authSession.hasRole(RoleNameEnum.Admin);
  const username = authSession.getUsername();
  const { data: currentProfile } = useQuery({
    ...accountQueries.currentProfile(),
    enabled: isLoggedIn,
  });

  const queryParams = new URLSearchParams(location.search);
  const initialQuery = queryParams.get("query") || queryParams.get("q") || "";
  const [query, setQuery] = useState(initialQuery);
  const pendingSearchQueriesRef = useRef(new Set<string>());

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const locationQuery = params.get("query") || params.get("q") || "";

    if (pendingSearchQueriesRef.current.delete(locationQuery.trim())) {
      return;
    }

    setQuery(locationQuery);
  }, [location.search]);

  const navigateToSearch = useCallback(
    (searchQuery: string, options?: { replace?: boolean }) => {
      const nextQuery = searchQuery.trim();
      pendingSearchQueriesRef.current.add(nextQuery);
      const params =
        location.pathname === "/movie-search"
          ? new URLSearchParams(location.search)
          : new URLSearchParams();

      params.delete("q");
      params.delete("page");
      if (nextQuery) {
        params.set("query", nextQuery);
      } else {
        params.delete("query");
      }

      const search = params.toString().replaceAll("+", "%20");
      void navigateTo(
        {
          pathname: "/movie-search",
          search: search ? `?${search}` : "",
        },
        { replace: options?.replace ?? false },
      );
    },
    [location.pathname, location.search, navigateTo],
  );

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const currentQuery = params.get("query") || params.get("q") || "";
    if (query.trim() === currentQuery.trim()) {
      return undefined;
    }

    const timeout = window.setTimeout(() => {
      navigateToSearch(query, { replace: true });
    }, SEARCH_DEBOUNCE_MS);

    return () => window.clearTimeout(timeout);
  }, [location.search, navigateToSearch, query]);

  const handleProfileMenuOpen = (event: MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    setAnchorEl(null);
    void logoutSession()
      .catch(() => undefined)
      .finally(() => {
        authSession.clear();
        void navigateTo("/");
      });
  };

  const handleSearch = (query: string) => {
    const nextQuery = query.trim();
    setQuery(nextQuery);
    navigateToSearch(nextQuery);
  };

  const handleClear = () => {
    setQuery("");
    navigateToSearch("");
  };

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar
        elevation={0}
        position="sticky"
        sx={{
          backdropFilter: "blur(18px)",
          bgcolor: `${movieColors.surface}eb`,
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
                imageUrlToken={currentProfile?.imageUrlToken}
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
        imageUrlToken={currentProfile?.imageUrlToken}
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
