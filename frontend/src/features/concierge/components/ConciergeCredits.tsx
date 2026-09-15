import { Box, Link, Typography } from "@mui/material";

export const ConciergeCredits = () => (
  <Box
    component="details"
    sx={{ mt: 1, color: "text.secondary", fontSize: 11 }}
  >
    <Box component="summary" sx={{ cursor: "pointer", py: 0.5 }}>
      Data sources & credits
    </Box>
    <Box sx={{ pt: 1, pb: 0.5 }}>
      <Link
        href="https://www.themoviedb.org"
        target="_blank"
        rel="noopener noreferrer"
      >
        <Box
          component="img"
          src="/credits/tmdb.svg"
          alt="The Movie Database (TMDB)"
          sx={{ width: 76, height: "auto", display: "block", mb: 1 }}
        />
      </Link>
      <Typography sx={{ fontSize: 11, lineHeight: 1.5 }}>
        This product uses the TMDB API but is not endorsed or certified by TMDB.
      </Typography>
      <Typography sx={{ fontSize: 11, lineHeight: 1.5, mt: 0.5 }}>
        Streaming availability is provided by{" "}
        <Link
          href="https://www.justwatch.com"
          target="_blank"
          rel="noopener noreferrer"
        >
          JustWatch
        </Link>{" "}
        via TMDB. Offers vary by country and can change; check the provider
        before subscribing or paying.
      </Typography>
      <Typography sx={{ fontSize: 11, lineHeight: 1.5, mt: 0.5 }}>
        Extra cast, crew and production facts come from TMDB when available.
        Catalog details and your ratings remain separate.
      </Typography>
    </Box>
  </Box>
);
