import {accountApi} from "../../client/movies/MoviesApi";
import {createModel} from "@rematch/core";
import {RootModel} from "../models";
import {AccountSummaryResponse,} from "../../client/movies/generator-output";
import {AxiosRequestConfig, AxiosResponse} from "axios";


export const account = createModel<RootModel>()({
    state: {
        isLoading: false,
        loaded: false,
        accountSummary: {}
    },
    reducers: {
        startLoading: (state) => reduce(state, {
            isLoading: true,
        }),
        stopLoading: (state) => reduce(state, {
            isLoading: false,
            loaded: true,
        }),
        setCurrentUser: (state, payload: AccountSummaryResponse) => reduce(state, {
            accountSummary: payload,
        }),
    },
    effects: (dispatch) => ({
        async getCurrentAccount() {
            const options: AxiosRequestConfig = {
                headers: {'Authorization': 'Bearer ' + window.localStorage.getItem('jwtToken')}
            };
            accountApi.getCurrentAccount(options)
                .then((response: AxiosResponse<AccountSummaryResponse>) => {
                    if (response.status === 200 && response.data !== null) {
                        dispatch.account.setCurrentUser(response.data)
                    }
                })
                .catch((reason: any) => {
                    // log
                    // notification
                    console.log(reason)
                })
        },

    }),
})

export const reduce = <T>(s: T, v: any) => Object.assign({}, s, v);
