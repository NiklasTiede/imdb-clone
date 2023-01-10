import {
  Button,
  Container,
  Grid,
  Paper,
  TextField,
  useTheme,
} from "@mui/material";
import React, { useState } from "react";
import { tokens } from "../../theme";
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { Dispatch } from "../../redux/store";
import { RegistrationRequest } from "../../client/movies/generator-output";

const Registration = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigateTo = useNavigate();
  const dispatch = useDispatch<Dispatch>();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleSubmit = (event: any) => {
    event.preventDefault();

    const payload: RegistrationRequest = {
      username: username,
      email: email,
      password: password,
    };
    console.log(payload);
    // dispatch.authentication.registerAccount(payload);
    // navigateTo("/login");
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
                      fullWidth
                      label={"Enter your Username"}
                      placeholder={"Username"}
                      variant={"outlined"}
                    />
                  </Grid>
                  <Grid item>
                    <TextField
                      type={"email"}
                      fullWidth
                      label={"Enter your Email"}
                      placeholder={"Email"}
                      variant={"outlined"}
                    />
                  </Grid>
                  <Grid item>
                    <TextField
                      type={"password"}
                      fullWidth
                      label={"Enter your Password"}
                      placeholder={"Password"}
                      variant={"outlined"}
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
                      onClick={() => console.log("you clicked me.")}
                    >
                      Register
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

export default Registration;
