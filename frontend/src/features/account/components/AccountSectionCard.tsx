import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import Grid from "@mui/material/Grid";
import TextField from "@mui/material/TextField";
import type { AccountProfile } from "../../../client/movies/generator-output";
import { i18n } from "../../../i18n";
import {
  AccountSectionForm,
  hasAccountSectionChanges,
} from "../utils/accountSettingsForm";
import SectionCard from "./SectionCard";

type AccountSectionCardProps = {
  accountProfile: AccountProfile;
  form: AccountSectionForm;
  isSaving: boolean;
  onFormChange: (form: AccountSectionForm) => void;
  onSave: () => void;
};

const AccountSectionCard = ({
  accountProfile,
  form,
  isSaving,
  onFormChange,
  onSave,
}: AccountSectionCardProps) => {
  const isDirty = hasAccountSectionChanges(accountProfile, form);

  return (
    <SectionCard
      title="Account"
      subtitle="Used for sign-in and notifications"
      actions={
        <Button
          disabled={!isDirty || isSaving}
          onClick={onSave}
          startIcon={isSaving ? <CircularProgress size={16} /> : undefined}
          variant="contained"
        >
          Save account
        </Button>
      }
    >
      <Grid container spacing={2}>
        <Grid size={{ xs: 12 }}>
          <TextField
            disabled
            fullWidth
            label={i18n.accountSettings.email}
            type="email"
            value={accountProfile.email ?? ""}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label={i18n.accountSettings.birthday}
            onChange={(event) =>
              onFormChange({
                ...form,
                birthday: event.target.value || null,
              })
            }
            slotProps={{ inputLabel: { shrink: true } }}
            type="date"
            value={form.birthday ?? ""}
          />
        </Grid>
      </Grid>
    </SectionCard>
  );
};

export default AccountSectionCard;
