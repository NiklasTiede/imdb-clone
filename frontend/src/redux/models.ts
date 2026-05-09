import { Models } from "@rematch/core";
import { notify } from "./model/notify";

export interface RootModel extends Models<RootModel> {
  notify: typeof notify;
}

export const models: RootModel = {
  notify,
};
