import { QueryClientProvider } from "@tanstack/react-query";
import { SnackbarProvider } from "notistack";
import { useEffect, type ReactNode } from "react";
import { BrowserRouter } from "react-router";
import { queryClient } from "../shared/api/queryClient";
import { bootstrapSession } from "../shared/auth/bootstrapSession";
import { RouteMetrics } from "../shared/observability";

const AppProviders = ({ children }: { children: ReactNode }) => {
  useEffect(() => {
    void bootstrapSession();
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <SnackbarProvider maxSnack={3}>
        <BrowserRouter>
          <RouteMetrics />
          {children}
        </BrowserRouter>
      </SnackbarProvider>
    </QueryClientProvider>
  );
};

export default AppProviders;
