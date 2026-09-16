import { readRenamedStorage } from "../../../shared/storage/readRenamedStorage";

const STORAGE_KEY = "popcorn-society:movie-concierge:browser-id";
const BROWSER_ID_PATTERN = /^browser-[a-f0-9-]{36}$/;

export const getConciergeBrowserId = (): string => {
  let browserId = readRenamedStorage(
    window.localStorage, STORAGE_KEY, "imdb-clone:movie-concierge:browser-id",
    (value) => BROWSER_ID_PATTERN.test(value),
  );
  if (!browserId || !BROWSER_ID_PATTERN.test(browserId)) {
    browserId = `browser-${window.crypto.randomUUID()}`;
    window.localStorage.setItem(STORAGE_KEY, browserId);
  }
  return browserId;
};

export const getConciergeClientId = (accountId: number | null): string =>
  `${getConciergeBrowserId()}:${accountId === null ? "anonymous" : `account-${accountId}`}`;
