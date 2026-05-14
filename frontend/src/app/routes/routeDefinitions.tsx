import React, { lazy } from "react";
import type { ReactElement } from "react";

import { RoleNameEnum } from "../../shared/auth";
import AppLayout from "../../shared/layout/AppLayout";
import AuthLayout from "../../shared/layout/AuthLayout";
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

const appRoute = (element: ReactElement): ReactElement => (
  <AppLayout>{element}</AppLayout>
);

const authRoute = ({
  altActionLabel,
  altLabel,
  altTo,
  element,
}: {
  altActionLabel?: string;
  altLabel?: string;
  altTo?: string;
  element: ReactElement;
}): ReactElement => (
  <AuthLayout altActionLabel={altActionLabel} altLabel={altLabel} altTo={altTo}>
    {publicRoute(element)}
  </AuthLayout>
);

export const routeDefinitions: RouteDefinition[] = [
  { path: "/", element: appRoute(<HomePage />) },
  { path: "/movie-search", element: appRoute(<MovieSearchPage />) },
  { path: "/movie", element: appRoute(<MovieDetailPage />) },
  { path: "/filter", element: appRoute(<FilterPanelPage />) },
  {
    path: "/login",
    element: authRoute({
      altActionLabel: "Sign up",
      altLabel: "Need an account?",
      altTo: "/registration",
      element: <LoginPage />,
    }),
  },
  {
    path: "/registration",
    element: authRoute({
      altActionLabel: "Sign in",
      altLabel: "Already have an account?",
      altTo: "/login",
      element: <RegistrationPage />,
    }),
  },
  {
    path: "/your-ratings",
    element: appRoute(privateRoute(RoleNameEnum.User, <YourRatingsPage />)),
  },
  {
    path: "/your-watchlist",
    element: appRoute(privateRoute(RoleNameEnum.User, <WatchlistPage />)),
  },
  {
    path: "/your-messages",
    element: appRoute(privateRoute(RoleNameEnum.User, <MessagesPage />)),
  },
  {
    path: "/account-settings",
    element: appRoute(privateRoute(RoleNameEnum.User, <AccountSettingsPage />)),
  },
  {
    path: "/editing",
    element: appRoute(privateRoute(RoleNameEnum.Admin, <EditMoviePage />)),
  },
  { path: "*", element: appRoute(<NotFoundPage />) },
];
