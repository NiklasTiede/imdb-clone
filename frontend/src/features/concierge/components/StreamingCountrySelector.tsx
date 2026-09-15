import { Box, TextField, Typography } from "@mui/material";
import { memo } from "react";
import { streamingCountries } from "../model/streamingCountry";

// Audio levels rerender the voice UI frequently; the country menu stays independent.
export const StreamingCountrySelector = memo(function StreamingCountrySelector({
  country,
  onChange,
}: {
  country: string;
  onChange: (country: string) => void;
}) {
  return (
    <Box sx={{ px: 2, pt: 1.5, pb: 0.5 }}>
      <TextField
        select
        fullWidth
        size="small"
        label="Streaming in"
        value={country}
        onChange={(event) => onChange(event.target.value)}
        slotProps={{ select: { native: true } }}
        sx={{ "& .MuiInputBase-root": { fontSize: 12 } }}
      >
        {streamingCountries.map(({ code, name }) => (
          <option key={code} value={code}>
            {name}
          </option>
        ))}
      </TextField>
      <Typography sx={{ fontSize: 10, color: "text.secondary", mt: 0.75 }}>
        Availability data by JustWatch via TMDB
      </Typography>
    </Box>
  );
});
