import { jwtDecode, JwtPayload } from "jwt-decode";
import { AxiosError } from "axios";
import {
  LoginRequest,
  LoginResponse,
  MessageResponse,
  RegistrationRequest,
} from "../../../client/movies/generator-output";
import { authApi } from "../../../shared/api/moviesApi";
import { AuthSessionData } from "../../../shared/auth/authSession";

interface ImdbJwtPayload extends JwtPayload {
  roles?: string;
  username?: string;
}

export const createAuthSession = (
  loginResponse: LoginResponse,
): AuthSessionData => {
  if (!loginResponse.accessToken) {
    throw new Error("Login response did not include an access token");
  }

  const decoded = jwtDecode<ImdbJwtPayload>(loginResponse.accessToken);
  return {
    accessToken: loginResponse.accessToken,
    expiresAt: decoded.exp,
    roles: decoded.roles,
    username: decoded.username,
  };
};

export const authenticateAccount = async (
  loginRequest: LoginRequest,
): Promise<AuthSessionData> => {
  const response = await authApi.authenticateAccount(loginRequest);
  return createAuthSession(response.data);
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
