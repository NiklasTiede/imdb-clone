import GitHubIcon from "@mui/icons-material/GitHub";
import GoogleIcon from "@mui/icons-material/Google";
import {
  Box,
  Button,
  CircularProgress,
  Divider,
  Stack,
  Typography,
} from "@mui/material";
import { useState } from "react";

const socialProviders = [
  {
    id: "google",
    label: "Continue with Google",
    icon: <GoogleIcon fontSize="small" />,
  },
  {
    id: "github",
    label: "Continue with GitHub",
    icon: <GitHubIcon fontSize="small" />,
  },
];

const SocialLoginButtons = () => {
  const [pendingProvider, setPendingProvider] = useState<string | null>(null);

  const startSocialLogin = (provider: string) => {
    setPendingProvider(provider);
    window.location.href = `/oauth2/authorization/${provider}`;
  };

  return (
    <Stack spacing={1.5}>
      <Divider
        sx={{
          "&::before, &::after": { borderColor: "divider" },
        }}
      >
        <Typography
          sx={{
            color: "text.secondary",
            fontSize: 12,
            px: 1,
          }}
        >
          or
        </Typography>
      </Divider>

      <Box
        sx={{
          display: "grid",
          gap: 1,
          gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" },
        }}
      >
        {socialProviders.map((provider) => (
          <Button
            disabled={pendingProvider !== null}
            key={provider.id}
            fullWidth
            onClick={() => startSocialLogin(provider.id)}
            startIcon={provider.icon}
            sx={{
              borderColor: "line.control",
              color: "text.primary",
              minHeight: 44,
              textTransform: "none",
              "&:hover": {
                bgcolor: "action.hover",
                borderColor: "text.secondary",
              },
            }}
            type="button"
            variant="outlined"
          >
            {pendingProvider === provider.id ? (
              <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                <CircularProgress color="inherit" size={14} />
                <span>Opening...</span>
              </Stack>
            ) : (
              provider.label
            )}
          </Button>
        ))}
      </Box>
    </Stack>
  );
};

export default SocialLoginButtons;
