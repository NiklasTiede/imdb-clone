import { useEffect, useState } from "react";

const canUseStorage = () => typeof window !== "undefined";

export const useLocalStorageState = <T extends string>(
  key: string,
  defaultValue: T,
  allowedValues: readonly T[],
) => {
  const [value, setValue] = useState<T>(() => {
    if (!canUseStorage()) {
      return defaultValue;
    }

    const stored = window.localStorage.getItem(key);
    return allowedValues.includes(stored as T) ? (stored as T) : defaultValue;
  });

  useEffect(() => {
    window.localStorage.setItem(key, value);
  }, [key, value]);

  return [value, setValue] as const;
};
