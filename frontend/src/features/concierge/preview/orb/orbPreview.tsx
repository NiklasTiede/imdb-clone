/** Standalone design study; deliberately not imported by the production app. */
import "../../../../styles/fonts.css";
import { useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Alert,
  Box,
  Button,
  CssBaseline,
  Divider,
  IconButton,
  Stack,
  ThemeProvider,
  Tooltip,
  Typography,
} from "@mui/material";
import { alpha } from "@mui/material/styles";
import MicRounded from "@mui/icons-material/MicRounded";
import MicOffRounded from "@mui/icons-material/MicOffRounded";
import StopRounded from "@mui/icons-material/StopRounded";
import ExpandMoreRounded from "@mui/icons-material/ExpandMoreRounded";
import ChatBubbleOutlineRounded from "@mui/icons-material/ChatBubbleOutlineRounded";
import ArrowOutwardRounded from "@mui/icons-material/ArrowOutwardRounded";
import SearchRounded from "@mui/icons-material/SearchRounded";
import { appTheme, movieColors } from "../../../../theme";
import { STOPS, VoiceOrb, restingStop, type OrbState } from "./VoiceOrb";
import { ConversationPreview } from "./ConversationPreview";
import { usePreviewMicrophone } from "./usePreviewMicrophone";
import catalogScreenshot from "../../../../../../docs/assets/imdb-clone-screenshot.webp";

const states: { state: OrbState; title: string; description: string }[] = [
  { state: "ready", title: "Ready", description: "Half open, waiting" },
  {
    state: "listening",
    title: "Listening",
    description: "Opens as you speak",
  },
  { state: "thinking", title: "Thinking", description: "Racking for a match" },
  { state: "speaking", title: "Speaking", description: "Gold floods the gate" },
  { state: "muted", title: "Mic off", description: "Blades closed" },
];
const labels: Record<OrbState, [string, string]> = {
  ready: [
    "Your movies. Just ask.",
    "Find a film, play a trailer, or make it personal.",
  ],

  listening: ["Listening to you", "The aperture opens with your voice."],
  thinking: [
    "Finding the right film",
    "Racking through the catalog for a match.",
  ],
  speaking: ["Concierge is speaking", "The reply prints in gold."],
  muted: ["Microphone is off", "Blades closed. Resume when you're ready."],
};

/** Every footprint the lens has to survive, smallest first. The panel above
 *  draws it at 340 px, which is the largest it would ever appear. */
const sizes = [
  { label: "Dock", width: 56, note: "minimised session" },
  { label: "Compact", width: 120, note: "inline status" },
  { label: "Drawer", width: 220, note: "440 px panel" },
];

function OrbPreview() {
  const [state, setState] = useState<OrbState>("listening");
  const [expanded, setExpanded] = useState(true);
  const [details, setDetails] = useState(true);
  const indexMark = useRef<HTMLSpanElement>(null);
  const mic = usePreviewMicrophone();
  const live = mic.status === "on" || mic.status === "muted";
  const visualState = mic.status === "muted" ? "muted" : state;
  const statusText =
    mic.status === "starting"
      ? "Opening your microphone"
      : labels[visualState][0];
  const end = () => {
    mic.stop();
    setState("ready");
    setExpanded(false);
  };
  const start = () => {
    setState("listening");
    setExpanded(true);
    void mic.start();
  };

  return (
    <Box
      sx={{
        minHeight: "100dvh",
        bgcolor: movieColors.backdrop,
        "& button": { textTransform: "none" },
      }}
    >
      <Box
        component="header"
        sx={{
          px: { xs: 2, md: 4 },
          py: 2,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          borderBottom: "1px solid",
          borderColor: "divider",
        }}
      >
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
          <Box
            component="img"
            src="/brand-logo.svg"
            alt=""
            sx={{ width: 30, height: 30 }}
          />
          <Typography sx={{ fontWeight: 800, fontSize: 13 }}>
            MOVIE CONCIERGE{" "}
            <Box
              component="span"
              sx={{
                color: "text.secondary",
                fontWeight: 400,
                ml: 1.5,
                display: { xs: "none", sm: "inline" },
              }}
            >
              Voice exploration
            </Box>
          </Typography>
        </Stack>
        <Typography
          sx={{ color: "text.secondary", fontSize: 11, letterSpacing: 1.5 }}
        >
          DESIGN STUDY / 02
        </Typography>
      </Box>

      <Box
        component="main"
        sx={{
          maxWidth: 1600,
          mx: "auto",
          display: "grid",
          gridTemplateColumns: { xs: "1fr", lg: "270px minmax(0, 1fr)" },
          gap: { xs: 3, lg: 5 },
          p: { xs: 2, md: 4 },
          pt: { md: 5 },
        }}
      >
        <Box component="aside" aria-label="Design controls">
          <Typography
            sx={{
              color: movieColors.brand,
              fontSize: 10,
              letterSpacing: 2,
              fontWeight: 800,
              mb: 1.5,
            }}
          >
            APERTURE AS INTERFACE
          </Typography>
          <Typography
            component="h1"
            sx={{
              fontSize: { xs: 30, lg: 38 },
              lineHeight: 1.15,
              letterSpacing: -1.4,
              fontWeight: 650,
            }}
          >
            It opens when
            <br />
            you speak.
          </Typography>
          <Typography
            sx={{
              mt: 2,
              color: "text.secondary",
              lineHeight: 1.8,
              fontSize: 13,
              maxWidth: 380,
            }}
          >
            Six blades, one gate. Your voice widens the aperture; the engraved
            ring keeps the last four seconds of it. Blue is you, gold is the
            reply.
          </Typography>
          <Box sx={{ mt: 3.5 }}>
            <Typography
              sx={{
                fontSize: 10,
                letterSpacing: 1.7,
                color: "text.secondary",
                mb: 1.5,
              }}
            >
              FIVE STOPS
            </Typography>
            <Stack
              spacing={0.6}
              sx={{
                flexDirection: { xs: "row", lg: "column" },
                flexWrap: "wrap",
                gap: 0.5,
              }}
            >
              {states.map((item) => (
                <Button
                  key={item.state}
                  aria-pressed={visualState === item.state}
                  onClick={() => {
                    if (item.state !== "muted" && mic.status === "muted")
                      mic.toggleMute();
                    setState(item.state);
                    if (item.state === "muted" && mic.status === "on")
                      mic.toggleMute();
                    setExpanded(true);
                  }}
                  sx={{
                    justifyContent: "flex-start",
                    textAlign: "left",
                    p: 1.3,
                    color:
                      visualState === item.state
                        ? "text.primary"
                        : "text.secondary",
                    border: "1px solid",
                    borderColor:
                      visualState === item.state
                        ? alpha(movieColors.info, 0.3)
                        : "transparent",
                    bgcolor:
                      visualState === item.state
                        ? alpha(movieColors.info, 0.06)
                        : "transparent",
                  }}
                >
                  <Typography
                    component="span"
                    sx={{
                      color:
                        visualState === item.state
                          ? movieColors.brand
                          : "text.secondary",
                      fontSize: 11,
                      fontVariantNumeric: "tabular-nums",
                      width: 34,
                      flexShrink: 0,
                      mr: 1.6,
                      textAlign: "right",
                      display: "inline-block",
                    }}
                  >
                    ƒ/{restingStop(item.state)}
                  </Typography>
                  <Box>
                    <Typography
                      component="span"
                      sx={{ fontWeight: 700, fontSize: 12 }}
                    >
                      {item.title}
                    </Typography>
                    <Typography
                      sx={{
                        fontSize: 10,
                        color: "text.secondary",
                        mt: 0.3,
                        display: { xs: "none", lg: "block" },
                      }}
                    >
                      {item.description}
                    </Typography>
                  </Box>
                </Button>
              ))}
            </Stack>
          </Box>
          <Divider sx={{ my: 3 }} />
          <Typography sx={{ fontWeight: 700, fontSize: 13, mb: 1 }}>
            Try your own voice
          </Typography>
          <Typography
            sx={{
              fontSize: 12,
              lineHeight: 1.8,
              color: "text.secondary",
              mb: 1.7,
            }}
          >
            The microphone test stays in this browser. No recording or upload.
            Agent replies and transcripts are simulated.
          </Typography>
          <Button
            fullWidth
            variant="outlined"
            startIcon={live ? <StopRounded /> : <MicRounded />}
            disabled={mic.status === "starting"}
            onClick={live ? mic.stop : start}
            sx={{
              minHeight: 44,
              borderColor: alpha(movieColors.info, 0.4),
              color: movieColors.info,
            }}
          >
            {live
              ? "Stop microphone test"
              : mic.status === "starting"
                ? "Requesting access…"
                : "Test my microphone"}
          </Button>
          {mic.status === "starting" && (
            <Button fullWidth onClick={mic.stop}>
              Cancel microphone request
            </Button>
          )}
          {mic.error && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              {mic.error}
            </Alert>
          )}
          <Typography
            role="status"
            sx={{
              fontSize: 11,
              color: live ? movieColors.info : "text.secondary",
              mt: 1.5,
            }}
          >
            {live
              ? mic.status === "muted"
                ? "Local microphone connected · muted"
                : "Local microphone connected"
              : "Simulated audio · microphone off"}
          </Typography>
        </Box>

        <Box sx={{ minWidth: 0 }}>
          <Box
            sx={{
              display: "flex",
              justifyContent: "space-between",
              mb: 1.5,
              gap: 2,
            }}
          >
            <Typography sx={{ fontSize: 11, color: "text.secondary" }}>
              The voice moment
            </Typography>
            <Typography sx={{ fontSize: 11, color: "text.secondary" }}>
              Placement is exploratory
            </Typography>
          </Box>
          <Box
            sx={{
              position: "relative",
              isolation: "isolate",
              minHeight: { xs: 650, md: 730 },
              overflow: "hidden",
              borderRadius: 1,
              border: "1px solid",
              borderColor: "divider",
              bgcolor: movieColors.surfaceInset,
            }}
          >
            <Box
              aria-hidden="true"
              sx={{
                height: 64,
                px: 2.5,
                display: "flex",
                alignItems: "center",
                gap: 2,
                borderBottom: "1px solid",
                borderColor: "divider",
                bgcolor: movieColors.surface,
              }}
            >
              <Box
                component="img"
                src="/brand-logo.svg"
                alt=""
                sx={{ width: 28, height: 28 }}
              />
              <Typography
                sx={{
                  fontSize: 12,
                  fontWeight: 800,
                  display: { xs: "none", sm: "block" },
                }}
              >
                Movie Browser
              </Typography>
              <Box
                sx={{
                  display: "flex",
                  gap: 1,
                  alignItems: "center",
                  flex: 1,
                  mx: { sm: 3 },
                  px: 1.5,
                  py: 1,
                  bgcolor: movieColors.backdrop,
                  borderRadius: 1,
                  color: "text.secondary",
                  fontSize: 11,
                }}
              >
                <SearchRounded sx={{ fontSize: 17 }} />
                Search movies
              </Box>
              <Box
                sx={{
                  width: 29,
                  height: 29,
                  borderRadius: "50%",
                  bgcolor: movieColors.surfaceElevated,
                  display: "grid",
                  placeItems: "center",
                  fontSize: 10,
                }}
              >
                NT
              </Box>
            </Box>
            <Box
              component="img"
              src={catalogScreenshot}
              alt="Static movie catalog background for the design study"
              sx={{
                position: "absolute",
                top: 64,
                width: "100%",
                height: "calc(100% - 64px)",
                objectFit: "cover",
                objectPosition: "center top",
                opacity: 0.2,
                filter: "blur(2px)",
                zIndex: -2,
              }}
            />
            <Box
              sx={{
                position: "absolute",
                inset: "64px 0 0",
                background: `linear-gradient(180deg, ${alpha(movieColors.surfaceInset, 0.25)}, ${alpha(movieColors.surfaceInset, 0.9)})`,
                zIndex: -1,
              }}
            />
            <Stack sx={{ alignItems: "center", pt: 2.5, px: 1.5, pb: 8 }}>
              <Button
                variant="outlined"
                startIcon={<MicRounded sx={{ fontSize: "16px !important" }} />}
                onClick={() => {
                  if (!live && mic.status !== "starting") start();
                  else if (!expanded) setExpanded(true);
                  else setExpanded(false);
                }}
                sx={{
                  minHeight: 40,
                  px: 2,
                  borderRadius: 8,
                  borderColor: alpha(movieColors.info, 0.32),
                  bgcolor: movieColors.surface,
                  color: "text.primary",
                  fontSize: 11,
                  boxShadow: "0 4px 20px #0004",
                }}
              >
                {live ? "Voice is on" : "Start voice"}
                <Box
                  component="span"
                  sx={{
                    width: 5,
                    height: 5,
                    borderRadius: "50%",
                    bgcolor: live ? movieColors.info : "text.secondary",
                    ml: 1.5,
                  }}
                />
              </Button>
              {expanded && !details && (
                <Box
                  component="section"
                  aria-label="Voice panel"
                  sx={{
                    width: "100%",
                    maxWidth: 380,
                    mt: 1.5,
                    border: "1px solid",
                    borderColor: alpha(movieColors.info, 0.17),
                    borderRadius: 1,
                    background: `linear-gradient(150deg, ${alpha(movieColors.surfaceElevated, 0.97)}, ${alpha(movieColors.surfaceInset, 0.98)} 70%)`,
                    boxShadow: "0 24px 80px #0009",
                    overflow: "hidden",
                  }}
                >
                  <Stack
                    direction="row"
                    sx={{
                      alignItems: "center",
                      justifyContent: "space-between",
                      pl: 2.5,
                      pr: 1,
                      pt: 0.7,
                    }}
                  >
                    <Typography
                      sx={{
                        fontSize: 10,
                        letterSpacing: 1.6,
                        color: "text.secondary",
                        fontWeight: 750,
                      }}
                    >
                      MOVIE CONCIERGE
                    </Typography>
                    <Tooltip title="Minimize voice panel">
                      <IconButton
                        aria-label="Minimize voice panel"
                        onClick={() => setExpanded(false)}
                        sx={{ width: 44, height: 44 }}
                      >
                        <ExpandMoreRounded fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                  <Box
                    sx={{
                      width: "100%",
                      maxWidth: { xs: 300, sm: 340 },
                      mx: "auto",
                      mt: -1,
                      mb: -1.5,
                    }}
                  >
                    <VoiceOrb
                      state={visualState}
                      live={live}
                      readLevel={mic.readLevel}
                      readoutRef={indexMark}
                    />
                  </Box>
                  {/* The engraved stop scale, read by the sliding index mark
                      the way a lens barrel is read. */}
                  <Box
                    aria-hidden="true"
                    sx={{
                      position: "relative",
                      width: "100%",
                      maxWidth: 246,
                      mx: "auto",
                      mt: 1.5,
                      pt: 2.2,
                    }}
                  >
                    <Box
                      component="span"
                      ref={indexMark}
                      data-stop="4"
                      sx={{
                        "--stop-index": 3,
                        position: "absolute",
                        top: 6,
                        left: "calc((var(--stop-index) + 0.5) * (100% / 9))",
                        transform: "translateX(-50%)",
                        width: "1px",
                        height: 9,
                        bgcolor: movieColors.brand,
                        transition: "left 110ms linear",
                        "@media (prefers-reduced-motion: reduce)": {
                          transition: "none",
                        },
                        "&::after": {
                          content: '"ƒ/" attr(data-stop)',
                          position: "absolute",
                          bottom: 11,
                          left: "50%",
                          transform: "translateX(-50%)",
                          color: movieColors.brand,
                          fontSize: 12,
                          fontWeight: 700,
                          fontVariantNumeric: "tabular-nums",
                          letterSpacing: 0.2,
                        },
                      }}
                    />
                    <Box
                      sx={{
                        display: "grid",
                        gridTemplateColumns: `repeat(${STOPS.length}, 1fr)`,
                        borderTop: "1px solid",
                        borderColor: alpha("#ffffff", 0.1),
                        pt: 0.7,
                      }}
                    >
                      {STOPS.map((stop) => (
                        <Typography
                          key={stop}
                          component="span"
                          sx={{
                            fontSize: 9,
                            textAlign: "center",
                            color: "text.secondary",
                            fontVariantNumeric: "tabular-nums",
                            letterSpacing: 0.3,
                          }}
                        >
                          {stop}
                        </Typography>
                      ))}
                    </Box>
                  </Box>
                  <Box sx={{ px: 2.5, textAlign: "center" }}>
                    <Typography
                      role="status"
                      sx={{
                        fontSize: 22,
                        fontWeight: 650,
                        letterSpacing: -0.6,
                      }}
                    >
                      {statusText}
                    </Typography>
                    <Typography
                      sx={{ fontSize: 11, mt: 1, color: "text.secondary" }}
                    >
                      {labels[visualState][1]}
                    </Typography>
                    <Stack
                      direction="row"
                      spacing={1.5}
                      sx={{ justifyContent: "center", mt: 3, mb: 2.5 }}
                    >
                      <Tooltip
                        title={
                          visualState === "muted"
                            ? "Resume microphone"
                            : "Mute microphone"
                        }
                      >
                        <IconButton
                          aria-label={
                            visualState === "muted"
                              ? "Resume microphone"
                              : "Mute microphone"
                          }
                          onClick={() => {
                            if (live) {
                              mic.toggleMute();
                              if (visualState === "muted")
                                setState("listening");
                            } else
                              setState(
                                state === "muted" ? "listening" : "muted",
                              );
                          }}
                          sx={{
                            width: 48,
                            height: 48,
                            border: "1px solid",
                            borderColor: "divider",
                            bgcolor: alpha(movieColors.info, 0.06),
                            color:
                              visualState === "muted"
                                ? "text.secondary"
                                : movieColors.info,
                          }}
                        >
                          {visualState === "muted" ? (
                            <MicOffRounded fontSize="small" />
                          ) : (
                            <MicRounded fontSize="small" />
                          )}
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="End voice">
                        <IconButton
                          aria-label="End voice"
                          onClick={end}
                          sx={{
                            width: 48,
                            height: 48,
                            border: "1px solid",
                            borderColor: "divider",
                            bgcolor: alpha("#ffffff", 0.05),
                          }}
                        >
                          <StopRounded fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </Stack>
                  </Box>
                  <Divider />
                  <Button
                    fullWidth
                    onClick={() => setDetails(!details)}
                    startIcon={
                      <ChatBubbleOutlineRounded
                        sx={{ fontSize: "16px !important" }}
                      />
                    }
                    endIcon={
                      <ArrowOutwardRounded
                        sx={{ fontSize: "14px !important" }}
                      />
                    }
                    sx={{ py: 1.7, color: "text.secondary", fontSize: 11 }}
                  >
                    Conversation & preferences
                  </Button>
                </Box>
              )}
            </Stack>

            <ConversationPreview
              open={details}
              state={visualState}
              live={live}
              statusText={statusText}
              readLevel={mic.readLevel}
              onClose={() => setDetails(false)}
              onMute={() => {
                mic.toggleMute();
                if (visualState === "muted") setState("listening");
              }}
              onEnd={end}
              onStart={start}
            />
            <Button
              onClick={() => setDetails(true)}
              startIcon={
                <ChatBubbleOutlineRounded
                  sx={{ fontSize: "15px !important" }}
                />
              }
              sx={{
                display: details ? "none" : "inline-flex",
                position: "absolute",
                bottom: 18,
                right: 18,
                bgcolor: movieColors.surfaceElevated,
                border: "1px solid",
                borderColor: "divider",
                color: "text.primary",
                px: 2,
                py: 1.2,
                fontSize: 11,
                borderRadius: 8,
              }}
            >
              Ask Concierge
            </Button>
            <Typography
              sx={{
                position: "absolute",
                bottom: 24,
                left: 20,
                fontSize: 10,
                color: "text.secondary",
                display: { xs: "none", sm: "block" },
              }}
            >
              CATALOG BACKGROUND · DESIGN PREVIEW
            </Typography>
          </Box>
          <Stack
            direction={{ xs: "column", sm: "row" }}
            spacing={3}
            sx={{ mt: 2.5 }}
          >
            <Typography
              sx={{
                fontSize: 11,
                color: "text.secondary",
                lineHeight: 1.8,
                flex: 1,
              }}
            >
              <Box component="span" sx={{ color: movieColors.info, mr: 1 }}>
                ●
              </Box>
              <strong>Your voice</strong> widens the blades and prints the ring.
              Newest mark sits at twelve o'clock, older ones trail clockwise.
            </Typography>
            <Typography
              sx={{
                fontSize: 11,
                color: "text.secondary",
                lineHeight: 1.8,
                flex: 1,
              }}
            >
              <Box component="span" sx={{ color: movieColors.brand, mr: 1 }}>
                ●
              </Box>
              <strong>The Concierge</strong> floods the gate and prints its
              reply on the same ring, so both halves of the turn stay visible.
            </Typography>
          </Stack>

          {/* The same live lens at every size it has to survive, so the
              overlay can be judged at its real footprint rather than at
              study scale. */}
          <Box sx={{ mt: 4 }}>
            <Typography
              sx={{
                fontSize: 10,
                letterSpacing: 1.7,
                color: "text.secondary",
                mb: 2,
              }}
            >
              AT REAL SIZE
            </Typography>
            <Stack
              direction="row"
              spacing={{ xs: 2.5, sm: 4 }}
              sx={{ alignItems: "flex-end", flexWrap: "wrap", rowGap: 3 }}
            >
              {sizes.map((item) => (
                <Box key={item.label} sx={{ width: item.width }}>
                  <VoiceOrb
                    state={visualState}
                    live={live}
                    readLevel={mic.readLevel}
                  />
                  <Typography sx={{ fontSize: 11, fontWeight: 700, mt: 1 }}>
                    {item.label}
                  </Typography>
                  <Typography
                    sx={{ fontSize: 10, color: "text.secondary", mt: 0.3 }}
                  >
                    {item.width} px · {item.note}
                  </Typography>
                </Box>
              ))}
            </Stack>
          </Box>
        </Box>
      </Box>
    </Box>
  );
}

const root = document.getElementById("root");
if (root && import.meta.env.DEV)
  createRoot(root).render(
    <ThemeProvider theme={appTheme}>
      <CssBaseline />
      <OrbPreview />
    </ThemeProvider>,
  );
