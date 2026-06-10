import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SnackbarProvider } from "notistack";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router";

import * as availabilityApi from "../api/identityAvailability";
import RegistrationPage from "./RegistrationPage";

vi.mock("../api/identityAvailability", () => ({
  checkEmailAvailability: vi.fn(),
  checkUsernameAvailability: vi.fn(),
}));

const renderRegistrationPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <SnackbarProvider>
        <MemoryRouter>{children}</MemoryRouter>
      </SnackbarProvider>
    </QueryClientProvider>
  );

  return render(<RegistrationPage />, { wrapper });
};

describe("RegistrationPage", () => {
  beforeEach(() => {
    vi.mocked(availabilityApi.checkUsernameAvailability).mockResolvedValue({
      isAvailable: true,
    });
    vi.mocked(availabilityApi.checkEmailAvailability).mockResolvedValue({
      isAvailable: true,
    });
  });

  it("renders the modern two-pane registration experience", () => {
    renderRegistrationPage();

    expect(
      screen.getByRole("heading", { name: "Create your account" }),
    ).toBeTruthy();
    expect(screen.getByText("Track your taste in cinema.")).toBeTruthy();
    expect(
      screen.getByRole("button", { name: "Create account" }),
    ).not.toHaveProperty("disabled", true);
    expect(
      screen.getByRole("button", { name: "Continue with Google" }),
    ).toBeTruthy();
    expect(
      screen.getByRole("button", { name: "Continue with GitHub" }),
    ).toBeTruthy();
  });

  it("shows live password rule feedback", async () => {
    const user = userEvent.setup();
    renderRegistrationPage();

    await user.type(screen.getByLabelText("Password"), "Abcdefg1");

    expect(
      screen.getByText("At least 8 characters").getAttribute("data-met"),
    ).toBe("true");
    expect(
      screen.getByText("One uppercase letter").getAttribute("data-met"),
    ).toBe("true");
    expect(
      screen.getByText("One lowercase letter").getAttribute("data-met"),
    ).toBe("true");
    expect(screen.getByText("One number").getAttribute("data-met")).toBe(
      "true",
    );
  });

  it("disables submit when username is taken", async () => {
    const user = userEvent.setup();
    vi.mocked(availabilityApi.checkUsernameAvailability).mockResolvedValue({
      isAvailable: false,
    });
    renderRegistrationPage();

    await user.type(screen.getByLabelText("Username"), "admin");

    expect(await screen.findByText("Taken")).toBeTruthy();
    expect(
      screen.getByRole("button", { name: "Create account" }),
    ).toHaveProperty("disabled", true);
  });
});
