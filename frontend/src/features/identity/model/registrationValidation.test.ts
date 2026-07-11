import { passwordRules, registrationSchema } from "./registrationValidation";

const validRegistration = {
  username: "movie_fan",
  email: "movie@example.com",
  password: "Movie!12",
  confirmPassword: "Movie!12",
};

describe("registration validation", () => {
  it.each(["ab", "movie_fan", "movie.fan", "abcdefghijklmnopqrst"])(
    "accepts supported username %s",
    (username) => {
      expect(
        registrationSchema.safeParse({ ...validRegistration, username })
          .success,
      ).toBe(true);
    },
  );

  it.each([
    "a",
    "abcdefghijklmnopqrstu",
    ".movie",
    "movie_",
    "movie..fan",
    "movie-fan",
  ])("rejects unsupported username %s", (username) => {
    expect(
      registrationSchema.safeParse({ ...validRegistration, username }).success,
    ).toBe(false);
  });

  it("keeps the visible password rules consistent with schema validation", () => {
    const password = "Movie!12";

    expect(passwordRules.every((rule) => rule.met(password))).toBe(true);
    expect(
      registrationSchema.safeParse({
        ...validRegistration,
        password,
        confirmPassword: password,
      }).success,
    ).toBe(true);
  });

  it.each([
    "Movie!1",
    "movie!12",
    "MOVIE!12",
    "MoviePass!",
    "Movie123",
    "A23456789012345678901234567890!",
  ])("rejects password %s", (password) => {
    expect(passwordRules.every((rule) => rule.met(password))).toBe(false);
    expect(
      registrationSchema.safeParse({
        ...validRegistration,
        password,
        confirmPassword: password,
      }).success,
    ).toBe(false);
  });

  it("reports confirmation mismatch without applying password complexity twice", () => {
    const result = registrationSchema.safeParse({
      ...validRegistration,
      confirmPassword: "Different!12",
    });

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues).toEqual([
        expect.objectContaining({
          path: ["confirmPassword"],
          message: "Passwords do not match",
        }),
      ]);
    }
  });
});
