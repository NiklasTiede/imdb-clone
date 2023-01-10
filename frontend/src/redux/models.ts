import { Models } from "@rematch/core";
import { movies } from "./model/movies";
import { authentication } from "./model/authentication";
import { account } from "./model/account";
import { notify } from "./model/notify";

export interface RootModel extends Models<RootModel> {
  movies: typeof movies;
  authentication: typeof authentication;
  account: typeof account;
  notify: typeof notify;
}

export const models: RootModel = {
  movies,
  authentication,
  account,
  notify,
};
