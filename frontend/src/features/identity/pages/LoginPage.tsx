import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import {
  Box,
  Button,
  Grid,
  IconButton,
  InputAdornment,
  Link,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { useSnackbar } from "notistack";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link as RouterLink, useNavigate } from "react-router";
import * as zod from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { i18n } from "../../../i18n";
import { authSession, useAuthSession } from "../../../shared/auth";
import { movieColors } from "../../../theme";
import { authenticateAccount } from "../api/identityMutations";
import AuthVisualPane from "../components/AuthVisualPane";
import { authTextFieldSx } from "../components/authFormStyles";
import PasskeyLoginButton from "../components/PasskeyLoginButton";
import SocialLoginButtons from "../components/SocialLoginButtons";
import type { LoginRequest } from "../model/identityRequests";

interface FormInputs {
  usernameOrEmail: string;
  password: string;
}

const schema = zod.object({
  usernameOrEmail: zod.string().min(1, "Email or username is required"),
  password: zod.string().min(1, "Password is required"),
});

const LoginPage = () => {
  const navigateTo = useNavigate();
  const isLoggedIn = useAuthSession();
  const { enqueueSnackbar } = useSnackbar();
  const [showPassword, setShowPassword] = useState(false);

  const authenticateAccountMutation = useMutation({
    mutationFn: authenticateAccount,
    onSuccess: (session) => {
      authSession.setSession(session);
      navigateTo("/");
    },
    onError: (error: unknown) => {
      const axiosError = error as AxiosError;
      enqueueSnackbar(
        axiosError.response?.status === 401
          ? i18n.login.badCredentials
          : i18n.login.loadingError,
        { variant: "error" },
      );
    },
  });

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormInputs>({
    mode: "onBlur",
    resolver: zodResolver(schema),
  });

  const onSubmit = (data: FormInputs) => {
    const payload: LoginRequest = {
      usernameOrEmail: data.usernameOrEmail,
      password: data.password,
    };
    authenticateAccountMutation.mutate(payload);
  };

  useEffect(() => {
    if (isLoggedIn) {
      navigateTo("/");
    }
  }, [isLoggedIn, navigateTo]);

  return (
    <Grid container sx={{ minHeight: "calc(100vh - 55px)" }}>
      <Grid size={{ xs: 12, md: 6 }}>
        <AuthVisualPane variant="login" />
      </Grid>
      <Grid
        size={{ xs: 12, md: 6 }}
        sx={{
          alignItems: "center",
          display: "flex",
          justifyContent: "center",
          px: { xs: 2.5, sm: 5, md: 7 },
          py: { xs: 4, md: 6 },
        }}
      >
        <Box
          component="form"
          onSubmit={handleSubmit(onSubmit)}
          sx={{ maxWidth: 440, width: "100%" }}
        >
          <Box sx={{ mb: 3.5 }}>
            <Typography
              component="h1"
              sx={{
                color: "common.white",
                fontSize: { xs: 24, sm: 26 },
                fontWeight: 500,
                mb: 0.75,
              }}
            >
              Sign in
            </Typography>
            <Typography color="text.secondary" variant="body2">
              Enter your credentials to continue.
            </Typography>
          </Box>

          <Stack spacing={2}>
            <TextField
              autoComplete="username"
              autoFocus
              error={!!errors.usernameOrEmail}
              fullWidth
              helperText={
                errors.usernameOrEmail ? errors.usernameOrEmail?.message : " "
              }
              label="Email or username"
              type="text"
              {...register("usernameOrEmail")}
              sx={authTextFieldSx}
            />

            <Box>
              <Stack
                direction="row"
                sx={{
                  alignItems: "baseline",
                  justifyContent: "space-between",
                  mb: 0.75,
                }}
              >
                <Typography
                  color="text.secondary"
                  component="label"
                  htmlFor="password"
                  variant="caption"
                >
                  Password
                </Typography>
                <Link
                  component={RouterLink}
                  sx={{
                    color: movieColors.brand,
                    fontSize: 12,
                    fontWeight: 600,
                    textDecoration: "none",
                    "&:hover": { textDecoration: "underline" },
                  }}
                  to="/reset-password"
                >
                  Forgot password?
                </Link>
              </Stack>

              <TextField
                autoComplete="current-password"
                error={!!errors.password}
                fullWidth
                helperText={errors.password ? errors.password?.message : " "}
                id="password"
                slotProps={{
                  input: {
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          aria-label={
                            showPassword ? "Hide password" : "Show password"
                          }
                          edge="end"
                          onClick={() => setShowPassword((current) => !current)}
                          size="small"
                        >
                          {showPassword ? (
                            <VisibilityOffIcon fontSize="small" />
                          ) : (
                            <VisibilityIcon fontSize="small" />
                          )}
                        </IconButton>
                      </InputAdornment>
                    ),
                  },
                }}
                type={showPassword ? "text" : "password"}
                {...register("password")}
                sx={authTextFieldSx}
              />
            </Box>

            <Button
              disabled={authenticateAccountMutation.isPending}
              fullWidth
              sx={{ py: 1.5, textTransform: "none" }}
              type="submit"
              variant="contained"
            >
              Sign in
            </Button>

            <PasskeyLoginButton />

            <SocialLoginButtons />
          </Stack>
        </Box>
      </Grid>
    </Grid>
  );
};

export default LoginPage;
