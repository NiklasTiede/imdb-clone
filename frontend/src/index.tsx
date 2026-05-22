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

registerBrowserErrorReporting();
registerWebVitals();

const root = ReactDOM.createRoot(
  document.getElementById("root") as HTMLElement,
);

reportAppBoot(entrypointStart);

root.render(
  // <React.StrictMode>
  <AppProviders>
    <App />
  </AppProviders>,
  // </React.StrictMode>
);
