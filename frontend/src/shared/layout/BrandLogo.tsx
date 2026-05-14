import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import type { SxProps, Theme } from "@mui/material/styles";
import { Link as RouterLink } from "react-router";
import { i18n } from "../../i18n";

type BrandLogoProps = {
  compact?: boolean;
  sx?: SxProps<Theme>;
};

const BrandLogo = ({ compact = false, sx }: BrandLogoProps) => (
  <Box
    aria-label={i18n.general.appName}
    component={RouterLink}
    sx={{
      alignItems: "center",
      color: "inherit",
      display: "inline-flex",
      gap: 1,
      minWidth: 0,
      textDecoration: "none",
      ...sx,
    }}
    to="/"
  >
    <Box
      aria-hidden="true"
      sx={{
        alignItems: "center",
        bgcolor: "#f5c518",
        borderRadius: 1,
        boxShadow: "inset 0 -1px 0 rgba(0,0,0,0.24)",
        color: "#101010",
        display: "flex",
        flex: "0 0 auto",
        fontSize: compact ? 13 : 14,
        fontWeight: 900,
        height: compact ? 28 : 34,
        justifyContent: "center",
        width: compact ? 32 : 42,
      }}
    >
      IM
    </Box>
    <Box sx={{ display: "grid", minWidth: 0 }}>
      <Typography
        component="span"
        sx={{
          color: "inherit",
          fontSize: compact ? 14 : 15,
          fontWeight: 800,
          lineHeight: 1.1,
          whiteSpace: "nowrap",
        }}
      >
        {i18n.general.appName}
      </Typography>
      {!compact && (
        <Typography
          component="span"
          sx={{
            color: "rgba(255,255,255,0.62)",
            display: { xs: "none", sm: "block" },
            fontSize: 11,
            fontWeight: 600,
            lineHeight: 1.2,
            whiteSpace: "nowrap",
          }}
        >
          Movies, ratings, watchlists
        </Typography>
      )}
    </Box>
  </Box>
);

export default BrandLogo;
