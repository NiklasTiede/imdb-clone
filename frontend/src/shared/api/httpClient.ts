import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { authSession } from "../auth/authSession";

export const apiHttpClient = axios.create();

apiHttpClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = authSession.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => Promise.reject(error),
);
