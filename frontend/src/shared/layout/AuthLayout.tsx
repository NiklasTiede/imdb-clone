import Box from "@mui/material/Box";
import Link from "@mui/material/Link";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { ReactNode } from "react";
import { Link as RouterLink } from "react-router";

type AuthLayoutProps = {
  altActionLabel?: string;
  altLabel?: string;
  altTo?: string;
  children: ReactNode;
};

const AuthLayout = ({
  altActionLabel,
  altLabel,
  altTo,
  children,
}: AuthLayoutProps) => (
  <Box sx={{ bgcolor: "background.default", minHeight: "100vh" }}>
    <Box
      component="header"
      sx={{
        alignItems: "center",
        bgcolor: "#16243a",
        borderBottom: "1px solid rgba(255,255,255,0.06)",
        display: "flex",
        justifyContent: "space-between",
        px: { xs: 2, sm: 3 },
        py: 1.75,
      }}
    >
      <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
        <Box
          sx={{
            alignItems: "center",
            background: "linear-gradient(135deg, #1976d2 0%, #4dabf7 100%)",
            borderRadius: 1,
            color: "common.white",
            display: "flex",
            fontSize: 14,
            fontWeight: 700,
            height: 26,
            justifyContent: "center",
            width: 26,
          }}
        >
          IM
        </Box>
        <Typography
          sx={{ color: "common.white", fontSize: 16, fontWeight: 500 }}
        >
          IMDb clone
        </Typography>
      </Stack>

      {altTo && altLabel && altActionLabel && (
        <Link
          color="inherit"
          component={RouterLink}
          sx={{
            color: "rgba(255,255,255,0.7)",
            fontSize: 13,
            textDecoration: "none",
            "&:hover": { color: "common.white" },
          }}
          to={altTo}
        >
          {altLabel}{" "}
          <Box component="span" sx={{ color: "#4dabf7", fontWeight: 500 }}>
            {altActionLabel}
          </Box>
        </Link>
      )}
    </Box>
    {children}
  </Box>
);

export default AuthLayout;
