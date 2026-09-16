import { getConciergeClientId } from "./browserIdentity";
import { installLocalStorageMock } from "../../../test/installLocalStorageMock";

describe("concierge browser identity", () => {
  beforeEach(installLocalStorageMock);

  it("keeps anonymous and account conversations isolated in one browser", () => {
    const anonymousId = getConciergeClientId(null);
    const accountId = getConciergeClientId(7);

    expect(anonymousId).not.toBe(accountId);
    expect(anonymousId.split(":")[0]).toBe(accountId.split(":")[0]);
    expect(accountId).toMatch(/:account-7$/);
  });
});

describe("browser identity rebranding", () => {
  beforeEach(installLocalStorageMock);
  const legacyKey = "imdb-clone:movie-concierge:browser-id";
  const currentKey = "popcorn-society:movie-concierge:browser-id";
  const existing = "browser-12345678-1234-1234-1234-123456789abc";

  it("preserves the voice quota identity and leaves it readable by old tabs", () => {
    localStorage.setItem(legacyKey, existing);
    expect(getConciergeClientId(null)).toBe(`${existing}:anonymous`);
    expect(localStorage.getItem(currentKey)).toBe(existing);
    expect(localStorage.getItem(legacyKey)).toBe(existing);
  });

  it("prefers the current key and rejects corrupt legacy values", () => {
    localStorage.setItem(legacyKey, "broken");
    const created = getConciergeClientId(null);
    expect(created).toMatch(/^browser-[a-f0-9-]{36}:anonymous$/);
    localStorage.setItem(legacyKey, existing);
    expect(getConciergeClientId(null)).toBe(created);
  });

  it("retains the old identity if copying it fails", () => {
    localStorage.setItem(legacyKey, existing);
    const set = vi.spyOn(localStorage, "setItem").mockImplementation(() => {
      throw new Error("storage full");
    });
    expect(getConciergeClientId(null)).toBe(`${existing}:anonymous`);
    set.mockRestore();
  });
});
