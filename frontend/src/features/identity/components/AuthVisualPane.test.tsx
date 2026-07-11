import { render, screen } from "@testing-library/react";
import AuthVisualPane from "./AuthVisualPane";

describe("AuthVisualPane", () => {
  it("uses accurate login copy and decorative media", () => {
    render(<AuthVisualPane variant="login" />);

    expect(screen.getByText("Welcome back.")).toBeTruthy();
    expect(screen.getByText("Continue exploring")).toBeTruthy();
    expect(screen.queryByText("Your conversations")).toBeNull();
    const media = screen.getByRole("presentation", { hidden: true });
    expect(media.getAttribute("alt")).toBe("");
    expect(media.getAttribute("aria-hidden")).toBe("true");
  });

  it("uses accurate signup copy", () => {
    render(<AuthVisualPane variant="signup" />);

    expect(screen.getByText("Track your taste in cinema.")).toBeTruthy();
    expect(screen.getByText("Discover new favorites")).toBeTruthy();
    expect(screen.queryByText(/Join thousands/i)).toBeNull();
  });
});
