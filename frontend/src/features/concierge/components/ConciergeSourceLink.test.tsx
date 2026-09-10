import "@testing-library/jest-dom/vitest";
import { render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import { ConciergeSourceLink } from "./ConciergeSourceLink";

it("opens only validated watch-page links in a separate tab", () => {
  const url = "https://www.themoviedb.org/movie/13/watch?locale=CH";
  render(<ConciergeSourceLink href={url}>Check offers</ConciergeSourceLink>);
  expect(screen.getByRole("link")).toHaveAttribute("href", url);
  expect(screen.getByRole("link")).toHaveAttribute(
    "rel",
    "noopener noreferrer",
  );
});

it.each([
  "https://www.themoviedb.org.attacker.invalid/movie/13/watch?locale=CH",
  "https://www.themoviedb.org/movie/13/watch?locale=CH&redirect=evil",
  "javascript:alert(1)",
])("renders untrusted link as plain text: %s", (href) => {
  render(<ConciergeSourceLink href={href}>Untrusted</ConciergeSourceLink>);
  expect(screen.queryByRole("link")).not.toBeInTheDocument();
  expect(screen.getByText("Untrusted")).toBeVisible();
});
