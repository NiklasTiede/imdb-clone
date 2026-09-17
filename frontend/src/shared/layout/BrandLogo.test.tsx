import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { defaultThemeId, getAppTheme } from "../../theme";
import BrandLogo from "./BrandLogo";

const { brand } = getAppTheme(defaultThemeId);

describe("BrandLogo", () => {
  it("renders the active theme's mark, wordmark and tagline", () => {
    render(
      <MemoryRouter>
        <BrandLogo />
      </MemoryRouter>,
    );

    const link = screen.getByRole("link", { name: "Popcorn Society" });
    expect(link.getAttribute("href")).toBe("/");
    expect(link.textContent).toContain("Popcorn Society");
    expect(screen.getByTestId("brand-mark").getAttribute("src")).toBe(
      brand.markSrc,
    );
    expect(screen.getByText(brand.tagline.text)).toBeTruthy();
    if (brand.wordmark.accentWord) {
      expect(screen.getByText(brand.wordmark.accentWord)).toBeTruthy();
    }
  });

  it("keeps the compact brand focused on the name", () => {
    render(
      <MemoryRouter>
        <BrandLogo compact />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole("link", { name: "Popcorn Society" }).textContent,
    ).toBe("Popcorn Society");
    expect(screen.queryByText(brand.tagline.text)).toBeNull();
  });
});
