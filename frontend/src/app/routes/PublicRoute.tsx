import { Navigate, useLocation } from "react-router";
import React from "react";
import { isJwtNotExpired } from "../../utils/jwtHelper";

const PublicRoute = ({ children }: { children: React.ReactElement }) => {
  const location = useLocation();
  const isLoggedIn: boolean = isJwtNotExpired();

  if (isLoggedIn) {
    return <Navigate to="/" state={{ from: location }} />;
  }
  return children;
};

export default PublicRoute;
