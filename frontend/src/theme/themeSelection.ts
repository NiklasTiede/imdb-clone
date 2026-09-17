import { defaultThemeId, isAppThemeId, type AppThemeId } from "./themes";

export const themeStorageKey = "popcorn-society.theme";

type StorageLike = Pick<Storage, "getItem" | "setItem">;

type ResolveThemeIdOptions = {
  /** Only development builds may override the default theme for now. */
  allowOverride: boolean;
  search: string;
  storage?: StorageLike | undefined;
};

/**
 * Picks the active theme. `?theme=<id>` wins and is remembered in storage so
 * in-app navigation keeps it; otherwise the stored choice, otherwise the default.
 * An account-level preference can later be passed in the same way.
 */
export const resolveThemeId = ({
  allowOverride,
  search,
  storage,
}: ResolveThemeIdOptions): AppThemeId => {
  if (!allowOverride) return defaultThemeId;

  const fromQuery = new URLSearchParams(search).get("theme");
  if (isAppThemeId(fromQuery)) {
    persistThemeId(fromQuery, storage);
    return fromQuery;
  }

  const stored = readStorage(storage);
  return isAppThemeId(stored) ? stored : defaultThemeId;
};

export const persistThemeId = (
  themeId: AppThemeId,
  storage: StorageLike | undefined,
) => {
  try {
    storage?.setItem(themeStorageKey, themeId);
  } catch {
    // Storage can be unavailable (private mode); the choice then lasts for the page.
  }
};

const readStorage = (storage: StorageLike | undefined) => {
  try {
    return storage?.getItem(themeStorageKey) ?? null;
  } catch {
    return null;
  }
};
