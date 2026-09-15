import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent,
} from "react";
import {
  Alert,
  Box,
  Button,
  Collapse,
  Drawer,
  IconButton,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import { alpha } from "@mui/material/styles";
import CloseRounded from "@mui/icons-material/CloseRounded";
import RefreshRounded from "@mui/icons-material/RefreshRounded";
import TuneRounded from "@mui/icons-material/TuneRounded";
import ArrowUpwardRounded from "@mui/icons-material/ArrowUpwardRounded";
import Markdown from "react-markdown";
import { movieColors } from "../../../theme";
import type { useConciergeChat } from "../hooks/useConciergeChat";
import type { ConciergeVoice } from "../hooks/useConciergeVoice";
import type { ChatTurn, ApplicationAction } from "../model/concierge";
import { chronologicalHistory } from "../model/conversationHistory";
import { ConciergeSourceLink } from "./ConciergeSourceLink";
import { StreamingCountrySelector } from "./StreamingCountrySelector";
import { ConciergeCredits } from "./ConciergeCredits";
import { ConciergeVoicePanel } from "./ConciergeVoicePanel";
import ConciergeEmptyState from "./ConciergeEmptyState";
import { ConciergeEvidence } from "./ConciergeEvidence";

type Props = {
  streamingCountry: string;
  onStreamingCountryChange: (country: string) => void;
  voice: ConciergeVoice;
  chat: ReturnType<typeof useConciergeChat>;
  signedIn: boolean;
  onClose: () => void;
  open: boolean;
};

const ConciergeDrawer = ({
  streamingCountry,
  onStreamingCountryChange,
  voice,
  chat,
  signedIn,
  onClose,
  open,
}: Props) => {
  const [draft, setDraft] = useState("");
  const [preferences, setPreferences] = useState(false);
  const [capabilities, setCapabilities] = useState(false);
  const [following, setFollowing] = useState(true);
  const followingRef = useRef(true);
  const lastVoiceError = useRef<string | null>(null);
  const closeButton = useRef<HTMLButtonElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const turns = useMemo(
    () => chronologicalHistory(chat.turns, voice.turns),
    [chat.turns, voice.turns],
  );
  const { isStreaming, status } = chat;
  useEffect(() => {
    if (!open) return;
    const previousFocus = document.activeElement;
    closeButton.current?.focus();
    return () => {
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected)
        previousFocus.focus();
    };
  }, [open]);
  useEffect(() => {
    if (voice.error && voice.error !== lastVoiceError.current)
      followingRef.current = true;
    lastVoiceError.current = voice.error;
    if (followingRef.current && scrollRef.current)
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [turns, status, open, voice.error]);
  const send = (message: string) => {
    const text = message.trim();
    if (!text || text.length > 600 || isStreaming) return;
    if (voice.active) {
      if (!voice.sendText(text)) return;
    } else {
      void chat.send(text);
    }
    followingRef.current = true;
    setFollowing(true);
    setCapabilities(false);
    setDraft("");
  };
  const submit = () => send(draft);
  const reset = () => {
    voice.end();
    voice.clearHistory();
    chat.reset();
    setDraft("");
    setCapabilities(false);
    followingRef.current = true;
    setFollowing(true);
  };

  return (
    <Drawer
      anchor="right"
      variant="persistent"
      open={open}
      onClose={onClose}
      slotProps={{
        paper: {
          id: "concierge-conversation",
          role: "complementary",
          "aria-label": "Movie Concierge",
          onKeyDown: (event: KeyboardEvent<HTMLElement>) => {
            if (event.key === "Escape" && !event.defaultPrevented) {
              event.stopPropagation();
              onClose();
            }
          },
          sx: {
            width: { xs: "100vw", md: 440 },
            height: {
              xs: voice.active
                ? "calc(100dvh - 184px - env(safe-area-inset-bottom, 0px))"
                : "100dvh",
              md: "100dvh",
            },
            maxWidth: "100vw",
            bgcolor: movieColors.surface,
            backgroundImage: "none",
            borderLeft: "1px solid",
            borderColor: "divider",
            boxShadow: "-24px 0 70px #0007",
          },
        },
      }}
    >
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          height: "100%",
          minHeight: 0,
          "& button": { textTransform: "none" },
        }}
      >
        <Stack
          direction="row"
          sx={{
            alignItems: "center",
            px: 1.5,
            py: 0.5,
            borderBottom: "1px solid",
            borderColor: "divider",
            flexShrink: 0,
          }}
        >
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography sx={{ fontWeight: 750, fontSize: 14 }}>
              Movie Concierge
            </Typography>
            <Typography
              sx={{
                fontSize: 10,
                color: voice.active ? movieColors.info : "text.secondary",
                mt: 0.3,
              }}
            >
              {voice.active
                ? "Conversation and actions"
                : "Your movies. Just ask."}
            </Typography>
          </Box>
          <Tooltip title="Preferences">
            <IconButton
              aria-label="Concierge preferences"
              aria-expanded={preferences}
              onClick={() => setPreferences(!preferences)}
              sx={{ width: 40, height: 44 }}
            >
              <TuneRounded fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Start a new conversation">
            <span>
              <IconButton
                aria-label="Start a new concierge conversation"
                disabled={isStreaming}
                onClick={reset}
                sx={{ width: 36, height: 44 }}
              >
                <RefreshRounded fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
          <IconButton
            aria-label="Close Movie Concierge"
            ref={closeButton}
            onClick={onClose}
            sx={{ width: 36, height: 44 }}
          >
            <CloseRounded fontSize="small" />
          </IconButton>
        </Stack>
        <Collapse in={preferences} sx={{ flexShrink: 0 }}>
          <StreamingCountrySelector
            country={streamingCountry}
            onChange={onStreamingCountryChange}
          />
          <Typography
            sx={{ px: 2, pb: 1.5, fontSize: 10, color: "text.secondary" }}
          >
            The latest 200 messages stay in this tab until reload, sign-out or a
            new conversation. While voice is on, speaking, typing and
            suggestions share its conversation. Earlier text-only chats are not
            transferred when starting voice.
          </Typography>
          <Box sx={{ px: 2, pb: 1.5 }}>
            <ConciergeCredits />
          </Box>
        </Collapse>
        <Box
          ref={scrollRef}
          role="log"
          aria-live="polite"
          aria-label="Movie Concierge conversation"
          onScroll={() => {
            const node = scrollRef.current;
            if (!node) return;
            const next =
              node.scrollHeight - node.scrollTop - node.clientHeight < 64;
            followingRef.current = next;
            setFollowing(next);
          }}
          sx={{
            flex: 1,
            minHeight: 0,
            overflowY: "auto",
            overscrollBehavior: "contain",
          }}
        >
          {turns.length > 0 && (
            <Button
              fullWidth
              aria-expanded={capabilities}
              onClick={() => setCapabilities(!capabilities)}
              sx={{
                px: 2.5,
                py: 1.5,
                justifyContent: "flex-start",
                color: "text.secondary",
                fontSize: 11,
              }}
            >
              {capabilities ? "Hide capabilities" : "Explore what I can do"}
            </Button>
          )}
          <Collapse in={turns.length === 0 || capabilities}>
            <ConciergeEmptyState
              onPrompt={send}
              disabled={isStreaming || (voice.active && !voice.canSendText)}
              signedIn={signedIn}
              country={streamingCountry}
            />
          </Collapse>
          <Stack spacing={2.5} sx={{ px: 2.5, py: turns.length ? 2 : 0 }}>
            {turns
              .filter(
                (turn) =>
                  turn.text ||
                  turn.movies.length ||
                  turn.actions?.length ||
                  turn.tools?.length ||
                  turn.error,
              )
              .map((turn) => (
                <ChatMessage key={turn.id} turn={turn} />
              ))}
            {isStreaming && status && (
              <Typography sx={{ fontSize: 11, color: "text.secondary" }}>
                {status}…
              </Typography>
            )}
          </Stack>
        </Box>
        {!following && turns.length > 0 && (
          <Button
            onClick={() => {
              if (scrollRef.current)
                scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
              followingRef.current = true;
              setFollowing(true);
            }}
          >
            Latest messages ↓
          </Button>
        )}
        <Box
          component="form"
          onSubmit={(event) => {
            event.preventDefault();
            submit();
          }}
          sx={{
            p: 1.5,
            pb: "max(12px, env(safe-area-inset-bottom))",
            bgcolor: movieColors.surfaceInset,
            borderTop: "1px solid",
            borderColor: "divider",
            flexShrink: 0,
          }}
        >
          <Stack direction="row" spacing={1} sx={{ alignItems: "flex-end" }}>
            <TextField
              fullWidth
              multiline
              maxRows={3}
              minRows={1}
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              onKeyDown={(event) => {
                if (
                  event.key === "Enter" &&
                  !event.shiftKey &&
                  !event.nativeEvent.isComposing
                ) {
                  event.preventDefault();
                  submit();
                }
              }}
              placeholder="Ask about movies…"
              slotProps={{
                htmlInput: {
                  "aria-label": "Ask the Movie Concierge",
                  maxLength: 600,
                },
              }}
              size="small"
              sx={{
                "& .MuiInputBase-root": {
                  fontSize: 12,
                  color: "rgba(255,255,255,0.92)",
                },
                "& .MuiInputBase-input::placeholder": {
                  color: "rgba(255,255,255,0.62)",
                  opacity: 1,
                },
              }}
            />
            <IconButton
              aria-label="Send concierge message"
              type="submit"
              disabled={
                !draft.trim() ||
                isStreaming ||
                (voice.active && !voice.canSendText)
              }
              sx={{
                width: 40,
                height: 40,
                borderRadius: 1,
                bgcolor: movieColors.brand,
                color: movieColors.brandInk,
                "&:hover": { bgcolor: movieColors.gold },
              }}
            >
              <ArrowUpwardRounded fontSize="small" />
            </IconButton>
          </Stack>
          {voice.active && (
            <Typography
              sx={{ fontSize: 10, color: "text.secondary", mt: 0.75 }}
            >
              Type or choose a suggestion. Replies are spoken while voice is on.
            </Typography>
          )}
          <ConciergeVoicePanel voice={voice} disabled={isStreaming} />
          {chat.usage && (
            <Typography sx={{ fontSize: 9, color: "text.secondary" }}>
              {chat.usage.totalTokens.toLocaleString()} tokens
            </Typography>
          )}
        </Box>
      </Box>
    </Drawer>
  );
};

function actionLabel(action: ApplicationAction): string {
  switch (action.type) {
    case "open_movie":
      return "Movie page";
    case "open_movie_trailer":
      return "Movie trailer section";
    case "open_page":
      return `${action.destination.charAt(0).toUpperCase()}${action.destination.slice(1)} page`;
    case "show_search_results":
      return "Search results";
    case "open_watchlist":
      return "Watchlist page";
    case "open_ratings":
      return "Ratings page";
    case "open_login":
      return "Sign-in page";
  }
}
function mutationLabel(action: ApplicationAction): string | null {
  if (action.type === "open_ratings")
    return action.score === null
      ? action.changed
        ? "Rating removed"
        : "No rating to remove"
      : action.changed
        ? `Rating saved: ${action.score}/10`
        : `Rating already ${action.score}/10`;
  if (action.type === "open_watchlist" && action.operationId) {
    if (action.removed != null)
      return action.removed ? "Removed from watchlist" : "Not on watchlist";
    if (action.created != null)
      return action.created ? "Added to watchlist" : "Already on watchlist";
  }
  return null;
}
const ChatMessage = ({ turn }: { turn: ChatTurn }) => (
  <Box>
    <Stack direction="row" spacing={1} sx={{ mb: 0.8, alignItems: "center" }}>
      <Typography
        sx={{
          fontSize: 10,
          color: turn.role === "user" ? movieColors.info : movieColors.brand,
          letterSpacing: 0.8,
        }}
      >
        {turn.role === "user" ? "YOU" : "CONCIERGE"} ·{" "}
        {(turn.channel ?? "text").toUpperCase()}
      </Typography>
      {turn.timestamp && (
        <Typography
          component="time"
          dateTime={new Date(turn.timestamp).toISOString()}
          sx={{ fontSize: 10, color: "text.secondary" }}
        >
          {new Date(turn.timestamp).toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          })}
        </Typography>
      )}
    </Stack>
    {turn.text && (
      <MessageText isUser={turn.role === "user"} text={turn.text} />
    )}
    {turn.interrupted && (
      <Typography sx={{ fontSize: 10, color: "text.secondary", mt: 1 }}>
        Reply interrupted · some of this text may not have been spoken.
      </Typography>
    )}
    <ConciergeEvidence turn={turn} />
    {turn.actions?.map((entry, index) => (
      <Box
        key={index}
        sx={{
          pl: 1.5,
          mt: 1.5,
          borderLeft: "2px solid",
          borderColor:
            entry.outcome === "rejected" ? "warning.main" : movieColors.info,
        }}
      >
        {mutationLabel(entry.action) && (
          <Typography sx={{ fontSize: 12, fontWeight: 700 }}>
            ✓ {mutationLabel(entry.action)}
          </Typography>
        )}
        <Typography sx={{ fontSize: 12 }}>
          {entry.outcome === "opened"
            ? "✓"
            : entry.outcome === "rejected"
              ? "!"
              : "→"}{" "}
          {actionLabel(entry.action)} ·{" "}
          {entry.outcome === "opened"
            ? "open"
            : entry.outcome === "rejected"
              ? "action could not be applied"
              : "navigation requested"}
        </Typography>
        {entry.action.type === "open_movie_trailer" && (
          <Typography sx={{ fontSize: 10, mt: 0.5, color: "text.secondary" }}>
            Playback is controlled by the trailer player.
          </Typography>
        )}
        <Box
          component="details"
          sx={{
            color: "text.secondary",
            fontSize: 10,
            "& summary": { cursor: "pointer", py: 1 },
          }}
        >
          <Box component="summary">Action details</Box>
          <Typography
            component="pre"
            sx={{
              fontSize: 10,
              whiteSpace: "pre-wrap",
              overflowWrap: "anywhere",
              fontFamily: "monospace",
            }}
          >
            {JSON.stringify(
              {
                result: entry.outcome,
                action: entry.action,
                context: turn.context,
                requestedAt: new Date(entry.timestamp).toISOString(),
              },
              null,
              2,
            )}
          </Typography>
        </Box>
      </Box>
    ))}
    {turn.error && (
      <Alert severity="warning" sx={{ mt: 1.5, fontSize: 12 }}>
        {turn.error.message}
      </Alert>
    )}
  </Box>
);
const messageTextSx = {
  color: "rgba(255,255,255,0.84)",
  fontSize: 12.5,
  lineHeight: 1.65,
  "& p": { m: 0 },
  "& p + p": { mt: 1 },
  "& strong": { color: "rgba(255,255,255,0.96)", fontWeight: 750 },
  "& ul, & ol": { my: 0.75, pl: 2.5 },
  "& li + li": { mt: 0.35 },
} as const;

const MessageText = ({ isUser, text }: { isUser: boolean; text: string }) => {
  if (!isUser) {
    return (
      <Box sx={{ ...messageTextSx, px: 0.4 }}>
        <Markdown
          allowedElements={["p", "strong", "em", "ul", "ol", "li", "br", "a"]}
          components={{ a: ConciergeSourceLink }}
          skipHtml
          unwrapDisallowed
        >
          {text}
        </Markdown>
      </Box>
    );
  }

  return (
    <Typography
      sx={{
        bgcolor: alpha(movieColors.info, 0.14),
        border: `1px solid ${alpha(movieColors.info, 0.2)}`,
        borderRadius: "16px 16px 4px 16px",
        color: "rgba(255,255,255,0.92)",
        fontSize: 12.5,
        lineHeight: 1.65,
        px: 1.6,
        py: 1.1,
        whiteSpace: "pre-wrap",
      }}
    >
      {text}
    </Typography>
  );
};

export default ConciergeDrawer;
