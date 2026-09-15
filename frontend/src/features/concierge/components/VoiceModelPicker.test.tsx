import "@testing-library/jest-dom/vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, expect, it, vi } from "vitest";
import { VoiceModelPicker } from "./VoiceModelPicker";

const available = vi.hoisted(() => ({ models: ["grok", "gpt-live-1"] }));
vi.mock("../api/voiceModels", async (original) => ({
  ...(await original<typeof import("../api/voiceModels")>()),
  useVoiceModels: () => ({ data: available.models }),
}));
beforeEach(() => {
  available.models = ["grok", "gpt-live-1"];
});

it("lets users choose an enabled model before starting voice", async () => {
  const onChange = vi.fn();
  render(<VoiceModelPicker model="grok" onChange={onChange} active={false} />);
  const user = userEvent.setup();
  await user.click(screen.getByRole("button", { name: "Voice model: Grok" }));
  await user.click(screen.getByRole("menuitem", { name: "GPT-Live 1" }));
  expect(onChange).toHaveBeenCalledWith("gpt-live-1");
});

it("keeps the current model fixed during a live session", () => {
  render(<VoiceModelPicker model="gpt-live-1" onChange={vi.fn()} active />);
  expect(
    screen.getByRole("button", { name: "Voice model: GPT-Live 1" }),
  ).toBeDisabled();
});

it("does not offer a model that the server has disabled", () => {
  available.models = ["grok"];
  render(<VoiceModelPicker model="grok" onChange={vi.fn()} active={false} />);
  expect(screen.queryByRole("button")).not.toBeInTheDocument();
});
