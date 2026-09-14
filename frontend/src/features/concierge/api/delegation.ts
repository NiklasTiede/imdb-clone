import { authSession } from "../../../shared/auth";
import { conciergeDelegationApi } from "../../../shared/api/moviesApi";

/** Kept in this request only: never persisted in browser storage or conversation history. */
export const getConciergeDelegation = async (): Promise<string | null> => {
  return (await getConciergeIdentity()).delegation;
};

export const getConciergeIdentity = async (): Promise<{
  accountId: number | null;
  delegation: string | null;
}> => {
  const session = authSession.getSnapshot().session;
  if (!session) return { accountId: null, delegation: null };
  const response = await conciergeDelegationApi.createConciergeDelegation();
  if (authSession.getSnapshot().session?.id !== session.id) {
    throw new Error("Your login changed. Start a new conversation.");
  }
  const token = response.data.token;
  if (!token) throw new Error("Sign in again to use your watchlist.");
  return { accountId: session.id, delegation: token };
};
