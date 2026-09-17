import ClearIcon from "@mui/icons-material/Clear";
import SearchIcon from "@mui/icons-material/Search";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import InputBase from "@mui/material/InputBase";
import { styled } from "@mui/material/styles";
import type React from "react";
import { useEffect, useRef } from "react";

type MovieSearchInputProps = {
  onClear: () => void;
  onQueryChange: (query: string) => void;
  onSearch: (query: string) => void;
  query: string;
};

const Search = styled("div")(({ theme }) => ({
  position: "relative",
  alignItems: "center",
  border: `1px solid ${theme.palette.line.divider}`,
  borderRadius: theme.shape.borderRadius,
  backgroundColor: theme.palette.surface.card,
  transition: "border-color 150ms ease, box-shadow 150ms ease",
  display: "grid",
  gridTemplateColumns: "42px minmax(0, 1fr) auto",
  minHeight: 42,
  "&:hover": {
    borderColor: theme.palette.line.control,
  },
  "&:focus-within": {
    borderColor: theme.alpha(theme.palette.accent.main, 0.78),
    boxShadow: `0 0 0 3px ${theme.alpha(theme.palette.accent.main, 0.14)}`,
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
  color: theme.palette.text.secondary,
}));

const StyledInputBase = styled(InputBase)(({ theme }) => ({
  color: "inherit",
  "& .MuiInputBase-input": {
    fontSize: 14,
    padding: theme.spacing(1.1, 1, 1.1, 0),
    width: "100%",
    "&::placeholder": {
      color: theme.palette.text.secondary,
      opacity: 1,
    },
  },
}));

const isApplePlatform = () =>
  typeof navigator !== "undefined" &&
  /mac|iphone|ipad/i.test(navigator.platform || navigator.userAgent);

const MovieSearchInput = ({
  onClear,
  onQueryChange,
  onSearch,
  query,
}: MovieSearchInputProps) => {
  const inputRef = useRef<HTMLInputElement>(null);
  const shortcutLabel = isApplePlatform() ? "⌘K" : "Ctrl K";

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
      event.preventDefault();
      onSearch(event.currentTarget.value);
      event.currentTarget.blur();
    }
  };

  return (
    <Search>
      <SearchIconWrapper>
        <SearchIcon />
      </SearchIconWrapper>
      <StyledInputBase
        placeholder="Search a title or describe a movie"
        inputProps={{ "aria-label": "search movies" }}
        inputRef={inputRef}
        value={query}
        onChange={(event) => onQueryChange(event.target.value)}
        onKeyDown={handleSearch}
      />
      <Box
        aria-hidden="true"
        sx={{
          border: "1px solid",
          borderColor: "divider",
          borderRadius: 1,
          color: "text.secondary",
          display: { xs: "none", sm: query.length > 0 ? "none" : "block" },
          fontSize: 11,
          fontWeight: 700,
          mr: 1,
          px: 0.75,
          py: 0.25,
        }}
      >
        {shortcutLabel}
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
