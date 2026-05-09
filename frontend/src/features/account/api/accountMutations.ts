import {
  AccountRecord,
  UpdatedAccountProfile,
} from "../../../client/movies/generator-output";
import { accountApi } from "../../../shared/api/moviesApi";
import { accountQueryKeys } from "./accountQueries";

export const accountMutationKeys = {
  currentProfile: accountQueryKeys.currentProfile,
};

export type UpdateAccountProfilePayload = {
  accountRecord: AccountRecord;
  username: string;
};

export const updateAccountProfile = async ({
  accountRecord,
  username,
}: UpdateAccountProfilePayload): Promise<UpdatedAccountProfile> => {
  const response = await accountApi.updateAccountProfile(
    username,
    accountRecord,
  );
  return response.data;
};
