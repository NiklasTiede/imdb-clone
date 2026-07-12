import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import BrandLogo from "./BrandLogo";

describe("BrandLogo", () => {
  it("renders the shared clapperboard mark and discovery tagline", () => {
    render(
      <MemoryRouter>
        <BrandLogo />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("link", { name: "IMDb Clone" }).getAttribute("href"),
    ).toBe("/");
    expect(screen.getByTestId("brand-mark").getAttribute("src")).toBe(
      "/brand-logo.svg",
    );
    expect(screen.getByText("Discover, rate, remember").textContent).toBe(
      "Discover, rate, remember",
    );
  });

  it("keeps the compact brand focused on the name", () => {
    render(
      <MemoryRouter>
        <BrandLogo compact />
      </MemoryRouter>,
    );

    expect(screen.getByText("IMDb Clone").textContent).toBe("IMDb Clone");
    expect(screen.queryByText("Discover, rate, remember")).toBeNull();
  });
});
