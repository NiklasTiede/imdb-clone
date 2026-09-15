import "@testing-library/jest-dom/vitest";
import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, vi } from "vitest";
import { authSession } from "../shared/auth";
import { installLocalStorageMock } from "../test/installLocalStorageMock";
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

  it("provides one closed lens without a header launcher or conversation drawer", () => {
    render(
      <AppProviders>
        <div>provider child</div>
      </AppProviders>,
    );

    expect(
      screen.queryByRole("button", { name: "Ask the Movie Concierge" }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Start voice" })).toHaveAttribute(
      "data-state",
      "closed",
    );
    expect(
      screen.queryByRole("complementary", { name: "Movie Concierge" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Start voice from header" }),
    ).not.toBeInTheDocument();
  });
});
