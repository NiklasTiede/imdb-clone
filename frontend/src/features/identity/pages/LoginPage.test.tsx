import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SnackbarProvider } from "notistack";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router";

import * as identityMutations from "../api/identityMutations";
import LoginPage from "./LoginPage";

vi.mock("../api/identityMutations", () => ({
  authenticateAccount: vi.fn(),
}));

const renderLoginPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: { mutations: { retry: false }, queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <SnackbarProvider>
        <MemoryRouter>{children}</MemoryRouter>
      </SnackbarProvider>
    </QueryClientProvider>
  );

  return render(<LoginPage />, { wrapper });
};

describe("LoginPage", () => {
  let localStorageData: Record<string, string>;

  beforeEach(() => {
    localStorageData = {};
    Object.defineProperty(window, "localStorage", {
      configurable: true,
      value: {
        getItem: vi.fn((key: string) => localStorageData[key] ?? null),
        removeItem: vi.fn((key: string) => {
          delete localStorageData[key];
        }),
        setItem: vi.fn((key: string, value: string) => {
          localStorageData[key] = value;
        }),
      },
    });
    vi.mocked(identityMutations.authenticateAccount).mockResolvedValue({
      accessToken: "token",
      expiresAt: 4102444800,
      roles: "User",
      username: "niklas",
    });
  });

  it("renders the modern two-pane login experience", () => {
    renderLoginPage();

    expect(screen.getByRole("heading", { name: "Sign in" })).toBeTruthy();
    expect(screen.getByText("Welcome back.")).toBeTruthy();
    expect(screen.queryByText(/Need an account/i)).toBeNull();
  });

  it("toggles password visibility from the field action", async () => {
    const user = userEvent.setup();
    renderLoginPage();

    const password = screen.getByLabelText("Password") as HTMLInputElement;
    expect(password.type).toBe("password");

    await user.click(screen.getByRole("button", { name: "Show password" }));

    expect(password.type).toBe("text");
    expect(screen.getByRole("button", { name: "Hide password" })).toBeTruthy();
  });

  it("submits short passwords so the server owns credential validation", async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await user.type(screen.getByLabelText("Email or username"), "niklas");
    await user.type(screen.getByLabelText("Password"), "x");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    await waitFor(() =>
      expect(identityMutations.authenticateAccount).toHaveBeenCalled(),
    );
    expect(
      vi.mocked(identityMutations.authenticateAccount).mock.calls[0][0],
    ).toEqual({
      usernameOrEmail: "niklas",
      password: "x",
    });
  });
});
