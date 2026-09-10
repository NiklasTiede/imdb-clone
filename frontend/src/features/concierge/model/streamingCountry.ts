// Curated ISO country selection; the order keeps Switzerland and its neighbours first.
const COUNTRIES = new Set([
  "CH",
  "AT",
  "DE",
  "IT",
  "FR",
  "GB",
  "ES",
  "PT",
  "NL",
  "BE",
  "IE",
  "SE",
  "NO",
  "PL",
  "US",
  "CA",
  "AU",
  "BR",
  "IN",
  "MX",
]);
const labels = new Intl.DisplayNames(["en"], { type: "region" });
export const streamingCountries = [...COUNTRIES].map((code) => ({
  code,
  name: labels.of(code) ?? code,
}));

const key = (accountId: number | null) =>
  `imdb-clone:streaming-country:${accountId === null ? "guest" : `account-${accountId}`}`;

export const readStreamingCountry = (accountId: number | null): string => {
  try {
    const value = window.localStorage.getItem(key(accountId));
    return value && COUNTRIES.has(value) ? value : "CH";
  } catch {
    return "CH";
  }
};

export const saveStreamingCountry = (
  accountId: number | null,
  country: string,
) => {
  if (!COUNTRIES.has(country)) return;
  try {
    window.localStorage.setItem(key(accountId), country);
  } catch {
    // The in-memory selection still works when storage is unavailable.
  }
};
