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
  const pendingSearchLocationRef = useRef<string | null>(null);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const locationQuery = params.get("query") || params.get("q") || "";

    const pendingLocation = pendingSearchLocationRef.current;
    pendingSearchLocationRef.current = null;
    if (pendingLocation === `${location.pathname}${location.search}`) {
      return;
    }

    setQuery(locationQuery);
  }, [location.pathname, location.search]);

  const navigateToSearch = useCallback(
    (searchQuery: string, options?: { replace?: boolean }) => {
      const nextQuery = searchQuery.trim();
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
      const nextLocation = `/movie-search${search ? `?${search}` : ""}`;
      // A no-op submission never produces a URL effect to consume pending state.
      pendingSearchLocationRef.current =
        nextLocation === `${location.pathname}${location.search}`
          ? null
          : nextLocation;
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
            columnGap: { xs: 1, md: 2 },
            rowGap: 1,
            gridTemplateColumns: {
              xs: "minmax(0, 1fr) auto",
              md: "minmax(176px, auto) minmax(280px, 620px) minmax(max-content, 1fr)",
            },
            gridTemplateRows: { xs: "auto auto", md: "auto" },
            minHeight: { xs: 70, md: 68 },
            px: { xs: 1.5, sm: 2.5, md: 3 },
            py: { xs: 0.75, md: 0 },
          }}
        >
          <BrandLogo
            sx={{
              gridColumn: 1,
              gridRow: 1,
              minWidth: 0,
              "& > img": {
                width: { xs: 32, md: 42 },
                height: { xs: 32, md: 42 },
              },
              "& span": { overflow: "hidden", textOverflow: "ellipsis" },
            }}
          />
          <Box
            sx={{
              gridColumn: { xs: "1 / -1", md: "2" },
              gridRow: { xs: 2, md: 1 },
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
              gridColumn: {
                xs: 2,
                md: 3,
              },
              gridRow: 1,
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
