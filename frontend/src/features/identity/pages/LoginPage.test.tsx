import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SnackbarProvider } from "notistack";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router";

import * as identityMutations from "../api/identityMutations";
import { authSession } from "../../../shared/auth";
import LoginPage, {
  getPostLoginDestination,
  parseLoginLocationState,
} from "./LoginPage";

vi.mock("../api/identityMutations", () => ({
  authenticateAccount: vi.fn(),
}));

vi.mock("../passkeys/passkeyApi", () => ({
  isPasskeySupported: () => true,
  loginWithPasskey: vi.fn(),
}));

const renderLoginPage = (
  initialEntry:
    | string
    | { pathname: string; state?: { registrationMessage?: string } } = "/login",
) => {
  const queryClient = new QueryClient({
    defaultOptions: { mutations: { retry: false }, queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <SnackbarProvider>
        <MemoryRouter initialEntries={[initialEntry]}>{children}</MemoryRouter>
      </SnackbarProvider>
    </QueryClientProvider>
  );

  return render(<LoginPage />, { wrapper });
};

describe("LoginPage", () => {
  beforeEach(() => {
    authSession.resetForTests();
    vi.mocked(identityMutations.authenticateAccount).mockResolvedValue({
      email: "niklas@example.com",
      id: 1,
      roles: ["ROLE_USER"],
      username: "niklas",
    });
  });

  it("renders the modern two-pane login experience", () => {
    renderLoginPage();

    expect(screen.getByRole("heading", { name: "Sign in" })).toBeTruthy();
    expect(
      screen.getByRole("button", { name: "Continue with Google" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", { name: "Continue with GitHub" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", { name: "Sign in with passkey" }),
    ).toBeTruthy();
    expect(screen.getByText("Welcome back.")).toBeTruthy();
    expect(screen.queryByText(/Need an account/i)).toBeNull();
  });

  it("returns to a safe internal destination after sign-in", () => {
    expect(
      getPostLoginDestination({
        from: { pathname: "/movie", search: "?id=42" },
      }),
    ).toBe("/movie?id=42");
    expect(
      getPostLoginDestination({ from: { pathname: "//example.com" } }),
    ).toBe("/");
  });

  it("rejects malformed login navigation state", () => {
    expect(parseLoginLocationState({ from: "//example.com" })).toBeNull();
    expect(parseLoginLocationState(null)).toBeNull();
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
      vi.mocked(identityMutations.authenticateAccount).mock.calls[0]?.[0],
    ).toEqual({
      usernameOrEmail: "niklas",
      password: "x",
    });
  });

  it("shows durable invalid-credential feedback", async () => {
    const user = userEvent.setup();
    vi.mocked(identityMutations.authenticateAccount).mockRejectedValue({
      response: { status: 401 },
    });
    renderLoginPage();

    await user.type(screen.getByLabelText("Email or username"), "niklas");
    await user.type(screen.getByLabelText("Password"), "wrong");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(
      await screen.findByText("Email/username or password is incorrect."),
    ).toBeTruthy();
  });

  it("shows registration completion passed through navigation state", () => {
    renderLoginPage({
      pathname: "/login",
      state: { registrationMessage: "Account created. You can sign in now." },
    });

    expect(
      screen.getByText("Account created. You can sign in now."),
    ).toBeTruthy();
  });

  it("shows provider-neutral social failure feedback", () => {
    renderLoginPage("/login?error=social");

    expect(
      screen.getByText(/Social sign-in could not be completed/i),
    ).toBeTruthy();
  });

  it("shows progress while password login is pending", async () => {
    const user = userEvent.setup();
    vi.mocked(identityMutations.authenticateAccount).mockReturnValue(
      new Promise(() => {}),
    );
    renderLoginPage();

    await user.type(screen.getByLabelText("Email or username"), "niklas");
    await user.type(screen.getByLabelText("Password"), "password");
    await user.click(screen.getByRole("button", { name: "Sign in" }));

    expect(screen.getByRole("button", { name: /Signing in/i })).toHaveProperty(
      "disabled",
      true,
    );
  });
});
