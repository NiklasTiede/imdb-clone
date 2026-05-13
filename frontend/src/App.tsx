import { Route, Routes } from "react-router";
import MyAppBar from "./shared/layout/AppBarTop";
import { appTheme } from "./theme";
import { CssBaseline, ThemeProvider } from "@mui/material";
import React, { lazy, Suspense } from "react";
import { RoleNameEnum } from "./shared/auth";
import { NotFoundPage, PrivateRoute, PublicRoute } from "./app/routes";
import PageContent from "./shared/layout/PageContent";
import Surface from "./shared/layout/Surface";

const lazyRoute = <T extends Record<string, unknown>, K extends keyof T>(
  loadModule: () => Promise<T>,
  exportName: K,
) =>
  lazy(async () => ({
    default: (await loadModule())[exportName] as React.ComponentType,
  }));

const HomePage = lazyRoute(() => import("./features/home"), "HomePage");
const LoginPage = lazyRoute(() => import("./features/identity"), "LoginPage");
const LogoutPage = lazyRoute(() => import("./features/identity"), "LogoutPage");
const RegistrationPage = lazyRoute(
  () => import("./features/identity"),
  "RegistrationPage",
);
const AccountSettingsPage = lazyRoute(
  () => import("./features/account"),
  "AccountSettingsPage",
);
const EditMoviePage = lazyRoute(
  () => import("./features/catalog"),
  "EditMoviePage",
);
const MovieDetailPage = lazyRoute(
  () => import("./features/catalog"),
  "MovieDetailPage",
);
const WatchlistPage = lazyRoute(
  () => import("./features/engagement"),
  "WatchlistPage",
);
const YourRatingsPage = lazyRoute(
  () => import("./features/engagement"),
  "YourRatingsPage",
);
const MessagesPage = lazyRoute(
  () => import("./features/notification"),
  "MessagesPage",
);
const FilterPanelPage = lazyRoute(
  () => import("./features/search"),
  "FilterPanelPage",
);
const MovieSearchPage = lazyRoute(
  () => import("./features/search"),
  "MovieSearchPage",
);

function App() {
  return (
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      <MyAppBar />
      <Suspense fallback={<RouteFallback />}>
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
                <MessagesPage />
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
      </Suspense>
    </ThemeProvider>
  );
}

const RouteFallback = () => (
  <PageContent maxWidth="760px">
    <Surface sx={{ p: 3, mt: 4 }}>Loading...</Surface>
  </PageContent>
);

export default App;
