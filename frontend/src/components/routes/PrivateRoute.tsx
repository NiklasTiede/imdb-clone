import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { RoleNameEnum } from "../../client/movies/generator-output";
import { hasUserRole, isJwtNotExpired } from "../../utils/jwtHelper";
import AccessDenied from "./AccessDenied";

const PrivateRoute = ({
  role,
  children,
}: {
  role: RoleNameEnum;
  children: JSX.Element;
}) => {
  const location = useLocation();
  const isLoggedIn: boolean = isJwtNotExpired();
  const hasRole: boolean = hasUserRole(role);

  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location }} />;
  }
  if (isLoggedIn && !hasRole) {
    return <AccessDenied role={role} />;
  }
  return children;
};

export default PrivateRoute;
