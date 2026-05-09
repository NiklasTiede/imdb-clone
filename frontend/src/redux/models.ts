import { Models } from "@rematch/core";
import { authentication } from "./model/authentication";
import { account } from "./model/account";
import { notify } from "./model/notify";
import { fileStorage } from "./model/filestorage";

export interface RootModel extends Models<RootModel> {
  authentication: typeof authentication;
  account: typeof account;
  notify: typeof notify;
  fileStorage: typeof fileStorage;
}

export const models: RootModel = {
  authentication,
  account,
  notify,
  fileStorage,
};
