import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, test, vi } from "vitest";
import MovieCommentsSection from "./MovieCommentsSection";

const mocks = vi.hoisted(() => ({
  accountApi: { getPublicAccountSummaries: vi.fn() },
  commentApi: {
    createComment: vi.fn(),
    deleteComment: vi.fn(),
    getCommentsByMovieId: vi.fn(),
    updateComment: vi.fn(),
  },
  session: null as null | {
    id: number;
    roles: string[];
    username: string;
  },
}));

vi.mock("../../../../shared/api/moviesApi", () => ({
  accountApi: mocks.accountApi,
  commentApi: mocks.commentApi,
}));

vi.mock("../../../../shared/auth", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../../../../shared/auth")>()),
  useAuthSessionSnapshot: () => ({
    bootstrapped: true,
    session: mocks.session,
  }),
}));

const makeQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      mutations: { retry: false },
      queries: { retry: false },
    },
  });

const renderSection = () => {
  const queryClient = makeQueryClient();
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  render(
    <MovieCommentsSection
      movieId={1}
      movieTitle="The Shawshank Redemption"
      onRequestSignIn={vi.fn()}
    />,
    { wrapper },
  );
};

describe("MovieCommentsSection", () => {
  beforeEach(() => {
    mocks.session = null;
    vi.clearAllMocks();
    mocks.commentApi.getCommentsByMovieId.mockResolvedValue({
      data: {
        content: [
          {
            accountId: 7,
            createdAtInUtc: "2026-07-11T10:00:00Z",
            id: 11,
            message: "A patient, hopeful film.",
            modifiedAtInUtc: "2026-07-11T10:00:00Z",
            movieId: 1,
          },
        ],
        last: true,
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1,
      },
    });
    mocks.accountApi.getPublicAccountSummaries.mockResolvedValue({
      data: [{ displayName: "Niklas Tiede", id: 7, username: "niklas" }],
    });
    mocks.commentApi.createComment.mockResolvedValue({ data: { id: 12 } });
  });

  test("shows a public comment feed with hydrated authors", async () => {
    renderSection();

    expect(await screen.findByText("A patient, hopeful film.")).toBeTruthy();
    expect(await screen.findByText("Niklas Tiede")).toBeTruthy();
    expect(screen.getByText("1 comment")).toBeTruthy();
    expect(
      screen.getByRole("button", { name: "Sign in to comment" }),
    ).toBeTruthy();
  });

  test("publishes comments for an authenticated account", async () => {
    const user = userEvent.setup();
    mocks.session = { id: 7, roles: ["ROLE_USER"], username: "niklas" };
    renderSection();
    const editor = await screen.findByRole("textbox", {
      name: "Comment on The Shawshank Redemption",
    });

    await user.type(editor, "Still resonates.");
    await user.click(screen.getByRole("button", { name: "Publish comment" }));

    expect(mocks.commentApi.createComment).toHaveBeenCalledWith(1, {
      message: "Still resonates.",
    });
    expect(await screen.findByText("Comment published.")).toBeTruthy();
  });
});
