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
});
