import { accountApi } from "../../client/movies/MoviesApi";
import { createModel } from "@rematch/core";
import { RootModel } from "../models";
import {
  AccountProfile,
  AccountSummaryResponse,
  UpdatedAccountProfile,
} from "../../client/movies/generator-output";
import { AxiosRequestConfig, AxiosResponse } from "axios";
import moment from "moment";

export type State = {
  isLoading: boolean;
  loaded: boolean;

  accountProfile: AccountProfile;
  username: string;
};

export const account = createModel<RootModel>()({
  state: {
    isLoading: false,
    loaded: false,

    accountProfile: {
      firstName: "",
      lastName: "",
      username: "",
      email: "",
      birthday: "",
      bio: "",
      phone: "",
      watchlistCount: 0,
      ratingsCount: 0,
      commentsCount: 0,
    },
    username: "",
  } as State,
  reducers: {
    startLoading: (state) =>
      reduce(state, {
        isLoading: true,
      }),
    stopLoading: (state) =>
      reduce(state, {
        isLoading: false,
        loaded: true,
      }),

    setAccountProfile: (state, payload: AccountProfile) =>
      reduce(state, {
        accountProfile: payload,
      }),
    setUpdateAccountProfile: (state, payload: UpdatedAccountProfile) =>
      reduce(state, {
        accountProfile: {
          ...state.accountProfile,
          firstName: payload.firstName,
          lastName: payload.lastName,
          birthday: moment(payload.birthday).toISOString(),
          phone: payload.phone,
          bio: payload.bio,
        },
      }),
    setUsername: (state, payload: string) =>
      reduce(state, {
        username: payload,
      }),
  },
  effects: (dispatch) => ({
    async getCurrentAccount() {
      const options: AxiosRequestConfig = {
        headers: {
          Authorization: "Bearer " + window.localStorage.getItem("jwtToken"),
        },
      };
      accountApi
        .getCurrentAccount(options)
        .then((response: AxiosResponse<AccountSummaryResponse>) => {
          if (
            response.status === 200 &&
            response.data !== null &&
            response.data.username
          ) {
            dispatch.account.setUsername(response.data.username);
            window.localStorage.setItem("username", response.data.username);
          }
        })
        .catch((reason: any) => {
          // log
          // notification
          console.log(reason);
        });
    },
    async getAccountProfileSettings(username: string) {
      const options: AxiosRequestConfig = {
        headers: {
          Authorization: "Bearer " + window.localStorage.getItem("jwtToken"),
        },
      };
      accountApi
        .getAccountProfile(username, options)
        .then((response: AxiosResponse<AccountProfile>) => {
          if (response.status === 200 && response.data !== null) {
            dispatch.account.setAccountProfile(response.data);
          }
        })
        .catch((reason: any) => {
          // log
          // notification
          console.log(reason);
        });
    },
    async updateAccountProfileSettings(payload) {
      const options: AxiosRequestConfig = {
        headers: {
          Authorization: "Bearer " + window.localStorage.getItem("jwtToken"),
        },
      };
      accountApi
        .updateAccountProfile(payload.username, payload.accountRecord, options)
        .then((response: AxiosResponse<UpdatedAccountProfile>) => {
          if (response.status === 200 && response.data !== null) {
            dispatch.account.setUpdateAccountProfile(response.data);
            // throw notification, to give user response!
            console.log("Account Profile was updated!");
          } else {
            console.log("warn: response was not 200!");
          }
        })
        .catch((reason: any) => {
          // log
          // notification
          console.log(reason);
        });
    },
  }),
});

export const reduce = <T>(s: T, v: any) => Object.assign({}, s, v);
