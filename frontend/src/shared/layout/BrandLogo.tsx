import Box from "@mui/material/Box";
import Typography from "@mui/material/Typography";
import { useTheme, type SxProps, type Theme } from "@mui/material/styles";
import { Link as RouterLink } from "react-router";
import { i18n } from "../../i18n";

type BrandLogoProps = {
  compact?: boolean;
  sx?: SxProps<Theme>;
};

const BrandLogo = ({ compact = false, sx }: BrandLogoProps) => {
  const theme = useTheme();
  const { tagline, wordmark } = theme.brand;

  return (
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
        alt=""
        aria-hidden="true"
        component="img"
        data-testid="brand-mark"
        src={theme.brand.markSrc}
        sx={{
          display: "block",
          flex: "0 0 auto",
          height: compact ? 32 : 42,
          width: compact ? 32 : 42,
        }}
      />
      <Box sx={{ display: "grid", justifyItems: tagline.align, minWidth: 0 }}>
        <Typography
          component="span"
          sx={{
            color: "inherit",
            fontFamily: wordmark.fontFamily,
            fontSize: compact ? wordmark.fontSize - 1 : wordmark.fontSize,
            fontWeight: wordmark.fontWeight,
            letterSpacing: wordmark.letterSpacing,
            lineHeight: 1.1,
            textTransform: wordmark.uppercase ? "uppercase" : "none",
            whiteSpace: "nowrap",
          }}
        >
          <AppName accentWord={wordmark.accentWord} />
        </Typography>
        {!compact && (
          <Typography
            component="span"
            sx={{
              color: tagline.color,
              display: { xs: "none", sm: "block" },
              fontSize: tagline.fontSize,
              fontWeight: 600,
              letterSpacing: tagline.letterSpacing,
              lineHeight: 1.2,
              whiteSpace: "nowrap",
            }}
          >
            {tagline.text}
          </Typography>
        )}
      </Box>
    </Box>
  );
};

/** The app name, with the theme's accent word (if any) in the accent colour. */
const AppName = ({ accentWord }: { accentWord?: string | undefined }) => {
  const name = i18n.general.appName;
  const index = accentWord ? name.indexOf(accentWord) : -1;
  if (!accentWord || index === -1) return <>{name}</>;

  return (
    <>
      {name.slice(0, index)}
      <Box component="span" sx={{ color: "accent.main" }}>
        {accentWord}
      </Box>
      {name.slice(index + accentWord.length)}
    </>
  );
};

export default BrandLogo;
