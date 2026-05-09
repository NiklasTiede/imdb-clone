import { render, screen } from "@testing-library/react";
import AppProviders from "./AppProviders";

describe("AppProviders", () => {
  it("renders child content through the application providers", () => {
    render(
      <AppProviders>
        <div>provider child</div>
      </AppProviders>,
    );

    expect(screen.getByText("provider child")).toBeTruthy();
  });
});
