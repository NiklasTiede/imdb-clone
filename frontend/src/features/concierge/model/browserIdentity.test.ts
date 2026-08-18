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
