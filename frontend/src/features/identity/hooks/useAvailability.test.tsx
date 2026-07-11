import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";

import { useAvailability } from "./useAvailability";

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe("useAvailability", () => {
  it("stays idle while disabled", () => {
    const checkFn = vi.fn();

    const { result } = renderHook(
      () =>
        useAvailability({
          checkFn,
          enabled: false,
          value: "les_grossman",
        }),
      { wrapper: createWrapper() },
    );

    expect(result.current.status).toBe("idle");
    expect(checkFn).not.toHaveBeenCalled();
  });

  it("reports available after a successful debounced check", async () => {
    const checkFn = vi.fn().mockResolvedValue({ isAvailable: true });

    const { result } = renderHook(
      () =>
        useAvailability({
          checkFn,
          debounceMs: 0,
          enabled: true,
          value: "les_grossman",
        }),
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(result.current.status).toBe("available"));
    expect(checkFn).toHaveBeenCalledWith("les_grossman");
  });

  it("reports taken when the backend returns unavailable", async () => {
    const checkFn = vi.fn().mockResolvedValue({ isAvailable: false });

    const { result } = renderHook(
      () =>
        useAvailability({
          checkFn,
          debounceMs: 0,
          enabled: true,
          value: "admin",
        }),
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(result.current.status).toBe("taken"));
  });

  it("reports a failed availability check without treating the value as taken", async () => {
    const checkFn = vi.fn().mockRejectedValue(new Error("network unavailable"));

    const { result } = renderHook(
      () =>
        useAvailability({
          checkFn,
          debounceMs: 0,
          enabled: true,
          value: "movie_fan",
        }),
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(result.current.status).toBe("error"));
  });
});
