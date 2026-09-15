/** Standalone design study. Not imported by the app; no microphone or provider connection. */
import "../../../styles/fonts.css";
import { useState, type ReactNode } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter } from "react-router";
import {
  Alert,
  Box,
  Button,
  CssBaseline,
  Divider,
  IconButton,
  Paper,
  Stack,
  ThemeProvider,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import { alpha } from "@mui/material/styles";
import MicRounded from "@mui/icons-material/MicRounded";
import MicOffRounded from "@mui/icons-material/MicOffRounded";
import CloseRounded from "@mui/icons-material/CloseRounded";
import KeyboardRounded from "@mui/icons-material/KeyboardRounded";
import ExpandMoreRounded from "@mui/icons-material/ExpandMoreRounded";
import StopRounded from "@mui/icons-material/StopRounded";
import ChatBubbleOutlineRounded from "@mui/icons-material/ChatBubbleOutlineRounded";
import AutoAwesomeRounded from "@mui/icons-material/AutoAwesomeRounded";
import ArrowForwardRounded from "@mui/icons-material/ArrowForwardRounded";
import CheckRounded from "@mui/icons-material/CheckRounded";
import { appTheme, movieColors } from "../../../theme";
import AppSurface from "../../../shared/layout/AppSurface";
import ConciergeMovieCard from "../components/ConciergeMovieCard";
import catalogScreenshot from "../../../../../docs/assets/imdb-clone-screenshot.webp";

type VoiceState =
  | "ready"
  | "listening"
  | "searching"
  | "speaking"
  | "muted"
  | "error";
const states: Record<VoiceState, { label: string; detail: string }> = {
  ready: {
    label: "Let's find your next movie",
    detail: "Talk naturally. Keep browsing.",
  },
  listening: {
    label: "Listening to you",
    detail: "Try “Find Forrest Gump and open it.”",
  },
  searching: {
    label: "Searching movies…",
    detail: "Checking the catalog for Forrest Gump",
  },
  speaking: {
    label: "Concierge is speaking",
    detail: "You can interrupt at any time",
  },
  muted: { label: "Microphone is off", detail: "Resume when you're ready" },
  error: {
    label: "Microphone unavailable",
    detail: "Allow microphone access or continue with text",
  },
};
const movie = {
  movieId: 1,
  primaryTitle: "Forrest Gump",
  movieType: "movie",
  startYear: 1994,
  runtimeMinutes: 142,
  genres: ["Drama", "Romance"],
  explanation: "The film you asked for, ready to explore.",
};

function Control({
  label,
  onClick,
  children,
  active = false,
}: {
  label: string;
  onClick: () => void;
  children: ReactNode;
  active?: boolean;
}) {
  return (
    <Tooltip title={label}>
      <IconButton
        aria-label={label}
        onClick={onClick}
        sx={{
          width: 44,
          height: 44,
          border: "1px solid",
          borderColor: "divider",
          borderRadius: 1,
          color: active ? movieColors.brand : "text.secondary",
          bgcolor: active ? alpha(movieColors.brand, 0.08) : "transparent",
        }}
      >
        {children}
      </IconButton>
    </Tooltip>
  );
}

function VoiceSignal({
  state,
  compact = false,
}: {
  state: VoiceState;
  compact?: boolean;
}) {
  const color = state === "listening" ? movieColors.info : movieColors.brand;
  const animated = state === "listening" || state === "speaking";
  return (
    <Box
      aria-hidden="true"
      sx={{
        width: compact ? 42 : { xs: 92, sm: 116 },
        height: compact ? 42 : { xs: 92, sm: 116 },
        flexShrink: 0,
        display: "grid",
        placeItems: "center",
        borderRadius: "50%",
        position: "relative",
        color,
        bgcolor: movieColors.surfaceInset,
        opacity: state === "muted" || state === "error" ? 0.45 : 1,
        border: `1px solid ${alpha(color, 0.3)}`,
        boxShadow: compact
          ? "none"
          : `0 0 36px ${alpha(color, 0.08)}, inset 0 0 24px ${alpha(color, 0.06)}`,
        "&::before": {
          content: '""',
          position: "absolute",
          inset: compact ? 4 : 8,
          borderRadius: "50%",
          border: `1px solid ${alpha(color, 0.12)}`,
          ...(state === "searching"
            ? { borderTopColor: color, animation: "orbit 2s linear infinite" }
            : {}),
        },
        "@keyframes orbit": { to: { transform: "rotate(360deg)" } },
        "@keyframes voiceBar": {
          "0%,100%": { transform: "scaleY(.4)" },
          "50%": { transform: "scaleY(1)" },
        },
        "@media (prefers-reduced-motion: reduce)": {
          "&::before, & *": { animation: "none !important" },
        },
      }}
    >
      {animated ? (
        <Stack
          direction="row"
          spacing={compact ? 0.25 : 0.5}
          sx={{ alignItems: "center" }}
        >
          {[10, 20, 32, 22, 38, 26, 14].map((height, index) => (
            <Box
              key={index}
              sx={{
                width: compact ? 2 : 3,
                height: compact ? height * 0.45 : height,
                borderRadius: 2,
                bgcolor: color,
                animation: "voiceBar 1s ease-in-out infinite",
                animationDelay: `${index * -0.17}s`,
              }}
            />
          ))}
        </Stack>
      ) : state === "muted" || state === "error" ? (
        <MicOffRounded />
      ) : (
        <MicRounded sx={{ fontSize: compact ? 18 : 28 }} />
      )}
    </Box>
  );
}

function VoiceDesignPreview() {
  const [state, setState] = useState<VoiceState>("ready");
  const [expanded, setExpanded] = useState(true);
  const [page, setPage] = useState<"catalog" | "movie" | "watchlist">(
    "catalog",
  );
  const [saved, setSaved] = useState(false);
  const [textMode, setTextMode] = useState(false);
  const active = state !== "ready" && state !== "error";
  const end = () => {
    setState("ready");
    setExpanded(false);
  };
  const showMovie = () => {
    setPage("movie");
    setExpanded(false);
    setState("listening");
  };
  const save = () => {
    setSaved(true);
    setPage("watchlist");
    setExpanded(false);
    setState("listening");
  };

  return (
    <Box
      sx={{
        height: "100dvh",
        display: "flex",
        flexDirection: "column",
        "& button": { textTransform: "none" },
      }}
    >
      <Box
        component="header"
        sx={{
          position: "relative",
          zIndex: 1,
          p: 2,
          borderBottom: "1px solid",
          borderColor: "divider",
          bgcolor: movieColors.surface,
        }}
      >
        <Stack direction="row" spacing={1} sx={{ alignItems: "center", mb: 1 }}>
          <AutoAwesomeRounded sx={{ color: movieColors.brand, fontSize: 18 }} />
          <Typography sx={{ fontWeight: 800, fontSize: 13 }}>
            VOICE DESIGN STUDY
          </Typography>
          <Button href="/voice-orb.html" size="small" sx={{ ml: "auto" }}>
            Explore the new voice orb
          </Button>
          <Typography sx={{ color: "text.secondary", fontSize: 11 }}>
            Simulation · no microphone access
          </Typography>
        </Stack>
        <Stack
          direction="row"
          useFlexGap
          spacing={0.5}
          sx={{ flexWrap: "wrap" }}
        >
          {(Object.keys(states) as VoiceState[]).map((value) => (
            <Button
              key={value}
              size="small"
              variant={state === value ? "contained" : "text"}
              onClick={() => {
                setState(value);
                setExpanded(true);
                setTextMode(false);
              }}
              aria-pressed={state === value}
              sx={{ fontSize: 11 }}
            >
              {value === "muted"
                ? "Mic off"
                : value.charAt(0).toUpperCase() + value.slice(1)}
            </Button>
          ))}
          <Button
            size="small"
            onClick={() => setExpanded(!expanded)}
            sx={{ ml: "auto", fontSize: 11 }}
          >
            {expanded ? "Preview compact dock" : "Expand conversation"}
          </Button>
        </Stack>
      </Box>

      <Box
        component="main"
        sx={{
          position: "relative",
          minHeight: 0,
          flex: 1,
          overflow: "hidden",
        }}
      >
        <Box
          component="img"
          src={catalogScreenshot}
          alt="Existing movie catalog design, used as a static layout reference"
          sx={{
            display: "block",
            width: "100%",
            height: "100%",
            objectFit: "cover",
            objectPosition: "left top",
            opacity: expanded ? 0.45 : 0.85,
          }}
        />
        {page !== "catalog" && (
          <Box
            sx={{
              position: "absolute",
              top: 92,
              left: { xs: 16, sm: 32 },
              right: { xs: 16, sm: 32 },
              maxWidth: 640,
            }}
          >
            <AppSurface accent="brand" sx={{ p: { xs: 2, sm: 3 } }}>
              <Typography
                sx={{
                  color: movieColors.brand,
                  fontSize: 11,
                  fontWeight: 750,
                  letterSpacing: 1.5,
                  mb: 1,
                }}
              >
                NAVIGATION PREVIEW
              </Typography>
              <Typography variant="h2" sx={{ fontWeight: 750, mb: 1 }}>
                {page === "movie" ? "Forrest Gump" : "Your watchlist"}
              </Typography>
              <Typography color="text.secondary" sx={{ mb: 2 }}>
                {page === "movie"
                  ? "1994 · 142 min · Drama, Romance"
                  : "Forrest Gump is on your list."}
              </Typography>
              {saved && (
                <Alert
                  severity="success"
                  icon={<CheckRounded />}
                  action={
                    <Button
                      color="inherit"
                      onClick={() => {
                        setSaved(false);
                        setPage("movie");
                      }}
                    >
                      Undo
                    </Button>
                  }
                >
                  Added to your watchlist
                </Alert>
              )}
              <Typography sx={{ mt: 2, color: "text.secondary", fontSize: 12 }}>
                Voice stays connected while you browse.
              </Typography>
              <Button
                sx={{ mt: 1 }}
                onClick={() => {
                  setPage("catalog");
                  setSaved(false);
                }}
              >
                Back to catalog preview
              </Button>
            </AppSurface>
          </Box>
        )}

        {expanded && (
          <Paper
            component="section"
            aria-label="Movie Concierge voice design"
            elevation={0}
            sx={{
              position: "absolute",
              inset: "0 0 0 auto",
              width: { xs: "100%", sm: 440 },
              bgcolor: movieColors.surfaceInset,
              borderLeft: "1px solid",
              borderColor: "divider",
              borderRadius: 0,
              display: "flex",
              flexDirection: "column",
              height: "100%",
              minHeight: 0,
              boxShadow: "-24px 0 60px rgba(0,0,0,.35)",
            }}
          >
            <Stack
              direction="row"
              spacing={1}
              sx={{
                alignItems: "center",
                p: 2,
                borderBottom: "1px solid",
                borderColor: "divider",
              }}
            >
              <AutoAwesomeRounded sx={{ color: movieColors.brand }} />
              <Box sx={{ flex: 1 }}>
                <Typography sx={{ fontWeight: 750, fontSize: 14 }}>
                  Movie Concierge
                </Typography>
                <Typography sx={{ color: "text.secondary", fontSize: 11 }}>
                  Your next movie starts here
                </Typography>
              </Box>
              <Control
                label="Minimize conversation"
                onClick={() => setExpanded(false)}
              >
                <ExpandMoreRounded />
              </Control>
            </Stack>
            <Box
              sx={{
                flex: 1,
                minHeight: 0,
                overflowY: "auto",
                p: { xs: 2, sm: 2.5 },
              }}
            >
              <Stack
                sx={{ alignItems: "center", pt: 1, pb: 3, textAlign: "center" }}
              >
                <VoiceSignal state={state} />
                <Typography
                  role="status"
                  sx={{
                    mt: 2,
                    fontSize: 19,
                    fontWeight: 750,
                    letterSpacing: -0.4,
                  }}
                >
                  {states[state].label}
                </Typography>
                <Typography
                  sx={{ mt: 0.8, fontSize: 12, color: "text.secondary" }}
                >
                  {states[state].detail}
                </Typography>
              </Stack>
              {state === "ready" ? (
                <>
                  <Button
                    variant="contained"
                    size="large"
                    fullWidth
                    startIcon={<MicRounded />}
                    onClick={() => {
                      setState("listening");
                      setTextMode(false);
                    }}
                    sx={{ fontWeight: 750 }}
                  >
                    Start voice
                  </Button>
                  <Typography
                    sx={{
                      mt: 1.2,
                      mb: 3,
                      fontSize: 11,
                      textAlign: "center",
                      color: "text.secondary",
                    }}
                  >
                    Microphone turns on only when you start.
                  </Typography>
                  <Divider sx={{ mb: 2 }} />
                  <Typography
                    sx={{
                      mb: 1,
                      fontSize: 11,
                      color: "text.secondary",
                      letterSpacing: 1,
                    }}
                  >
                    TRY SAYING
                  </Typography>
                  {[
                    "Find Forrest Gump and open it.",
                    "Suggest something uplifting tonight.",
                  ].map((prompt) => (
                    <Button
                      key={prompt}
                      fullWidth
                      variant="outlined"
                      onClick={() => setState("listening")}
                      sx={{
                        justifyContent: "flex-start",
                        mb: 1,
                        py: 1.2,
                        borderColor: "divider",
                        color: "text.primary",
                        fontSize: 12,
                      }}
                    >
                      {prompt}
                    </Button>
                  ))}
                </>
              ) : (
                <>
                  {state === "error" ? (
                    <Alert severity="warning">
                      Voice couldn't start. Check your microphone permission or
                      type below.
                    </Alert>
                  ) : (
                    <>
                      <Typography
                        sx={{
                          color: movieColors.info,
                          fontSize: 10,
                          fontWeight: 750,
                          mb: 0.7,
                        }}
                      >
                        YOU
                      </Typography>
                      <Box
                        sx={{
                          bgcolor: alpha(movieColors.info, 0.1),
                          borderLeft: `2px solid ${movieColors.info}`,
                          p: 1.5,
                          mb: 2,
                          fontSize: 13,
                        }}
                      >
                        Find Forrest Gump and open it.
                      </Box>
                      {state !== "searching" && (
                        <>
                          <Typography
                            sx={{
                              color: movieColors.brand,
                              fontSize: 10,
                              fontWeight: 750,
                              mb: 0.7,
                            }}
                          >
                            CONCIERGE
                          </Typography>
                          <Typography
                            sx={{ fontSize: 13, lineHeight: 1.7, mb: 1.5 }}
                          >
                            Found it. Forrest Gump is 142 minutes long.
                          </Typography>
                          <Box
                            onClickCapture={(event) => {
                              if ((event.target as HTMLElement).closest("a")) {
                                event.preventDefault();
                                showMovie();
                              }
                            }}
                          >
                            <ConciergeMovieCard movie={movie} />
                          </Box>
                          <Stack direction="row" spacing={1} sx={{ mt: 1.3 }}>
                            <Button
                              variant="outlined"
                              fullWidth
                              endIcon={<ArrowForwardRounded />}
                              onClick={showMovie}
                            >
                              Open movie
                            </Button>
                            <Button fullWidth onClick={save}>
                              Add to watchlist
                            </Button>
                          </Stack>
                          <Typography
                            sx={{
                              fontSize: 10,
                              color: "text.secondary",
                              mt: 1,
                            }}
                          >
                            Example result · actions are simulated
                          </Typography>
                        </>
                      )}
                    </>
                  )}
                </>
              )}
            </Box>
            <Box
              sx={{
                borderTop: "1px solid",
                borderColor: "divider",
                bgcolor: movieColors.surface,
                p: 2,
                pb: "max(16px, env(safe-area-inset-bottom))",
              }}
            >
              {active && (
                <Stack
                  direction="row"
                  spacing={1}
                  sx={{ justifyContent: "center", mb: 1.5 }}
                >
                  <Control
                    label={
                      state === "muted"
                        ? "Resume microphone"
                        : "Mute microphone"
                    }
                    active={state === "muted"}
                    onClick={() =>
                      setState(state === "muted" ? "listening" : "muted")
                    }
                  >
                    <MicOffRounded />
                  </Control>
                  {state === "speaking" && (
                    <Button
                      variant="outlined"
                      startIcon={<StopRounded />}
                      onClick={() => setState("listening")}
                    >
                      Interrupt
                    </Button>
                  )}
                  <Control label="End voice session" onClick={end}>
                    <CloseRounded />
                  </Control>
                </Stack>
              )}
              {textMode ? (
                <Box
                  component="form"
                  onSubmit={(event) => {
                    event.preventDefault();
                    setState("searching");
                  }}
                  sx={{ display: "flex", gap: 1 }}
                >
                  <TextField
                    size="small"
                    fullWidth
                    slotProps={{
                      htmlInput: { "aria-label": "Message the Concierge" },
                    }}
                    placeholder="Type a message…"
                    sx={{
                      minWidth: 0,
                      bgcolor: movieColors.surfaceInset,
                    }}
                  />
                  <Button type="submit">Send</Button>
                </Box>
              ) : (
                <Button
                  fullWidth
                  startIcon={<KeyboardRounded />}
                  onClick={() => {
                    setTextMode(true);
                    if (active) setState("muted");
                  }}
                  sx={{ color: "text.secondary" }}
                >
                  Prefer typing? Open text chat
                </Button>
              )}
            </Box>
          </Paper>
        )}
      </Box>

      {!expanded && (
        <Paper
          elevation={0}
          sx={{
            position: "fixed",
            right: { xs: 12, sm: 24 },
            left: { xs: 12, sm: "auto" },
            bottom: "max(18px, env(safe-area-inset-bottom))",
            zIndex: 3,
            width: { xs: "auto", sm: active ? 410 : "auto" },
            bgcolor: alpha(movieColors.surfaceElevated, 0.97),
            border: `1px solid ${alpha(movieColors.brand, 0.3)}`,
            borderRadius: 1,
            boxShadow: "0 12px 36px rgba(0,0,0,.45)",
            p: 1.25,
          }}
        >
          {active ? (
            <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
              {state === "speaking" ? (
                <Tooltip title="Interrupt response">
                  <IconButton
                    aria-label="Interrupt response"
                    onClick={() => setState("listening")}
                    sx={{ p: 0, width: 44, height: 44 }}
                  >
                    <VoiceSignal state={state} compact />
                  </IconButton>
                </Tooltip>
              ) : (
                <VoiceSignal state={state} compact />
              )}
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography sx={{ fontSize: 12, fontWeight: 750 }}>
                  Movie Concierge
                </Typography>
                <Typography sx={{ fontSize: 11, color: "text.secondary" }}>
                  {states[state].label}
                </Typography>
              </Box>
              <Control
                label={
                  state === "muted" ? "Resume microphone" : "Mute microphone"
                }
                onClick={() =>
                  setState(state === "muted" ? "listening" : "muted")
                }
              >
                <MicOffRounded fontSize="small" />
              </Control>
              <Control
                label="Expand conversation"
                onClick={() => setExpanded(true)}
              >
                <ChatBubbleOutlineRounded fontSize="small" />
              </Control>
              <Control label="End voice session" onClick={end}>
                <CloseRounded fontSize="small" />
              </Control>
            </Stack>
          ) : (
            <Button
              startIcon={<AutoAwesomeRounded />}
              onClick={() => setExpanded(true)}
              sx={{ fontWeight: 750 }}
            >
              Ask Concierge
            </Button>
          )}
        </Paper>
      )}
    </Box>
  );
}

const root = document.getElementById("root");
if (root)
  createRoot(root).render(
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      <MemoryRouter>
        <VoiceDesignPreview />
      </MemoryRouter>
    </ThemeProvider>,
  );
