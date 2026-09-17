import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import ExploreOutlinedIcon from "@mui/icons-material/ExploreOutlined";
import StarBorderIcon from "@mui/icons-material/StarBorder";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useTheme } from "@mui/material/styles";
import type { ReactNode } from "react";

type AuthVisualPaneVariant = "login" | "signup";

const copyByVariant: Record<
  AuthVisualPaneVariant,
  {
    features: Array<{
      description: string;
      icon: ReactNode;
      label: string;
    }>;
    subtitle: string;
    title: string;
  }
> = {
  signup: {
    title: "Track your taste in cinema.",
    subtitle:
      "Build a personal catalog around the movies you want to watch and the ones you already love.",
    features: [
      {
        description: "Save movies for movie night.",
        icon: <BookmarkBorderIcon fontSize="small" />,
        label: "Build your watchlist",
      },
      {
        description: "Keep your scores in one place.",
        icon: <StarBorderIcon fontSize="small" />,
        label: "Rate what you watch",
      },
      {
        description: "Explore more of the catalog.",
        icon: <ExploreOutlinedIcon fontSize="small" />,
        label: "Discover new favorites",
      },
    ],
  },
  login: {
    title: "Welcome back.",
    subtitle:
      "Pick up where you left off - your watchlist and ratings are ready.",
    features: [
      {
        description: "The movies you've saved.",
        icon: <BookmarkBorderIcon fontSize="small" />,
        label: "Your watchlist",
      },
      {
        description: "The scores you've recorded.",
        icon: <StarBorderIcon fontSize="small" />,
        label: "Your ratings",
      },
      {
        description: "Find something worth watching next.",
        icon: <ExploreOutlinedIcon fontSize="small" />,
        label: "Continue exploring",
      },
    ],
  },
};

type AuthVisualPaneProps = {
  variant?: AuthVisualPaneVariant;
};

const AuthVisualPane = ({ variant = "signup" }: AuthVisualPaneProps) => {
  const copy = copyByVariant[variant];
  const { authBackdropSrc } = useTheme().brand;

  return (
    <Box
      data-testid="auth-visual-pane"
      sx={{
        alignItems: "center",
        bgcolor: "surface.inset",
        display: { xs: "none", md: "flex" },
        height: "100%",
        overflow: "hidden",
        px: { md: 6, lg: 8 },
        py: 6,
        position: "relative",
      }}
    >
      <Box
        alt=""
        aria-hidden="true"
        component="img"
        src={authBackdropSrc}
        sx={{
          height: "100%",
          inset: 0,
          objectFit: "cover",
          objectPosition: "center",
          position: "absolute",
          width: "100%",
        }}
      />
      <Box
        aria-hidden="true"
        sx={{
          background: (theme) =>
            `linear-gradient(90deg, ${theme.alpha(theme.palette.scrim, 0.92)} 0%, ${theme.alpha(theme.palette.scrim, 0.78)} 58%, ${theme.alpha(theme.palette.scrim, 0.48)} 100%)`,
          inset: 0,
          position: "absolute",
        }}
      />

      <Stack
        spacing={4}
        sx={{ maxWidth: 420, position: "relative", width: "100%", zIndex: 1 }}
      >
        <Box>
          <Typography
            component="h2"
            sx={{
              color: "text.primary",
              fontSize: 30,
              fontWeight: 600,
              mb: 1.5,
            }}
          >
            {copy.title}
          </Typography>
          <Typography
            sx={{
              color: "text.secondary",
              fontSize: 14,
              lineHeight: 1.6,
              maxWidth: 340,
            }}
          >
            {copy.subtitle}
          </Typography>
        </Box>

        <Stack component="ul" spacing={2} sx={{ m: 0, p: 0 }}>
          {copy.features.map((feature) => (
            <Stack
              component="li"
              direction="row"
              key={feature.label}
              spacing={1.5}
              sx={{ color: "text.primary", listStyle: "none" }}
            >
              <Box
                sx={{
                  alignItems: "center",
                  bgcolor: (theme) =>
                    theme.alpha(theme.palette.accent.main, 0.12),
                  borderRadius: 1,
                  color: "accent.core",
                  display: "flex",
                  flexShrink: 0,
                  height: 28,
                  justifyContent: "center",
                  width: 28,
                }}
              >
                {feature.icon}
              </Box>
              <Box>
                <Typography
                  sx={{ color: "text.primary", fontSize: 13, fontWeight: 500 }}
                >
                  {feature.label}
                </Typography>
                <Typography sx={{ color: "text.secondary", fontSize: 12 }}>
                  {feature.description}
                </Typography>
              </Box>
            </Stack>
          ))}
        </Stack>
      </Stack>
    </Box>
  );
};

export default AuthVisualPane;
