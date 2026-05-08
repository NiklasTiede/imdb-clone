import React, { useEffect } from "react";
import { Link, useNavigate } from "react-router";
import { LoginRequest } from "../../client/movies/generator-output";
import { useDispatch, useSelector } from "react-redux";
import { Dispatch } from "../../redux/store";
import {
  Button,
  Container,
  Grid,
  Paper,
  TextField,
  useTheme,
} from "@mui/material";
import { tokens } from "../../theme";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as zod from "zod";
import { i18n } from "../../i18n";
import { State as AuthState } from "../../redux/model/authentication";
import { isJwtNotExpired } from "../../utils/jwtHelper";

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

const Login = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigateTo = useNavigate();
  const dispatch = useDispatch<Dispatch>();

  const isAuthenticated = useSelector(
    (state: { authentication: AuthState }) =>
      state.authentication.isAuthenticated,
  );
  const isLoggedIn: boolean = isJwtNotExpired();

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
    dispatch.authentication.authenticateAccount(payload);
  };

  useEffect(() => {
    if (isLoggedIn) {
      navigateTo("/");
    }
  }, [isAuthenticated, isLoggedIn, navigateTo]);

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

export default Login;
