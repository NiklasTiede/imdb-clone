import { createModel } from "@rematch/core";
import { RootModel } from "../models";
import {
  LoginRequest,
  LoginResponse,
  MessageResponse,
  UserIdentityAvailability,
} from "../../client/movies/generator-output";
import { authApi } from "../../client/movies/MoviesApi";
import { AxiosResponse } from "axios";
import jwt_decode, { JwtPayload } from "jwt-decode";
import { i18n } from "../../i18n";

interface MyJwtPayload extends JwtPayload {
  roles: string;
}

export type State = {
  isEmailAvailable: boolean;
  isUsernameAvailable: boolean;
  isAuthenticated: boolean;
};

export const authentication = createModel<RootModel>()({
  state: {
    isEmailAvailable: false,
    isUsernameAvailable: false,
    isAuthenticated: false,
  } as State,
  reducers: {
    isEmailAvailable: (state, payload: boolean) =>
      reduce(state, {
        isEmailAvailable: payload,
      }),
    isUsernameAvailable: (state, payload: boolean) =>
      reduce(state, {
        isUsernameAvailable: payload,
      }),
    setIsAuthenticated: (state, payload: boolean) =>
      reduce(state, {
        isAuthenticated: payload,
      }),
  },
  effects: (dispatch) => ({
    async checkEmailAvailability(email: string) {
      authApi
        .checkEmailAvailability(email)
        .then((response: AxiosResponse<UserIdentityAvailability>) => {
          if (response.status === 200 && response.data !== null) {
            dispatch.isEmailAvailable(response.data.isAvailable);
          }
        })
        .catch((reason: any) => {
          dispatch.notify.error(i18n.registration.loadingError);
          console.log(
            `Error while registering User, reason: ${JSON.stringify(reason)}`
          );
        });
    },
    async checkUsernameAvailability(username: string) {
      authApi
        .checkUsernameAvailability(username)
        .then((response: AxiosResponse<UserIdentityAvailability>) => {
          if (response.status === 200 && response.data !== null) {
            dispatch.isUsernameAvailable(response.data.isAvailable);
          }
        })
        .catch((reason: any) => {
          dispatch.notify.error(i18n.registration.loadingError);
          console.log(
            `Error while registering User, reason: ${JSON.stringify(reason)}`
          );
        });
    },
    async registerAccount({ registrationRequest, options }) {
      authApi
        .registerAccount(registrationRequest, options)
        .then((response: AxiosResponse<MessageResponse>) => {
          if (response.status === 200 && response.data !== null) {
            dispatch.notify.success(i18n.registration.registrationSuccessful);
            console.log(response.data.message);
          }
        })
        .catch((reason: any) => {
          dispatch.notify.error(i18n.registration.loadingError);
          console.log(
            `Error while registering User, reason: ${JSON.stringify(reason)}`
          );
        });
    },
    async authenticateAccount(loginRequest: LoginRequest) {
      authApi
        .authenticateAccount(loginRequest)
        .then((response: AxiosResponse<LoginResponse>) => {
          if (
            response.status === 200 &&
            response.data !== null &&
            response.data.accessToken !== null &&
            response.data.accessToken !== undefined
          ) {
            window.localStorage.setItem("jwtToken", response.data.accessToken);
            let decoded = jwt_decode<MyJwtPayload>(response.data.accessToken);
            window.localStorage.setItem("rolesFromJwt", decoded.roles);
            if (decoded.exp !== undefined) {
              window.localStorage.setItem(
                "jwtExpiresAt",
                decoded.exp.toString()
              );
            }
            dispatch.authentication.setIsAuthenticated(true);
            dispatch.account.getCurrentAccount();
          }
          if (response.status === 401) {
            dispatch.notify.error(i18n.login.badCredentials);
            console.log("401: Bad Credentials");
          }
        })
        .catch((reason: unknown) => {
          dispatch.notify.error(i18n.login.loadingError);
          console.log(
            `Error while attempting to login, reason: ${JSON.stringify(reason)}`
          );
        });
    },
  }),
});

export const reduce = <T>(s: T, v: any) => Object.assign({}, s, v);
