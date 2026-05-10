import { render, screen } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import ProfileSectionCard from "./ProfileSectionCard";

const accountProfile = {
  bio: "Original bio",
  firstName: "Ada",
  lastName: "Lovelace",
};

describe("ProfileSectionCard", () => {
  test("disables save when profile fields are unchanged", () => {
    render(
      <ProfileSectionCard
        accountProfile={accountProfile}
        form={{ bio: "Original bio", firstName: "Ada", lastName: "Lovelace" }}
        isSaving={false}
        onFormChange={vi.fn()}
        onSave={vi.fn()}
      />,
    );

    expect(
      (screen.getByRole("button", { name: "Save profile" }) as HTMLButtonElement)
        .disabled,
    ).toBe(true);
  });

  test("enables save when profile fields changed", () => {
    render(
      <ProfileSectionCard
        accountProfile={accountProfile}
        form={{ bio: "Updated bio", firstName: "Ada", lastName: "Lovelace" }}
        isSaving={false}
        onFormChange={vi.fn()}
        onSave={vi.fn()}
      />,
    );

    expect(
      (screen.getByRole("button", { name: "Save profile" }) as HTMLButtonElement)
        .disabled,
    ).toBe(false);
  });
});
