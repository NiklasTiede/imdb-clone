import type {
  AccountProfile,
  AccountRecord,
} from "../../../client/movies/generator-output";

export const MAX_BIO_LENGTH = 300;

export type ProfileSectionForm = {
  bio: string;
  firstName: string;
  lastName: string;
};

export type AccountSectionForm = {
  birthday: string | null;
};

const emptyToUndefined = (value: string): string | undefined =>
  value.trim() === "" ? undefined : value;

const basePayloadFromProfile = (
  accountProfile: AccountProfile,
): AccountRecord => {
  const payload: AccountRecord = {};

  if (accountProfile.bio !== undefined) payload.bio = accountProfile.bio;
  if (accountProfile.birthday !== undefined) {
    payload.birthday = accountProfile.birthday;
  }
  if (accountProfile.email !== undefined) payload.email = accountProfile.email;
  if (accountProfile.firstName !== undefined) {
    payload.firstName = accountProfile.firstName;
  }
  if (accountProfile.lastName !== undefined) {
    payload.lastName = accountProfile.lastName;
  }
  if (accountProfile.phone !== undefined) payload.phone = accountProfile.phone;
  if (accountProfile.username !== undefined) {
    payload.username = accountProfile.username;
  }

  return payload;
};

export const buildProfileSectionPayload = (
  accountProfile: AccountProfile,
  form: ProfileSectionForm,
): AccountRecord => {
  const payload = basePayloadFromProfile(accountProfile);
  const firstName = emptyToUndefined(form.firstName);
  const lastName = emptyToUndefined(form.lastName);

  payload.bio = form.bio;
  if (firstName === undefined) delete payload.firstName;
  else payload.firstName = firstName;
  if (lastName === undefined) delete payload.lastName;
  else payload.lastName = lastName;

  return payload;
};

export const buildAccountSectionPayload = (
  accountProfile: AccountProfile,
  form: AccountSectionForm,
): AccountRecord => {
  const payload = basePayloadFromProfile(accountProfile);

  if (form.birthday === null) delete payload.birthday;
  else payload.birthday = form.birthday;

  return payload;
};

export const hasProfileSectionChanges = (
  accountProfile: AccountProfile,
  form: ProfileSectionForm,
): boolean =>
  form.firstName !== (accountProfile.firstName ?? "") ||
  form.lastName !== (accountProfile.lastName ?? "") ||
  form.bio !== (accountProfile.bio ?? "");

export const hasAccountSectionChanges = (
  accountProfile: AccountProfile,
  form: AccountSectionForm,
): boolean => form.birthday !== (accountProfile.birthday ?? null);
