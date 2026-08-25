import AutoAwesomeRoundedIcon from "@mui/icons-material/AutoAwesomeRounded";
import { alpha } from "@mui/material/styles";
import { Box, Button, Stack, Typography } from "@mui/material";
import { movieColors } from "../../../theme";

const prompts = [
  {
    label: "See what the Concierge can do",
    message: "What can you do for me?",
    featured: true,
  },
  {
    label: "Something thoughtful under 2 hours",
    message: "Something thoughtful under 2 hours",
    featured: false,
  },
  {
    label: "Three upbeat picks for tonight",
    message: "Three upbeat picks for tonight",
    featured: false,
  },
  {
    label: "Find a tense science-fiction movie",
    message: "Find a tense science-fiction movie",
    featured: false,
  },
] as const;

const ConciergeEmptyState = ({
  onPrompt,
}: {
  onPrompt: (prompt: string) => void;
}) => (
  <Box
    sx={{
      display: "flex",
      flex: 1,
      flexDirection: "column",
      justifyContent: "center",
      minHeight: 420,
      px: 2.5,
      py: 4,
    }}
  >
    <Box
      sx={{
        alignItems: "center",
        bgcolor: alpha(movieColors.brand, 0.12),
        border: `1px solid ${alpha(movieColors.brand, 0.24)}`,
        borderRadius: "50%",
        display: "flex",
        height: 52,
        justifyContent: "center",
        mb: 2.5,
        width: 52,
      }}
    >
      <AutoAwesomeRoundedIcon sx={{ color: movieColors.brand, fontSize: 25 }} />
    </Box>
    <Typography
      component="h2"
      sx={{
        color: "text.primary",
        fontSize: 24,
        fontWeight: 750,
        letterSpacing: -0.7,
        mb: 1,
      }}
    >
      What fits tonight?
    </Typography>
    <Typography
      sx={{ color: "text.secondary", fontSize: 13, lineHeight: 1.65, mb: 3 }}
    >
      Describe a mood, a time limit, or a movie you already love. Every
      recommendation is grounded in this catalog.
    </Typography>
    <Stack spacing={1}>
      {prompts.map((prompt) => (
        <Button
          key={prompt.label}
          onClick={() => onPrompt(prompt.message)}
          variant="outlined"
          sx={{
            bgcolor: prompt.featured
              ? alpha(movieColors.brand, 0.08)
              : "transparent",
            borderColor: prompt.featured
              ? alpha(movieColors.brand, 0.38)
              : alpha("#ffffff", 0.11),
            color: prompt.featured
              ? movieColors.brand
              : "rgba(255,255,255,0.84)",
            fontSize: 11.5,
            fontWeight: prompt.featured ? 700 : 500,
            justifyContent: "flex-start",
            lineHeight: 1.4,
            px: 1.5,
            py: 1.2,
            textAlign: "left",
            textTransform: "none",
            "&:hover": {
              bgcolor: alpha(movieColors.brand, 0.07),
              borderColor: alpha(movieColors.brand, 0.38),
            },
          }}
        >
          {prompt.label}
        </Button>
      ))}
    </Stack>
  </Box>
);

export default ConciergeEmptyState;
