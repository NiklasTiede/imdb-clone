import { authSession } from "./authSession";
import { RoleNameEnum } from "./roles";

export const hasUserRole = (role: RoleNameEnum) => {
  return authSession.hasRole(role);
};

export const isAuthenticated = () => {
  return authSession.isAuthenticated();
};

export const isAuthBootstrapComplete = () => {
  return authSession.isBootstrapped();
};

export const getUsername = () => {
  return authSession.getUsername();
};
