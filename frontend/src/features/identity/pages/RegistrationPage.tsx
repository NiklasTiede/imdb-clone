import {
  Box,
  Button,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  Grid,
  InputAdornment,
  Link,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import CancelIcon from "@mui/icons-material/Cancel";
import CheckIcon from "@mui/icons-material/Check";
import React, { useState } from "react";
import { Link as RouterLink, useNavigate } from "react-router";
import { i18n } from "../../../i18n";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as zod from "zod";
import type { RegistrationRequest } from "../model/identityRequests";
import { useMutation } from "@tanstack/react-query";
import {
  getRegistrationInvalidParams,
  registerAccount,
} from "../api/identityMutations";
import {
  checkEmailAvailability,
  checkUsernameAvailability,
} from "../api/identityAvailability";
import AuthVisualPane from "../components/AuthVisualPane";
import { authTextFieldSx } from "../components/authFormStyles";
import SocialLoginButtons from "../components/SocialLoginButtons";
import { useAvailability } from "../hooks/useAvailability";
import { useSnackbar } from "notistack";

export interface FormInputs {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export const schema = zod
  .object({
    username: zod
      .string()
      .min(2, "Username must contain at least 2 character(s)")
      .max(30, "Username must contain at most 30 character(s)")
      .regex(
        new RegExp(i18n.regex.username.pattern),
        i18n.regex.username.rules,
      ),
    email: zod
      .string()
      .regex(new RegExp(i18n.regex.email.pattern), i18n.regex.email.rules),
    password: zod
      .string()
      .min(8, "Password must contain at least 8 characters")
      .max(30, "Password must contain at most 30 characters")
      .regex(
        new RegExp(i18n.regex.password.pattern),
        i18n.regex.password.rules,
      ),
    confirmPassword: zod
      .string()
      .min(8, "Password must contain at least 8 characters")
      .max(30, "Password must contain at most 30 characters")
      .regex(
        new RegExp(i18n.regex.password.pattern),
        i18n.regex.password.rules,
      ),
  })
  .superRefine(({ confirmPassword, password }, ctx) => {
    if (confirmPassword !== password) {
      ctx.addIssue({
        path: ["confirmPassword"],
        code: "custom",
        message: "Passwords do not match",
      });
    }
  });

const RegistrationPage = () => {
  const navigateTo = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [showPassword, setShowPassword] = useState(false);

  const {
    setError,
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FormInputs>({
    mode: "onBlur",
    resolver: zodResolver(schema),
  });

  const username = watch("username") ?? "";
  const email = watch("email") ?? "";
  const password = watch("password") ?? "";

  const usernameAvailability = useAvailability({
    checkFn: checkUsernameAvailability,
    enabled: !errors.username && username.length >= 2,
    value: username,
  });
  const emailAvailability = useAvailability({
    checkFn: checkEmailAvailability,
    enabled: !errors.email && /.+@.+\..+/.test(email),
    value: email,
  });

  const registerAccountMutation = useMutation({
    mutationFn: registerAccount,
    onSuccess: () => {
      enqueueSnackbar(i18n.registration.registrationSuccessful, {
        variant: "success",
      });
      navigateTo("/login");
    },
    onError: (error: unknown) => {
      const invalidParams = getRegistrationInvalidParams(error);

      if (invalidParams.email) {
        setError("email", {
          type: "custom",
          message: invalidParams.email,
        });
      }
      if (invalidParams.username) {
        setError("username", {
          type: "custom",
          message: invalidParams.username,
        });
      }
      if (!invalidParams.email && !invalidParams.username) {
        enqueueSnackbar(i18n.registration.loadingError, {
          variant: "error",
        });
      }
    },
  });

  const onSubmit = (data: FormInputs) => {
    const request: RegistrationRequest = {
      username: data.username,
      email: data.email,
      password: data.password,
    };
    registerAccountMutation.mutate(request);
  };

  const canSubmit =
    !registerAccountMutation.isPending &&
    usernameAvailability.status !== "checking" &&
    usernameAvailability.status !== "taken" &&
    emailAvailability.status !== "checking" &&
    emailAvailability.status !== "taken";

  return (
    <Grid container sx={{ minHeight: "calc(100vh - 55px)" }}>
      <Grid size={{ xs: 12, md: 6 }}>
        <AuthVisualPane />
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
          sx={{ maxWidth: 480, width: "100%" }}
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
              Create your account
            </Typography>
            <Typography color="text.secondary" variant="body2">
              Free, takes less than a minute.
            </Typography>
          </Box>

          <Stack spacing={2}>
            <TextField
              autoComplete="username"
              autoFocus
              error={!!errors.username}
              fullWidth
              helperText={errors.username ? errors.username?.message : " "}
              label="Username"
              slotProps={{
                input: {
                  endAdornment: (
                    <AvailabilityAdornment
                      status={usernameAvailability.status}
                      takenLabel="Taken"
                    />
                  ),
                },
              }}
              type="text"
              {...register("username")}
              sx={authTextFieldSx}
            />

            <TextField
              autoComplete="email"
              error={!!errors.email}
              fullWidth
              helperText={errors.email ? errors.email?.message : " "}
              label="Email"
              slotProps={{
                input: {
                  endAdornment: (
                    <AvailabilityAdornment
                      status={emailAvailability.status}
                      takenLabel="In use"
                    />
                  ),
                },
              }}
              type="email"
              {...register("email")}
              sx={authTextFieldSx}
            />

            <Box>
              <Grid container spacing={1.5}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    autoComplete="new-password"
                    error={!!errors.password}
                    fullWidth
                    helperText={
                      errors.password ? errors.password?.message : " "
                    }
                    label="Password"
                    type={showPassword ? "text" : "password"}
                    {...register("password")}
                    sx={authTextFieldSx}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    autoComplete="new-password"
                    error={!!errors.confirmPassword}
                    fullWidth
                    helperText={
                      errors.confirmPassword
                        ? errors.confirmPassword?.message
                        : " "
                    }
                    label="Confirm password"
                    type={showPassword ? "text" : "password"}
                    {...register("confirmPassword")}
                    sx={authTextFieldSx}
                  />
                </Grid>
              </Grid>

              <PasswordRules password={password} />

              <FormControlLabel
                control={
                  <Checkbox
                    checked={showPassword}
                    onChange={() => setShowPassword((current) => !current)}
                    size="small"
                  />
                }
                label="Show passwords"
                sx={{
                  mt: 1,
                  "& .MuiFormControlLabel-label": {
                    color: "text.secondary",
                    fontSize: 13,
                  },
                }}
              />
            </Box>

            <Button
              disabled={!canSubmit}
              fullWidth
              sx={{ py: 1.5, textTransform: "none" }}
              type="submit"
              variant="contained"
            >
              Create account
            </Button>

            <SocialLoginButtons />

            <Typography
              sx={{
                color: "text.secondary",
                fontSize: 12,
                lineHeight: 1.6,
                textAlign: "center",
              }}
            >
              By creating an account, you agree to our{" "}
              <Link component={RouterLink} to="#" underline="hover">
                Terms
              </Link>{" "}
              and{" "}
              <Link component={RouterLink} to="#" underline="hover">
                Privacy Policy
              </Link>
              .
            </Typography>
          </Stack>
        </Box>
      </Grid>
    </Grid>
  );
};

type AvailabilityAdornmentProps = {
  status: "idle" | "checking" | "available" | "taken" | "error";
  takenLabel: string;
};

const AvailabilityAdornment = ({
  status,
  takenLabel,
}: AvailabilityAdornmentProps) => {
  if (status === "idle" || status === "error") {
    return null;
  }

  const color =
    status === "available"
      ? "success.main"
      : status === "taken"
        ? "error.main"
        : "text.secondary";
  const label =
    status === "available"
      ? "Available"
      : status === "taken"
        ? takenLabel
        : "Checking...";

  return (
    <InputAdornment position="end">
      <Stack direction="row" spacing={0.5} sx={{ alignItems: "center" }}>
        {status === "checking" && <CircularProgress size={14} />}
        {status === "available" && <CheckIcon sx={{ color, fontSize: 16 }} />}
        {status === "taken" && <CancelIcon sx={{ color, fontSize: 16 }} />}
        <Typography sx={{ color, fontSize: 12 }}>{label}</Typography>
      </Stack>
    </InputAdornment>
  );
};

const passwordRules = [
  {
    label: "At least 8 characters",
    met: (password: string) => password.length >= 8,
  },
  {
    label: "One uppercase letter",
    met: (password: string) => /[A-Z]/.test(password),
  },
  {
    label: "One lowercase letter",
    met: (password: string) => /[a-z]/.test(password),
  },
  { label: "One number", met: (password: string) => /[0-9]/.test(password) },
];

const PasswordRules = ({ password }: { password: string }) => (
  <Box
    sx={{
      display: "grid",
      gap: "4px 12px",
      gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" },
      mt: 1,
    }}
  >
    {passwordRules.map((rule) => {
      const met = rule.met(password);
      return (
        <Stack
          direction="row"
          key={rule.label}
          spacing={0.75}
          sx={{ alignItems: "center" }}
        >
          <Box
            sx={{
              alignItems: "center",
              bgcolor: met ? "success.main" : "transparent",
              border: "1px solid",
              borderColor: met ? "success.main" : "rgba(255,255,255,0.3)",
              borderRadius: "50%",
              color: "common.white",
              display: "inline-flex",
              height: 12,
              justifyContent: "center",
              width: 12,
            }}
          >
            {met && <CheckIcon sx={{ fontSize: 9 }} />}
          </Box>
          <Typography
            component="span"
            data-met={met ? "true" : "false"}
            sx={{
              color: met ? "success.main" : "rgba(255,255,255,0.5)",
              fontSize: 11,
            }}
          >
            {rule.label}
          </Typography>
        </Stack>
      );
    })}
  </Box>
);

export default RegistrationPage;
