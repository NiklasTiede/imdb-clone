import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, test, vi } from "vitest";
import UserSettingsMenu from "./UserSettingsMenu";

const renderMenu = (path: string) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <UserSettingsMenu
        anchorEl={document.body}
        email="niklas@example.com"
        menuId="account-menu"
        onClose={vi.fn()}
        onLogout={vi.fn()}
        open
        username="niklas"
      />
    </MemoryRouter>,
  );

describe("UserSettingsMenu", () => {
  test("marks the current page and shows the account email", () => {
    renderMenu("/your-ratings");

    expect(screen.getByText("niklas@example.com")).toBeTruthy();
    expect(
      screen
        .getByRole("menuitem", { name: /your ratings/i })
        .getAttribute("aria-current"),
    ).toBe("page");
    expect(
      screen
        .getByRole("menuitem", { name: /your watchlist/i })
        .hasAttribute("aria-current"),
    ).toBe(false);
  });

  test("keeps sign out as a neutral item", () => {
    renderMenu("/");

    const signOut = screen.getByRole("menuitem", { name: /sign out/i });
    expect(signOut.hasAttribute("aria-current")).toBe(false);
    expect(signOut.className).not.toContain("Mui-selected");
  });
});
