import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import type { UserIdentityAvailability } from "../../../client/movies/generator-output";

type UseAvailabilityArgs = {
  checkFn: (value: string) => Promise<UserIdentityAvailability>;
  debounceMs?: number;
  enabled: boolean;
  value: string;
};

export type AvailabilityState =
  | { status: "idle" }
  | { status: "checking" }
  | { status: "available" }
  | { status: "taken" }
  | { status: "error" };

export const useAvailability = ({
  checkFn,
  debounceMs = 400,
  enabled,
  value,
}: UseAvailabilityArgs): AvailabilityState => {
  const debouncedValue = useDebouncedValue(value.trim(), debounceMs);
  const query = useQuery({
    enabled: enabled && debouncedValue.length > 0,
    queryFn: () => checkFn(debouncedValue),
    queryKey: ["identity-availability", checkFn.name, debouncedValue],
  });

  if (!enabled || debouncedValue.length === 0) {
    return { status: "idle" };
  }
  if (query.isPending || query.isFetching) {
    return { status: "checking" };
  }
  if (query.isError) {
    return { status: "error" };
  }
  return query.data?.isAvailable === false
    ? { status: "taken" }
    : { status: "available" };
};

const useDebouncedValue = (value: string, debounceMs: number): string => {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const timeoutId = window.setTimeout(
      () => setDebouncedValue(value),
      debounceMs,
    );
    return () => window.clearTimeout(timeoutId);
  }, [debounceMs, value]);

  return debouncedValue;
};
