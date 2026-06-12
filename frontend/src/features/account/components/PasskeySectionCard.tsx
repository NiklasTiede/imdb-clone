import AddIcon from "@mui/icons-material/Add";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import FingerprintIcon from "@mui/icons-material/Fingerprint";
import {
  Box,
  Button,
  CircularProgress,
  IconButton,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import { useState } from "react";
import { movieColors } from "../../../theme";
import type { PasskeyCredential } from "../../identity";
import SectionCard from "./SectionCard";

type PasskeySectionCardProps = {
  isAdding: boolean;
  isDeleting: boolean;
  isLoading: boolean;
  onAdd: (label: string) => void;
  onDelete: (credentialId: string) => void;
  passkeys: PasskeyCredential[];
};

const PasskeySectionCard = ({
  isAdding,
  isDeleting,
  isLoading,
  onAdd,
  onDelete,
  passkeys,
}: PasskeySectionCardProps) => {
  const [label, setLabel] = useState(defaultPasskeyLabel());

  const addPasskey = () => {
    const trimmedLabel = label.trim();
    onAdd(trimmedLabel || defaultPasskeyLabel());
  };

  return (
    <SectionCard
      subtitle="Use device-bound credentials for passwordless sign-in"
      title="Passkeys"
      actions={
        <Button
          disabled={isAdding}
          onClick={addPasskey}
          startIcon={<AddIcon />}
          sx={{ textTransform: "none" }}
          variant="contained"
        >
          Add passkey
        </Button>
      }
    >
      <Stack spacing={2}>
        <TextField
          fullWidth
          label="Passkey label"
          onChange={(event) => setLabel(event.target.value)}
          size="small"
          value={label}
        />

        {isLoading ? (
          <Box sx={{ alignItems: "center", display: "flex", gap: 1 }}>
            <CircularProgress size={18} />
            <Typography color="text.secondary" variant="body2">
              Loading passkeys
            </Typography>
          </Box>
        ) : passkeys.length === 0 ? (
          <Typography color="text.secondary" variant="body2">
            No passkeys registered.
          </Typography>
        ) : (
          <Stack spacing={1}>
            {passkeys.map((passkey) => (
              <Box
                key={passkey.credentialId}
                sx={{
                  alignItems: "center",
                  bgcolor: "rgba(255,255,255,0.035)",
                  border: "1px solid rgba(255,255,255,0.08)",
                  borderRadius: 1,
                  display: "grid",
                  gap: 1.5,
                  gridTemplateColumns: "auto 1fr auto",
                  px: 1.5,
                  py: 1.25,
                }}
              >
                <FingerprintIcon sx={{ color: movieColors.brand }} />
                <Box sx={{ minWidth: 0 }}>
                  <Typography noWrap sx={{ fontWeight: 600 }} variant="body2">
                    {passkey.label}
                  </Typography>
                  <Typography color="text.secondary" variant="caption">
                    Last used {formatDateTime(passkey.lastUsedAt)}
                  </Typography>
                </Box>
                <Tooltip title="Delete passkey">
                  <span>
                    <IconButton
                      aria-label={`Delete ${passkey.label}`}
                      disabled={isDeleting}
                      onClick={() => onDelete(passkey.credentialId)}
                      size="small"
                    >
                      <DeleteOutlineOutlinedIcon fontSize="small" />
                    </IconButton>
                  </span>
                </Tooltip>
              </Box>
            ))}
          </Stack>
        )}
      </Stack>
    </SectionCard>
  );
};

const defaultPasskeyLabel = () => {
  if (typeof navigator === "undefined") {
    return "This device";
  }
  return navigator.platform ? `${navigator.platform} passkey` : "This device";
};

const formatDateTime = (value: string) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "unknown";
  }
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
};

export default PasskeySectionCard;
