import type * as TestingLibrary from "@testing-library/react";
import type { ComponentType, ReactNode } from "react";
import { vi } from "vitest";
import { AppThemeProvider } from "../theme";

type Wrapper = ComponentType<{ children: ReactNode }>;

// Components read theme tokens (surface, accent, star, ...). Render every test
// inside the application theme so they see the same tokens as production.
vi.mock("@testing-library/react", async (importOriginal) => {
  const actual = await importOriginal<typeof TestingLibrary>();

  const withAppTheme = (wrapper: unknown): Wrapper => {
    const Inner = wrapper as Wrapper | undefined;
    const AppThemeWrapper = ({ children }: { children: ReactNode }) => (
      <AppThemeProvider>
        {Inner ? <Inner>{children}</Inner> : children}
      </AppThemeProvider>
    );
    return AppThemeWrapper;
  };

  const render = ((
    ui: Parameters<typeof actual.render>[0],
    options?: TestingLibrary.RenderOptions,
  ) =>
    actual.render(ui, {
      ...options,
      wrapper: withAppTheme(options?.wrapper),
    })) as typeof actual.render;

  const renderHook = (<Result, Props>(
    callback: (props: Props) => Result,
    options?: TestingLibrary.RenderHookOptions<Props>,
  ) =>
    actual.renderHook(callback, {
      ...options,
      wrapper: withAppTheme(options?.wrapper),
    })) as typeof actual.renderHook;

  return { ...actual, render, renderHook };
});
