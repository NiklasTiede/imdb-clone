import { movieColors } from "../../../theme";

export const authTextFieldSx = {
  "& .MuiOutlinedInput-root": {
    bgcolor: movieColors.surface,
    "& fieldset": { borderColor: "rgba(255,255,255,0.12)" },
    "&:hover fieldset": { borderColor: "rgba(255,255,255,0.24)" },
    "&.Mui-focused fieldset": { borderColor: movieColors.brand },
    "&.Mui-error fieldset": { borderColor: "#f87171" },
  },
};
