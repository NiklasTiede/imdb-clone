import DeleteIcon from "@mui/icons-material/DeleteSharp";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { AccountProfile } from "../../../client/movies/generator-output";
import { ProfileAvatar } from "../../../shared/media";
import ProfileImageUpload from "./ProfileImageUpload";

type ProfileHeaderCardProps = {
  accountProfile: AccountProfile;
  isRemovingPhoto: boolean;
  onRemovePhoto: () => void;
};

const ProfileHeaderCard = ({
  accountProfile,
  isRemovingPhoto,
  onRemovePhoto,
}: ProfileHeaderCardProps) => {
  const displayName = [accountProfile.firstName, accountProfile.lastName]
    .filter(Boolean)
    .join(" ");
  const username = accountProfile.username ? `@${accountProfile.username}` : "";
  const stats = [
    `${accountProfile.ratingsCount ?? 0} ratings`,
    `${accountProfile.watchlistCount ?? 0} watchlist`,
    `${accountProfile.commentsCount ?? 0} comments`,
  ].join(" · ");

  return (
    <Card
      sx={{
        backgroundColor: "background.paper",
        border: "1px solid",
        borderColor: "divider",
        borderRadius: 1,
        p: 2,
      }}
    >
      <Stack
        direction={{ xs: "column", sm: "row" }}
        spacing={2}
        sx={{ alignItems: { xs: "flex-start", sm: "center" } }}
      >
        <ProfileAvatar
          imageUrlToken={accountProfile.imageUrlToken}
          sx={{ height: 72, width: 72 }}
        />
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography sx={{ fontSize: 18, fontWeight: 600 }}>
            {displayName || username || "Account"}
          </Typography>
          <Typography sx={{ color: "text.secondary", fontSize: 13 }}>
            {[username, stats].filter(Boolean).join(" · ")}
          </Typography>
        </Box>
        <Stack direction="row" spacing={1} sx={{ flexWrap: "wrap" }}>
          <ProfileImageUpload buttonVariant="outlined" />
          <Button
            color="inherit"
            disabled={!accountProfile.imageUrlToken || isRemovingPhoto}
            onClick={onRemovePhoto}
            startIcon={<DeleteIcon />}
            variant="outlined"
          >
            Remove
          </Button>
        </Stack>
      </Stack>
    </Card>
  );
};

export default ProfileHeaderCard;
