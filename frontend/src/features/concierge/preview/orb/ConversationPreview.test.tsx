import "@testing-library/jest-dom/vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ThemeProvider } from "@mui/material";
import { expect, it, vi } from "vitest";
import { appTheme } from "../../../../theme";
import { ConversationPreview } from "./ConversationPreview";

vi.mock("./VoiceOrb", () => ({ VoiceOrb: () => null }));

const props = {
  open: true,
  state: "ready" as const,
  live: false,
  statusText: "Ready",
  readLevel: () => 0,
  onClose: vi.fn(),
  onMute: vi.fn(),
  onEnd: vi.fn(),
  onStart: vi.fn(),
};
const view = (open = true) => (
  <ThemeProvider theme={appTheme}>
    <ConversationPreview {...props} open={open} />
  </ThemeProvider>
);

it("fills the draft without sending and preserves it across closing", async () => {
  const user = userEvent.setup();
  const { rerender } = render(view());
  await user.click(screen.getByRole("button", { name: /Movies & trailers/ }));
  expect(screen.getByRole("textbox", { name: "Preview message" })).toHaveValue(
    "Show me the trailer for Forrest Gump.",
  );
  expect(screen.queryByText("YOU · PREVIEW")).not.toBeInTheDocument();
  rerender(view(false));
  expect(screen.queryByRole("region")).not.toBeInTheDocument();
  rerender(view());
  expect(screen.getByRole("textbox", { name: "Preview message" })).toHaveValue(
    "Show me the trailer for Forrest Gump.",
  );
  await user.click(
    screen.getByRole("button", { name: "Preview message submission" }),
  );
  expect(screen.getByText("YOU · PREVIEW")).toBeVisible();
  expect(
    screen.getByText(/This design preview does not send messages/),
  ).toBeVisible();
});

it("preserves action context when the current streaming country changes", async () => {
  const user = userEvent.setup();
  render(view());
  await user.click(
    screen.getByRole("button", { name: "View example conversation" }),
  );
  expect(screen.getByText("Watchlist updated · Forrest Gump")).toBeVisible();
  expect(screen.getByText("Trailer unavailable")).toBeVisible();
  expect(screen.getByRole("link", { name: "Source: TMDB ↗" })).toHaveAttribute(
    "rel",
    "noopener noreferrer",
  );
  await user.click(screen.getByRole("button", { name: "Preferences" }));
  await user.click(screen.getByRole("combobox", { name: "Streaming country" }));
  await user.click(screen.getByRole("option", { name: "Germany" }));
  expect(screen.getByText("Guest preview · DE")).toBeVisible();
  expect(screen.getByText(/Streaming country: CH/)).toBeInTheDocument();
  expect(screen.queryByText(/Streaming country: DE/)).not.toBeInTheDocument();
});

it("labels personal examples for guests while keeping them discoverable", async () => {
  const user = userEvent.setup();
  render(view());
  expect(screen.getAllByText("Sign in required")).toHaveLength(4);
  await user.click(screen.getByRole("button", { name: /Your watchlist/ }));
  expect(screen.getByRole("textbox", { name: "Preview message" })).toHaveValue(
    "Add Forrest Gump to my watchlist.",
  );
  await user.click(screen.getByRole("button", { name: "Preferences" }));
  await user.click(
    screen.getByRole("button", { name: "Preview as signed in" }),
  );
  expect(screen.queryByText("Sign in required")).not.toBeInTheDocument();
});
