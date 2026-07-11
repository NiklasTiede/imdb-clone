import { fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, test, vi } from "vitest";
import CommentItem from "./CommentItem";

const comment = {
  accountId: 7,
  createdAtInUtc: "2026-07-10T10:00:00Z",
  id: 11,
  message: "Hope can be a dangerous thing.",
  modifiedAtInUtc: "2026-07-10T10:10:00Z",
  movieId: 1,
};

const renderItem = (
  overrides: Partial<Parameters<typeof CommentItem>[0]> = {},
) => {
  const props = {
    author: { displayName: "Niklas Tiede", id: 7, username: "niklas" },
    canManage: true,
    comment,
    isDeleting: false,
    isUpdating: false,
    onDelete: vi.fn().mockResolvedValue(undefined),
    onUpdate: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  };
  render(<CommentItem {...props} />);
  return props;
};

describe("CommentItem", () => {
  test("renders author identity, text, and edited state", () => {
    renderItem();

    expect(screen.getByText("Niklas Tiede")).toBeTruthy();
    expect(screen.getByText("@niklas")).toBeTruthy();
    expect(screen.getByText("Hope can be a dangerous thing.")).toBeTruthy();
    expect(screen.getByText("(edited)")).toBeTruthy();
  });

  test("edits a managed comment inline", async () => {
    const user = userEvent.setup();
    const { onUpdate } = renderItem();

    await user.click(
      screen.getByRole("button", { name: "Manage comment by Niklas Tiede" }),
    );
    await user.click(screen.getByRole("menuitem", { name: "Edit" }));
    const editor = screen.getByRole("textbox", { name: "Edit comment" });
    fireEvent.change(editor, {
      target: { value: "Fear can hold you prisoner." },
    });
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(onUpdate).toHaveBeenCalledWith(11, "Fear can hold you prisoner.");
  });

  test("confirms deletion before calling the mutation", async () => {
    const user = userEvent.setup();
    const { onDelete } = renderItem();

    await user.click(
      screen.getByRole("button", { name: "Manage comment by Niklas Tiede" }),
    );
    await user.click(screen.getByRole("menuitem", { name: "Delete" }));
    const dialog = screen.getByRole("dialog", { name: "Delete comment?" });
    await user.click(within(dialog).getByRole("button", { name: "Delete" }));

    expect(onDelete).toHaveBeenCalledWith(11);
  });

  test("keeps a failed deletion actionable inside the dialog", async () => {
    const user = userEvent.setup();
    renderItem({ onDelete: vi.fn().mockRejectedValue(new Error("offline")) });

    await user.click(
      screen.getByRole("button", { name: "Manage comment by Niklas Tiede" }),
    );
    await user.click(screen.getByRole("menuitem", { name: "Delete" }));
    const dialog = screen.getByRole("dialog", { name: "Delete comment?" });
    await user.click(within(dialog).getByRole("button", { name: "Delete" }));

    expect(
      within(dialog).getByText(
        "Could not delete this comment. Please try again.",
      ),
    ).toBeTruthy();
    expect(dialog).toBeTruthy();
  });
});
