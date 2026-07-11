import Box from "@mui/material/Box";
import Link from "@mui/material/Link";
import type { ReactNode } from "react";
import { Link as RouterLink } from "react-router";
import { movieColors } from "../../theme";
import BrandLogo from "./BrandLogo";

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
        bgcolor: `${movieColors.surface}f0`,
        borderBottom: "1px solid rgba(255,255,255,0.06)",
        display: "flex",
        gap: 2,
        justifyContent: "space-between",
        px: { xs: 2, sm: 3 },
        py: 1.75,
      }}
    >
      <BrandLogo compact sx={{ color: "common.white" }} />

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
          <Box component="span" sx={{ display: { xs: "none", sm: "inline" } }}>
            {altLabel}{" "}
          </Box>
          <Box
            component="span"
            sx={{ color: movieColors.info, fontWeight: 500 }}
          >
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
