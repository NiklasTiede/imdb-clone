import moment from "moment";
import { RoleNameEnum } from "../types/roles";

export const hasUserRole = (role: RoleNameEnum) => {
  const roles = window.localStorage.getItem("rolesFromJwt");
  return roles?.indexOf(role) !== undefined ? roles?.indexOf(role) > -1 : false;
};

export const isJwtNotExpired = () => {
  let isNotExpired = false;
  const jwtExpiresAt = window.localStorage.getItem("jwtExpiresAt");
  if (jwtExpiresAt !== null) {
    isNotExpired = moment.unix(parseInt(jwtExpiresAt)).isAfter(moment.now());
  }
  return isNotExpired;
};

export const getUsername = () => {
  return window.localStorage.getItem("username");
};
