import { authSession, parseAccountSessionResponse } from "./authSession";

describe("parseAccountSessionResponse", () => {
  it("accepts a complete authenticated session and removes unknown fields", () => {
    expect(
      parseAccountSessionResponse({
        email: "test@example.com",
        id: 1,
        ignored: "transport detail",
        roles: ["ROLE_USER"],
        username: "test_user",
      }),
    ).toEqual({
      email: "test@example.com",
      id: 1,
      roles: ["ROLE_USER"],
      username: "test_user",
    });
  });

  it("rejects incomplete or malformed authenticated sessions", () => {
    expect(() =>
      parseAccountSessionResponse({
        id: "1",
        roles: ["ROLE_USER"],
        username: "test_user",
      }),
    ).toThrow();
  });
});

describe("authSession", () => {
  beforeEach(() => {
    authSession.resetForTests();
  });

  afterEach(() => {
    authSession.resetForTests();
  });

  it("stores session values in memory", () => {
    authSession.setSession({
      id: 1,
      email: "test@example.com",
      roles: ["ROLE_USER", "ROLE_ADMIN"],
      username: "test_user",
    });

    expect(authSession.getUsername()).toBe("test_user");
    expect(authSession.hasRole("ROLE_ADMIN")).toBe(true);
    expect(authSession.hasRole("ROLE_USER")).toBe(true);
    expect(authSession.hasRole("ROLE_OTHER")).toBe(false);
    expect(authSession.isAuthenticated()).toBe(true);
  });

  it("tracks bootstrap completion separately from authentication", () => {
    expect(authSession.isBootstrapped()).toBe(false);

    authSession.completeBootstrap(null);

    expect(authSession.isBootstrapped()).toBe(true);
    expect(authSession.isAuthenticated()).toBe(false);
  });

  it("clears the in-memory session", () => {
    authSession.setSession({
      email: "test@example.com",
      id: 1,
      roles: ["ROLE_USER"],
      username: "test_user",
    });

    authSession.clear();

    expect(authSession.getUsername()).toBeNull();
    expect(authSession.isAuthenticated()).toBe(false);
    expect(authSession.isBootstrapped()).toBe(true);
  });

  it("notifies subscribers when the session changes", () => {
    const listener = vi.fn();

    const unsubscribe = authSession.subscribe(listener);
    authSession.setSession({
      email: "test@example.com",
      id: 1,
      roles: ["ROLE_USER"],
      username: "test_user",
    });
    authSession.clear();
    unsubscribe();
    authSession.setSession({
      email: "another@example.com",
      id: 2,
      roles: ["ROLE_USER"],
      username: "another_user",
    });

    expect(listener).toHaveBeenCalledTimes(2);
  });
});
