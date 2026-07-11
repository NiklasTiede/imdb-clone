import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, test, vi } from "vitest";
import CommentComposer from "./CommentComposer";

const renderComposer = (
  overrides: Partial<Parameters<typeof CommentComposer>[0]> = {},
) => {
  const props = {
    isAuthenticated: true,
    isSubmitting: false,
    movieTitle: "Arrival",
    onRequestSignIn: vi.fn(),
    onSubmit: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  };
  render(<CommentComposer {...props} />);
  return props;
};

describe("CommentComposer", () => {
  test("guides anonymous visitors to sign in", async () => {
    const user = userEvent.setup();
    const { onRequestSignIn } = renderComposer({ isAuthenticated: false });

    await user.click(
      screen.getByRole("button", { name: "Sign in to comment" }),
    );

    expect(onRequestSignIn).toHaveBeenCalledTimes(1);
  });

  test("publishes a trimmed comment and clears the draft", async () => {
    const user = userEvent.setup();
    const { onSubmit } = renderComposer();
    const editor = screen.getByRole("textbox", { name: "Comment on Arrival" });

    await user.type(editor, "  Beautiful and humane.  ");
    await user.click(screen.getByRole("button", { name: "Publish comment" }));

    expect(onSubmit).toHaveBeenCalledWith("Beautiful and humane.");
    expect(editor).toHaveProperty("value", "");
  });

  test("preserves the draft and shows mutation feedback on failure", async () => {
    const user = userEvent.setup();
    renderComposer({
      errorMessage: "Could not publish your comment. Please try again.",
      onSubmit: vi.fn().mockRejectedValue(new Error("Unavailable")),
    });
    const editor = screen.getByRole("textbox", { name: "Comment on Arrival" });

    await user.type(editor, "Keep this draft");
    await user.click(screen.getByRole("button", { name: "Publish comment" }));

    expect(editor).toHaveProperty("value", "Keep this draft");
    expect(screen.getByRole("alert").textContent).toContain(
      "Could not publish",
    );
  });
});
