import { render, screen } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import AccountSectionCard from "./AccountSectionCard";

const accountProfile = {
  birthday: "1990-04-12",
  email: "ada@example.com",
};

describe("AccountSectionCard", () => {
  test("renders email as read-only and disables save when birthday is unchanged", () => {
    render(
      <AccountSectionCard
        accountProfile={accountProfile}
        form={{ birthday: "1990-04-12" }}
        isSaving={false}
        onFormChange={vi.fn()}
        onSave={vi.fn()}
      />,
    );

    expect(
      (screen.getByRole("textbox", { name: "Email Address" }) as HTMLInputElement)
        .disabled,
    ).toBe(true);
    expect(
      (screen.getByRole("button", { name: "Save account" }) as HTMLButtonElement)
        .disabled,
    ).toBe(true);
  });

  test("enables save when birthday changed", () => {
    render(
      <AccountSectionCard
        accountProfile={accountProfile}
        form={{ birthday: null }}
        isSaving={false}
        onFormChange={vi.fn()}
        onSave={vi.fn()}
      />,
    );

    expect(
      (screen.getByRole("button", { name: "Save account" }) as HTMLButtonElement)
        .disabled,
    ).toBe(false);
  });
});
