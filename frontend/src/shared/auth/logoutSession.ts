import { apiHttpClient } from "../api/httpClient";

export const logoutSession = async (): Promise<void> => {
  await apiHttpClient.post("/api/auth/logout");
};
