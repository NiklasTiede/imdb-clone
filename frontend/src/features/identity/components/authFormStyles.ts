import type { SxProps, Theme } from "@mui/material/styles";

export const authTextFieldSx: SxProps<Theme> = {
  "& .MuiOutlinedInput-root": {
    bgcolor: "surface.page",
    "& fieldset": { borderColor: "line.control" },
    "&:hover fieldset": { borderColor: "text.secondary" },
    "&.Mui-focused fieldset": { borderColor: "accent.main" },
    "&.Mui-error fieldset": { borderColor: "error.main" },
  },
};
