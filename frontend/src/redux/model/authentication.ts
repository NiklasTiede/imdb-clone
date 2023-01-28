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
import { RegisterRequest } from "../../components/authentication/Registration";

interface MyJwtPayload extends JwtPayload {
  roles: string;
}

export type State = {
  isEmailAvailable: boolean;
  isUsernameAvailable: boolean | null;
  updateSwitch: boolean;
  isAuthenticated: boolean;
  registrationCompleted: boolean;
};

export const authentication = createModel<RootModel>()({
  state: {
    isEmailAvailable: false,
    isUsernameAvailable: null,
    updateSwitch: false,
    isAuthenticated: false,
    registrationCompleted: false,
  } as State,
  reducers: {
    isEmailAvailable: (state, payload: boolean) =>
      reduce(state, {
        isEmailAvailable: payload,
        updateSwitch: !state.updateSwitch,
      }),
    isUsernameAvailable: (state, payload: boolean | null) =>
      reduce(state, {
        isUsernameAvailable: payload,
        updateSwitch: !state.updateSwitch,
      }),
    setIsAuthenticated: (state, payload: boolean) =>
      reduce(state, {
        isAuthenticated: payload,
      }),
    setRegistrationCompleted: (state, payload: boolean) =>
      reduce(state, {
        registrationCompleted: payload,
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
            `Error while validating email, reason: ${JSON.stringify(reason)}`
          );
        });
    },
    async checkUsernameAvailability(username: string) {
      authApi
        .checkUsernameAvailability(username)
        .then((response: AxiosResponse<UserIdentityAvailability>) => {
          if (
            response.status === 200 &&
            response.data !== null &&
            response.data.isAvailable !== undefined
          ) {
            dispatch.authentication.isUsernameAvailable(
              response.data.isAvailable
            );
          }
        })
        .catch((reason: any) => {
          dispatch.notify.error(i18n.registration.loadingError);
          console.log(
            `Error while validating username, reason: ${JSON.stringify(reason)}`
          );
        });
    },
    async registerAccount(request: RegisterRequest) {
      const setError = request.error;
      authApi
        .registerAccount(request.payload)
        .then((response: AxiosResponse<MessageResponse>) => {
          if (response.status === 201) {
            dispatch.authentication.setRegistrationCompleted(true);
            dispatch.notify.success(i18n.registration.registrationSuccessful);
          }
        })
        .catch(function (error) {
          if (
            error.response.status == 400 &&
            error.response.data &&
            error.response.data.invalidParams
          ) {
            if (error.response.data.invalidParams["email"]) {
              setError("email", {
                type: "custom",
                message: error.response.data.invalidParams["email"],
              });
            }
            if (error.response.data.invalidParams["username"]) {
              setError("username", {
                type: "custom",
                message: error.response.data.invalidParams["username"],
              });
            }
          } else {
            dispatch.notify.success(i18n.registration.loadingError);
            console.log(`Error while attempting to login`);
          }
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
