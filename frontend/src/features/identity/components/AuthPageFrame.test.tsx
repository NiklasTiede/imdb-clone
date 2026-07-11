import { render, screen } from "@testing-library/react";
import AuthPageFrame from "./AuthPageFrame";

describe("AuthPageFrame", () => {
  it("combines the login visual pane with a constrained form slot", () => {
    render(
      <AuthPageFrame variant="login">
        <form aria-label="Login form" />
      </AuthPageFrame>,
    );

    expect(screen.getByTestId("auth-visual-pane")).toBeTruthy();
    expect(screen.getByText("Welcome back.")).toBeTruthy();
    expect(screen.getByRole("form", { name: "Login form" })).toBeTruthy();
  });
});
