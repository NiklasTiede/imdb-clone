import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import AuthLayout from "./AuthLayout";

describe("AuthLayout", () => {
  it("uses the shared brand but omits app-only controls", () => {
    render(
      <MemoryRouter>
        <AuthLayout
          altActionLabel="Sign in"
          altLabel="Already have an account?"
          altTo="/login"
        >
          <div>auth body</div>
        </AuthLayout>
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("link", { name: /IMDb clone/i }).getAttribute("href"),
    ).toBe("/");
    expect(
      screen.queryByRole("textbox", { name: /search movies/i }),
    ).toBeNull();
    expect(
      screen.queryByRole("button", { name: /account of current user/i }),
    ).toBeNull();
  });
});
