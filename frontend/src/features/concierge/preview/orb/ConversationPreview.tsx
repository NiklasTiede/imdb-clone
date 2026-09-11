import { useRef, useState } from "react";
import {
  Box,
  Button,
  Chip,
  Collapse,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import { alpha } from "@mui/material/styles";
import CloseRounded from "@mui/icons-material/CloseRounded";
import MicRounded from "@mui/icons-material/MicRounded";
import MicOffRounded from "@mui/icons-material/MicOffRounded";
import StopRounded from "@mui/icons-material/StopRounded";
import TuneRounded from "@mui/icons-material/TuneRounded";
import ArrowUpwardRounded from "@mui/icons-material/ArrowUpwardRounded";
import AutoAwesomeRounded from "@mui/icons-material/AutoAwesomeRounded";
import PlayCircleOutlineRounded from "@mui/icons-material/PlayCircleOutlineRounded";
import BookmarkBorderRounded from "@mui/icons-material/BookmarkBorderRounded";
import StarOutlineRounded from "@mui/icons-material/StarOutlineRounded";
import PublicRounded from "@mui/icons-material/PublicRounded";
import ExploreOutlined from "@mui/icons-material/ExploreOutlined";
import CheckRounded from "@mui/icons-material/CheckRounded";
import ErrorOutlineRounded from "@mui/icons-material/ErrorOutlineRounded";
import { movieColors } from "../../../../theme";
import { streamingCountries } from "../../model/streamingCountry";
import { VoiceOrb, type OrbState } from "./VoiceOrb";

const capabilities = [
  {
    title: "Discover your next film",
    example: "Recommend something based on my ratings.",
    icon: AutoAwesomeRounded,
    personal: true,
  },
  {
    title: "Movies & trailers",
    example: "Show me the trailer for Forrest Gump.",
    icon: PlayCircleOutlineRounded,
    personal: false,
  },
  {
    title: "Your watchlist",
    example: "Add Forrest Gump to my watchlist.",
    icon: BookmarkBorderRounded,
    personal: true,
  },
  {
    title: "Your ratings",
    example: "Which movies have I rated highest?",
    icon: StarOutlineRounded,
    personal: true,
  },
  {
    title: "Where to watch",
    example: "Where can I watch Forrest Gump in Switzerland?",
    icon: PublicRounded,
    personal: false,
  },
  {
    title: "Find your way",
    example: "Open my settings.",
    icon: ExploreOutlined,
    personal: true,
  },
];

function SampleAction({
  failed,
  title,
  children,
}: {
  failed?: boolean;
  title: string;
  children: string;
}) {
  return (
    <Box
      sx={{
        mt: 1.2,
        borderLeft: "2px solid",
        borderColor: failed ? "warning.main" : movieColors.info,
        pl: 1.5,
      }}
    >
      <Stack direction="row" spacing={0.8} sx={{ alignItems: "center" }}>
        {failed ? (
          <ErrorOutlineRounded sx={{ fontSize: 15, color: "warning.main" }} />
        ) : (
          <CheckRounded sx={{ fontSize: 15, color: movieColors.info }} />
        )}
        <Typography sx={{ fontSize: 12, fontWeight: 700 }}>{title}</Typography>
      </Stack>
      <Box
        component="details"
        sx={{
          mt: 0.7,
          color: "text.secondary",
          fontSize: 11,
          "& summary": {
            cursor: "pointer",
            py: 0.7,
            width: "fit-content",
            "&:focus-visible": { outline: `2px solid ${movieColors.info}` },
          },
        }}
      >
        <Box component="summary">Action details</Box>
        <Typography
          component="pre"
          sx={{
            whiteSpace: "pre-wrap",
            fontFamily: "monospace",
            fontSize: 10,
            lineHeight: 1.8,
            bgcolor: movieColors.surfaceInset,
            p: 1.2,
            borderRadius: 1,
          }}
        >
          {children}
        </Typography>
      </Box>
    </Box>
  );
}

function SampleConversation({ country }: { country: string }) {
  return (
    <Stack spacing={3}>
      <Box sx={{ textAlign: "center" }}>
        <Chip size="small" label="Example conversation" variant="outlined" />
        <Typography sx={{ fontSize: 10, color: "text.secondary", mt: 0.8 }}>
          Illustrative results and timings. No actions are executed.
        </Typography>
      </Box>
      <Box>
        <Typography
          sx={{
            color: movieColors.info,
            fontSize: 10,
            letterSpacing: 1,
            mb: 0.8,
          }}
        >
          YOU · VOICE · 14:32
        </Typography>
        <Typography sx={{ fontSize: 13, lineHeight: 1.7 }}>
          Tell me about Forrest Gump.
        </Typography>
        <Typography
          sx={{
            color: movieColors.brand,
            fontSize: 10,
            letterSpacing: 1,
            mt: 2,
            mb: 0.8,
          }}
        >
          CONCIERGE
        </Typography>
        <Typography sx={{ fontSize: 13, lineHeight: 1.8 }}>
          Forrest Gump is a 1994 drama starring Tom Hanks, directed by Robert
          Zemeckis. Would you like to watch the trailer?
        </Typography>
        <Box
          sx={{
            mt: 1.5,
            p: 1.5,
            bgcolor: alpha(movieColors.info, 0.04),
            border: "1px solid",
            borderColor: "divider",
            borderRadius: 1,
          }}
        >
          <Typography sx={{ fontWeight: 750, fontSize: 14 }}>
            Forrest Gump{" "}
            <Box
              component="span"
              sx={{ color: "text.secondary", fontWeight: 400, ml: 0.5 }}
            >
              1994
            </Box>
          </Typography>
          <Typography sx={{ fontSize: 11, color: "text.secondary", mt: 0.5 }}>
            142 min · Drama, Romance
          </Typography>
          <Button
            component="a"
            href="https://www.themoviedb.org/movie/13-forrest-gump"
            target="_blank"
            rel="noopener noreferrer"
            size="small"
            sx={{ color: movieColors.info, p: 0, mt: 1, fontSize: 11 }}
          >
            Source: TMDB ↗
          </Button>
        </Box>
      </Box>
      <Box sx={{ borderTop: "1px solid", borderColor: "divider", pt: 2.5 }}>
        <Typography
          sx={{
            color: movieColors.info,
            fontSize: 10,
            letterSpacing: 1,
            mb: 0.8,
          }}
        >
          YOU · VOICE · 14:33
        </Typography>
        <Typography sx={{ fontSize: 13 }}>Add it to my watchlist.</Typography>
        <Typography
          sx={{
            color: movieColors.brand,
            fontSize: 10,
            letterSpacing: 1,
            mt: 2,
            mb: 0.8,
          }}
        >
          CONCIERGE
        </Typography>
        <Typography sx={{ fontSize: 13, lineHeight: 1.8 }}>
          Forrest Gump is on your watchlist.
        </Typography>
        <SampleAction title="Watchlist updated · Forrest Gump">{`EXAMPLE · turn 02\nResult: added (confirmed)\nDuration: 420 ms\nPage at request: Forrest Gump\nStreaming country: ${country}\nOperation: example-watchlist-02`}</SampleAction>
        <SampleAction title="Watchlist page opened">
          {
            "EXAMPLE · turn 02\nUI result: navigation completed\nDestination: /your-watchlist"
          }
        </SampleAction>
      </Box>
      <Box sx={{ borderTop: "1px solid", borderColor: "divider", pt: 2.5 }}>
        <Typography
          sx={{
            color: movieColors.info,
            fontSize: 10,
            letterSpacing: 1,
            mb: 0.8,
          }}
        >
          YOU · TEXT · 14:34
        </Typography>
        <Typography sx={{ fontSize: 13 }}>Let me watch its trailer.</Typography>
        <Typography
          sx={{
            color: movieColors.brand,
            fontSize: 10,
            letterSpacing: 1,
            mt: 2,
            mb: 0.8,
          }}
        >
          CONCIERGE
        </Typography>
        <Typography sx={{ fontSize: 13, lineHeight: 1.8 }}>
          The movie page is open, but the trailer couldn’t load. You can try the
          player again.
        </Typography>
        <SampleAction title="Movie page opened · Forrest Gump">
          {
            "EXAMPLE · turn 03\nUI result: navigation completed\nDestination: movie detail"
          }
        </SampleAction>
        <SampleAction failed title="Trailer unavailable">
          {
            "EXAMPLE · turn 03\nPlayer result: load failed\nPlayback: not started\nDuration: 2,000 ms\nError: example-player-timeout"
          }
        </SampleAction>
      </Box>
    </Stack>
  );
}

export function ConversationPreview({
  open,
  state,
  live,
  statusText,
  readLevel,
  onClose,
  onMute,
  onEnd,
  onStart,
}: {
  open: boolean;
  state: OrbState;
  live: boolean;
  statusText: string;
  readLevel: () => number;
  onClose: () => void;
  onMute: () => void;
  onEnd: () => void;
  onStart: () => void;
}) {
  const [example, setExample] = useState(false);
  const [capabilitiesOpen, setCapabilitiesOpen] = useState(false);
  const [preferencesOpen, setPreferencesOpen] = useState(false);
  const [guest, setGuest] = useState(true);
  const [country, setCountry] = useState("CH");
  const [exampleCountry, setExampleCountry] = useState("CH");
  const [draft, setDraft] = useState("");
  const [submitted, setSubmitted] = useState<string[]>([]);
  const input = useRef<HTMLInputElement>(null);
  const body = useRef<HTMLDivElement>(null);
  const hasConversation = example || submitted.length > 0;
  const selectPrompt = (prompt: string) => {
    setDraft(prompt);
    input.current?.focus();
  };
  if (!open) return null;

  return (
    <Box
      component="section"
      aria-label="Conversation and preferences"
      sx={{
        position: "absolute",
        top: 64,
        right: 0,
        bottom: 0,
        width: { xs: "100%", sm: 440 },
        maxWidth: "100%",
        display: "flex",
        flexDirection: "column",
        bgcolor: movieColors.surface,
        borderLeft: "1px solid",
        borderColor: "divider",
        boxShadow: "-20px 0 80px #0008",
        zIndex: 2,
      }}
    >
      <Stack
        direction="row"
        sx={{
          alignItems: "center",
          px: 1.5,
          py: 0.8,
          borderBottom: "1px solid",
          borderColor: "divider",
          flexShrink: 0,
        }}
      >
        <Box sx={{ width: 66, flexShrink: 0 }}>
          <VoiceOrb state={state} live={live} readLevel={readLevel} />
        </Box>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography sx={{ fontSize: 14, fontWeight: 750 }}>
            Movie Concierge
          </Typography>
          <Typography
            role="status"
            sx={{
              fontSize: 10,
              color: live ? movieColors.info : "text.secondary",
              mt: 0.3,
            }}
          >
            {live ? statusText : "Design preview · microphone off"}
          </Typography>
        </Box>
        <Tooltip title="Preferences">
          <IconButton
            aria-label="Preferences"
            aria-expanded={preferencesOpen}
            onClick={() => setPreferencesOpen(!preferencesOpen)}
            sx={{ width: 44, height: 44 }}
          >
            <TuneRounded fontSize="small" />
          </IconButton>
        </Tooltip>
        <Tooltip title="Close conversation">
          <IconButton
            aria-label="Close conversation and preferences"
            onClick={onClose}
            sx={{ width: 44, height: 44 }}
          >
            <CloseRounded fontSize="small" />
          </IconButton>
        </Tooltip>
      </Stack>
      <Collapse in={preferencesOpen} sx={{ flexShrink: 0 }}>
        <Box
          sx={{
            px: 2.5,
            py: 2,
            borderBottom: "1px solid",
            borderColor: "divider",
            bgcolor: movieColors.surfaceInset,
          }}
        >
          <TextField
            select
            fullWidth
            size="small"
            label="Streaming country"
            value={country}
            onChange={(event) => setCountry(event.target.value)}
          >
            {streamingCountries.map((item) => (
              <MenuItem key={item.code} value={item.code}>
                {item.name}
              </MenuItem>
            ))}
          </TextField>
          <Button size="small" onClick={() => setGuest(!guest)} sx={{ mt: 1 }}>
            {guest ? "Preview as signed in" : "Preview as guest"}
          </Button>
          <Typography sx={{ fontSize: 10, color: "text.secondary" }}>
            Local design settings only. Your account stays unchanged.
          </Typography>
        </Box>
      </Collapse>
      <Stack
        direction="row"
        sx={{
          px: 2,
          py: 0.8,
          gap: 1,
          alignItems: "center",
          justifyContent: "space-between",
          borderBottom: "1px solid",
          borderColor: "divider",
          flexShrink: 0,
        }}
      >
        <Typography sx={{ fontSize: 10, color: "text.secondary" }}>
          {guest ? "Guest preview" : "Signed-in preview"} · {country}
        </Typography>
        <Button
          size="small"
          onClick={() => {
            setExample(!example);
            setExampleCountry(country);
            setCapabilitiesOpen(false);
            setSubmitted([]);
            if (body.current) body.current.scrollTop = 0;
          }}
          sx={{ fontSize: 11 }}
        >
          {example ? "Back to welcome" : "View example conversation"}
        </Button>
      </Stack>
      <Box
        ref={body}
        sx={{
          flex: 1,
          minHeight: 0,
          overflowY: "auto",
          overscrollBehavior: "contain",
          px: 2.5,
          py: 2.5,
        }}
      >
        {hasConversation && (
          <Button
            fullWidth
            aria-expanded={capabilitiesOpen}
            onClick={() => setCapabilitiesOpen(!capabilitiesOpen)}
            startIcon={
              <AutoAwesomeRounded sx={{ fontSize: "15px !important" }} />
            }
            sx={{
              mb: 2,
              justifyContent: "flex-start",
              fontSize: 11,
              color: "text.secondary",
            }}
          >
            {capabilitiesOpen ? "Hide capabilities" : "Explore what I can do"}
          </Button>
        )}
        <Collapse in={!hasConversation || capabilitiesOpen}>
          <Typography
            component="h2"
            sx={{
              fontSize: 24,
              fontWeight: 650,
              lineHeight: 1.3,
              letterSpacing: -0.7,
              mb: 1,
            }}
          >
            What can I help you with?
          </Typography>
          <Typography
            sx={{
              fontSize: 12,
              color: "text.secondary",
              lineHeight: 1.8,
              mb: 2,
            }}
          >
            Choose an example, or ask in your own words.
          </Typography>
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: 1,
              mb: 2,
            }}
          >
            {capabilities.map((item) => (
              <Button
                key={item.title}
                onClick={() => selectPrompt(item.example)}
                sx={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "flex-start",
                  justifyContent: "flex-start",
                  textAlign: "left",
                  p: 1.25,
                  minWidth: 0,
                  border: "1px solid",
                  borderColor: "divider",
                  borderRadius: 1,
                  bgcolor: alpha(movieColors.info, 0.025),
                  color: "text.primary",
                  "&:hover": {
                    bgcolor: alpha(movieColors.info, 0.08),
                    borderColor: alpha(movieColors.info, 0.35),
                  },
                }}
              >
                <Stack
                  component="span"
                  direction="row"
                  spacing={0.7}
                  sx={{ alignItems: "center", mb: 0.8 }}
                >
                  <item.icon sx={{ fontSize: 16, color: movieColors.info }} />
                  <Typography
                    component="span"
                    sx={{
                      fontWeight: 750,
                      fontSize: 12,
                      lineHeight: 1.4,
                    }}
                  >
                    {item.title}
                  </Typography>
                </Stack>
                <Typography
                  component="span"
                  sx={{
                    color: "text.secondary",
                    fontSize: 11,
                    lineHeight: 1.7,
                  }}
                >
                  “{item.example}”
                </Typography>
                {guest && item.personal && (
                  <Typography
                    component="span"
                    sx={{ fontSize: 9, color: movieColors.brand, mt: 1 }}
                  >
                    Sign in required
                  </Typography>
                )}
              </Button>
            ))}
          </Box>
        </Collapse>
        {example && <SampleConversation country={exampleCountry} />}
        {submitted.map((message, index) => (
          <Box
            key={index}
            sx={{
              mt: 2,
              pt: 2,
              borderTop: "1px solid",
              borderColor: "divider",
            }}
          >
            <Typography sx={{ color: movieColors.info, fontSize: 10, mb: 1 }}>
              YOU · PREVIEW
            </Typography>
            <Typography sx={{ fontSize: 13, whiteSpace: "pre-wrap" }}>
              {message}
            </Typography>
            <Typography
              sx={{
                fontSize: 11,
                color: "text.secondary",
                lineHeight: 1.8,
                mt: 1,
              }}
            >
              This design preview does not send messages to the agent. Use “View
              example conversation” to explore answers and action results.
            </Typography>
          </Box>
        ))}
      </Box>
      <Box
        component="form"
        onSubmit={(event) => {
          event.preventDefault();
          if (!draft.trim()) return;
          setSubmitted((items) => [...items, draft.trim()]);
          setDraft("");
          setCapabilitiesOpen(false);
          requestAnimationFrame(() => {
            if (body.current)
              body.current.scrollTop = body.current.scrollHeight;
          });
        }}
        sx={{
          p: 1.5,
          bgcolor: movieColors.surfaceInset,
          borderTop: "1px solid",
          borderColor: "divider",
          flexShrink: 0,
        }}
      >
        <Stack direction="row" spacing={1} sx={{ alignItems: "flex-end" }}>
          <TextField
            inputRef={input}
            fullWidth
            multiline
            maxRows={3}
            size="small"
            placeholder="Ask about movies…"
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            slotProps={{
              htmlInput: { "aria-label": "Preview message", maxLength: 600 },
            }}
            sx={{ "& .MuiInputBase-root": { fontSize: 12 } }}
          />
          <Tooltip title="Preview message">
            <span>
              <IconButton
                type="submit"
                aria-label="Preview message submission"
                disabled={!draft.trim()}
                sx={{
                  width: 40,
                  height: 40,
                  bgcolor: movieColors.brand,
                  color: movieColors.brandInk,
                  borderRadius: 1,
                  "&:hover": { bgcolor: movieColors.gold },
                }}
              >
                <ArrowUpwardRounded fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
        </Stack>
        <Stack
          direction="row"
          sx={{ alignItems: "center", justifyContent: "space-between", mt: 1 }}
        >
          <Typography sx={{ fontSize: 9, color: "text.secondary" }}>
            Preview only · nothing is sent
          </Typography>
          <Stack direction="row">
            {live ? (
              <>
                <Tooltip
                  title={
                    state === "muted" ? "Resume microphone" : "Mute microphone"
                  }
                >
                  <IconButton
                    aria-label={
                      state === "muted"
                        ? "Resume microphone"
                        : "Mute microphone"
                    }
                    onClick={onMute}
                    sx={{ width: 44, height: 44, color: movieColors.info }}
                  >
                    {state === "muted" ? (
                      <MicOffRounded fontSize="small" />
                    ) : (
                      <MicRounded fontSize="small" />
                    )}
                  </IconButton>
                </Tooltip>
                <Tooltip title="End voice">
                  <IconButton
                    aria-label="End voice"
                    onClick={onEnd}
                    sx={{ width: 44, height: 44 }}
                  >
                    <StopRounded fontSize="small" />
                  </IconButton>
                </Tooltip>
              </>
            ) : (
              <Button
                size="small"
                startIcon={<MicRounded />}
                onClick={onStart}
                sx={{ fontSize: 11 }}
              >
                Test voice
              </Button>
            )}
          </Stack>
        </Stack>
      </Box>
    </Box>
  );
}
