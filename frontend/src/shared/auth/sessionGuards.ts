import { authSession } from "./authSession";
import { RoleNameEnum } from "./roles";

export const hasUserRole = (role: RoleNameEnum) => {
  return authSession.hasRole(role);
};

export const isJwtNotExpired = () => {
  return authSession.isAuthenticated();
};

export const getUsername = () => {
  return authSession.getUsername();
};
