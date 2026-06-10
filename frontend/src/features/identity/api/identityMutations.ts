import { AxiosError } from "axios";
import {
  LoginRequest,
  MessageResponse,
  RegistrationRequest,
} from "../../../client/movies/generator-output";
import { authApi } from "../../../shared/api/moviesApi";
import { AuthSessionData } from "../../../shared/auth";

export const authenticateAccount = async (
  loginRequest: LoginRequest,
): Promise<AuthSessionData> => {
  const response = await authApi.authenticateAccount(loginRequest);
  return response.data as AuthSessionData;
};

export const registerAccount = async (
  registrationRequest: RegistrationRequest,
): Promise<MessageResponse> => {
  const response = await authApi.registerAccount(registrationRequest);
  return response.data;
};

type RegistrationErrorResponse = {
  invalidParams?: Record<string, string>;
};

export type RegistrationInvalidParams = Partial<
  Record<"email" | "username", string>
>;

export const getRegistrationInvalidParams = (
  error: unknown,
): RegistrationInvalidParams => {
  const axiosError = error as AxiosError<RegistrationErrorResponse>;
  const invalidParams = axiosError.response?.data?.invalidParams;

  return {
    email: invalidParams?.email,
    username: invalidParams?.username,
  };
};
