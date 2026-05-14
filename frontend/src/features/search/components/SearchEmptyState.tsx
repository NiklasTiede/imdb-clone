import SearchOffIcon from "@mui/icons-material/SearchOff";
import StatusState from "../../../shared/layout/StatusState";

const SearchEmptyState = () => (
  <StatusState icon={<SearchOffIcon />} title="No movies found">
    Try adjusting your search term or filters.
  </StatusState>
);

export default SearchEmptyState;
