import FingerprintIcon from "@mui/icons-material/Fingerprint";
import { Button, CircularProgress, Stack } from "@mui/material";
import { useMutation } from "@tanstack/react-query";
import { useSnackbar } from "notistack";
import { useNavigate } from "react-router";
import { authSession } from "../../../shared/auth";
import { isPasskeySupported, loginWithPasskey } from "../passkeys/passkeyApi";
import { getPasskeyFeedback } from "../model/authFeedback";

const PasskeyLoginButton = () => {
  const navigateTo = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const passkeysSupported = isPasskeySupported();

  const loginMutation = useMutation({
    mutationFn: loginWithPasskey,
    onSuccess: (session) => {
      authSession.setSession(session);
      void navigateTo("/");
    },
    onError: (error) => {
      const feedback = getPasskeyFeedback(error);
      enqueueSnackbar(feedback.message, { variant: feedback.severity });
    },
  });

  return (
    <Button
      disabled={!passkeysSupported || loginMutation.isPending}
      fullWidth
      onClick={() => loginMutation.mutate()}
      startIcon={<FingerprintIcon />}
      sx={{
        borderColor: "line.control",
        color: "text.primary",
        minHeight: 46,
        textTransform: "none",
        "&:hover": {
          bgcolor: "action.hover",
          borderColor: "text.secondary",
        },
      }}
      type="button"
      variant="outlined"
    >
      {loginMutation.isPending ? (
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          <CircularProgress color="inherit" size={16} />
          <span>Waiting for passkey...</span>
        </Stack>
      ) : (
        "Sign in with passkey"
      )}
    </Button>
  );
};

export default PasskeyLoginButton;
