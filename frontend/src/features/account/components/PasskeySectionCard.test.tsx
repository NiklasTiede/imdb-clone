import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, test, vi } from "vitest";
import PasskeySectionCard from "./PasskeySectionCard";

const passkeys = [
  {
    credentialId: "credential-one",
    createdAt: "2026-06-10T10:00:00Z",
    label: "MacBook passkey",
    lastUsedAt: "2026-06-10T11:00:00Z",
  },
];

describe("PasskeySectionCard", () => {
  test("renders passkeys and deletes a selected credential", async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn();

    render(
      <PasskeySectionCard
        isAdding={false}
        isDeleting={false}
        isLoading={false}
        onAdd={vi.fn()}
        onDelete={onDelete}
        passkeys={passkeys}
      />,
    );

    expect(screen.getByText("MacBook passkey")).toBeTruthy();

    await user.click(screen.getByRole("button", { name: "Delete MacBook passkey" }));

    expect(onDelete).toHaveBeenCalledWith("credential-one");
  });

  test("uses the entered label when adding a passkey", async () => {
    const user = userEvent.setup();
    const onAdd = vi.fn();

    render(
      <PasskeySectionCard
        isAdding={false}
        isDeleting={false}
        isLoading={false}
        onAdd={onAdd}
        onDelete={vi.fn()}
        passkeys={[]}
      />,
    );

    await user.clear(screen.getByLabelText("Passkey label"));
    await user.type(screen.getByLabelText("Passkey label"), "Phone passkey");
    await user.click(screen.getByRole("button", { name: "Add passkey" }));

    expect(onAdd).toHaveBeenCalledWith("Phone passkey");
  });
});
