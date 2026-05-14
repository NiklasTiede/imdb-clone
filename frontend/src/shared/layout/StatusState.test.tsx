import SearchOffIcon from "@mui/icons-material/SearchOff";
import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import StatusState from "./StatusState";

describe("StatusState", () => {
  test("renders a themed status panel with action content", () => {
    render(
      <StatusState
        action={<button type="button">Try again</button>}
        icon={<SearchOffIcon />}
        title="Nothing here"
      >
        Change your filters and search again.
      </StatusState>,
    );

    expect(screen.getByRole("heading", { name: "Nothing here" })).toBeTruthy();
    expect(
      screen.getByText("Change your filters and search again."),
    ).toBeTruthy();
    expect(screen.getByRole("button", { name: "Try again" })).toBeTruthy();
  });
});
