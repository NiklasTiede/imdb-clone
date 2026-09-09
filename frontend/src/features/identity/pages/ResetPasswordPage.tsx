import {
  Alert,
  Box,
  Button,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useSearchParams } from "react-router";
import AuthPageFrame from "../components/AuthPageFrame";
import { authTextFieldSx } from "../components/authFormStyles";
import {
  requestPasswordReset,
  saveResetPassword,
} from "../api/recoveryMutations";
import { passwordRules } from "../model/registrationValidation";

const ResetPasswordPage = () => {
  const [params] = useSearchParams();
  const token = params.get("token");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);
  const reset = useMutation({ mutationFn: requestPasswordReset });
  const save = useMutation({ mutationFn: saveResetPassword });
  const mutation = token ? save : reset;

  return (
    <AuthPageFrame variant="login">
      <Box
        component="form"
        onSubmit={(event) => {
          event.preventDefault();
          setValidationError(null);
          if (token) {
            if (!passwordRules.every((rule) => rule.met(password))) {
              setValidationError(
                "Use 8–30 characters with uppercase, lowercase, a number and a special character (#?!@$%^&*-).",
              );
              return;
            }
            if (password !== confirmation) {
              setValidationError("Passwords must match.");
              return;
            }
            save.mutate(
              { token, newPassword: password },
              {
                onSuccess: () => {
                  setPassword("");
                  setConfirmation("");
                },
              },
            );
          } else {
            reset.mutate(email);
          }
        }}
      >
        <Stack spacing={2}>
          <Typography component="h1" variant="h5">
            {token ? "Choose a new password" : "Reset password"}
          </Typography>
          {mutation.isSuccess ? (
            <>
              <Alert severity="success">
                {token
                  ? "Your password has been updated."
                  : "If an account exists, reset instructions will arrive by email."}
              </Alert>
              <Button component={Link} to="/login" variant="contained">
                Back to sign in
              </Button>
            </>
          ) : (
            <>
              <Typography color="text.secondary" variant="body2">
                {token
                  ? "Enter and confirm your new password."
                  : "Enter your email address to request reset instructions."}
              </Typography>
              {token ? (
                <>
                  <TextField
                    required
                    label="New password"
                    type="password"
                    autoComplete="new-password"
                    fullWidth
                    sx={authTextFieldSx}
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                  />
                  <TextField
                    required
                    label="Confirm password"
                    type="password"
                    autoComplete="new-password"
                    fullWidth
                    sx={authTextFieldSx}
                    value={confirmation}
                    onChange={(event) => setConfirmation(event.target.value)}
                  />
                </>
              ) : (
                <TextField
                  required
                  label="Email"
                  type="email"
                  autoComplete="email"
                  fullWidth
                  sx={authTextFieldSx}
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              )}
              {validationError && (
                <Alert severity="error">{validationError}</Alert>
              )}
              {mutation.isError && (
                <Alert severity="error">
                  {token
                    ? "The password could not be updated. The link may have expired; request a new one."
                    : "We could not send reset instructions. Please try again."}
                </Alert>
              )}
              <Button
                type="submit"
                fullWidth
                variant="contained"
                disabled={mutation.isPending}
                sx={{ py: 1.5, textTransform: "none" }}
              >
                {mutation.isPending
                  ? "Please wait…"
                  : token
                    ? "Save password"
                    : "Send reset link"}
              </Button>
            </>
          )}
        </Stack>
      </Box>
    </AuthPageFrame>
  );
};

export default ResetPasswordPage;
