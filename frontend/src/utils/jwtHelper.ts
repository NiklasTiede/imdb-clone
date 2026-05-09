import { RoleNameEnum } from "../types/roles";
import { authSession } from "../shared/auth/authSession";

export const hasUserRole = (role: RoleNameEnum) => {
  return authSession.hasRole(role);
};

export const isJwtNotExpired = () => {
  return authSession.isAuthenticated();
};

export const getUsername = () => {
  return authSession.getUsername();
};
