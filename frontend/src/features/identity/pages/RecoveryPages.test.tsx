import "@testing-library/jest-dom/vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router";
import type { ReactElement } from "react";
import { authApi } from "../../../shared/api/moviesApi";
import ConfirmEmailPage from "./ConfirmEmailPage";
import ResetPasswordPage from "./ResetPasswordPage";

const show = (page: ReactElement, path: string) =>
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { mutations: { retry: false } } })
      }
    >
      <MemoryRouter initialEntries={[path]}>{page}</MemoryRouter>
    </QueryClientProvider>,
  );

afterEach(() => vi.restoreAllMocks());

it("confirms an email only after an explicit click", async () => {
  const confirm = vi
    .spyOn(authApi, "confirmEmailAddress")
    .mockResolvedValue({} as never);
  show(<ConfirmEmailPage />, "/confirm-email?token=example-token");
  expect(confirm).not.toHaveBeenCalled();
  await userEvent.click(screen.getByRole("button", { name: "Confirm email" }));
  await waitFor(() =>
    expect(confirm).toHaveBeenCalledWith({ token: "example-token" }),
  );
  expect(
    await screen.findByText("Your email is confirmed. You can now sign in."),
  ).toBeVisible();
});

it("does not submit a confirmation without a token", () => {
  show(<ConfirmEmailPage />, "/confirm-email");
  expect(screen.getByRole("button", { name: "Confirm email" })).toBeDisabled();
});

it("requests reset instructions with an email body", async () => {
  const reset = vi
    .spyOn(authApi, "resetPassword")
    .mockResolvedValue({} as never);
  show(<ResetPasswordPage />, "/reset-password");
  await userEvent.type(
    screen.getByRole("textbox", { name: "Email" }),
    "member@example.com",
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Send reset link" }),
  );
  await waitFor(() =>
    expect(reset).toHaveBeenCalledWith({ email: "member@example.com" }),
  );
});

it("checks matching passwords before submitting a reset token", async () => {
  const save = vi
    .spyOn(authApi, "saveNewPassword")
    .mockResolvedValue({} as never);
  show(<ResetPasswordPage />, "/reset-password?token=example-token");
  const password = "Example!123";
  await userEvent.type(screen.getByLabelText(/New password/), password);
  await userEvent.type(
    screen.getByLabelText(/Confirm password/),
    "Different!123",
  );
  await userEvent.click(screen.getByRole("button", { name: "Save password" }));
  expect(screen.getByText("Passwords must match.")).toBeVisible();
  expect(save).not.toHaveBeenCalled();
  await userEvent.clear(screen.getByLabelText(/Confirm password/));
  await userEvent.type(screen.getByLabelText(/Confirm password/), password);
  await userEvent.click(screen.getByRole("button", { name: "Save password" }));
  await waitFor(() =>
    expect(save).toHaveBeenCalledWith({
      token: "example-token",
      newPassword: password,
    }),
  );
});

it("keeps confirmation available after a server error", async () => {
  vi.spyOn(authApi, "confirmEmailAddress").mockRejectedValue(
    new Error("failed"),
  );
  show(<ConfirmEmailPage />, "/confirm-email?token=example-token");
  await userEvent.click(screen.getByRole("button", { name: "Confirm email" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "We could not confirm this email",
  );
  expect(screen.getByRole("button", { name: "Confirm email" })).toBeEnabled();
});
