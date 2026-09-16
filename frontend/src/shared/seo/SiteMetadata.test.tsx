/* eslint-disable testing-library/no-node-access -- Metadata in document.head has no accessible roles; inspect the actual crawler-facing tags. */
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Link, MemoryRouter } from "react-router";
import SiteMetadata from "./SiteMetadata";

it("replaces metadata on navigation and removes public URLs from reset-password pages", async () => {
  const user = userEvent.setup();
  const view = render(
    <MemoryRouter initialEntries={["/"]}>
      <SiteMetadata />
      <Link to="/movie?id=123&utm_source=mail">Movie</Link>
      <Link to="/reset-password?token=secret">Reset</Link>
    </MemoryRouter>,
  );
  expect(
    document.head.querySelector('script[type="application/ld+json"]'),
  ).not.toBeNull();
  await user.click(screen.getByRole("link", { name: "Movie" }));
  expect(document.head.querySelectorAll('link[rel="canonical"]')).toHaveLength(
    1,
  );
  expect(
    document.head.querySelector('link[rel="canonical"]')?.getAttribute("href"),
  ).toBe(`${window.location.origin}/movie?id=123`);
  expect(
    document.head.querySelector('script[type="application/ld+json"]'),
  ).toBeNull();
  await user.click(screen.getByRole("link", { name: "Reset" }));
  expect(document.head.querySelector('link[rel="canonical"]')).toBeNull();
  expect(document.head.querySelector('meta[property="og:url"]')).toBeNull();
  expect(
    document.head.querySelector('meta[name="robots"]')?.getAttribute("content"),
  ).toBe("noindex, follow");
  expect(document.head.innerHTML).not.toContain("token=secret");
  view.unmount();
  expect(document.head.querySelector('meta[name="robots"]')).toBeNull();
});
