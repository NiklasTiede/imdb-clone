import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Grid from "@mui/material/Grid";
import TextField from "@mui/material/TextField";
import type { AccountProfile } from "../model/accountProfile";
import { i18n } from "../../../i18n";
import {
  hasProfileSectionChanges,
  MAX_BIO_LENGTH,
  ProfileSectionForm,
} from "../utils/accountSettingsForm";
import SectionCard from "./SectionCard";

type ProfileSectionCardProps = {
  accountProfile: AccountProfile;
  form: ProfileSectionForm;
  isSaving: boolean;
  onFormChange: (form: ProfileSectionForm) => void;
  onSave: () => void;
};

const ProfileSectionCard = ({
  accountProfile,
  form,
  isSaving,
  onFormChange,
  onSave,
}: ProfileSectionCardProps) => {
  const isDirty = hasProfileSectionChanges(accountProfile, form);

  return (
    <SectionCard
      title="Profile"
      subtitle="Visible on your public profile"
      actions={
        <Button
          disabled={!isDirty || isSaving || form.bio.length > MAX_BIO_LENGTH}
          onClick={onSave}
          startIcon={isSaving ? <CircularProgress size={16} /> : undefined}
          variant="contained"
        >
          Save profile
        </Button>
      }
    >
      <Grid container spacing={2}>
        <Grid size={{ xs: 12 }}>
          <TextField
            disabled
            fullWidth
            label={i18n.accountSettings.username}
            value={accountProfile.username ?? ""}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label={i18n.accountSettings.firstName}
            onChange={(event) =>
              onFormChange({ ...form, firstName: event.target.value })
            }
            slotProps={{ htmlInput: { spellCheck: false } }}
            value={form.firstName}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label={i18n.accountSettings.lastName}
            onChange={(event) =>
              onFormChange({ ...form, lastName: event.target.value })
            }
            slotProps={{ htmlInput: { spellCheck: false } }}
            value={form.lastName}
          />
        </Grid>
        <Grid size={{ xs: 12 }}>
          <TextField
            fullWidth
            helperText={`${form.bio.length} / ${MAX_BIO_LENGTH}`}
            label={i18n.accountSettings.bio}
            multiline
            onChange={(event) =>
              onFormChange({
                ...form,
                bio: event.target.value.slice(0, MAX_BIO_LENGTH),
              })
            }
            rows={3}
            slotProps={{ htmlInput: { maxLength: MAX_BIO_LENGTH } }}
            value={form.bio}
          />
        </Grid>
      </Grid>
    </SectionCard>
  );
};

export default ProfileSectionCard;
