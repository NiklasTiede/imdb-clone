import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { LoginRequest } from "../../client/movies/generator-output";
import { useDispatch } from "react-redux";
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

const Login = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigateTo = useNavigate();
  const dispatch = useDispatch<Dispatch>();

  const [usernameOrEmail, setUsernameOrEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleSubmit = (e: any) => {
    e.preventDefault();
    const payload = {
      usernameOrEmail: usernameOrEmail,
      password: password,
    } satisfies LoginRequest;
    dispatch.authentication.authenticateAccount(payload);
    navigateTo("/home");
  };

  return (
    <>
      <div>
        <Container maxWidth={"sm"}>
          <form onSubmit={handleSubmit}>
            <Grid
              container
              spacing={2}
              direction={"column"}
              justifyContent={"center"}
              style={{ minHeight: "80vh" }}
            >
              <Paper elevation={2} sx={{ padding: 5 }}>
                <Grid container direction={"column"} spacing={2}>
                  <Grid item>
                    <TextField
                      type={"text"}
                      fullWidth={true}
                      label={"Email / Username"}
                      variant={"outlined"}
                      onChange={(e) => setUsernameOrEmail(e.target.value)}
                      value={usernameOrEmail}
                    />
                  </Grid>
                  <Grid item>
                    <TextField
                      type={"password"}
                      fullWidth={true}
                      label={"Password"}
                      variant={"outlined"}
                      onChange={(e) => setPassword(e.target.value)}
                      value={password}
                    />
                  </Grid>
                  <Grid item textAlign="center">
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
                      Submit
                    </Button>
                  </Grid>
                  <Grid item textAlign="center">
                    Need an account?{" "}
                    <Link
                      to="/registration"
                      style={{
                        textDecoration: "none",
                        color: colors.redAccent[300],
                      }}
                    >
                      Registration
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
