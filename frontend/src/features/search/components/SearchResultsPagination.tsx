import Box from "@mui/material/Box";
import Pagination from "@mui/material/Pagination";
import { accentTint } from "../../../theme";

type SearchResultsPaginationProps = {
  onPageChange: (page: number) => void;
  page: number;
  pageCount?: number | undefined;
};

const SearchResultsPagination = ({
  onPageChange,
  page,
  pageCount = 0,
}: SearchResultsPaginationProps) => {
  if (pageCount <= 1) {
    return null;
  }

  return (
    <Box
      sx={{
        borderTop: "1px solid",
        borderColor: "divider",
        display: "flex",
        justifyContent: "center",
        pt: 2.5,
      }}
    >
      <Pagination
        count={pageCount}
        onChange={(_event, nextPage) => onPageChange(nextPage)}
        page={page + 1}
        shape="rounded"
        siblingCount={1}
        sx={{
          "& .MuiPaginationItem-root": {
            borderColor: "line.control",
            color: "text.secondary",
          },
          "& .Mui-selected": {
            backgroundColor: accentTint(0.16),
            color: "text.primary",
          },
        }}
        variant="outlined"
      />
    </Box>
  );
};

export default SearchResultsPagination;
