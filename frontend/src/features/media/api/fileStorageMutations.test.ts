import { mediaMutationKeys } from "./fileStorageMutations";

describe("mediaMutationKeys", () => {
  it("uses the current profile query key for profile photo invalidation", () => {
    expect(mediaMutationKeys.currentProfile).toEqual([
      "account",
      "current-profile",
    ]);
  });
});
