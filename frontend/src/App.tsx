import { Route, Routes } from "react-router";
import MyAppBar from "./shared/layout/AppBarTop";
import HomePage from "./pages/HomePage";
import { LoginPage, LogoutPage, RegistrationPage } from "./features/identity";
import { AccountSettingsPage } from "./features/account";
import { EditMoviePage, MovieDetailPage } from "./features/catalog";
import { WatchlistPage, YourRatingsPage } from "./features/engagement";
import { FilterPanelPage, MovieSearchPage } from "./features/search";
import { appTheme } from "./theme";
import { CssBaseline, ThemeProvider } from "@mui/material";
import React from "react";
import Messages from "./components/profile/Messages";
import { RoleNameEnum } from "./types/roles";
import { NotFoundPage, PrivateRoute, PublicRoute } from "./app/routes";

function App() {
  return (
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      <MyAppBar />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/movie-search" element={<MovieSearchPage />} />
        <Route path="/movie" element={<MovieDetailPage />} />
        <Route path="/filter" element={<FilterPanelPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/registration"
          element={
            <PublicRoute>
              <RegistrationPage />
            </PublicRoute>
          }
        />
        <Route
          path="/logout"
          element={
            <PublicRoute>
              <LogoutPage />
            </PublicRoute>
          }
        />
        <Route
          path="/your-ratings"
          element={
            <PrivateRoute role={RoleNameEnum.User}>
              <YourRatingsPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/your-watchlist"
          element={
            <PrivateRoute role={RoleNameEnum.User}>
              <WatchlistPage />
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
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </ThemeProvider>
  );
}

export default App;
