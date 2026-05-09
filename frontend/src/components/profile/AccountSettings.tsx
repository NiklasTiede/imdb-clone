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
import { getUsername } from "../../utils/jwtHelper";
import moment, { type Moment } from "moment";
import { i18n } from "../../i18n";
import UploadProfileImage from "./UploadProfileImage";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  accountMutationKeys,
  accountQueries,
  updateAccountProfile,
} from "../../features/account";
import { useSnackbar } from "notistack";

const AccountSettings = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  const username = getUsername();
  const { data: accountProfile = emptyAccountProfile } = useQuery(
    accountQueries.currentProfile(),
  );
  const updateAccountProfileMutation = useMutation({
    mutationFn: updateAccountProfile,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: accountMutationKeys.currentProfile,
      });
      enqueueSnackbar(i18n.accountSettings.successfulUpdate, {
        variant: "info",
      });
    },
  });

  // set-able
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phone, setPhone] = useState("");
  const [bio, setBio] = useState("");
  const [birthdayDate, setBirthdayDate] = useState<Moment | null>(null);

  const [characterCount, setCharacterCount] = useState(0);
  const maxBioLength = 300;

  useEffect(() => {
    setFirstName(accountProfile.firstName ?? "");
    setLastName(accountProfile.lastName ?? "");
    setPhone(accountProfile.phone ?? "");
    setBirthdayDate(
      accountProfile.birthday ? moment(accountProfile.birthday) : null,
    );
    setBio(accountProfile.bio ?? "");
    setCharacterCount(accountProfile.bio ? accountProfile.bio.length : 0);
    if (accountProfile.imageUrlToken) {
      setImageUrl(
        `http://192.168.178.49:9000/imdb-clone/profile-photos/${accountProfile.imageUrlToken}_size_800x800.jpg`,
      );
    } else {
      setImageUrl(undefined);
    }
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
          firstName: firstName || undefined,
          lastName: lastName || undefined,
          birthday: birthdayDate
            ? birthdayDate.format("YYYY-MM-DD")
            : undefined,
          phone: phone,
          bio: bio,
        },
      };
      updateAccountProfileMutation.mutate(payload);
    } else {
      console.log("Username is not present!");
    }
  };

  const [imageUrl, setImageUrl] = useState<string | undefined>();

  return (
    <Box sx={{ p: 5 }}>
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
          <UploadProfileImage />
        </Box>

        <form onSubmit={handleSubmit} noValidate>
          <Stack spacing={6}>
            <Grid>
              <Typography gutterBottom sx={{ fontSize: 14 }}>
                {i18n.accountSettings.username}
              </Typography>
              <TextField
                type={"email"}
                fullWidth
                variant={"outlined"}
                autoComplete="off"
                slotProps={{ htmlInput: { spellCheck: false } }}
                disabled
                value={accountProfile.username}
              />
            </Grid>
            <Grid>
              <Typography gutterBottom sx={{ fontSize: 14 }}>
                {i18n.accountSettings.email}
              </Typography>
              <TextField
                type={"email"}
                fullWidth
                variant={"outlined"}
                autoComplete="off"
                slotProps={{ htmlInput: { spellCheck: false } }}
                disabled
                value={accountProfile.email}
              />
            </Grid>
            <Grid>
              <Typography gutterBottom sx={{ fontSize: 14 }}>
                {i18n.accountSettings.firstName}
              </Typography>
              <TextField
                type={"text"}
                fullWidth
                slotProps={{ htmlInput: { spellCheck: false } }}
                variant={"outlined"}
                onChange={(e) => setFirstName(e.target.value)}
                value={firstName}
              />
            </Grid>
            <Grid>
              <Typography gutterBottom sx={{ fontSize: 14 }}>
                {i18n.accountSettings.lastName}
              </Typography>
              <TextField
                type={"text"}
                fullWidth
                slotProps={{ htmlInput: { spellCheck: false } }}
                variant={"outlined"}
                onChange={(e) => setLastName(e.target.value)}
                value={lastName}
              />
            </Grid>
            <Grid container sx={{ flexDirection: "column" }}>
              <Typography gutterBottom>
                {i18n.accountSettings.birthday}
              </Typography>
              <LocalizationProvider dateAdapter={AdapterMoment}>
                <DatePicker
                  disableFuture
                  openTo="year"
                  views={["year", "month", "day"]}
                  value={birthdayDate}
                  onChange={(newValue) => {
                    setBirthdayDate(newValue);
                  }}
                  slotProps={{ textField: { variant: "outlined" } }}
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

const emptyAccountProfile = {
  bio: "",
  birthday: "",
  commentsCount: 0,
  email: "",
  firstName: "",
  imageUrlToken: "",
  lastName: "",
  phone: "",
  ratingsCount: 0,
  username: "",
  watchlistCount: 0,
};

export default AccountSettings;
