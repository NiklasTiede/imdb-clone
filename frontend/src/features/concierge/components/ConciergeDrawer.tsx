import ArrowUpwardRoundedIcon from "@mui/icons-material/ArrowUpwardRounded";
import AutoAwesomeRoundedIcon from "@mui/icons-material/AutoAwesomeRounded";
import CloseRoundedIcon from "@mui/icons-material/CloseRounded";
import MoreHorizRoundedIcon from "@mui/icons-material/MoreHorizRounded";
import RefreshRoundedIcon from "@mui/icons-material/RefreshRounded";
import { alpha } from "@mui/material/styles";
import {
  Alert,
  Box,
  Drawer,
  IconButton,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import { useEffect, useRef, useState } from "react";
import Markdown from "react-markdown";
import { movieColors } from "../../../theme";
import { useConciergeChat } from "../hooks/useConciergeChat";
import type { ChatTurn, OpenMovieAction } from "../model/concierge";
import ConciergeEmptyState from "./ConciergeEmptyState";
import ConciergeMovieCard from "./ConciergeMovieCard";

type ConciergeDrawerProps = {
  clientId: string;
  onClose: () => void;
  onUiAction: (action: OpenMovieAction) => void;
  open: boolean;
};

const ConciergeDrawer = ({
  clientId,
  onClose,
  onUiAction,
  open,
}: ConciergeDrawerProps) => {
  const [draft, setDraft] = useState("");
  const { isStreaming, reset, send, status, turns, usage } = useConciergeChat(
    clientId,
    onUiAction,
  );
  const scrollRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const node = scrollRef.current;
    if (node) {
      node.scrollTop = node.scrollHeight;
    }
  }, [isStreaming, status, turns]);

  const submit = (message: string) => {
    const normalized = message.trim();
    if (!normalized || isStreaming) {
      return;
    }
    setDraft("");
    void send(normalized);
  };

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      slotProps={{
        paper: {
          "aria-label": "Movie Concierge",
          sx: {
            background: `radial-gradient(circle at 85% 0%, ${alpha(movieColors.brand, 0.09)}, transparent 28%), ${movieColors.surfaceInset}`,
            borderLeft: `1px solid ${alpha("#ffffff", 0.1)}`,
            boxShadow: "-28px 0 70px rgba(0,0,0,0.42)",
            color: "text.primary",
            maxWidth: "100vw",
            width: { xs: "100vw", sm: 440 },
          },
        },
        root: { keepMounted: true },
      }}
    >
      <Box sx={{ display: "flex", flexDirection: "column", height: "100dvh" }}>
        <Stack
          direction="row"
          sx={{
            alignItems: "center",
            borderBottom: `1px solid ${alpha("#ffffff", 0.08)}`,
            minHeight: 72,
            px: 2,
          }}
        >
          <Box
            sx={{
              alignItems: "center",
              bgcolor: movieColors.brand,
              borderRadius: 1.5,
              color: movieColors.brandInk,
              display: "flex",
              height: 36,
              justifyContent: "center",
              mr: 1.25,
              width: 36,
            }}
          >
            <AutoAwesomeRoundedIcon sx={{ fontSize: 19 }} />
          </Box>
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography
              sx={{ color: "text.primary", fontSize: 14, fontWeight: 750 }}
            >
              Movie Concierge
            </Typography>
            <Stack direction="row" spacing={0.7} sx={{ alignItems: "center" }}>
              <Box
                sx={{
                  bgcolor: "#55d98a",
                  borderRadius: "50%",
                  boxShadow: "0 0 9px rgba(85,217,138,0.55)",
                  height: 6,
                  width: 6,
                }}
              />
              <Typography sx={{ color: "text.secondary", fontSize: 10.5 }}>
                Grounded in this catalog
              </Typography>
            </Stack>
          </Box>
          <Tooltip title="Start a new conversation">
            <span>
              <IconButton
                aria-label="Start a new concierge conversation"
                disabled={isStreaming}
                onClick={reset}
                size="small"
                sx={{ color: "text.secondary" }}
              >
                <RefreshRoundedIcon fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
          <IconButton
            aria-label="Close Movie Concierge"
            onClick={onClose}
            size="small"
            sx={{ color: "text.secondary", ml: 0.5 }}
          >
            <CloseRoundedIcon fontSize="small" />
          </IconButton>
        </Stack>

        <Box
          ref={scrollRef}
          role="log"
          aria-live="polite"
          aria-label="Movie Concierge conversation"
          sx={{ flex: 1, overflowY: "auto", overscrollBehavior: "contain" }}
        >
          {turns.length === 0 ? (
            <ConciergeEmptyState onPrompt={submit} />
          ) : (
            <Stack spacing={2.25} sx={{ px: 2, py: 2.5 }}>
              {turns.map((turn) => (
                <ChatMessage key={turn.id} turn={turn} />
              ))}
              {isStreaming && status && (
                <Stack
                  direction="row"
                  spacing={0.8}
                  sx={{
                    alignItems: "center",
                    color: "text.secondary",
                    pl: 0.5,
                  }}
                >
                  <MoreHorizRoundedIcon
                    sx={{ color: movieColors.brand, fontSize: 18 }}
                  />
                  <Typography sx={{ color: "text.secondary", fontSize: 10.5 }}>
                    {status}
                  </Typography>
                </Stack>
              )}
            </Stack>
          )}
        </Box>

        <Box
          component="form"
          onSubmit={(event) => {
            event.preventDefault();
            submit(draft);
          }}
          sx={{
            bgcolor: alpha(movieColors.surface, 0.94),
            borderTop: `1px solid ${alpha("#ffffff", 0.08)}`,
            p: 1.5,
          }}
        >
          <Box sx={{ position: "relative" }}>
            <TextField
              fullWidth
              multiline
              disabled={isStreaming}
              maxRows={4}
              minRows={2}
              onChange={(event) => setDraft(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  submit(draft);
                }
              }}
              placeholder="Describe what you want to watch…"
              slotProps={{
                htmlInput: {
                  "aria-label": "Ask the Movie Concierge",
                  maxLength: 600,
                },
              }}
              value={draft}
              sx={{
                "& .MuiInputBase-root": {
                  bgcolor: alpha("#ffffff", 0.045),
                  borderRadius: 2,
                  color: "rgba(255,255,255,0.92)",
                  fontSize: 12,
                  pb: 1,
                  pr: 6,
                  pt: 1,
                },
                "& .MuiInputBase-input::placeholder": {
                  color: "rgba(255,255,255,0.62)",
                  opacity: 1,
                },
                "& fieldset": { borderColor: alpha("#ffffff", 0.12) },
                "& .Mui-focused fieldset": {
                  borderColor: `${alpha(movieColors.brand, 0.6)} !important`,
                },
              }}
            />
            <IconButton
              aria-label="Send concierge message"
              disabled={isStreaming || !draft.trim()}
              type="submit"
              sx={{
                bgcolor: movieColors.brand,
                bottom: 10,
                color: movieColors.brandInk,
                height: 34,
                position: "absolute",
                right: 9,
                width: 34,
                "&:hover": { bgcolor: "#ffe053" },
                "&.Mui-disabled": {
                  bgcolor: alpha("#ffffff", 0.08),
                  color: alpha("#ffffff", 0.3),
                },
              }}
            >
              <ArrowUpwardRoundedIcon sx={{ fontSize: 18 }} />
            </IconButton>
          </Box>
          <Stack
            direction="row"
            sx={{
              alignItems: "center",
              justifyContent: "space-between",
              mt: 0.8,
              px: 0.3,
            }}
          >
            <Typography sx={{ color: "text.secondary", fontSize: 9.5 }}>
              Read-only preview · Verify details before deciding
            </Typography>
            {usage && (
              <Typography sx={{ color: "text.secondary", fontSize: 9 }}>
                {usage.totalTokens.toLocaleString()} tokens
              </Typography>
            )}
          </Stack>
        </Box>
      </Box>
    </Drawer>
  );
};

const ChatMessage = ({ turn }: { turn: ChatTurn }) => {
  const isUser = turn.role === "user";
  return (
    <Box
      sx={{
        alignSelf: isUser ? "flex-end" : "stretch",
        maxWidth: isUser ? "86%" : "100%",
      }}
    >
      {turn.text && <MessageText isUser={isUser} text={turn.text} />}
      {turn.movies.length > 0 && (
        <Stack spacing={1} sx={{ mt: turn.text ? 1.25 : 0 }}>
          {turn.movies.map((movie) => (
            <ConciergeMovieCard key={movie.movieId} movie={movie} />
          ))}
        </Stack>
      )}
      {turn.error && (
        <Alert severity="warning" sx={{ fontSize: 11.5, mt: 1 }}>
          {turn.error.message}
        </Alert>
      )}
    </Box>
  );
};

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
          allowedElements={["p", "strong", "em", "ul", "ol", "li", "br"]}
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
