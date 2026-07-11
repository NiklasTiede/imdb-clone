import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import {
  Box,
  Alert,
  Button,
  CircularProgress,
  IconButton,
  InputAdornment,
  Link,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import {
  Link as RouterLink,
  useLocation,
  useNavigate,
  useSearchParams,
} from "react-router";
import * as zod from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { authSession, useAuthSession } from "../../../shared/auth";
import { movieColors } from "../../../theme";
import { authenticateAccount } from "../api/identityMutations";
import AuthPageFrame from "../components/AuthPageFrame";
import { authTextFieldSx } from "../components/authFormStyles";
import PasskeyLoginButton from "../components/PasskeyLoginButton";
import SocialLoginButtons from "../components/SocialLoginButtons";
import type { LoginRequest } from "../model/identityRequests";
import {
  getPasswordLoginFeedback,
  socialLoginFailure,
  type AuthFeedback,
} from "../model/authFeedback";

interface FormInputs {
  usernameOrEmail: string;
  password: string;
}

const loginLocationStateSchema = zod.object({
  from: zod
    .object({
      hash: zod.string().optional(),
      pathname: zod.string().optional(),
      search: zod.string().optional(),
    })
    .optional(),
  registrationMessage: zod.string().optional(),
});

type LoginLocationState = zod.infer<typeof loginLocationStateSchema>;

export const parseLoginLocationState = (
  value: unknown,
): LoginLocationState | null => {
  const result = loginLocationStateSchema.safeParse(value);
  return result.success ? result.data : null;
};

const schema = zod.object({
  usernameOrEmail: zod.string().min(1, "Email or username is required"),
  password: zod.string().min(1, "Password is required"),
});

const LoginPage = () => {
  const navigateTo = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const isLoggedIn = useAuthSession();
  const [showPassword, setShowPassword] = useState(false);
  const [feedback, setFeedback] = useState<AuthFeedback | null>(null);
  const locationState = parseLoginLocationState(location.state);
  const registrationMessage = locationState?.registrationMessage;
  const socialFeedback =
    searchParams.get("error") === "social" ? socialLoginFailure : null;

  const authenticateAccountMutation = useMutation({
    mutationFn: authenticateAccount,
    onMutate: () => setFeedback(null),
    onSuccess: (session) => {
      authSession.setSession(session);
      void navigateTo(getPostLoginDestination(locationState));
    },
    onError: (error: unknown) => {
      setFeedback(getPasswordLoginFeedback(error));
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
      void navigateTo(getPostLoginDestination(locationState));
    }
  }, [isLoggedIn, locationState, navigateTo]);

  return (
    <AuthPageFrame variant="login">
      <Box
        component="form"
        onSubmit={(event) => {
          void handleSubmit(onSubmit)(event);
        }}
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

        {registrationMessage && (
          <Alert severity="success" sx={{ mb: 2.5 }}>
            {registrationMessage}
          </Alert>
        )}
        {(feedback ?? socialFeedback) && (
          <Alert
            severity={(feedback ?? socialFeedback)?.severity}
            sx={{ mb: 2.5 }}
          >
            {(feedback ?? socialFeedback)?.message}
          </Alert>
        )}

        <Stack spacing={2}>
          <TextField
            autoComplete="username"
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
            {authenticateAccountMutation.isPending ? (
              <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                <CircularProgress color="inherit" size={16} />
                <span>Signing in...</span>
              </Stack>
            ) : (
              "Sign in"
            )}
          </Button>

          <PasskeyLoginButton />

          <SocialLoginButtons />
        </Stack>
      </Box>
    </AuthPageFrame>
  );
};

export const getPostLoginDestination = (
  state: LoginLocationState | null,
): string => {
  const from = state?.from;
  const pathname = from?.pathname;
  if (!pathname?.startsWith("/") || pathname.startsWith("//")) {
    return "/";
  }

  return `${pathname}${from?.search ?? ""}${from?.hash ?? ""}`;
};

export default LoginPage;
