import { describe, expect, test } from "vitest";
import {
  buildAccountSectionPayload,
  buildProfileSectionPayload,
  hasAccountSectionChanges,
  hasProfileSectionChanges,
} from "./accountSettingsForm";

const accountProfile = {
  bio: "Original bio",
  birthday: "1990-04-12",
  commentsCount: 3,
  email: "ada@example.com",
  firstName: "Ada",
  imageUrlToken: "profile-token",
  lastName: "Lovelace",
  phone: "+491234",
  ratingsCount: 7,
  username: "ada",
  watchlistCount: 11,
};

describe("accountSettingsForm", () => {
  test("builds a profile save payload without clearing hidden account fields", () => {
    expect(
      buildProfileSectionPayload(accountProfile, {
        bio: "Updated bio",
        firstName: "Augusta",
        lastName: "Lovelace",
      }),
    ).toEqual({
      bio: "Updated bio",
      birthday: "1990-04-12",
      email: "ada@example.com",
      firstName: "Augusta",
      lastName: "Lovelace",
      phone: "+491234",
      username: "ada",
    });
  });

  test("builds an account save payload without clearing profile fields or phone", () => {
    expect(
      buildAccountSectionPayload(accountProfile, {
        birthday: null,
      }),
    ).toEqual({
      bio: "Original bio",
      email: "ada@example.com",
      firstName: "Ada",
      lastName: "Lovelace",
      phone: "+491234",
      username: "ada",
    });
  });

  test("detects profile section changes", () => {
    expect(
      hasProfileSectionChanges(accountProfile, {
        bio: "Original bio",
        firstName: "Ada",
        lastName: "Lovelace",
      }),
    ).toBe(false);

    expect(
      hasProfileSectionChanges(accountProfile, {
        bio: "Changed",
        firstName: "Ada",
        lastName: "Lovelace",
      }),
    ).toBe(true);
  });

  test("detects account section changes", () => {
    expect(
      hasAccountSectionChanges(accountProfile, {
        birthday: "1990-04-12",
      }),
    ).toBe(false);

    expect(
      hasAccountSectionChanges(accountProfile, {
        birthday: null,
      }),
    ).toBe(true);
  });
});
