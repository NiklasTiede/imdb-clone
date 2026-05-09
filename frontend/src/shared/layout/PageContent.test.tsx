import { render, screen } from "@testing-library/react";
import PageContent from "./PageContent";

describe("PageContent", () => {
  it("renders children in the main page region", () => {
    render(<PageContent>page body</PageContent>);

    expect(screen.getByRole("main").textContent).toBe("page body");
  });
});
