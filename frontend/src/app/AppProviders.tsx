import { QueryClientProvider } from "@tanstack/react-query";
import { SnackbarProvider } from "notistack";
import type { ReactNode } from "react";
import { Provider } from "react-redux";
import { BrowserRouter } from "react-router";
import { store } from "../redux/store";
import { queryClient } from "../shared/api/queryClient";

const AppProviders = ({ children }: { children: ReactNode }) => (
  <Provider store={store}>
    <QueryClientProvider client={queryClient}>
      <SnackbarProvider maxSnack={3}>
        <BrowserRouter>{children}</BrowserRouter>
      </SnackbarProvider>
    </QueryClientProvider>
  </Provider>
);

export default AppProviders;
