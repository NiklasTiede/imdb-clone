import Box from "@mui/material/Box";
import Link from "@mui/material/Link";
import type { ReactNode } from "react";
import { Link as RouterLink } from "react-router";
import BrandLogo from "./BrandLogo";

type AuthLayoutProps = {
  altActionLabel?: string | undefined;
  altLabel?: string | undefined;
  altTo?: string | undefined;
  children: ReactNode;
};

const AuthLayout = ({
  altActionLabel,
  altLabel,
  altTo,
  children,
}: AuthLayoutProps) => (
  <Box
    sx={{
      bgcolor: "background.default",
      display: "grid",
      gridTemplateRows: "auto minmax(0, 1fr)",
      minHeight: "100dvh",
    }}
  >
    <Box
      component="header"
      sx={{
        alignItems: "center",
        backdropFilter: "blur(18px)",
        bgcolor: (theme) => theme.alpha(theme.palette.surface.card, 0.94),
        borderBottom: "1px solid",
        borderColor: "divider",
        display: "flex",
        gap: 2,
        justifyContent: "space-between",
        px: { xs: 2, sm: 3 },
        py: 1.75,
      }}
    >
      <BrandLogo compact sx={{ color: "text.primary" }} />

      {altTo && altLabel && altActionLabel && (
        <Link
          color="inherit"
          component={RouterLink}
          sx={{
            color: "text.secondary",
            fontSize: 13,
            textDecoration: "none",
            "&:hover": { color: "text.primary" },
          }}
          to={altTo}
        >
          <Box component="span" sx={{ display: { xs: "none", sm: "inline" } }}>
            {altLabel}{" "}
          </Box>
          <Box component="span" sx={{ color: "accent.main", fontWeight: 500 }}>
            {altActionLabel}
          </Box>
        </Link>
      )}
    </Box>
    <Box component="main" sx={{ minHeight: 0 }}>
      {children}
    </Box>
  </Box>
);

export default AuthLayout;
