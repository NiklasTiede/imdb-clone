import { QueryClientProvider } from "@tanstack/react-query";
import { SnackbarProvider } from "notistack";
import { lazy, Suspense, useEffect, type ReactNode } from "react";
import { BrowserRouter } from "react-router";
import { ConciergeExperience } from "../features/concierge";
import { queryClient } from "../shared/api/queryClient";
import { bootstrapSession } from "../shared/auth/bootstrapSession";
import { RouteMetrics } from "../shared/observability";
import { AppThemeProvider } from "../theme";
import SiteMetadata from "../shared/seo/SiteMetadata";

const ThemeSwitcher =
  import.meta.env.DEV && import.meta.env.MODE !== "test"
    ? lazy(() => import("./dev/ThemeSwitcher"))
    : null;

const AppProviders = ({ children }: { children: ReactNode }) => {
  useEffect(() => {
    void bootstrapSession();
  }, []);

  return (
    <AppThemeProvider>
      <QueryClientProvider client={queryClient}>
        <SnackbarProvider maxSnack={3}>
          <BrowserRouter>
            <SiteMetadata />
            <RouteMetrics />
            {children}
            <ConciergeExperience />
            {ThemeSwitcher && (
              <Suspense fallback={null}>
                <ThemeSwitcher />
              </Suspense>
            )}
          </BrowserRouter>
        </SnackbarProvider>
      </QueryClientProvider>
    </AppThemeProvider>
  );
};

export default AppProviders;
