/** Copy a valid legacy value once; keep it available to tabs running the old app. */
export const readRenamedStorage = (
  storage: Storage,
  key: string,
  legacyKey: string,
  valid: (value: string) => boolean = () => true,
): string | null => {
  const current = storage.getItem(key);
  if (current && valid(current)) return current;
  const legacy = storage.getItem(legacyKey);
  if (!legacy || !valid(legacy)) return null;
  try {
    storage.setItem(key, legacy);
  } catch {
    // Preserve the existing identity even when storage has become read-only/full.
  }
  return legacy;
};
