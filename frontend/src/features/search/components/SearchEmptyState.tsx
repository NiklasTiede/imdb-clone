import SearchOffIcon from "@mui/icons-material/SearchOff";
import Button from "@mui/material/Button";
import StatusState from "../../../shared/layout/StatusState";

type SearchEmptyStateProps = {
  onClearFilters?: (() => void) | undefined;
};

const SearchEmptyState = ({ onClearFilters }: SearchEmptyStateProps) => (
  <StatusState
    action={
      onClearFilters ? (
        <Button onClick={onClearFilters} variant="outlined">
          Clear filters
        </Button>
      ) : undefined
    }
    icon={<SearchOffIcon />}
    title="No movies found"
  >
    Try adjusting your search term or filters.
  </StatusState>
);

export default SearchEmptyState;
