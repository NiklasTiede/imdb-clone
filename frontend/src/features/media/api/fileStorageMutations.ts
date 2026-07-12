import { fileStorageApi } from "../../../shared/api/moviesApi";
import { accountQueryKeys } from "../../../shared/api/accountProfileQueries";

export const mediaMutationKeys = {
  currentProfile: accountQueryKeys.currentProfile,
};

export const storeUserProfilePhoto = async (
  image: File,
): Promise<Array<string>> => {
  const response = await fileStorageApi.storeUserProfilePhoto(image);
  return response.data;
};

export const deleteUserProfilePhoto = async (): Promise<void> => {
  await fileStorageApi.deleteUserProfilePhoto();
};
