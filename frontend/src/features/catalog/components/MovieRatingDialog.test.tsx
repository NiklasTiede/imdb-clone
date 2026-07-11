import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, test, vi } from "vitest";
import MovieRatingDialog from "./MovieRatingDialog";

const renderDialog = (
  overrides: Partial<Parameters<typeof MovieRatingDialog>[0]> = {},
) => {
  const props = {
    currentRating: null,
    movieTitle: "The Shawshank Redemption",
    onClose: vi.fn(),
    onSubmit: vi.fn(),
    open: true,
    ...overrides,
  };

  render(<MovieRatingDialog {...props} />);
  return props;
};

describe("MovieRatingDialog", () => {
  test("selects and submits a score from 1 to 10", async () => {
    const user = userEvent.setup();
    const { onSubmit } = renderDialog();

    await user.click(screen.getByRole("radio", { name: "8 out of 10" }));
    expect(screen.getByText("8", { exact: true })).toBeTruthy();
    await user.click(screen.getByRole("button", { name: "Save rating" }));

    expect(onSubmit).toHaveBeenCalledWith(8);
  });

  test("starts from the saved score and allows removal", async () => {
    const user = userEvent.setup();
    const { onSubmit } = renderDialog({ currentRating: 7 });

    expect(screen.getByRole("radio", { name: "7 out of 10" })).toHaveProperty(
      "checked",
      true,
    );
    await user.click(screen.getByRole("button", { name: "Remove" }));

    expect(onSubmit).toHaveBeenCalledWith(null);
  });

  test("keeps actions disabled while saving and displays an error", () => {
    renderDialog({
      currentRating: 7,
      errorMessage: "Could not save your rating. Please try again.",
      isPending: true,
    });

    expect(screen.getByRole("alert").textContent).toContain(
      "Could not save your rating",
    );
    expect(screen.getByRole("button", { name: "Saving..." })).toHaveProperty(
      "disabled",
      true,
    );
    expect(screen.getByRole("button", { name: "Cancel" })).toHaveProperty(
      "disabled",
      true,
    );
  });

  test("cancels without submitting", async () => {
    const user = userEvent.setup();
    const { onClose, onSubmit } = renderDialog();

    await user.click(screen.getByRole("button", { name: "Cancel" }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
