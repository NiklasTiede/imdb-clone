import React from "react";
import { Navigate, useLocation } from "react-router";
import {
  hasUserRole,
  RoleNameEnum,
  useAuthSessionSnapshot,
} from "../../shared/auth";
import AccessDeniedPage from "./AccessDeniedPage";

const PrivateRoute = ({
  role,
  children,
}: {
  role: RoleNameEnum;
  children: React.ReactElement;
}) => {
  const location = useLocation();
  const { bootstrapped, session } = useAuthSessionSnapshot();
  const isLoggedIn = Boolean(session);
  const hasRole: boolean = hasUserRole(role);

  if (!bootstrapped) {
    return null;
  }
  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location }} />;
  }
  if (isLoggedIn && !hasRole) {
    return <AccessDeniedPage role={role} />;
  }
  return children;
};

export default PrivateRoute;
