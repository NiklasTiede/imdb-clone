import {
  create,
  get,
  supported,
  type CredentialCreationOptionsJSON,
  type CredentialRequestOptionsJSON,
} from "@github/webauthn-json";
import type { PasskeyCredentialResponse } from "../../../client/movies/generator-output";
import { apiHttpClient } from "../../../shared/api/httpClient";
import { passkeyManagementApi } from "../../../shared/api/moviesApi";
import type { AccountSessionResponse } from "../../../shared/auth";

type PublicKeyCredentialCreationOptionsJSON =
  CredentialCreationOptionsJSON["publicKey"];
type PublicKeyCredentialRequestOptionsJSON =
  CredentialRequestOptionsJSON["publicKey"];

export type PasskeyCredential = {
  credentialId: string;
  createdAt: string;
  label: string;
  lastUsedAt: string;
};

export const passkeyQueryKeys = {
  all: ["account", "passkeys"] as const,
};

export const isPasskeySupported = supported;

export const loginWithPasskey = async (): Promise<AccountSessionResponse> => {
  assertPasskeySupported();

  const optionsResponse =
    await apiHttpClient.post<PublicKeyCredentialRequestOptionsJSON>(
      "/webauthn/authenticate/options",
    );
  const publicKey = optionsResponse.data;
  if (publicKey === undefined) {
    throw new Error("Passkey authentication options are missing.");
  }
  const credential = await get({ publicKey });
  await apiHttpClient.post("/login/webauthn", credential);

  const sessionResponse =
    await apiHttpClient.get<AccountSessionResponse>("/api/auth/me");
  return sessionResponse.data;
};

export const registerPasskey = async (label: string): Promise<void> => {
  assertPasskeySupported();

  const optionsResponse =
    await apiHttpClient.post<PublicKeyCredentialCreationOptionsJSON>(
      "/webauthn/register/options",
    );
  const publicKey = optionsResponse.data;
  if (publicKey === undefined) {
    throw new Error("Passkey registration options are missing.");
  }
  const credential = await create({ publicKey });

  await apiHttpClient.post("/webauthn/register", {
    publicKey: {
      credential,
      label,
    },
  });
};

export const listPasskeys = async (): Promise<PasskeyCredential[]> => {
  const response = await passkeyManagementApi.listPasskeys();
  return response.data.map(toPasskeyCredential);
};

export const deletePasskey = async (credentialId: string): Promise<void> => {
  await passkeyManagementApi.deletePasskey(credentialId);
};

const assertPasskeySupported = () => {
  if (!supported()) {
    throw new Error("Passkeys are not supported by this browser.");
  }
};

const toPasskeyCredential = (
  response: PasskeyCredentialResponse,
): PasskeyCredential => ({
  credentialId: response.credentialId ?? "",
  createdAt: response.createdAt ?? "",
  label: response.label ?? "Passkey",
  lastUsedAt: response.lastUsedAt ?? response.createdAt ?? "",
});
