import { QueryClientProvider } from "@tanstack/react-query";
import { SnackbarProvider } from "notistack";
import type { ReactNode } from "react";
import { BrowserRouter } from "react-router";
import { queryClient } from "../shared/api/queryClient";
import { RouteMetrics } from "../shared/observability";

const AppProviders = ({ children }: { children: ReactNode }) => (
  <QueryClientProvider client={queryClient}>
    <SnackbarProvider maxSnack={3}>
      <BrowserRouter>
        <RouteMetrics />
        {children}
      </BrowserRouter>
    </SnackbarProvider>
  </QueryClientProvider>
);

export default AppProviders;
