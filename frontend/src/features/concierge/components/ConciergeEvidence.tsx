import { Box, Stack, Typography } from "@mui/material";
import { toolLabels, type ChatTurn } from "../model/concierge";
import ConciergeMovieCard from "./ConciergeMovieCard";

/** Supporting lookups are distinct from what the assistant actually said. */
export const ConciergeEvidence = ({ turn }: { turn: ChatTurn }) => {
  if (!turn.movies.length && !turn.tools?.length) return null;
  return (
    <Box
      component="details"
      sx={{
        mt: 1.5,
        color: "text.secondary",
        fontSize: 12,
        "& > summary": { cursor: "pointer", py: 1, minHeight: 36 },
      }}
    >
      <Box component="summary">Tools &amp; results</Box>
      <Typography sx={{ fontSize: 11, lineHeight: 1.5, mb: 1.5 }}>
        Supporting lookups for this reply. Retrieved movies may include
        candidates the Concierge did not discuss.
      </Typography>
      {turn.tools && turn.tools.length > 0 && (
        <Stack component="ul" spacing={0.75} sx={{ pl: 2, my: 1.5 }}>
          {turn.tools.map((tool) => (
            <Box component="li" key={tool.callId}>
              {toolLabels[tool.tool]} ·{" "}
              {tool.status === "completed"
                ? "Completed"
                : tool.status === "failed"
                  ? "Failed"
                  : turn.final || turn.interrupted || turn.error
                    ? "No result received"
                    : "In progress"}
            </Box>
          ))}
        </Stack>
      )}
      {turn.movies.length > 0 && (
        <>
          <Typography sx={{ fontSize: 11, mb: 1 }}>
            Retrieved movies ({turn.movies.length})
          </Typography>
          <Stack spacing={1}>
            {turn.movies.map((movie) => (
              <ConciergeMovieCard key={movie.movieId} movie={movie} />
            ))}
          </Stack>
        </>
      )}
    </Box>
  );
};
