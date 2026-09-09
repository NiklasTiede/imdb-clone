import { authApi } from "../../../shared/api/moviesApi";
import type { PasswordResetRequest } from "../../../client/movies/generator-output";

export const requestPasswordReset = async (email: string) => {
  await authApi.resetPassword({ email });
};

export const saveResetPassword = async (request: PasswordResetRequest) => {
  await authApi.saveNewPassword(request);
};

export const confirmEmail = async (token: string) => {
  await authApi.confirmEmailAddress({ token });
};
