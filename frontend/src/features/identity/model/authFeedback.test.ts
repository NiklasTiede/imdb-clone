import {
  getPasskeyFeedback,
  getPasswordLoginFeedback,
  socialLoginFailure,
} from "./authFeedback";

describe("auth feedback", () => {
  it("distinguishes invalid credentials from rate limiting", () => {
    expect(
      getPasswordLoginFeedback({ response: { status: 401 } }).message,
    ).toBe("Email/username or password is incorrect.");
    expect(
      getPasswordLoginFeedback({ response: { status: 429 } }).severity,
    ).toBe("warning");
  });

  it("uses a neutral message for a canceled passkey ceremony", () => {
    const feedback = getPasskeyFeedback(
      new DOMException("The operation was canceled", "NotAllowedError"),
    );

    expect(feedback.severity).toBe("info");
    expect(feedback.message).toMatch(/canceled/i);
  });

  it("keeps social login failures provider-neutral", () => {
    expect(socialLoginFailure.message).not.toMatch(/google|github/i);
  });
});
