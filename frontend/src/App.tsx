import { Route, Routes } from "react-router";
import { Box, CircularProgress } from "@mui/material";
import { Suspense } from "react";
import { routeDefinitions } from "./app/routes/routeDefinitions";

function App() {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
        {routeDefinitions.map((route) => (
          <Route key={route.path} path={route.path} element={route.element} />
        ))}
      </Routes>
    </Suspense>
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
