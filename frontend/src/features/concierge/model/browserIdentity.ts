const STORAGE_KEY = "imdb-clone:movie-concierge:browser-id";
const BROWSER_ID_PATTERN = /^browser-[a-f0-9-]{36}$/;

export const getConciergeBrowserId = (): string => {
  let browserId = window.localStorage.getItem(STORAGE_KEY);
  if (!browserId || !BROWSER_ID_PATTERN.test(browserId)) {
    browserId = `browser-${window.crypto.randomUUID()}`;
    window.localStorage.setItem(STORAGE_KEY, browserId);
  }
  return browserId;
};

export const getConciergeClientId = (accountId: number | null): string =>
  `${getConciergeBrowserId()}:${accountId === null ? "anonymous" : `account-${accountId}`}`;
