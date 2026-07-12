import { render, screen } from "@testing-library/react";
import { describe, expect, test } from "vitest";
import ProfileAvatar from "./ProfileAvatar";

describe("ProfileAvatar", () => {
  test("renders a stored profile image with accessible fallback content", () => {
    render(
      <ProfileAvatar
        alt="Niklas profile"
        fallback="NT"
        imageUrlToken="profile-token"
      />,
    );

    expect(screen.getByRole("img", { name: "Niklas profile" }).getAttribute("src"))
      .toContain("profile-photos/profile-token_size_800x800.jpg");
  });
});
