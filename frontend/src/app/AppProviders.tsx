import { QueryClientProvider } from "@tanstack/react-query";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { SnackbarProvider } from "notistack";
import { useEffect, type ReactNode } from "react";
import { BrowserRouter } from "react-router";
import { ConciergeExperience } from "../features/concierge";
import { queryClient } from "../shared/api/queryClient";
import { bootstrapSession } from "../shared/auth/bootstrapSession";
import { RouteMetrics } from "../shared/observability";
import { appTheme } from "../theme";

const AppProviders = ({ children }: { children: ReactNode }) => {
  useEffect(() => {
    void bootstrapSession();
  }, []);

  return (
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <SnackbarProvider maxSnack={3}>
          <BrowserRouter>
            <RouteMetrics />
            {children}
            <ConciergeExperience />
          </BrowserRouter>
        </SnackbarProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
};

export default AppProviders;
