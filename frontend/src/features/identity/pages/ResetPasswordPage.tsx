import { Box, Button, Grid, Stack, TextField, Typography } from "@mui/material";
import AuthVisualPane from "../components/AuthVisualPane";
import { authTextFieldSx } from "../components/authFormStyles";

const ResetPasswordPage = () => (
  <Grid container sx={{ minHeight: "calc(100vh - 55px)" }}>
    <Grid size={{ xs: 12, md: 6 }}>
      <AuthVisualPane variant="login" />
    </Grid>
    <Grid
      size={{ xs: 12, md: 6 }}
      sx={{
        alignItems: "center",
        display: "flex",
        justifyContent: "center",
        px: { xs: 2.5, sm: 5, md: 7 },
        py: { xs: 4, md: 6 },
      }}
    >
      <Box component="form" sx={{ maxWidth: 440, width: "100%" }}>
        <Box sx={{ mb: 3.5 }}>
          <Typography
            component="h1"
            sx={{
              color: "common.white",
              fontSize: { xs: 24, sm: 26 },
              fontWeight: 500,
              mb: 0.75,
            }}
          >
            Reset password
          </Typography>
          <Typography color="text.secondary" variant="body2">
            Enter your email address and we will send reset instructions if an
            account exists.
          </Typography>
        </Box>

        <Stack spacing={2}>
          <TextField
            autoComplete="email"
            autoFocus
            fullWidth
            helperText=" "
            label="Email"
            sx={authTextFieldSx}
            type="email"
          />
          <Button
            fullWidth
            sx={{ py: 1.5, textTransform: "none" }}
            variant="contained"
          >
            Send reset link
          </Button>
        </Stack>
      </Box>
    </Grid>
  </Grid>
);

export default ResetPasswordPage;
