import { Route, Routes } from "react-router";
import MyAppBar from "./components/AppBarTop";
import Home from "./components/Home";
import Login from "./components/authentication/Login";
import Logout from "./components/authentication/Logout";
import Registration from "./components/authentication/Registration";
import YourRatings from "./components/profile/YourRatings";
import YourWatchlist from "./components/profile/YourWatchlist";
import { AccountSettingsPage } from "./features/account";
import { EditMoviePage, MovieDetailPage } from "./features/catalog";
import { FilterPanelPage, MovieSearchPage } from "./features/search";
import { useMode, ColorModeContext } from "./theme";
import { CssBaseline, ThemeProvider } from "@mui/material";
import React from "react";
import Messages from "./components/profile/Messages";
import { RoleNameEnum } from "./types/roles";
import PublicRoute from "./components/routes/PublicRoute";
import PrivateRoute from "./components/routes/PrivateRoute";
import NotFound from "./components/routes/NotFound";

function App() {
  const [theme, colorMode] = useMode();

  return (
    <ColorModeContext.Provider value={colorMode}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <MyAppBar />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/movie-search" element={<MovieSearchPage />} />
          <Route path="/movie" element={<MovieDetailPage />} />
          <Route path="/filter" element={<FilterPanelPage />} />
          <Route path="/login" element={<Login />} />
          <Route
            path="/registration"
            element={
              <PublicRoute>
                <Registration />
              </PublicRoute>
            }
          />
          <Route
            path="/logout"
            element={
              <PublicRoute>
                <Logout />
              </PublicRoute>
            }
          />
          <Route
            path="/your-ratings"
            element={
              <PrivateRoute role={RoleNameEnum.User}>
                <YourRatings />
              </PrivateRoute>
            }
          />
          <Route
            path="/your-watchlist"
            element={
              <PrivateRoute role={RoleNameEnum.User}>
                <YourWatchlist />
              </PrivateRoute>
            }
          />
          <Route
            path="/your-messages"
            element={
              <PrivateRoute role={RoleNameEnum.User}>
                <Messages />
              </PrivateRoute>
            }
          />
          <Route
            path="/account-settings"
            element={
              <PrivateRoute role={RoleNameEnum.User}>
                <AccountSettingsPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/editing"
            element={
              <PrivateRoute role={RoleNameEnum.Admin}>
                <EditMoviePage />
              </PrivateRoute>
            }
          />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </ThemeProvider>
    </ColorModeContext.Provider>
  );
}

export default App;
