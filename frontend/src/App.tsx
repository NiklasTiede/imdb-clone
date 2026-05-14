import { Route, Routes } from "react-router";
import { appTheme } from "./theme";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { Suspense } from "react";
import PageContent from "./shared/layout/PageContent";
import Surface from "./shared/layout/Surface";
import { routeDefinitions } from "./app/routes/routeDefinitions";

function App() {
  return (
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      <Suspense fallback={<RouteFallback />}>
        <Routes>
          {routeDefinitions.map((route) => (
            <Route key={route.path} path={route.path} element={route.element} />
          ))}
        </Routes>
      </Suspense>
    </ThemeProvider>
  );
}

const RouteFallback = () => (
  <PageContent maxWidth="760px">
    <Surface sx={{ p: 3, mt: 4 }}>Loading...</Surface>
  </PageContent>
);

export default App;
