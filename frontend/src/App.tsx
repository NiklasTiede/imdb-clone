import { Route, Routes } from "react-router-dom";
import MyAppBar from "./components/AppBarTop";
import Home from "./components/Home";
import Login from "./components/authentication/Login";
import Logout from "./components/authentication/Logout";
import Registration from "./components/authentication/Registration";
import YourRatings from "./components/profile/YourRatings";
import YourWatchlist from "./components/profile/YourWatchlist";
import AccountSettings from "./components/profile/AccountSettings";
import MovieSearch from "./components/movies/MovieSearch";
import { useMode, ColorModeContext } from "./theme";
import { CssBaseline, ThemeProvider } from "@mui/material";
import React from "react";
import EditMovie from "./components/movies/EditMovie";
import Messages from "./components/profile/Messages";

function App() {
  const [theme, colorMode] = useMode();

  return (
    <ColorModeContext.Provider value={colorMode}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <MyAppBar />
        <Routes>
          <Route path="/home" element={<Home />} />

          <Route path="/login" element={<Login />} />
          <Route path="/logout" element={<Logout />} />
          <Route path="/registration" element={<Registration />} />

          <Route path="/your-ratings" element={<YourRatings />} />
          <Route path="/your-watchlist" element={<YourWatchlist />} />
          <Route path="/account-settings" element={<AccountSettings />} />
          <Route path="/your-messages" element={<Messages />} />

          <Route path="/movie-search" element={<MovieSearch />} />
          <Route path="/editing" element={<EditMovie />} />
        </Routes>
      </ThemeProvider>
    </ColorModeContext.Provider>
  );
}

export default App;
