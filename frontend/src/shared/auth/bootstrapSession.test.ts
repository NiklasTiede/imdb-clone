import {
  AxiosHeaders,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { apiHttpClient } from "../api/httpClient";
import { authSession } from "./authSession";
import { bootstrapSession } from "./bootstrapSession";

const response = (data: unknown): AxiosResponse<unknown> => ({
  config: { headers: new AxiosHeaders() } as InternalAxiosRequestConfig,
  data,
  headers: new AxiosHeaders(),
  status: 200,
  statusText: "OK",
});

describe("bootstrapSession", () => {
  beforeEach(() => {
    authSession.resetForTests();
    vi.restoreAllMocks();
  });

  afterEach(() => {
    authSession.resetForTests();
    vi.restoreAllMocks();
  });

  it("accepts a valid session response", async () => {
    vi.spyOn(apiHttpClient, "get").mockResolvedValue(
      response({
        email: "test@example.com",
        id: 1,
        roles: ["ROLE_USER"],
        username: "test_user",
      }),
    );

    await bootstrapSession();

    expect(authSession.isBootstrapped()).toBe(true);
    expect(authSession.isAuthenticated()).toBe(true);
    expect(authSession.getUsername()).toBe("test_user");
  });

  it("rejects an invalid session response before it reaches auth state", async () => {
    vi.spyOn(apiHttpClient, "get").mockResolvedValue(
      response({
        id: "1",
        roles: ["ROLE_USER"],
        username: "test_user",
      }),
    );

    await bootstrapSession();

    expect(authSession.isBootstrapped()).toBe(true);
    expect(authSession.isAuthenticated()).toBe(false);
    expect(authSession.getUsername()).toBeNull();
  });
});
