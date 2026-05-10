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

const basePayloadFromProfile = (accountProfile: AccountProfile): AccountRecord => ({
  bio: accountProfile.bio ?? undefined,
  birthday: accountProfile.birthday ?? undefined,
  email: accountProfile.email ?? undefined,
  firstName: accountProfile.firstName ?? undefined,
  lastName: accountProfile.lastName ?? undefined,
  phone: accountProfile.phone ?? undefined,
  username: accountProfile.username ?? undefined,
});

export const buildProfileSectionPayload = (
  accountProfile: AccountProfile,
  form: ProfileSectionForm,
): AccountRecord => ({
  ...basePayloadFromProfile(accountProfile),
  bio: form.bio,
  firstName: emptyToUndefined(form.firstName),
  lastName: emptyToUndefined(form.lastName),
});

export const buildAccountSectionPayload = (
  accountProfile: AccountProfile,
  form: AccountSectionForm,
): AccountRecord => ({
  ...basePayloadFromProfile(accountProfile),
  birthday: form.birthday ?? undefined,
});

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
