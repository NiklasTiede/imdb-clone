import moment from "moment";

const ROLES = {
  ROLE_ADMIN: "ROLE_ADMIN",
  ROLE_USER: "ROLE_USER",
} as const;

export const isUserAdmin = () => {
  let roles = window.localStorage.getItem("rolesFromJwt");
  return roles?.indexOf(ROLES.ROLE_ADMIN) !== undefined
    ? roles?.indexOf(ROLES.ROLE_ADMIN) > -1
    : false;
};

export const isJwtNotExpired = () => {
  let isNotExpired = false;
  let jwtExpiresAt = window.localStorage.getItem("jwtExpiresAt");
  if (jwtExpiresAt !== null) {
    isNotExpired = moment.unix(parseInt(jwtExpiresAt)).isAfter(moment.now());
  }
  return isNotExpired;
};

export const getUsername = () => {
  return window.localStorage.getItem("username");
};
