import { Box, Button, Stack, Typography } from "@mui/material";
import { alpha } from "@mui/material/styles";
import AutoAwesomeRounded from "@mui/icons-material/AutoAwesomeRounded";
import PlayCircleOutlineRounded from "@mui/icons-material/PlayCircleOutlineRounded";
import BookmarkBorderRounded from "@mui/icons-material/BookmarkBorderRounded";
import StarOutlineRounded from "@mui/icons-material/StarOutlineRounded";
import PublicRounded from "@mui/icons-material/PublicRounded";
import ExploreOutlined from "@mui/icons-material/ExploreOutlined";
import { movieColors } from "../../../theme";
import { streamingCountries } from "../model/streamingCountry";

const ConciergeEmptyState = ({
  onPrompt,
  signedIn = false,
  country = "CH",
  disabled = false,
}: {
  onPrompt: (prompt: string) => void;
  signedIn?: boolean;
  country?: string;
  disabled?: boolean;
}) => {
  const region =
    streamingCountries.find((item) => item.code === country)?.name ?? country;
  const prompts = [
    {
      title: "Discover your next film",
      text: signedIn
        ? "Recommend something based on my ratings."
        : "Recommend a movie for tonight.",
      icon: AutoAwesomeRounded,
      personal: signedIn,
    },
    {
      title: "Movies & trailers",
      text: "Show me the trailer for Forrest Gump.",
      icon: PlayCircleOutlineRounded,
      personal: false,
    },
    {
      title: "Your watchlist",
      text: "Show me my watchlist.",
      icon: BookmarkBorderRounded,
      personal: true,
    },
    {
      title: "Your ratings",
      text: "Which movies have I rated highest?",
      icon: StarOutlineRounded,
      personal: true,
    },
    {
      title: "Where to watch",
      text: `Where can I watch Forrest Gump in ${region}?`,
      icon: PublicRounded,
      personal: false,
    },
    {
      title: "Find your way",
      text: "Open my settings.",
      icon: ExploreOutlined,
      personal: true,
    },
  ];
  return (
    <Box sx={{ p: 2.5 }}>
      <Typography
        component="h2"
        sx={{
          color: "text.primary",
          fontSize: 24,
          fontWeight: 650,
          letterSpacing: -0.7,
          mb: 1,
        }}
      >
        What can I help you with?
      </Typography>
      <Typography
        sx={{ fontSize: 12, color: "text.secondary", lineHeight: 1.8, mb: 2 }}
      >
        Choose an example, or ask in your own words.
      </Typography>
      <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 1 }}>
        {prompts.map((item) => (
          <Button
            key={item.title}
            disabled={disabled}
            onClick={() => onPrompt(item.text)}
            sx={{
              p: 1.25,
              minWidth: 0,
              display: "flex",
              flexDirection: "column",
              alignItems: "flex-start",
              justifyContent: "flex-start",
              textAlign: "left",
              textTransform: "none",
              color: "text.primary",
              border: "1px solid",
              borderColor: "divider",
              borderRadius: 1,
              bgcolor: alpha(movieColors.info, 0.025),
              "&:hover": { bgcolor: alpha(movieColors.info, 0.08) },
            }}
          >
            <Stack
              component="span"
              direction="row"
              spacing={0.7}
              sx={{ alignItems: "center", mb: 0.8 }}
            >
              <item.icon sx={{ fontSize: 16, color: movieColors.info }} />
              <Typography
                component="span"
                sx={{ fontSize: 12, fontWeight: 750, lineHeight: 1.4 }}
              >
                {item.title}
              </Typography>
            </Stack>
            <Typography
              component="span"
              sx={{ fontSize: 11, color: "text.secondary", lineHeight: 1.7 }}
            >
              “{item.text}”
            </Typography>
            {!signedIn && item.personal && (
              <Typography
                component="span"
                sx={{ fontSize: 9, color: movieColors.brand, mt: 1 }}
              >
                Sign in required
              </Typography>
            )}
          </Button>
        ))}
      </Box>
    </Box>
  );
};
export default ConciergeEmptyState;
