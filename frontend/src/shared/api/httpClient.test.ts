import { AxiosHeaders, type InternalAxiosRequestConfig } from "axios";
import { authSession } from "../auth/authSession";
import { installLocalStorageMock } from "../../test/installLocalStorageMock";
import {
  configurePerformanceReporter,
  resetPerformanceReporterForTests,
} from "../observability/performanceReporter";
import { apiHttpClient } from "./httpClient";

describe("apiHttpClient", () => {
  beforeEach(() => {
    installLocalStorageMock();
  });

  afterEach(() => {
    resetPerformanceReporterForTests();
    window.localStorage.clear();
  });

  it("adds the bearer token to outgoing requests", async () => {
    authSession.setAccessToken("test-token");
    let capturedConfig: InternalAxiosRequestConfig | undefined;

    await apiHttpClient.get("/secure-resource", {
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
    expect(new AxiosHeaders(capturedConfig?.headers).get("Authorization")).toBe(
      "Bearer test-token",
    );
  });

  it("does not add an authorization header when no token exists", async () => {
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
