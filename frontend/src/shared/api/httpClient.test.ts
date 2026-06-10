import { AxiosHeaders, type InternalAxiosRequestConfig } from "axios";
import {
  configurePerformanceReporter,
  resetPerformanceReporterForTests,
} from "../observability/performanceReporter";
import { apiHttpClient } from "./httpClient";

describe("apiHttpClient", () => {
  afterEach(() => {
    resetPerformanceReporterForTests();
  });

  it("does not expose an authorization header to outgoing requests", async () => {
    let capturedConfig: InternalAxiosRequestConfig | undefined;

    await apiHttpClient.get("/public-resource", {
      adapter: async (config) => {
        capturedConfig = config;
        return {
          config,
          data: {},
          headers: {},
          status: 200,
          statusText: "OK",
        };
      },
    });

    expect(capturedConfig).toBeDefined();
    expect(new AxiosHeaders(capturedConfig?.headers).has("Authorization")).toBe(
      false,
    );
  });

  it("uses the Spring CSRF cookie and header names", () => {
    expect(apiHttpClient.defaults.xsrfCookieName).toBe("XSRF-TOKEN");
    expect(apiHttpClient.defaults.xsrfHeaderName).toBe("X-XSRF-TOKEN");
  });

  it("reports successful API request timing without query parameters", async () => {
    const events: unknown[] = [];
    configurePerformanceReporter({
      report: (event) => {
        events.push(event);
      },
    });

    await apiHttpClient.get("/movies/search?q=matrix", {
      adapter: async (config) => ({
        config,
        data: {},
        headers: {},
        status: 200,
        statusText: "OK",
      }),
    });

    expect(events).toEqual([
      expect.objectContaining({
        method: "GET",
        name: "api_request",
        status: 200,
        success: true,
        type: "api_request",
        url: "/movies/search",
      }),
    ]);
  });

  it("reports failed API request timing without request or response payloads", async () => {
    const events: unknown[] = [];
    configurePerformanceReporter({
      report: (event) => {
        events.push(event);
      },
    });

    await expect(
      apiHttpClient.get("/movies/42?token=secret", {
        adapter: async (config) =>
          Promise.reject({
            config,
            isAxiosError: true,
            message: "Request failed",
            response: {
              config,
              data: { token: "secret" },
              headers: {},
              status: 500,
              statusText: "Server Error",
            },
            toJSON: () => ({}),
          }),
      }),
    ).rejects.toBeDefined();

    expect(events).toEqual([
      expect.objectContaining({
        failureKind: "http",
        method: "GET",
        status: 500,
        success: false,
        type: "api_request",
        url: "/movies/42",
      }),
    ]);
    expect(JSON.stringify(events[0])).not.toContain("secret");
  });
});
