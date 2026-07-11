import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SnackbarProvider } from "notistack";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router";
import * as passkeyApi from "../passkeys/passkeyApi";
import PasskeyLoginButton from "./PasskeyLoginButton";

vi.mock("../passkeys/passkeyApi", () => ({
  isPasskeySupported: () => true,
  loginWithPasskey: vi.fn(),
}));

const renderButton = () => {
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

  return render(<PasskeyLoginButton />, { wrapper });
};

describe("PasskeyLoginButton", () => {
  it("shows progress while the browser ceremony is active", async () => {
    const user = userEvent.setup();
    vi.mocked(passkeyApi.loginWithPasskey).mockReturnValue(
      new Promise(() => {}),
    );
    renderButton();

    await user.click(
      screen.getByRole("button", { name: "Sign in with passkey" }),
    );

    expect(
      screen.getByRole("button", { name: /Waiting for passkey/i }),
    ).toHaveProperty("disabled", true);
  });

  it("treats cancellation as neutral feedback", async () => {
    const user = userEvent.setup();
    vi.mocked(passkeyApi.loginWithPasskey).mockRejectedValue(
      new DOMException("Canceled", "NotAllowedError"),
    );
    renderButton();

    await user.click(
      screen.getByRole("button", { name: "Sign in with passkey" }),
    );

    expect(
      await screen.findByText(/Passkey sign-in was canceled/i),
    ).toBeTruthy();
  });
});
