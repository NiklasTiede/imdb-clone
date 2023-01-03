import { useDispatch } from "react-redux";
import { Dispatch } from "../../redux/store";
import {
  Button,
  Container,
  Grid,
  Paper,
  Stack,
  styled,
  TextField,
  Typography,
  useTheme,
} from "@mui/material";
import { tokens } from "../../theme";
import { useNavigate } from "react-router-dom";
import React, { useState } from "react";
import { AccountRecord } from "../../client/movies/generator-output";
import { DatePicker, LocalizationProvider } from "@mui/x-date-pickers";
import { AdapterMoment } from "@mui/x-date-pickers/AdapterMoment";
import Box from "@mui/material/Box";
import moment from "moment/moment";

const StyledContainer = styled(Container)(({ theme }) => ({
  sx: {
    padding: 5,
  },
  backgroundColor: theme.palette.mode === "dark" ? "#1A2027" : "#fff",
  padding: theme.spacing(1),
}));

const AccountSettings = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const navigateTo = useNavigate();
  const dispatch = useDispatch<Dispatch>();

  const [usernameOrEmail, setUsernameOrEmail] = useState("");
  const [password, setPassword] = useState("");
  const [birthdayDate, setBirthdayDate] = useState(moment("01/01/1990"));

  dispatch.account.getCurrentAccount();

  dispatch.account.getAccountProfileSettings("superman");

  // as soon as opened: load names

  const handleSubmit = (e: any) => {
    e.preventDefault();

    console.log(usernameOrEmail);

    const payload: AccountRecord = {
      firstName: usernameOrEmail,
      lastName: password,
      phone: "",
      bio: "",
    };

    // @ts-ignore
    dispatch.account.updateAccountProfileSettings("superman", payload);
  };

  return (
    <Box padding={5}>
      <Paper
        elevation={24}
        sx={{
          padding: 5,
          backgroundColor: colors.primary["900"],
          maxWidth: 500,
          margin: "auto",
        }}
      >
        <form onSubmit={handleSubmit} noValidate>
          <Stack spacing={6}>
            <Grid>
              <Typography gutterBottom fontSize={14}>
                Full Name
              </Typography>
              <TextField
                type={"text"}
                fullWidth
                inputProps={{ spellCheck: "false" }}
                variant={"outlined"}
                // onChange={(e) => setUsernameOrEmail(e.target.value)}
                value={"Niklas Tiede"}
              />
            </Grid>
            <Grid>
              <Typography gutterBottom fontSize={14}>
                Email Address
              </Typography>
              <TextField
                type={"email"}
                fullWidth
                variant={"outlined"}
                autoComplete="off"
                inputProps={{ spellCheck: "false" }}
                // onChange={(e) => setUsernameOrEmail(e.target.value)}
                value={"niklastiede2@gmail.com"}
              />
            </Grid>
            <Grid container direction={"column"}>
              <Typography gutterBottom>Birthday</Typography>
              <LocalizationProvider dateAdapter={AdapterMoment}>
                <DatePicker
                  disableFuture
                  openTo="year"
                  views={["year", "month", "day"]}
                  value={birthdayDate}
                  onChange={(newValue: any) => {
                    setBirthdayDate(newValue);
                  }}
                  renderInput={(params) => <TextField {...params} />}
                />
              </LocalizationProvider>
            </Grid>
            <Grid>
              <Typography gutterBottom>Biography</Typography>
              <TextField
                type={"text"}
                fullWidth
                variant={"outlined"}
                autoComplete={"false"}
                onChange={(e) => setUsernameOrEmail(e.target.value)}
                // value={"Hi, this is my profile!"}
                value={usernameOrEmail}
              />
            </Grid>
            <Button
              sx={{
                marginTop: 5,
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
              Update
            </Button>
          </Stack>
        </form>
      </Paper>
    </Box>
  );
};

export default AccountSettings;
