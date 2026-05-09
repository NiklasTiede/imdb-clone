import {
  Button,
  Container,
  FormControlLabel,
  Grid,
  Checkbox,
  Paper,
  TextField,
  useTheme,
} from "@mui/material";
import React, { useEffect, useState } from "react";
import { tokens } from "../../../theme";
import { useNavigate } from "react-router";
import { useDispatch, useSelector } from "react-redux";
import { Dispatch } from "../../../redux/store";
import { i18n } from "../../../i18n";
import { useForm, UseFormSetError } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as zod from "zod";
import { RegistrationRequest } from "../../../client/movies/generator-output";
import { State as AuthState } from "../../../redux/model/authentication";

export interface FormInputs {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface RegisterRequest {
  payload: RegistrationRequest;
  error: UseFormSetError<FormInputs>;
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
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigateTo = useNavigate();
  const dispatch = useDispatch<Dispatch>();

  const [showPassword, setShowPassword] = useState(false);

  const registeredSuccessfully = useSelector(
    (state: { authentication: AuthState }) =>
      state.authentication.registrationCompleted,
  );

  const {
    setError,
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormInputs>({
    mode: "onBlur",
    resolver: zodResolver(schema),
  });

  const onSubmit = (data: FormInputs) => {
    const request: RegisterRequest = {
      payload: {
        username: data.username,
        email: data.email,
        password: data.password,
      },
      error: setError,
    };
    dispatch.authentication.registerAccount(request);
  };

  useEffect(() => {
    if (registeredSuccessfully) {
      navigateTo("/login");
      dispatch.authentication.setRegistrationCompleted(false);
    }
  }, [dispatch.authentication, navigateTo, registeredSuccessfully]);

  return (
    <>
      <div>
        <Container maxWidth={"sm"}>
          <form onSubmit={handleSubmit(onSubmit)}>
            <Grid
              container
              spacing={4}
              sx={{
                flexDirection: "column",
                justifyContent: "center",
                minHeight: "80vh",
              }}
            >
              <Paper elevation={2} sx={{ padding: 4 }}>
                <Grid container spacing={1} sx={{ flexDirection: "column" }}>
                  <Grid>
                    <TextField
                      label={"Username"}
                      type={"text"}
                      variant={"outlined"}
                      error={!!errors.username}
                      helperText={
                        errors.username ? errors.username?.message : " "
                      }
                      fullWidth
                      {...register("username")}
                    />
                  </Grid>
                  <Grid>
                    <TextField
                      label={"Email"}
                      type={"email"}
                      variant={"outlined"}
                      error={!!errors.email}
                      helperText={errors.email ? errors.email?.message : " "}
                      fullWidth
                      {...register("email")}
                    />
                  </Grid>
                  <Grid>
                    <Grid container spacing={3}>
                      <Grid size={{ xs: 5 }}>
                        <TextField
                          label={"Password"}
                          type={showPassword ? "text" : "password"}
                          variant={"outlined"}
                          error={!!errors.password}
                          helperText={
                            errors.password ? errors.password?.message : " "
                          }
                          fullWidth
                          {...register("password")}
                        />
                      </Grid>
                      <Grid size={{ xs: 5 }}>
                        <TextField
                          label={"Confirm Password"}
                          type={showPassword ? "text" : "password"}
                          variant={"outlined"}
                          error={!!errors.confirmPassword}
                          helperText={
                            errors.confirmPassword
                              ? errors.confirmPassword?.message
                              : " "
                          }
                          fullWidth
                          {...register("confirmPassword")}
                        />
                      </Grid>
                      <Grid
                        size={{ xs: 2 }}
                        sx={{ marginTop: 1, marginLeft: -1 }}
                      >
                        <FormControlLabel
                          control={
                            <Checkbox
                              onClick={() => setShowPassword(!showPassword)}
                            />
                          }
                          label="Show"
                          labelPlacement="end"
                        />
                      </Grid>
                    </Grid>
                  </Grid>
                  <Grid>
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
                      {i18n.registration.register}
                    </Button>
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

export default RegistrationPage;
