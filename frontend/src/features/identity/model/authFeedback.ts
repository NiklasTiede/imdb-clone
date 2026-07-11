import type { AlertColor } from "@mui/material/Alert";
import type { AxiosError } from "axios";

export type AuthFeedback = {
  message: string;
  severity: AlertColor;
};

export const socialLoginFailure: AuthFeedback = {
  message:
    "Social sign-in could not be completed. Try again or choose another method.",
  severity: "error",
};

export const getPasswordLoginFeedback = (error: unknown): AuthFeedback => {
  const status = getHttpStatus(error);
  if (status === 401) {
    return {
      message: "Email/username or password is incorrect.",
      severity: "error",
    };
  }
  if (status === 429) {
    return {
      message: "Too many sign-in attempts. Wait a moment and try again.",
      severity: "warning",
    };
  }
  return {
    message: "We could not sign you in. Check your connection and try again.",
    severity: "error",
  };
};

export const getPasskeyFeedback = (error: unknown): AuthFeedback => {
  if (isPasskeyCancellation(error)) {
    return {
      message:
        "Passkey sign-in was canceled or no matching passkey was available.",
      severity: "info",
    };
  }
  if (getHttpStatus(error) === 429) {
    return {
      message: "Too many passkey attempts. Wait a moment and try again.",
      severity: "warning",
    };
  }
  return {
    message: "Passkey sign-in could not be completed. Try again.",
    severity: "error",
  };
};

export const getRegistrationFeedback = (): AuthFeedback => ({
  message:
    "We could not create your account. Check your connection and try again.",
  severity: "error",
});

const getHttpStatus = (error: unknown): number | undefined =>
  (error as AxiosError | undefined)?.response?.status;

const isPasskeyCancellation = (error: unknown): boolean =>
  error instanceof DOMException && error.name === "NotAllowedError";
