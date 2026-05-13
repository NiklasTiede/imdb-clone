import {
  createAuthSession,
  getRegistrationInvalidParams,
} from "./identityMutations";

const createToken = (payload: Record<string, unknown>) => {
  const encode = (value: Record<string, unknown>) =>
    btoa(JSON.stringify(value))
      .replaceAll("+", "-")
      .replaceAll("/", "_")
      .replaceAll("=", "");

  return `${encode({ alg: "none", typ: "JWT" })}.${encode(payload)}.`;
};

describe("createAuthSession", () => {
  it("maps a login response token to auth session data", () => {
    const session = createAuthSession({
      accessToken: createToken({
        exp: 9999999999,
        roles: "ROLE_USER,ROLE_ADMIN",
        username: "test_user",
      }),
    });

    expect(session.username).toBe("test_user");
    expect(session.roles).toBe("ROLE_USER,ROLE_ADMIN");
    expect(session.expiresAt).toBe(9999999999);
  });
});

describe("getRegistrationInvalidParams", () => {
  it("extracts backend field validation errors", () => {
    const invalidParams = getRegistrationInvalidParams({
      response: {
        data: {
          invalidParams: {
            email: "Email is already used",
            username: "Username is already used",
          },
        },
      },
    });

    expect(invalidParams).toEqual({
      email: "Email is already used",
      username: "Username is already used",
    });
  });
});
