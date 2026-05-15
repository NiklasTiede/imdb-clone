import Box from "@mui/material/Box";
import LinearProgress from "@mui/material/LinearProgress";
import type { SxProps, Theme } from "@mui/material/styles";

type SearchLoadingSlotProps = {
  loading: boolean;
};

export const searchLoadingSlotSx = {
  minHeight: 4,
} satisfies SxProps<Theme>;

const SearchLoadingSlot = ({ loading }: SearchLoadingSlotProps) => (
  <Box aria-hidden={!loading} sx={searchLoadingSlotSx}>
    {loading && <LinearProgress aria-label="Loading search results" />}
  </Box>
);

export default SearchLoadingSlot;
