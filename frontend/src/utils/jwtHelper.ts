import moment from "moment";
import { RoleNameEnum } from "../client/movies/generator-output";

export const hasUserRole = (role: RoleNameEnum) => {
  let roles = window.localStorage.getItem("rolesFromJwt");
  return roles?.indexOf(role) !== undefined ? roles?.indexOf(role) > -1 : false;
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
