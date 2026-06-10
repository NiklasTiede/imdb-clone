import { Navigate, useLocation } from "react-router";
import React from "react";
import { useAuthSessionSnapshot } from "../../shared/auth";

const PublicRoute = ({ children }: { children: React.ReactElement }) => {
  const location = useLocation();
  const { bootstrapped, session } = useAuthSessionSnapshot();
  const isLoggedIn = Boolean(session);

  if (!bootstrapped) {
    return null;
  }
  if (isLoggedIn) {
    return <Navigate to="/" state={{ from: location }} />;
  }
  return children;
};

export default PublicRoute;
