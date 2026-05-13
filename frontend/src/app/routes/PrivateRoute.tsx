import React from "react";
import { Navigate, useLocation } from "react-router";
import { hasUserRole, isJwtNotExpired, RoleNameEnum } from "../../shared/auth";
import AccessDeniedPage from "./AccessDeniedPage";

const PrivateRoute = ({
  role,
  children,
}: {
  role: RoleNameEnum;
  children: React.ReactElement;
}) => {
  const location = useLocation();
  const isLoggedIn: boolean = isJwtNotExpired();
  const hasRole: boolean = hasUserRole(role);

  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location }} />;
  }
  if (isLoggedIn && !hasRole) {
    return <AccessDeniedPage role={role} />;
  }
  return children;
};

export default PrivateRoute;
