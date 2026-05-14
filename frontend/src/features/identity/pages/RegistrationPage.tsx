import {
  Button,
  Container,
  FormControlLabel,
  Grid,
  Checkbox,
  Paper,
  TextField,
} from "@mui/material";
import React, { useState } from "react";
import { tokens } from "../../../theme";
import { useNavigate } from "react-router";
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
  const colors = tokens();
  const navigateTo = useNavigate();
  const { enqueueSnackbar } = useSnackbar();

  const [showPassword, setShowPassword] = useState(false);

  const {
    setError,
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormInputs>({
    mode: "onBlur",
    resolver: zodResolver(schema),
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
                      disabled={registerAccountMutation.isPending}
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
