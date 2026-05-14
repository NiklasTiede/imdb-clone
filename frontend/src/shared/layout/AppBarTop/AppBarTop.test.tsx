import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router";
import { beforeEach, vi } from "vitest";
import AppBarTop from "./AppBarTop";

const LocationProbe = () => {
  const location = useLocation();
  return <div data-testid="location">{location.pathname}</div>;
};

const renderAppBar = (initialPath = "/movie-search") =>
  render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AppBarTop />
      <Routes>
        <Route path="*" element={<LocationProbe />} />
      </Routes>
    </MemoryRouter>,
  );

describe("AppBarTop", () => {
  let localStorageData: Record<string, string>;

  beforeEach(() => {
    localStorageData = {};
    Object.defineProperty(window, "localStorage", {
      configurable: true,
      value: {
        getItem: vi.fn((key: string) => localStorageData[key] ?? null),
        removeItem: vi.fn((key: string) => {
          delete localStorageData[key];
        }),
        setItem: vi.fn((key: string, value: string) => {
          localStorageData[key] = value;
        }),
      },
    });
  });

  it("clears the session and returns home when signing out", () => {
    window.localStorage.setItem("jwtToken", "token");
    window.localStorage.setItem("jwtExpiresAt", "4102444800");
    window.localStorage.setItem("rolesFromJwt", "User");
    window.localStorage.setItem("username", "niklas");

    renderAppBar("/your-ratings");

    fireEvent.click(
      screen.getByRole("button", { name: /account of current user/i }),
    );
    fireEvent.click(screen.getByRole("menuitem", { name: /sign out/i }));

    expect(window.localStorage.getItem("jwtToken")).toBeNull();
    expect(screen.getByTestId("location").textContent).toBe("/");
  });
});
