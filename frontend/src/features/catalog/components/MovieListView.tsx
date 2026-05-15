import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { createContext, useContext, type ReactNode } from "react";

export type MovieListColumns = {
  genre?: string;
  primaryRating: string;
  runtime?: string;
  secondaryRating?: string;
  timestamp?: string;
};

type MovieListLayout = {
  columns: MovieListColumns;
  hasRowActions: boolean;
  gridTemplateColumns: {
    md: string;
    xs: string;
  };
  rowGridTemplateColumns: {
    md: string;
    xs: string;
  };
};

type MovieListViewProps = {
  ariaLabel: string;
  children: ReactNode;
  columns: MovieListColumns;
  hasRowActions?: boolean;
};

const defaultLayout: MovieListLayout = {
  columns: {
    genre: "Genre",
    primaryRating: "Rating",
    runtime: "Runtime",
  },
  hasRowActions: false,
  gridTemplateColumns: {
    xs: "44px minmax(0, 1fr) auto",
    md: "52px minmax(0, 1fr) 160px 64px 86px",
  },
  rowGridTemplateColumns: {
    xs: "44px minmax(0, 1fr) auto",
    md: "52px minmax(0, 1fr) 160px 64px 86px",
  },
};

const MovieListLayoutContext = createContext<MovieListLayout>(defaultLayout);

export const useMovieListLayout = () => useContext(MovieListLayoutContext);

const createGridTemplate = (
  columns: MovieListColumns,
  hasRowActions: boolean,
) => {
  const desktopColumns = [
    "52px",
    "minmax(0, 1fr)",
    columns.genre ? "160px" : null,
    columns.runtime ? "64px" : null,
    "86px",
    columns.secondaryRating ? "72px" : null,
    columns.timestamp ? "96px" : null,
  ].filter(Boolean);

  const mobileColumns = [
    "44px",
    "minmax(0, 1fr)",
    "auto",
    columns.secondaryRating ? "auto" : null,
  ].filter(Boolean);

  return {
    content: {
      xs: mobileColumns.join(" "),
      md: desktopColumns.join(" "),
    },
    row: {
      xs: [...mobileColumns, hasRowActions ? "32px" : null]
        .filter(Boolean)
        .join(" "),
      md: [...desktopColumns, hasRowActions ? "32px" : null]
        .filter(Boolean)
        .join(" "),
    },
  };
};

const HeaderCell = ({
  align = "left",
  children,
  hideOnMobile = false,
}: {
  align?: "left" | "right";
  children?: ReactNode;
  hideOnMobile?: boolean;
}) => (
  <Typography
    component="span"
    sx={{
      color: "rgba(255,255,255,0.48)",
      display: hideOnMobile ? { xs: "none", md: "block" } : "block",
      fontSize: 11,
      fontWeight: 700,
      lineHeight: 1,
      overflow: "hidden",
      textAlign: align,
      textOverflow: "ellipsis",
      textTransform: "uppercase",
      whiteSpace: "nowrap",
    }}
  >
    {children}
  </Typography>
);

const MovieListView = ({
  ariaLabel,
  children,
  columns,
  hasRowActions = false,
}: MovieListViewProps) => {
  const gridTemplates = createGridTemplate(columns, hasRowActions);
  const layout = {
    columns,
    gridTemplateColumns: gridTemplates.content,
    hasRowActions,
    rowGridTemplateColumns: gridTemplates.row,
  };

  return (
    <MovieListLayoutContext.Provider value={layout}>
      <Box
        sx={{
          borderTop: "1px solid",
          borderColor: "divider",
          pt: 1,
        }}
      >
        <Box
          aria-hidden
          sx={{
            alignItems: "center",
            display: "grid",
            gap: { xs: 1.25, md: 1.75 },
            gridTemplateColumns: gridTemplates.row,
            px: { xs: 1.25, sm: 1.75 },
            py: 1,
          }}
        >
          <span />
          <HeaderCell>Title</HeaderCell>
          {columns.genre && (
            <HeaderCell hideOnMobile>{columns.genre}</HeaderCell>
          )}
          {columns.runtime && (
            <HeaderCell align="right" hideOnMobile>
              {columns.runtime}
            </HeaderCell>
          )}
          <HeaderCell align="right">{columns.primaryRating}</HeaderCell>
          {columns.secondaryRating && (
            <HeaderCell align="right">{columns.secondaryRating}</HeaderCell>
          )}
          {columns.timestamp && (
            <HeaderCell align="right" hideOnMobile>
              {columns.timestamp}
            </HeaderCell>
          )}
          {hasRowActions && <span />}
        </Box>

        <Stack
          aria-label={ariaLabel}
          component="ul"
          role="list"
          spacing={0.25}
          sx={{ m: 0, p: 0 }}
        >
          {children}
        </Stack>
      </Box>
    </MovieListLayoutContext.Provider>
  );
};

export default MovieListView;
