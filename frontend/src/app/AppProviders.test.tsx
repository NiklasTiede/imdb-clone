import "@testing-library/jest-dom/vitest";
import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, vi } from "vitest";
import { authSession } from "../shared/auth";
import { installLocalStorageMock } from "../test/installLocalStorageMock";
import { appTheme } from "../theme";
import AppProviders from "./AppProviders";

vi.mock("../shared/auth/bootstrapSession", () => ({
  bootstrapSession: vi.fn().mockResolvedValue(undefined),
}));

describe("AppProviders", () => {
  beforeEach(() => {
    installLocalStorageMock();
    authSession.completeBootstrap(null);
  });

  afterEach(() => {
    act(() => authSession.resetForTests());
  });

  it("renders child content through the application providers", () => {
    render(
      <AppProviders>
        <div>provider child</div>
      </AppProviders>,
    );

    expect(screen.getByText("provider child")).toBeTruthy();
  });

  it("applies the dark movie theme to the global concierge", async () => {
    const user = userEvent.setup();
    render(
      <AppProviders>
        <div>provider child</div>
      </AppProviders>,
    );

    await user.click(
      screen.getByRole("button", { name: "Ask the Movie Concierge" }),
    );

    expect(
      screen.getByRole("heading", { name: "What fits tonight?" }),
    ).toHaveStyle({ color: appTheme.palette.text.primary });
    expect(screen.getByText("Grounded in this catalog")).toHaveStyle({
      color: appTheme.palette.text.secondary,
    });
  });
});
