import MicRoundedIcon from "@mui/icons-material/MicRounded";
import MicOffRoundedIcon from "@mui/icons-material/MicOffRounded";
import StopRoundedIcon from "@mui/icons-material/StopRounded";
import CloseFullscreenRoundedIcon from "@mui/icons-material/CloseFullscreenRounded";
import OpenInFullRoundedIcon from "@mui/icons-material/OpenInFullRounded";
import {
  Box,
  Button,
  IconButton,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import { alpha } from "@mui/material/styles";
import { visuallyHidden } from "@mui/utils";
import { useCallback } from "react";
import { movieColors } from "../../../theme";
import type { ConciergeVoice } from "../hooks/useConciergeVoice";
import { voiceLabels } from "../model/voice";
import { VoiceLens, type OrbState } from "./VoiceLens";

export const voiceLabel = (voice: ConciergeVoice) =>
  voice.levels.playing
    ? "Concierge is speaking"
    : voice.muted
      ? "Microphone is off"
      : voiceLabels[voice.status];

export function VoiceSignal({
  voice,
  size = 66,
}: {
  voice: ConciergeVoice;
  size?: number;
}) {
  const { readLevels } = voice;
  const input = useCallback(() => readLevels().input, [readLevels]);
  const state: OrbState = voice.muted
    ? "muted"
    : voice.status === "listening"
      ? "listening"
      : voice.active
        ? "thinking"
        : "ready";
  return (
    <Box sx={{ width: size, flexShrink: 0 }}>
      <VoiceLens
        state={state}
        closed={!voice.active || (voice.muted && !voice.levels.playing)}
        live
        readLevel={input}
        readAudio={voice.readLevels}
      />
    </Box>
  );
}

export const VoiceControls = ({ voice }: { voice: ConciergeVoice }) => (
  <Stack direction="row" spacing={0.5}>
    <Tooltip title={voice.muted ? "Resume microphone" : "Mute microphone"}>
      <IconButton
        aria-label={voice.muted ? "Resume microphone" : "Mute microphone"}
        onClick={voice.toggleMute}
        sx={{
          minWidth: 44,
          minHeight: 44,
          color: voice.muted ? "text.secondary" : movieColors.info,
        }}
      >
        {voice.muted ? <MicOffRoundedIcon /> : <MicRoundedIcon />}
      </IconButton>
    </Tooltip>
    <Tooltip title="End voice session">
      <IconButton
        aria-label="End voice session"
        onClick={voice.end}
        sx={{ minWidth: 44, minHeight: 44 }}
      >
        <StopRoundedIcon />
      </IconButton>
    </Tooltip>
  </Stack>
);

export const ConciergeVoicePanel = ({
  voice,
  disabled = false,
}: {
  voice: ConciergeVoice;
  disabled?: boolean;
}) =>
  voice.active ? null : (
    <Button
      startIcon={<MicRoundedIcon />}
      disabled={disabled}
      onClick={() => void voice.start()}
      sx={{ minHeight: 44, color: movieColors.brand }}
    >
      {voice.status === "error" ? "Reconnect voice" : "Start voice"}
    </Button>
  );

export const ConciergeVoiceDock = ({
  voice,
  conversationOpen,
  toggleConversation,
  debug = false,
  disabled = false,
}: {
  voice: ConciergeVoice;
  conversationOpen: boolean;
  toggleConversation: () => void;
  debug?: boolean;
  disabled?: boolean;
}) => {
  const label =
    voice.status === "connecting"
      ? "Cancel voice connection"
      : voice.active
        ? "End voice session"
        : voice.status === "standby"
          ? "Resume voice"
          : voice.status === "error"
            ? "Reconnect voice"
            : "Start voice";
  const actionLabel =
    debug && voice.active
      ? voice.levels.playing
        ? "Interrupt reply"
        : conversationOpen
          ? "Close conversation"
          : "Open conversation"
      : debug
        ? `${label} using lens`
        : label;
  return (
    <Stack
      component="section"
      aria-label="Voice overlay"
      sx={{
        position: "fixed",
        bottom: { xs: "max(12px, env(safe-area-inset-bottom))", sm: 24 },
        left: {
          xs: "50%",
          md: conversationOpen ? "calc((100% - 440px) / 2)" : "50%",
        },
        transform: "translateX(-50%)",
        width: "max-content",
        maxWidth: "calc(100vw - 24px)",
        alignItems: "center",
        pointerEvents: "none",
        zIndex: (theme) => theme.zIndex.drawer + 1,
      }}
    >
      <Tooltip
        title={actionLabel}
        describeChild
        placement="top"
        disableInteractive
      >
        <Box
          component="button"
          type="button"
          disabled={disabled}
          data-testid="voice-lens-toggle"
          data-state={voice.active ? voice.status : "closed"}
          aria-pressed={voice.active}
          onClick={
            debug && voice.active
              ? voice.levels.playing
                ? voice.interrupt
                : toggleConversation
              : voice.active
                ? voice.end
                : () => void voice.start()
          }
          aria-label={actionLabel}
          sx={{
            border: 0,
            p: 0,
            background: "transparent",
            pointerEvents: "auto",
            cursor: "pointer",
            borderRadius: "50%",
            opacity: 0.93,
            mb: debug ? -1 : 0,
            "&:disabled": { cursor: "wait", opacity: 0.5 },
            filter: "drop-shadow(0 8px 28px rgba(0,0,0,.6))",
            "&:focus-visible": {
              outline: `2px solid ${movieColors.brand}`,
              outlineOffset: 4,
            },
          }}
        >
          <VoiceSignal voice={voice} size={115} />
        </Box>
      </Tooltip>
      {!debug && (
        <Box role="status" sx={visuallyHidden}>
          {voice.active
            ? voiceLabel(voice)
            : voice.status === "standby"
              ? voiceLabels.standby
              : "Voice is off. Click the lens to start."}
        </Box>
      )}
      {debug && voice.active && (
        <Stack
          direction="row"
          sx={{
            alignItems: "center",
            px: 0.75,
            borderRadius: 8,
            bgcolor: movieColors.surface,
            opacity: 0.85,
            backdropFilter: "blur(16px)",
            border: `1px solid ${alpha(movieColors.brand, 0.22)}`,
            boxShadow: "0 8px 30px rgba(0,0,0,0.35)",
            pointerEvents: "auto",
            "& .MuiIconButton-root": {
              minWidth: { xs: 40, sm: 36 },
              minHeight: { xs: 40, sm: 36 },
              p: 0.75,
            },
            "& .MuiSvgIcon-root": { fontSize: 19 },
          }}
        >
          <Typography
            sx={{ fontSize: 10.5, minWidth: 0, px: 0.5 }}
            role="status"
          >
            {voiceLabel(voice)}
          </Typography>
          <VoiceControls voice={voice} />
          <Tooltip
            title={
              conversationOpen
                ? "Close conversation"
                : "Conversation and preferences"
            }
          >
            <IconButton
              aria-label={
                conversationOpen
                  ? "Collapse voice conversation"
                  : "Expand voice conversation"
              }
              aria-expanded={conversationOpen}
              aria-controls="concierge-conversation"
              onClick={toggleConversation}
              sx={{ minHeight: 44, minWidth: 44 }}
            >
              {conversationOpen ? (
                <CloseFullscreenRoundedIcon fontSize="small" />
              ) : (
                <OpenInFullRoundedIcon fontSize="small" />
              )}
            </IconButton>
          </Tooltip>
        </Stack>
      )}
    </Stack>
  );
};
