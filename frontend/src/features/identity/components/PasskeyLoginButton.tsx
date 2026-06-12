import FingerprintIcon from "@mui/icons-material/Fingerprint";
import { Button } from "@mui/material";
import { useMutation } from "@tanstack/react-query";
import { useSnackbar } from "notistack";
import { useNavigate } from "react-router";
import { authSession } from "../../../shared/auth";
import {
  isPasskeySupported,
  loginWithPasskey,
} from "../passkeys/passkeyApi";

const PasskeyLoginButton = () => {
  const navigateTo = useNavigate();
  const { enqueueSnackbar } = useSnackbar();
  const passkeysSupported = isPasskeySupported();

  const loginMutation = useMutation({
    mutationFn: loginWithPasskey,
    onSuccess: (session) => {
      authSession.setSession(session);
      navigateTo("/");
    },
    onError: (error) => {
      enqueueSnackbar(
        error instanceof Error
          ? error.message
          : "Passkey sign-in was not completed.",
        { variant: "error" },
      );
    },
  });

  return (
    <Button
      disabled={!passkeysSupported || loginMutation.isPending}
      fullWidth
      onClick={() => loginMutation.mutate()}
      startIcon={<FingerprintIcon />}
      sx={{
        borderColor: "rgba(255,255,255,0.2)",
        color: "common.white",
        minHeight: 46,
        textTransform: "none",
        "&:hover": {
          bgcolor: "rgba(255,255,255,0.06)",
          borderColor: "rgba(255,255,255,0.34)",
        },
      }}
      type="button"
      variant="outlined"
    >
      Sign in with passkey
    </Button>
  );
};

export default PasskeyLoginButton;
