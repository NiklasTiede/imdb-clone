import React, { lazy } from "react";
import type { ReactElement } from "react";

import { RoleNameEnum } from "../../shared/auth";
import NotFoundPage from "./NotFoundPage";
import PrivateRoute from "./PrivateRoute";
import PublicRoute from "./PublicRoute";

const lazyRoute = <T extends Record<string, unknown>, K extends keyof T>(
  loadModule: () => Promise<T>,
  exportName: K,
) =>
  lazy(async () => ({
    default: (await loadModule())[exportName] as React.ComponentType,
  }));

const HomePage = lazyRoute(() => import("../../features/home"), "HomePage");
const LoginPage = lazyRoute(
  () => import("../../features/identity"),
  "LoginPage",
);
const LogoutPage = lazyRoute(
  () => import("../../features/identity"),
  "LogoutPage",
);
const RegistrationPage = lazyRoute(
  () => import("../../features/identity"),
  "RegistrationPage",
);
const AccountSettingsPage = lazyRoute(
  () => import("../../features/account"),
  "AccountSettingsPage",
);
const EditMoviePage = lazyRoute(
  () => import("../../features/catalog"),
  "EditMoviePage",
);
const MovieDetailPage = lazyRoute(
  () => import("../../features/catalog"),
  "MovieDetailPage",
);
const WatchlistPage = lazyRoute(
  () => import("../../features/engagement"),
  "WatchlistPage",
);
const YourRatingsPage = lazyRoute(
  () => import("../../features/engagement"),
  "YourRatingsPage",
);
const MessagesPage = lazyRoute(
  () => import("../../features/notification"),
  "MessagesPage",
);
const FilterPanelPage = lazyRoute(
  () => import("../../features/search"),
  "FilterPanelPage",
);
const MovieSearchPage = lazyRoute(
  () => import("../../features/search"),
  "MovieSearchPage",
);

type RouteDefinition = {
  element: ReactElement;
  path: string;
};

const privateRoute = (
  role: RoleNameEnum,
  element: ReactElement,
): ReactElement => <PrivateRoute role={role}>{element}</PrivateRoute>;

const publicRoute = (element: ReactElement): ReactElement => (
  <PublicRoute>{element}</PublicRoute>
);

export const routeDefinitions: RouteDefinition[] = [
  { path: "/", element: <HomePage /> },
  { path: "/movie-search", element: <MovieSearchPage /> },
  { path: "/movie", element: <MovieDetailPage /> },
  { path: "/filter", element: <FilterPanelPage /> },
  { path: "/login", element: <LoginPage /> },
  { path: "/registration", element: publicRoute(<RegistrationPage />) },
  { path: "/logout", element: publicRoute(<LogoutPage />) },
  {
    path: "/your-ratings",
    element: privateRoute(RoleNameEnum.User, <YourRatingsPage />),
  },
  {
    path: "/your-watchlist",
    element: privateRoute(RoleNameEnum.User, <WatchlistPage />),
  },
  {
    path: "/your-messages",
    element: privateRoute(RoleNameEnum.User, <MessagesPage />),
  },
  {
    path: "/account-settings",
    element: privateRoute(RoleNameEnum.User, <AccountSettingsPage />),
  },
  {
    path: "/editing",
    element: privateRoute(RoleNameEnum.Admin, <EditMoviePage />),
  },
  { path: "*", element: <NotFoundPage /> },
];
