// @filename: models.ts
import { Models } from "@rematch/core"
import { movies } from "./model/movies"
import {authentication} from "./model/authentication";
import {account} from "./model/account";

export interface RootModel extends Models<RootModel> {
  movies: typeof movies,
  authentication: typeof authentication,
  account: typeof account
}

export const models: RootModel = {
  movies,
  authentication,
  account,

}
