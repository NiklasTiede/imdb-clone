import { useDispatch, useSelector } from "react-redux";
import { Dispatch } from "../../redux/store";
import {
  Box,
  Avatar,
  Button,
  FormHelperText,
  Grid,
  Paper,
  Stack,
  TextField,
  Typography,
  useTheme,
} from "@mui/material";
import { tokens } from "../../theme";
import React, { useEffect, useState } from "react";
import { DatePicker, LocalizationProvider } from "@mui/x-date-pickers";
import { AdapterMoment } from "@mui/x-date-pickers/AdapterMoment";
import { State as AccountStatus } from "../../redux/model/account";
import { getUsername } from "../../utils/jwtHelper";
import moment from "moment";
import { i18n } from "../../i18n";
import UploadProfileImage from "./UploadProfileImage";

const AccountSettings = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const dispatch = useDispatch<Dispatch>();

  const username = getUsername();
  const accountProfile = useSelector(
    (state: { account: AccountStatus }) => state.account.accountProfile
  );

  // set-able
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phone, setPhone] = useState("");
  const [bio, setBio] = useState("");
  const [birthdayDate, setBirthdayDate] = useState("");

  const [characterCount, setCharacterCount] = useState(0);
  const maxBioLength = 300;

  useEffect(() => {
    if (username) {
      dispatch.account.getAccountProfileSettings(username);
    }
  }, [username]);

  useEffect(() => {
    setFirstName(accountProfile.firstName ?? "");
    setLastName(accountProfile.lastName ?? "");
    setPhone(accountProfile.phone ?? "");
    setBirthdayDate(accountProfile.birthday ?? "");
    setBio(accountProfile.bio ?? "");
    setCharacterCount(accountProfile.bio ? accountProfile.bio.length : 0);
  }, [accountProfile]);

  const handleBioChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.value.length <= maxBioLength) {
      setBio(event.target.value);
      setCharacterCount(event.target.value.length);
    }
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (username) {
      const payload = {
        username: username,
        accountRecord: {
          firstName: firstName ? firstName : null,
          lastName: lastName ? lastName : null,
          birthday: moment(birthdayDate).toISOString(),
          phone: phone,
          bio: bio,
        },
      };
      dispatch.account.updateAccountProfileSettings(payload);
    } else {
      console.log("Username is not present!");
    }
  };

  const [imageUrl, setImageUrl] = useState<string | undefined>(
    "http://192.168.178.49:9000/imdb-clone/profile-photos/1_size_500x500.jpg"
  );

  const handleImageUpload = (url: string) => {
    setImageUrl(url);
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
        <Box sx={{ display: "flex", alignItems: "center" }}>
          <Avatar src={imageUrl} sx={{ width: 100, height: 100 }} />
          <UploadProfileImage onUpload={handleImageUpload} />
        </Box>

        <form onSubmit={handleSubmit} noValidate>
          <Stack spacing={6}>
            <Grid>
              <Typography gutterBottom fontSize={14}>
                {i18n.accountSettings.username}
              </Typography>
              <TextField
                type={"email"}
                fullWidth
                variant={"outlined"}
                autoComplete="off"
                inputProps={{ spellCheck: "false" }}
                disabled
                value={accountProfile.username}
              />
            </Grid>
            <Grid>
              <Typography gutterBottom fontSize={14}>
                {i18n.accountSettings.email}
              </Typography>
              <TextField
                type={"email"}
                fullWidth
                variant={"outlined"}
                autoComplete="off"
                inputProps={{ spellCheck: "false" }}
                disabled
                value={accountProfile.email}
              />
            </Grid>
            <Grid>
              <Typography gutterBottom fontSize={14}>
                {i18n.accountSettings.firstName}
              </Typography>
              <TextField
                type={"text"}
                fullWidth
                inputProps={{ spellCheck: "false" }}
                variant={"outlined"}
                onChange={(e) => setFirstName(e.target.value)}
                value={firstName}
              />
            </Grid>
            <Grid>
              <Typography gutterBottom fontSize={14}>
                {i18n.accountSettings.lastName}
              </Typography>
              <TextField
                type={"text"}
                fullWidth
                inputProps={{ spellCheck: "false" }}
                variant={"outlined"}
                onChange={(e) => setLastName(e.target.value)}
                value={lastName}
              />
            </Grid>
            <Grid container direction={"column"}>
              <Typography gutterBottom>
                {i18n.accountSettings.birthday}
              </Typography>
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
              <Typography gutterBottom>{i18n.accountSettings.phone}</Typography>
              <TextField
                type={"text"}
                fullWidth
                variant={"outlined"}
                autoComplete={"false"}
                onChange={(e) => setPhone(e.target.value)}
                value={phone}
              />
            </Grid>
            <Grid>
              <Typography gutterBottom>{i18n.accountSettings.bio}</Typography>
              <TextField
                type={"text"}
                fullWidth
                variant={"outlined"}
                autoComplete={"false"}
                onChange={handleBioChange}
                value={bio}
                multiline
              />
              <FormHelperText
                sx={{
                  textAlign: "right",
                  marginRight: 0.5,
                  color: colors.primary[200],
                }}
              >
                {characterCount} / {maxBioLength}
              </FormHelperText>
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
              {i18n.accountSettings.submit}
            </Button>
          </Stack>
        </form>
      </Paper>
    </Box>
  );
};

export default AccountSettings;
