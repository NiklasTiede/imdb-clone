import ClearIcon from "@mui/icons-material/Clear";
import SearchIcon from "@mui/icons-material/Search";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import InputBase from "@mui/material/InputBase";
import { alpha, styled } from "@mui/material/styles";
import type React from "react";
import { useEffect, useRef } from "react";
import { movieColors } from "../../../theme";

type MovieSearchInputProps = {
  onClear: () => void;
  onQueryChange: (query: string) => void;
  onSearch: (query: string) => void;
  query: string;
};

const Search = styled("div")(({ theme }) => ({
  position: "relative",
  alignItems: "center",
  border: `1px solid ${alpha(theme.palette.common.white, 0.16)}`,
  borderRadius: 7,
  backgroundColor: alpha(theme.palette.common.white, 0.08),
  display: "grid",
  gridTemplateColumns: "42px minmax(0, 1fr) auto",
  minHeight: 42,
  "&:hover": {
    backgroundColor: alpha(theme.palette.common.white, 0.12),
  },
  "&:focus-within": {
    borderColor: alpha(movieColors.brand, 0.78),
    boxShadow: `0 0 0 3px ${alpha(movieColors.brand, 0.14)}`,
  },
  margin: 0,
  width: "100%",
  [theme.breakpoints.up("md")]: {
    maxWidth: 620,
  },
}));

const SearchIconWrapper = styled("div")(({ theme }) => ({
  padding: theme.spacing(0, 2),
  height: "100%",
  pointerEvents: "none",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  color: alpha(theme.palette.common.white, 0.72),
}));

const StyledInputBase = styled(InputBase)(({ theme }) => ({
  color: "inherit",
  "& .MuiInputBase-input": {
    fontSize: 14,
    padding: theme.spacing(1.1, 1, 1.1, 0),
    width: "100%",
    "&::placeholder": {
      color: alpha(theme.palette.common.white, 0.72),
      opacity: 1,
    },
  },
}));

const MovieSearchInput = ({
  onClear,
  onQueryChange,
  onSearch,
  query,
}: MovieSearchInputProps) => {
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const focusSearch = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        inputRef.current?.focus();
      }
    };

    document.addEventListener("keydown", focusSearch);
    return () => document.removeEventListener("keydown", focusSearch);
  }, []);

  const handleSearch = (
    event: React.KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    if (event.key === "Enter") {
      onSearch(event.currentTarget.value);
    }
  };

  return (
    <Search>
      <SearchIconWrapper>
        <SearchIcon />
      </SearchIconWrapper>
      <StyledInputBase
        placeholder="Search movies by title, actor, or year"
        inputProps={{ "aria-label": "search movies" }}
        inputRef={inputRef}
        value={query}
        onChange={(event) => onQueryChange(event.target.value)}
        onKeyDown={handleSearch}
      />
      <Box
        aria-hidden="true"
        sx={{
          border: "1px solid rgba(255,255,255,0.16)",
          borderRadius: 1,
          color: "rgba(255,255,255,0.58)",
          display: { xs: "none", sm: query.length > 0 ? "none" : "block" },
          fontSize: 11,
          fontWeight: 700,
          mr: 1,
          px: 0.75,
          py: 0.25,
        }}
      >
        Ctrl K
      </Box>
      {query.length > 0 && (
        <IconButton
          aria-label="clear"
          color="inherit"
          onClick={onClear}
          size="small"
          sx={{ mr: 0.75 }}
        >
          <ClearIcon />
        </IconButton>
      )}
    </Search>
  );
};

export default MovieSearchInput;
