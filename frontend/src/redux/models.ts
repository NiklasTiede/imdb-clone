import { Models } from "@rematch/core";
import { authentication } from "./model/authentication";
import { account } from "./model/account";
import { movies } from "./model/movies";
import { search } from "./model/search";
import { notify } from "./model/notify";

export interface RootModel extends Models<RootModel> {
  authentication: typeof authentication;
  account: typeof account;
  movies: typeof movies;
  search: typeof search;
  notify: typeof notify;
}

export const models: RootModel = {
  authentication,
  account,
  movies,
  search,
  notify,
};
