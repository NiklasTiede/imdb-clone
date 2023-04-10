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
import { i18n } from "../../i18n";

export type State = {
  accountProfile: AccountProfile;
  username: string;
};

export const account = createModel<RootModel>()({
  state: {
    accountProfile: {
      firstName: "",
      lastName: "",
      username: "",
      email: "",
      birthday: "",
      bio: "",
      phone: "",
      imageUrlToken: "",
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
      accountApi
        .getCurrentAccount()
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
          dispatch.notify.error(
            i18n.accountSettings.loadingError("get current User")
          );
          console.log(
            `Error while attempting to get current User, reason: ${JSON.stringify(
              reason
            )}`
          );
        });
    },
    async getAccountProfileSettings(username: string) {
      accountApi
        .getAccountProfile(username)
        .then((response: AxiosResponse<AccountProfile>) => {
          if (response.status === 200 && response.data !== null) {
            dispatch.account.setAccountProfile(response.data);
          }
        })
        .catch((reason: any) => {
          dispatch.notify.error(
            i18n.accountSettings.loadingError("get Account Profile")
          );
          console.log(
            `Error while attempting to get current User, reason: ${JSON.stringify(
              reason
            )}`
          );
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
            dispatch.notify.info(i18n.accountSettings.successfulUpdate);
            console.log(i18n.accountSettings.successfulUpdate);
          } else {
            dispatch.notify.error(
              i18n.accountSettings.loadingError(
                "update Account Profile, http code not 200"
              )
            );
            console.log(
              i18n.accountSettings.loadingError(
                "update Account Profile, http code not 200"
              )
            );
          }
        })
        .catch((reason: any) => {
          dispatch.notify.error(
            i18n.accountSettings.loadingError("update Account Profile")
          );
          console.log(
            `Error while attempting to update Account Profile, reason: ${JSON.stringify(
              reason
            )}`
          );
        });
    },
  }),
});

export const reduce = <T>(s: T, v: any) => Object.assign({}, s, v);
