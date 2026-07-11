import { Route, Routes } from "react-router";
import { appTheme } from "./theme";
import {
  Box,
  CircularProgress,
  CssBaseline,
  ThemeProvider,
} from "@mui/material";
import { Suspense } from "react";
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
  <Box
    sx={{
      alignItems: "center",
      bgcolor: "background.default",
      display: "flex",
      justifyContent: "center",
      minHeight: "100dvh",
    }}
  >
    <CircularProgress aria-label="Loading page" size={28} />
  </Box>
);

export default App;
