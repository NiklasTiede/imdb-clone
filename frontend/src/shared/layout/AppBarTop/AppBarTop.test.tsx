import { act, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router";
import { afterEach, beforeEach, vi } from "vitest";
import AppBarTop from "./AppBarTop";

const LocationProbe = () => {
  const location = useLocation();
  return <div data-testid="location">{`${location.pathname}${location.search}`}</div>;
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
    vi.useRealTimers();
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

  afterEach(() => {
    vi.useRealTimers();
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

  it("updates the movie search URL after typing pauses", () => {
    vi.useFakeTimers();
    renderAppBar("/movie-search?genre=HORROR&page=3");

    fireEvent.change(screen.getByRole("textbox", { name: "search movies" }), {
      target: { value: "alien" },
    });

    expect(screen.getByTestId("location").textContent).toBe(
      "/movie-search?genre=HORROR&page=3",
    );

    act(() => {
      vi.advanceTimersByTime(300);
    });

    expect(screen.getByTestId("location").textContent).toBe(
      "/movie-search?genre=HORROR&query=alien",
    );
  });

  it("submits immediately with Enter and preserves search filters", () => {
    vi.useFakeTimers();
    renderAppBar("/movie-search?genre=SCI_FI&page=2");
    const searchInput = screen.getByRole("textbox", { name: "search movies" });

    fireEvent.change(searchInput, {
      target: { value: "arrival" },
    });
    fireEvent.keyDown(searchInput, {
      key: "Enter",
      target: { value: "arrival" },
    });

    expect(screen.getByTestId("location").textContent).toBe(
      "/movie-search?genre=SCI_FI&query=arrival",
    );
  });

  it("encodes multi-word search queries with percent-encoded spaces", () => {
    vi.useFakeTimers();
    renderAppBar("/");
    const searchInput = screen.getByRole("textbox", { name: "search movies" });

    fireEvent.change(searchInput, {
      target: { value: "it follows" },
    });
    fireEvent.keyDown(searchInput, {
      key: "Enter",
      target: { value: "it follows" },
    });

    expect(screen.getByTestId("location").textContent).toBe(
      "/movie-search?query=it%20follows",
    );
  });

  it("clears pending debounced searches when clearing the input", () => {
    vi.useFakeTimers();
    renderAppBar("/movie-search?genre=DRAMA");

    fireEvent.change(screen.getByRole("textbox", { name: "search movies" }), {
      target: { value: "heat" },
    });
    fireEvent.click(screen.getByRole("button", { name: "clear" }));

    act(() => {
      vi.advanceTimersByTime(300);
    });

    expect(screen.getByTestId("location").textContent).toBe(
      "/movie-search?genre=DRAMA",
    );
  });
});
