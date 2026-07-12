import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes, useLocation } from "react-router";
import { afterEach, beforeEach, vi } from "vitest";
import { accountQueries } from "../../api/accountProfileQueries";
import { authSession } from "../../auth";
import AppBarTop from "./AppBarTop";

vi.mock("../../auth/logoutSession", () => ({
  logoutSession: vi.fn().mockResolvedValue(undefined),
}));

let unmountAppBar: (() => void) | undefined;

const LocationProbe = () => {
  const location = useLocation();
  return <div data-testid="location">{`${location.pathname}${location.search}`}</div>;
};

const renderAppBar = (
  initialPath = "/movie-search",
  currentProfile: { imageUrlToken?: string } = {},
) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  });
  if (authSession.isAuthenticated()) {
    queryClient.setQueryData(
      accountQueries.currentProfile().queryKey,
      currentProfile,
    );
  }
  const view = render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <AppBarTop />
        <Routes>
          <Route path="*" element={<LocationProbe />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  unmountAppBar = view.unmount;
  return view;
};

describe("AppBarTop", () => {
  beforeEach(() => {
    unmountAppBar = undefined;
    vi.useRealTimers();
    authSession.resetForTests();
  });

  afterEach(() => {
    // Unmount before resetting this module's external session and timer state.
    unmountAppBar?.();
    authSession.resetForTests();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it("clears the session and returns home when signing out", async () => {
    authSession.setSession({
      email: "niklas@example.com",
      id: 1,
      roles: ["ROLE_USER"],
      username: "niklas",
    });

    renderAppBar("/your-ratings");

    fireEvent.click(
      screen.getByRole("button", { name: /account of current user/i }),
    );
    fireEvent.click(screen.getByRole("menuitem", { name: /sign out/i }));

    await waitFor(() => expect(authSession.isAuthenticated()).toBe(false));
    await waitFor(() =>
      expect(screen.getByTestId("location").textContent).toBe("/"),
    );
  });

  it("renders the current profile photo in the account action", async () => {
    authSession.setSession({
      email: "niklas@example.com",
      id: 1,
      roles: ["ROLE_USER"],
      username: "niklas",
    });
    renderAppBar("/movie-search", { imageUrlToken: "avatar-token" });

    expect(
      (
        await screen.findByRole("img", { name: "niklas profile" })
      ).getAttribute("src"),
    ).toContain("profile-photos/avatar-token_size_800x800.jpg");
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

  it("keeps the latest input while debounced URL updates settle", async () => {
    const user = userEvent.setup();
    renderAppBar("/movie-search");
    const searchInput = screen.getByRole("textbox", {
      name: "search movies",
    });

    let expectedQuery = "";
    for (const characters of ["s", "p", "i", "rited"]) {
      await user.type(searchInput, characters);
      expectedQuery += characters;
      await waitFor(() =>
        expect(screen.getByTestId("location").textContent).toBe(
          `/movie-search?query=${expectedQuery}`,
        ),
      );
    }

    expect((searchInput as HTMLInputElement).value).toBe("spirited");
    expect(screen.getByTestId("location").textContent).toBe(
      "/movie-search?query=spirited",
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
