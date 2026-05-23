import "./styles/fonts.css";
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import AppProviders from "./app/AppProviders";
import {
  registerBrowserErrorReporting,
  registerWebVitals,
  reportAppBoot,
} from "./shared/observability";

const entrypointStart = performance.now();

const cleanupBrowserErrorReporting = registerBrowserErrorReporting();
registerWebVitals();

const root = ReactDOM.createRoot(
  document.getElementById("root") as HTMLElement,
);

root.render(
  // <React.StrictMode>
  <AppProviders>
    <App />
  </AppProviders>,
  // </React.StrictMode>
);

reportAppBoot(entrypointStart);

if (import.meta.hot) {
  import.meta.hot.dispose(cleanupBrowserErrorReporting);
}
