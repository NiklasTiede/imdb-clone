import { getRegistrationInvalidParams } from "./identityMutations";

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
