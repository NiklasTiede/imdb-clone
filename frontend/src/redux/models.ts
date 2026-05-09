import { Models } from "@rematch/core";
import { authentication } from "./model/authentication";
import { notify } from "./model/notify";

export interface RootModel extends Models<RootModel> {
  authentication: typeof authentication;
  notify: typeof notify;
}

export const models: RootModel = {
  authentication,
  notify,
};
