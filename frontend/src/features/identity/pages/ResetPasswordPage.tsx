import { Box, Button, Stack, TextField, Typography } from "@mui/material";
import AuthPageFrame from "../components/AuthPageFrame";
import { authTextFieldSx } from "../components/authFormStyles";

const ResetPasswordPage = () => (
  <AuthPageFrame variant="login">
    <Box component="form">
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
  </AuthPageFrame>
);

export default ResetPasswordPage;
