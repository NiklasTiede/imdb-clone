import MicRoundedIcon from "@mui/icons-material/MicRounded";
import MicOffRoundedIcon from "@mui/icons-material/MicOffRounded";
import StopRoundedIcon from "@mui/icons-material/StopRounded";
import OpenInFullRoundedIcon from "@mui/icons-material/OpenInFullRounded";
import {
  Alert,
  Box,
  Button,
  IconButton,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import { alpha } from "@mui/material/styles";
import { movieColors } from "../../../theme";
import type { ConciergeVoice } from "../hooks/useConciergeVoice";
import { voiceLabels } from "../model/voice";
import ConciergeMovieCard from "./ConciergeMovieCard";

const label = (voice: ConciergeVoice) =>
  voice.levels.playing
    ? "Concierge is speaking"
    : voice.muted
      ? "Microphone is off"
      : voiceLabels[voice.status];

const VoiceSignal = ({
  voice,
  compact = false,
}: {
  voice: ConciergeVoice;
  compact?: boolean;
}) => {
  const speaking = voice.levels.playing;
  const level = speaking ? voice.levels.output : voice.levels.input;
  const color = speaking ? movieColors.brand : movieColors.info;
  return (
    <Box
      aria-hidden="true"
      sx={{
        width: compact ? 42 : { xs: 92, sm: 116 },
        height: compact ? 42 : { xs: 92, sm: 116 },
        borderRadius: "50%",
        border: `1px solid ${alpha(color, 0.5)}`,
        background: `radial-gradient(circle, ${alpha(color, 0.13)}, transparent 70%)`,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        gap: compact ? "2px" : "4px",
        flexShrink: 0,
      }}
    >
      {[0.45, 0.7, 0.9, 1, 0.8, 0.6, 0.4].map((weight, i) => (
        <Box
          key={i}
          sx={{
            width: compact ? 2 : 4,
            borderRadius: 3,
            bgcolor: color,
            height: Math.max(3, level * weight * (compact ? 25 : 58)),
            transition: "height 70ms linear",
            "@media (prefers-reduced-motion: reduce)": {
              height: compact ? 8 : 18,
              transition: "none",
            },
          }}
        />
      ))}
    </Box>
  );
};

const VoiceControls = ({ voice }: { voice: ConciergeVoice }) => (
  <Stack direction="row" spacing={1}>
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
}) => {
  if (!voice.active)
    return (
      <Stack
        spacing={1}
        sx={{
          px: 2,
          py: 1.5,
          borderBottom: `1px solid ${alpha(movieColors.brand, 0.12)}`,
        }}
      >
        {voice.error && <Alert severity="warning">{voice.error}</Alert>}
        <Button
          startIcon={<MicRoundedIcon />}
          disabled={disabled}
          onClick={() => void voice.start()}
          sx={{
            justifyContent: "flex-start",
            minHeight: 44,
            color: movieColors.brand,
          }}
        >
          {voice.status === "error" ? "Reconnect voice" : "Start voice"}
        </Button>
        <Typography variant="caption" color="text.secondary">
          Speak English. Try “Find Forrest Gump and open it.”
        </Typography>
      </Stack>
    );

  return (
    <Stack
      spacing={2.5}
      sx={{ px: 2, py: 3, overflowY: "auto", flex: 1, minHeight: 0 }}
    >
      <Stack spacing={1.5} sx={{ alignItems: "center", textAlign: "center" }}>
        <VoiceSignal voice={voice} />
        <Typography role="status" sx={{ fontSize: 18, fontWeight: 750 }}>
          {label(voice)}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {voice.status === "connecting"
            ? "Allow microphone access when your browser asks."
            : "English · Live movie catalog"}
        </Typography>
        <VoiceControls voice={voice} />
        {voice.levels.playing && (
          <Button onClick={voice.interrupt} sx={{ minHeight: 44 }}>
            Interrupt reply
          </Button>
        )}
      </Stack>
      {voice.userText && (
        <Box
          sx={{
            alignSelf: "flex-end",
            maxWidth: "88%",
            p: 1.5,
            bgcolor: alpha(movieColors.info, 0.12),
            borderRadius: 1,
          }}
        >
          <Typography variant="caption" color="text.secondary">
            You
          </Typography>
          <Typography sx={{ fontSize: 13 }}>{voice.userText}</Typography>
        </Box>
      )}
      {voice.assistantText && (
        <Typography sx={{ fontSize: 14, lineHeight: 1.7 }}>
          {voice.assistantText}
        </Typography>
      )}
      {voice.movies.map((movie) => (
        <ConciergeMovieCard key={movie.movieId} movie={movie} />
      ))}
      <Button
        onClick={voice.end}
        sx={{ alignSelf: "center", color: "text.secondary", minHeight: 44 }}
      >
        Prefer typing? End voice
      </Button>
    </Stack>
  );
};

export const ConciergeVoiceDock = ({
  voice,
  expand,
}: {
  voice: ConciergeVoice;
  expand: () => void;
}) => (
  <Stack
    direction="row"
    spacing={1}
    sx={{
      position: "fixed",
      bottom: { xs: "max(12px, env(safe-area-inset-bottom))", sm: 24 },
      right: { xs: 12, sm: 24 },
      left: { xs: 12, sm: "auto" },
      bgcolor: movieColors.surfaceElevated,
      border: `1px solid ${alpha(movieColors.brand, 0.3)}`,
      borderRadius: 1,
      p: 1,
      alignItems: "center",
      boxShadow: "0 12px 38px rgba(0,0,0,0.4)",
      zIndex: (theme) => theme.zIndex.fab,
    }}
  >
    <Box
      onClick={voice.levels.playing ? voice.interrupt : expand}
      component="button"
      aria-label={
        voice.levels.playing ? "Interrupt reply" : "Expand voice conversation"
      }
      sx={{ border: 0, p: 0, background: "transparent", cursor: "pointer" }}
    >
      <VoiceSignal voice={voice} compact />
    </Box>
    <Typography sx={{ fontSize: 12, flex: 1, minWidth: 0 }}>
      {label(voice)}
    </Typography>
    <VoiceControls voice={voice} />
    <IconButton
      aria-label="Expand voice conversation"
      onClick={expand}
      sx={{ minHeight: 44, minWidth: 44 }}
    >
      <OpenInFullRoundedIcon fontSize="small" />
    </IconButton>
  </Stack>
);
