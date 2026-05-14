import BookmarkBorderIcon from "@mui/icons-material/BookmarkBorder";
import ChatBubbleOutlineIcon from "@mui/icons-material/ChatBubbleOutlineOutlined";
import StarBorderIcon from "@mui/icons-material/StarBorder";
import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { ReactNode } from "react";

const posters = [
  { title: "HEREDITARY", bg: "#2a1a1a", fg: "#d4a574" },
  { title: "THE WITCH", bg: "#1a2a1f", fg: "#8fbc8f" },
  { title: "MIDSOMMAR", bg: "#2c1f2a", fg: "#d896c8" },
  { title: "IT FOLLOWS", bg: "#1a1a08", fg: "#fbbf24" },
  { title: "GET OUT", bg: "#0a1a14", fg: "#4ade80" },
  { title: "BABADOOK", bg: "#1a1f2e", fg: "#6fa3c7" },
  { title: "INTERSTELLAR", bg: "#0a0f1f", fg: "#a5b4cb" },
  { title: "DUNE", bg: "#1f1408", fg: "#fbbf24" },
  { title: "ARRIVAL", bg: "#0a1410", fg: "#86efac" },
];

const features: Array<{
  description: string;
  icon: ReactNode;
  label: string;
}> = [
  {
    description: "Save movies for movie night.",
    icon: <BookmarkBorderIcon fontSize="small" />,
    label: "Build your watchlist",
  },
  {
    description: "Build a personal taste profile.",
    icon: <StarBorderIcon fontSize="small" />,
    label: "Rate what you watch",
  },
  {
    description: "Comment on movies and discover other fans.",
    icon: <ChatBubbleOutlineIcon fontSize="small" />,
    label: "Join the conversation",
  },
];

const AuthVisualPane = () => (
  <Box
    sx={{
      background: "linear-gradient(135deg, #0a1a2e 0%, #16243a 100%)",
      display: { xs: "none", md: "flex" },
      flexDirection: "column",
      justifyContent: "space-between",
      overflow: "hidden",
      p: 6,
      position: "relative",
    }}
  >
    <Box
      aria-hidden
      sx={{
        display: "grid",
        gap: 1,
        gridTemplateColumns: "repeat(3, 1fr)",
        inset: 0,
        opacity: 0.15,
        p: 1.5,
        position: "absolute",
        transform: "rotate(-8deg) scale(1.4)",
        transformOrigin: "center",
      }}
    >
      {posters.map((poster) => (
        <Box
          key={poster.title}
          sx={{
            alignItems: "flex-end",
            aspectRatio: "2 / 3",
            bgcolor: poster.bg,
            borderRadius: 0.5,
            color: poster.fg,
            display: "flex",
            fontSize: 9,
            fontWeight: 500,
            justifyContent: "center",
            letterSpacing: 1,
            pb: 0.75,
            textAlign: "center",
          }}
        >
          {poster.title}
        </Box>
      ))}
    </Box>

    <Box sx={{ position: "relative", zIndex: 1 }}>
      <Typography
        component="h2"
        sx={{ color: "common.white", fontSize: 28, fontWeight: 500, mb: 1.5 }}
      >
        Track your taste in cinema.
      </Typography>
      <Typography
        sx={{
          color: "rgba(255,255,255,0.65)",
          fontSize: 14,
          lineHeight: 1.6,
          maxWidth: 340,
        }}
      >
        Join thousands of movie lovers cataloguing, rating, and discussing the
        films that matter to them.
      </Typography>
    </Box>

    <Stack component="ul" spacing={1.75} sx={{ m: 0, p: 0, zIndex: 1 }}>
      {features.map((feature) => (
        <Stack
          component="li"
          direction="row"
          key={feature.label}
          spacing={1.5}
          sx={{ color: "rgba(255,255,255,0.85)", listStyle: "none" }}
        >
          <Box
            sx={{
              alignItems: "center",
              bgcolor: "rgba(77,171,247,0.12)",
              borderRadius: 1,
              color: "#4dabf7",
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
              sx={{ color: "common.white", fontSize: 13, fontWeight: 500 }}
            >
              {feature.label}
            </Typography>
            <Typography sx={{ color: "rgba(255,255,255,0.55)", fontSize: 12 }}>
              {feature.description}
            </Typography>
          </Box>
        </Stack>
      ))}
    </Stack>
  </Box>
);

export default AuthVisualPane;
