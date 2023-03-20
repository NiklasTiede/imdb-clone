import { Route, Routes } from "react-router-dom";
import MyAppBar from "./components/AppBarTop";
import Home from "./components/Home";
import Login from "./components/authentication/Login";
import Logout from "./components/authentication/Logout";
import Registration from "./components/authentication/Registration";
import YourRatings from "./components/profile/YourRatings";
import YourWatchlist from "./components/profile/YourWatchlist";
import AccountSettings from "./components/profile/AccountSettings";
import MovieSearch from "./components/movies/MovieSearch";
import { useMode, ColorModeContext } from "./theme";
import { CssBaseline, ThemeProvider } from "@mui/material";
import React from "react";
import EditMovie from "./components/movies/EditMovie";
import Messages from "./components/profile/Messages";
import { RoleNameEnum } from "./client/movies/generator-output";
import PublicRoute from "./components/routes/PublicRoute";
import PrivateRoute from "./components/routes/PrivateRoute";
import NotFound from "./components/routes/NotFound";
import MovieDetail from "./components/movies/MovieDetail";
import FilterPanel from "./components/movies/search/FilterPanel";

function App() {
  const [theme, colorMode] = useMode();

  return (
    <ColorModeContext.Provider value={colorMode}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <MyAppBar />
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/movie-search" element={<MovieSearch />} />
          <Route path="/movie" element={<MovieDetail />} />
          <Route path="/filter" element={<FilterPanel />} />
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
                <AccountSettings />
              </PrivateRoute>
            }
          />
          <Route
            path="/editing"
            element={
              <PrivateRoute role={RoleNameEnum.Admin}>
                <EditMovie />
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
