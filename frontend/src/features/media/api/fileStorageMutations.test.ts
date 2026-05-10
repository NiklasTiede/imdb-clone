import { describe, expect, test, vi } from "vitest";
import { fileStorageApi } from "../../../shared/api/moviesApi";
import {
  deleteUserProfilePhoto,
  mediaMutationKeys,
} from "./fileStorageMutations";

describe("mediaMutationKeys", () => {
  it("uses the current profile query key for profile photo invalidation", () => {
    expect(mediaMutationKeys.currentProfile).toEqual([
      "account",
      "current-profile",
    ]);
  });

  test("deletes the current user's profile photo", async () => {
    const deleteSpy = vi
      .spyOn(fileStorageApi, "deleteUserProfilePhoto")
      .mockResolvedValue({} as never);

    await deleteUserProfilePhoto();

    expect(deleteSpy).toHaveBeenCalled();
    deleteSpy.mockRestore();
  });
});
