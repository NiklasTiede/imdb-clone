import { AxiosHeaders, type InternalAxiosRequestConfig } from "axios";
import { authSession } from "../auth/authSession";
import { installLocalStorageMock } from "../../test/installLocalStorageMock";
import { apiHttpClient } from "./httpClient";

describe("apiHttpClient", () => {
  beforeEach(() => {
    installLocalStorageMock();
  });

  afterEach(() => {
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
});
