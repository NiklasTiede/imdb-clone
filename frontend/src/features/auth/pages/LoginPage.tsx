import React, { useEffect } from "react";
import { Link, useNavigate } from "react-router";
import { LoginRequest } from "../../../client/movies/generator-output";
import { Button, Container, Grid, Paper, TextField } from "@mui/material";
import { tokens } from "../../../theme";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as zod from "zod";
import { i18n } from "../../../i18n";
import { useMutation } from "@tanstack/react-query";
import { authenticateAccount } from "../api/authMutations";
import { authSession } from "../../../shared/auth/authSession";
import { useAuthSession } from "../../../shared/auth/useAuthSession";
import { useSnackbar } from "notistack";
import { AxiosError } from "axios";

interface FormInputs {
  usernameOrEmail: string;
  password: string;
}

const schema = zod.object({
  usernameOrEmail: zod
    .string()
    .regex(new RegExp(i18n.regex.username.pattern), "Invalid Email / Username")
    .or(zod.string().regex(new RegExp(i18n.regex.email.pattern))),
  password: zod
    .string()
    .min(8, "Password must contain at least 8 characters")
    .max(30, "Password must contain at most 30 characters")
    .regex(new RegExp(i18n.regex.password.pattern), "Invalid Password"),
});

const LoginPage = () => {
  const colors = tokens();
  const navigateTo = useNavigate();
  const isLoggedIn = useAuthSession();
  const { enqueueSnackbar } = useSnackbar();
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
    <>
      <div>
        <Container maxWidth={"sm"}>
          <form onSubmit={handleSubmit(onSubmit)}>
            <Grid
              container
              spacing={2}
              sx={{
                flexDirection: "column",
                justifyContent: "center",
                minHeight: "80vh",
              }}
            >
              <Paper elevation={2} sx={{ padding: 5 }}>
                <Grid container spacing={2} sx={{ flexDirection: "column" }}>
                  <Grid>
                    <TextField
                      type={"text"}
                      label={"Email / Username"}
                      variant={"outlined"}
                      error={!!errors.usernameOrEmail}
                      helperText={
                        errors.usernameOrEmail
                          ? errors.usernameOrEmail?.message
                          : " "
                      }
                      fullWidth
                      {...register("usernameOrEmail")}
                    />
                  </Grid>
                  <Grid>
                    <TextField
                      type={"password"}
                      label={"Password"}
                      variant={"outlined"}
                      error={!!errors.password}
                      helperText={
                        errors.password ? errors.password?.message : " "
                      }
                      fullWidth
                      {...register("password")}
                    />
                  </Grid>
                  <Grid sx={{ textAlign: "center" }}>
                    <Button
                      sx={{
                        color: colors.primary[100],
                        backgroundColor: colors.blueAccent[700],
                        ":hover": {
                          color: colors.primary[100],
                          backgroundColor: colors.blueAccent[600],
                        },
                      }}
                      variant={"contained"}
                      type={"submit"}
                      fullWidth
                      disabled={authenticateAccountMutation.isPending}
                    >
                      {i18n.login.login}
                    </Button>
                  </Grid>
                  <Grid sx={{ textAlign: "center" }}>
                    Need an account?{" "}
                    <Link
                      to="/registration"
                      style={{
                        textDecoration: "none",
                        color: colors.redAccent[300],
                      }}
                    >
                      {i18n.login.registration}
                    </Link>
                  </Grid>
                </Grid>
              </Paper>
            </Grid>
          </form>
        </Container>
      </div>
    </>
  );
};

export default LoginPage;
