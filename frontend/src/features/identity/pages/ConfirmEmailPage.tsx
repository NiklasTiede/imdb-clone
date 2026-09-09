import { Alert, Button, Stack, Typography } from "@mui/material";
import { useMutation } from "@tanstack/react-query";
import { Link, useSearchParams } from "react-router";
import AuthPageFrame from "../components/AuthPageFrame";
import { confirmEmail } from "../api/recoveryMutations";

const ConfirmEmailPage = () => {
  const [params] = useSearchParams();
  const token = params.get("token");
  const confirmation = useMutation({ mutationFn: confirmEmail });

  return (
    <AuthPageFrame variant="login">
      <Stack spacing={2}>
        <Typography component="h1" variant="h5">
          Confirm your email
        </Typography>
        {confirmation.isSuccess ? (
          <>
            <Alert severity="success">
              Your email is confirmed. You can now sign in.
            </Alert>
            <Button component={Link} to="/login" variant="contained">
              Sign in
            </Button>
          </>
        ) : (
          <>
            <Typography color="text.secondary">
              Confirm your email address to activate your account.
            </Typography>
            {!token && (
              <Alert severity="error">
                This confirmation link is incomplete. Open the link from your
                email.
              </Alert>
            )}
            {confirmation.isError && (
              <Alert severity="error">
                We could not confirm this email. Check the link or try again.
              </Alert>
            )}
            <Button
              variant="contained"
              disabled={!token || confirmation.isPending}
              onClick={() => {
                if (token) confirmation.mutate(token);
              }}
            >
              {confirmation.isPending ? "Confirming…" : "Confirm email"}
            </Button>
          </>
        )}
      </Stack>
    </AuthPageFrame>
  );
};

export default ConfirmEmailPage;
