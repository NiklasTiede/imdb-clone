import axios, {
  AxiosError,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { authSession } from "../auth/authSession";
import { createPerformanceEventContext } from "../observability/config";
import { reportPerformanceEvent } from "../observability/performanceReporter";
import { sanitizeUrlForTelemetry } from "../observability/urlSanitizer";

type TimedAxiosRequestConfig = InternalAxiosRequestConfig & {
  metadata?: {
    requestStartedAt: number;
  };
};

export const apiHttpClient = axios.create();

apiHttpClient.defaults.xsrfCookieName = "XSRF-TOKEN";
apiHttpClient.defaults.xsrfHeaderName = "X-XSRF-TOKEN";

const classifyFailure = (
  error: AxiosError | undefined,
  status: number | undefined,
): "network" | "http" | "unknown" => {
  if (status !== undefined) {
    return "http";
  }

  if (error?.request) {
    return "network";
  }

  return "unknown";
};

const reportApiTiming = ({
  config,
  error,
  response,
}: {
  config?: TimedAxiosRequestConfig;
  error?: AxiosError;
  response?: AxiosResponse;
}): void => {
  const startedAt = config?.metadata?.requestStartedAt ?? performance.now();
  const finishedAt = performance.now();
  const status = response?.status ?? error?.response?.status;
  const success = response !== undefined;

  reportPerformanceEvent({
    context: createPerformanceEventContext(window.location.pathname),
    ...(success ? {} : { failureKind: classifyFailure(error, status) }),
    method: (config?.method ?? "GET").toUpperCase(),
    name: "api_request",
    ...(status === undefined ? {} : { status }),
    success,
    timestamp: finishedAt,
    type: "api_request",
    url: sanitizeUrlForTelemetry(config?.url),
    value: Math.max(0, finishedAt - startedAt),
  });
};

apiHttpClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const timedConfig = config as TimedAxiosRequestConfig;
    timedConfig.metadata = { requestStartedAt: performance.now() };
    return timedConfig;
  },
  (error: AxiosError) => Promise.reject(error),
);

apiHttpClient.interceptors.response.use(
  (response) => {
    reportApiTiming({
      config: response.config as TimedAxiosRequestConfig,
      response,
    });
    return response;
  },
  (error: AxiosError) => {
    reportApiTiming({
      error,
      ...(error.config === undefined
        ? {}
        : { config: error.config as TimedAxiosRequestConfig }),
    });
    if (error.response?.status === 401) {
      authSession.clear();
    }
    return Promise.reject(error);
  },
);
