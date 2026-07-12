import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { MemoryRouter } from "react-router";
import { beforeEach, describe, expect, test, vi } from "vitest";
import YourCommentsPage from "./YourCommentsPage";

const mocks = vi.hoisted(() => ({
  accountApi: { getCurrentAccountProfile: vi.fn() },
  accountEngagementApi: { getCommentsByAccount: vi.fn() },
  commentApi: { deleteComment: vi.fn(), updateComment: vi.fn() },
}));

vi.mock("../../../../shared/api/moviesApi", () => ({
  accountApi: mocks.accountApi,
  accountEngagementApi: mocks.accountEngagementApi,
  commentApi: mocks.commentApi,
}));

vi.mock("../../../../shared/auth", () => ({ getUsername: () => "ada" }));

const makeQueryClient = () =>
  new QueryClient({
    defaultOptions: { mutations: { retry: false }, queries: { retry: false } },
  });

const renderPage = () => {
  const queryClient = makeQueryClient();
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>{children}</MemoryRouter>
    </QueryClientProvider>
  );
  render(<YourCommentsPage />, { wrapper });
};

const commentPage = (page: number, message: string, totalPages = 1) => ({
  data: {
    content: [
      {
        accountId: 12,
        createdAtInUtc: "2026-07-11T10:00:00Z",
        id: page + 1,
        message,
        modifiedAtInUtc: "2026-07-11T10:00:00Z",
        movieId: 31 + page,
      },
    ],
    last: page === totalPages - 1,
    page,
    size: 20,
    totalElements: totalPages,
    totalPages,
  },
});

describe("YourCommentsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.accountApi.getCurrentAccountProfile.mockResolvedValue({
      data: { imageUrlToken: "ada-avatar-token" },
    });
    mocks.commentApi.deleteComment.mockResolvedValue({});
    mocks.commentApi.updateComment.mockResolvedValue({ data: {} });
  });

  test("lists the signed-in member's comments with links back to each discussion", async () => {
    mocks.accountEngagementApi.getCommentsByAccount.mockResolvedValue(
      commentPage(0, "A patient, hopeful film."),
    );
    renderPage();

    expect(await screen.findByText("A patient, hopeful film.")).toBeTruthy();
    expect(screen.getByRole("heading", { name: "Your comments" })).toBeTruthy();
    expect(
      screen.getByRole("link", { name: "View movie discussion" }).getAttribute("href"),
    ).toBe("/movie?id=31#comment-1");
    expect(
      (await screen.findByAltText("ada profile")).getAttribute("src"),
    ).toContain("profile-photos/ada-avatar-token_size_800x800.jpg");
  });

  test("loads another page without mixing comment pages", async () => {
    const user = userEvent.setup();
    mocks.accountEngagementApi.getCommentsByAccount.mockImplementation(
      (_username: string, page: number) =>
        Promise.resolve(commentPage(page, page === 0 ? "First page" : "Second page", 2)),
    );
    renderPage();

    expect(await screen.findByText("First page")).toBeTruthy();
    await user.click(screen.getByRole("button", { name: "Go to page 2" }));

    expect(await screen.findByText("Second page")).toBeTruthy();
    expect(mocks.accountEngagementApi.getCommentsByAccount).toHaveBeenLastCalledWith(
      "ada",
      1,
      20,
    );
  });

  test("edits a comment and refreshes the personal library", async () => {
    const user = userEvent.setup();
    mocks.accountEngagementApi.getCommentsByAccount.mockResolvedValue(
      commentPage(0, "Original thought"),
    );
    renderPage();

    await screen.findByText("Original thought");
    await user.click(screen.getByRole("button", { name: "Manage comment by ada" }));
    await user.click(screen.getByRole("menuitem", { name: "Edit" }));
    const editor = screen.getByRole("textbox", { name: "Edit comment" });
    await user.clear(editor);
    await user.type(editor, "Edited thought");
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(mocks.commentApi.updateComment).toHaveBeenCalledWith(1, {
      message: "Edited thought",
    });
    expect(mocks.accountEngagementApi.getCommentsByAccount).toHaveBeenCalledTimes(2);
  });
});
