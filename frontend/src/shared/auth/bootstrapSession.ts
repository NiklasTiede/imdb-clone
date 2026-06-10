import { apiHttpClient } from "../api/httpClient";
import { authSession, type AccountSessionResponse } from "./authSession";

let bootstrapPromise: Promise<void> | null = null;

export const bootstrapSession = (): Promise<void> => {
  if (bootstrapPromise) {
    return bootstrapPromise;
  }

  authSession.markBootstrapStarted();
  bootstrapPromise = apiHttpClient
    .get<AccountSessionResponse>("/api/auth/me", {
      validateStatus: (status) => status === 200 || status === 401,
    })
    .then((response) => {
      authSession.completeBootstrap(
        response.status === 200 ? response.data : null,
      );
    })
    .catch(() => {
      authSession.completeBootstrap(null);
    })
    .finally(() => {
      bootstrapPromise = null;
    });

  return bootstrapPromise;
};
