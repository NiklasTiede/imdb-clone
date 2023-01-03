import { accountApi } from "../../client/movies/MoviesApi";
import { createModel } from "@rematch/core";
import { RootModel } from "../models";
import {
  Account,
  AccountProfile,
  AccountSummaryResponse,
} from "../../client/movies/generator-output";
import { AxiosRequestConfig, AxiosResponse } from "axios";

export type State = {
  isLoading: boolean;
  loaded: boolean;
  accountSummary: AccountSummaryResponse;
  accountProfile: AccountProfile;
  updatedAccountProfile: Account;
};

export const account = createModel<RootModel>()({
  state: {
    isLoading: false,
    loaded: false,
    accountSummary: {},
    accountProfile: {},
    updatedAccountProfile: {},
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
    setCurrentUser: (state, payload: AccountSummaryResponse) =>
      reduce(state, {
        accountSummary: payload,
      }),
    setAccountProfile: (state, payload: AccountProfile) =>
      reduce(state, {
        accountProfile: payload,
      }),
    setUpdatedAccountProfile: (state, payload: Account) =>
      reduce(state, {
        updatedAccountProfile: payload,
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
          if (response.status === 200 && response.data !== null) {
            dispatch.account.setCurrentUser(response.data);
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
    async updateAccountProfileSettings(username: string, accountRecord: any) {
      const options: AxiosRequestConfig = {
        headers: {
          Authorization: "Bearer " + window.localStorage.getItem("jwtToken"),
        },
      };
      accountApi
        .updateAccount(username, accountRecord, options)
        .then((response: AxiosResponse<Account>) => {
          if (response.status === 200 && response.data !== null) {
            dispatch.account.setUpdatedAccountProfile(response.data);
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
