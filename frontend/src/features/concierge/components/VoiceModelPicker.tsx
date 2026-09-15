import { useState } from "react";
import { Button, Menu, MenuItem, Tooltip } from "@mui/material";
import ExpandMoreRoundedIcon from "@mui/icons-material/ExpandMoreRounded";
import {
  useVoiceModels,
  voiceModelLabels,
  type VoiceModel,
} from "../api/voiceModels";

export function VoiceModelPicker({
  model,
  onChange,
  active,
}: {
  model: VoiceModel;
  onChange: (model: VoiceModel) => void;
  active: boolean;
}) {
  const { data: models = [] } = useVoiceModels();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  if (!models.length || (models.length === 1 && models.includes(model)))
    return null;
  return (
    <>
      <Tooltip
        title={
          active
            ? "End this conversation to change models"
            : "Choose your voice model"
        }
      >
        <span style={{ pointerEvents: "auto" }}>
          <Button
            aria-label={`Voice model: ${voiceModelLabels[model]}`}
            aria-haspopup="menu"
            aria-expanded={!!anchor}
            disabled={active}
            onClick={(event) => setAnchor(event.currentTarget)}
            endIcon={<ExpandMoreRoundedIcon />}
            sx={{
              minHeight: 44,
              px: 1.5,
              color: "text.secondary",
              fontSize: 12,
            }}
          >
            {voiceModelLabels[model]}
          </Button>
        </span>
      </Tooltip>
      <Menu
        anchorEl={anchor}
        open={!!anchor && !active}
        onClose={() => setAnchor(null)}
      >
        {models.map((option) => (
          <MenuItem
            key={option}
            selected={model === option}
            onClick={() => {
              onChange(option);
              setAnchor(null);
            }}
          >
            {voiceModelLabels[option]}
          </MenuItem>
        ))}
      </Menu>
    </>
  );
}
