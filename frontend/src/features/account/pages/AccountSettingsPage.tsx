import { Stack } from "@mui/material";
import { useEffect, useState } from "react";
import { i18n } from "../../../i18n";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  accountMutationKeys,
  updateAccountProfile,
} from "../api/accountMutations";
import { accountQueries } from "../api/accountQueries";
import { useSnackbar } from "notistack";
import PageContent from "../../../shared/layout/PageContent";
import PageHeader from "../../../shared/layout/PageHeader";
import AccountSectionCard from "../components/AccountSectionCard";
import ProfileHeaderCard from "../components/ProfileHeaderCard";
import ProfileSectionCard from "../components/ProfileSectionCard";
import {
  AccountSectionForm,
  buildAccountSectionPayload,
  buildProfileSectionPayload,
  ProfileSectionForm,
} from "../utils/accountSettingsForm";
import { deleteUserProfilePhoto } from "../../media";

const AccountSettingsPage = () => {
  const queryClient = useQueryClient();
  const { enqueueSnackbar } = useSnackbar();

  const { data: accountProfile = emptyAccountProfile } = useQuery(
    accountQueries.currentProfile(),
  );
  const updateProfileSection = useMutation({
    mutationFn: updateAccountProfile,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: accountMutationKeys.currentProfile,
      });
      enqueueSnackbar(i18n.accountSettings.profileSaved, {
        variant: "info",
      });
    },
    onError: () => {
      enqueueSnackbar(i18n.accountSettings.loadingError("save profile"), {
        variant: "error",
      });
    },
  });

  const updateAccountSection = useMutation({
    mutationFn: updateAccountProfile,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: accountMutationKeys.currentProfile,
      });
      enqueueSnackbar(i18n.accountSettings.accountSaved, {
        variant: "info",
      });
    },
    onError: () => {
      enqueueSnackbar(i18n.accountSettings.loadingError("save account"), {
        variant: "error",
      });
    },
  });

  const removeProfilePhoto = useMutation({
    mutationFn: deleteUserProfilePhoto,
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: accountMutationKeys.currentProfile,
      });
      enqueueSnackbar(i18n.accountSettings.profilePhotoRemoved, {
        variant: "info",
      });
    },
    onError: () => {
      enqueueSnackbar(i18n.accountSettings.loadingError("remove profile photo"), {
        variant: "error",
      });
    },
  });

  const [profileForm, setProfileForm] = useState<ProfileSectionForm>({
    bio: "",
    firstName: "",
    lastName: "",
  });
  const [accountForm, setAccountForm] = useState<AccountSectionForm>({
    birthday: null,
  });

  useEffect(() => {
    setProfileForm({
      bio: accountProfile.bio ?? "",
      firstName: accountProfile.firstName ?? "",
      lastName: accountProfile.lastName ?? "",
    });
    setAccountForm({
      birthday: accountProfile.birthday ?? null,
    });
  }, [accountProfile]);

  const saveProfileSection = () => {
    if (accountProfile.username) {
      updateProfileSection.mutate({
        accountRecord: buildProfileSectionPayload(accountProfile, profileForm),
        username: accountProfile.username,
      });
    }
  };

  const saveAccountSection = () => {
    if (accountProfile.username) {
      updateAccountSection.mutate({
        accountRecord: buildAccountSectionPayload(accountProfile, accountForm),
        username: accountProfile.username,
      });
    }
  };

  return (
    <PageContent maxWidth="760px">
      <Stack spacing={2}>
        <PageHeader
          eyebrow="Account"
          title="Account settings"
          subtitle="Manage your profile and account details"
        />

        <ProfileHeaderCard
          accountProfile={accountProfile}
          isRemovingPhoto={removeProfilePhoto.isPending}
          onRemovePhoto={() => removeProfilePhoto.mutate()}
        />

        <ProfileSectionCard
          accountProfile={accountProfile}
          form={profileForm}
          isSaving={updateProfileSection.isPending}
          onFormChange={setProfileForm}
          onSave={saveProfileSection}
        />

        <AccountSectionCard
          accountProfile={accountProfile}
          form={accountForm}
          isSaving={updateAccountSection.isPending}
          onFormChange={setAccountForm}
          onSave={saveAccountSection}
        />
      </Stack>
    </PageContent>
  );
};

const emptyAccountProfile = {
  bio: "",
  birthday: "",
  commentsCount: 0,
  email: "",
  firstName: "",
  imageUrlToken: "",
  lastName: "",
  phone: "",
  ratingsCount: 0,
  username: "",
  watchlistCount: 0,
};

export default AccountSettingsPage;
