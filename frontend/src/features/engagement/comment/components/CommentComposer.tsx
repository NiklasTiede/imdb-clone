import SendIcon from "@mui/icons-material/Send";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useState } from "react";
import AppSurface from "../../../../shared/layout/AppSurface";
import { ProfileAvatar } from "../../../../shared/media";
import { movieColors } from "../../../../theme";
import type { CommentAuthor } from "../model/comment";

const MAX_COMMENT_LENGTH = 1000;

type CommentComposerProps = {
  author?: CommentAuthor;
  errorMessage?: string | null;
  isAuthenticated: boolean;
  isSubmitting: boolean;
  movieTitle: string;
  onRequestSignIn: () => void;
  onSubmit: (message: string) => Promise<void>;
};

const CommentComposer = ({
  author,
  errorMessage,
  isAuthenticated,
  isSubmitting,
  movieTitle,
  onRequestSignIn,
  onSubmit,
}: CommentComposerProps) => {
  const [message, setMessage] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);

  if (!isAuthenticated) {
    return (
      <AppSurface
        accent="info"
        sx={{
          alignItems: { xs: "flex-start", sm: "center" },
          display: "flex",
          flexDirection: { xs: "column", sm: "row" },
          gap: 2,
          justifyContent: "space-between",
          p: { xs: 2, sm: 2.5 },
        }}
      >
        <Box>
          <Typography sx={{ fontSize: 16, fontWeight: 700 }}>
            Join the discussion
          </Typography>
          <Typography sx={{ color: "text.secondary", mt: 0.5 }}>
            Sign in to share your take on {movieTitle}.
          </Typography>
        </Box>
        <Button onClick={onRequestSignIn} variant="contained">
          Sign in to comment
        </Button>
      </AppSurface>
    );
  }

  const submit = async () => {
    const normalizedMessage = message.trim();
    if (!normalizedMessage) {
      setValidationError("Write a comment before publishing.");
      return;
    }

    setValidationError(null);
    try {
      await onSubmit(normalizedMessage);
      setMessage("");
    } catch {
      // The parent keeps the draft visible and provides mutation feedback.
    }
  };

  return (
    <AppSurface
      accent="info"
      sx={{
        backgroundColor: movieColors.surface,
        p: { xs: 2, sm: 2.5 },
      }}
    >
      <Stack direction="row" spacing={1.5} sx={{ alignItems: "flex-start" }}>
        <ProfileAvatar
          imageUrlToken={author?.imageUrlToken}
          sx={{ height: 40, mt: 0.25, width: 40 }}
        />
        <Box
          component="form"
          onSubmit={(event) => event.preventDefault()}
          sx={{ flex: 1 }}
        >
          <TextField
            disabled={isSubmitting}
            fullWidth
            label={`Comment on ${movieTitle}`}
            maxRows={8}
            minRows={3}
            multiline
            onChange={(event) => {
              setMessage(event.target.value);
              if (validationError) {
                setValidationError(null);
              }
            }}
            onKeyDown={(event) => {
              if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
                event.preventDefault();
                void submit();
              }
            }}
            placeholder="What did you think?"
            slotProps={{ htmlInput: { maxLength: MAX_COMMENT_LENGTH } }}
            value={message}
          />
          {(validationError || errorMessage) && (
            <Alert severity="error" sx={{ mt: 1.25 }}>
              {validationError ?? errorMessage}
            </Alert>
          )}
          <Stack
            direction="row"
            sx={{
              alignItems: "center",
              justifyContent: "space-between",
              mt: 1.25,
            }}
          >
            <Typography sx={{ color: "text.secondary", fontSize: 12 }}>
              {message.length} / {MAX_COMMENT_LENGTH}
            </Typography>
            <Button
              disabled={isSubmitting || !message.trim()}
              endIcon={<SendIcon />}
              onClick={() => void submit()}
              type="button"
              variant="contained"
            >
              {isSubmitting ? "Publishing..." : "Publish comment"}
            </Button>
          </Stack>
        </Box>
      </Stack>
    </AppSurface>
  );
};

export default CommentComposer;
