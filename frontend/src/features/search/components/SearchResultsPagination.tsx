import Box from "@mui/material/Box";
import Pagination from "@mui/material/Pagination";

type SearchResultsPaginationProps = {
  onPageChange: (page: number) => void;
  page: number;
  pageCount?: number;
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
            borderColor: "rgba(255,255,255,0.12)",
            color: "text.secondary",
          },
          "& .Mui-selected": {
            backgroundColor: "rgba(245,197,24,0.16)",
            color: "text.primary",
          },
        }}
        variant="outlined"
      />
    </Box>
  );
};

export default SearchResultsPagination;
