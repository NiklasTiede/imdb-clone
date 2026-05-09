import { authSession } from "./authSession";
import { installLocalStorageMock } from "../../test/installLocalStorageMock";

describe("authSession", () => {
  beforeEach(() => {
    installLocalStorageMock();
  });

  afterEach(() => {
    window.localStorage.clear();
  });

  it("stores and reads the current access token", () => {
    authSession.setAccessToken("test-token");

    expect(authSession.getAccessToken()).toBe("test-token");
  });

  it("stores decoded login session values", () => {
    authSession.setSession({
      accessToken: "test-token",
      roles: "ROLE_USER,ROLE_ADMIN",
      username: "test_user",
      expiresAt: 9999999999,
    });

    expect(authSession.getAccessToken()).toBe("test-token");
    expect(authSession.getUsername()).toBe("test_user");
    expect(authSession.hasRole("ROLE_ADMIN")).toBe(true);
    expect(authSession.hasRole("ROLE_USER")).toBe(true);
    expect(authSession.hasRole("ROLE_OTHER")).toBe(false);
    expect(authSession.isAuthenticated()).toBe(true);
  });

  it("reports expired sessions as unauthenticated", () => {
    authSession.setSession({
      accessToken: "test-token",
      roles: "ROLE_USER",
      username: "test_user",
      expiresAt: 1,
    });

    expect(authSession.isAuthenticated()).toBe(false);
  });

  it("clears all authentication values", () => {
    authSession.setAccessToken("test-token");
    window.localStorage.setItem("rolesFromJwt", "ROLE_USER");
    window.localStorage.setItem("jwtExpiresAt", "9999999999");
    window.localStorage.setItem("username", "test_user");

    authSession.clear();

    expect(authSession.getAccessToken()).toBeNull();
    expect(window.localStorage.getItem("rolesFromJwt")).toBeNull();
    expect(window.localStorage.getItem("jwtExpiresAt")).toBeNull();
    expect(window.localStorage.getItem("username")).toBeNull();
  });

  it("notifies subscribers when the session changes", () => {
    const listener = vi.fn();

    const unsubscribe = authSession.subscribe(listener);
    authSession.setSession({
      accessToken: "test-token",
      roles: "ROLE_USER",
      username: "test_user",
      expiresAt: 9999999999,
    });
    authSession.clear();
    unsubscribe();
    authSession.setAccessToken("another-token");

    expect(listener).toHaveBeenCalledTimes(2);
  });
});
