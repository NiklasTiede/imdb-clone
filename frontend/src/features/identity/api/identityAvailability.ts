import type { UserIdentityAvailability } from "../../../client/movies/generator-output";
import { authApi } from "../../../shared/api/moviesApi";

export const checkUsernameAvailability = async (
  username: string,
): Promise<UserIdentityAvailability> => {
  const response = await authApi.checkUsernameAvailability(username);
  return response.data;
};

export const checkEmailAvailability = async (
  email: string,
): Promise<UserIdentityAvailability> => {
  const response = await authApi.checkEmailAvailability(email);
  return response.data;
};
