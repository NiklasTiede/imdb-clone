import { Button } from "@mui/material";
import { useEffect } from "react";
import { useLocation } from "react-router";
import { useConciergeChat } from "../hooks/useConciergeChat";
import type { ConciergeVoice } from "../hooks/useConciergeVoice";
import type { ApplicationAction } from "../model/concierge";
import type { PageContext } from "../model/pageContext";
import { getConciergeClientId } from "../model/browserIdentity";
import ConciergeDrawer from "./ConciergeDrawer";
import { ConciergeVoiceDock } from "./ConciergeVoicePanel";

/** Loaded only through the debug URL; keep mounted when the drawer is closed. */
export default function ConciergeDebug({
  accountId,
  voice,
  pageContext,
  onAction,
  open,
  onClose,
  onToggle,
  streamingCountry,
  onStreamingCountryChange,
}: {
  accountId: number | null;
  voice: ConciergeVoice;
  pageContext: PageContext;
  onAction: (action: ApplicationAction) => string;
  open: boolean;
  onClose: () => void;
  onToggle: () => void;
  streamingCountry: string;
  onStreamingCountryChange: (country: string) => void;
}) {
  const chat = useConciergeChat(
    getConciergeClientId(accountId),
    onAction,
    pageContext,
  );
  const { confirmNavigation, turns } = chat;
  const { pathname, search, hash } = useLocation();
  useEffect(() => {
    confirmNavigation(`${pathname}${search}${hash}`);
  }, [pathname, search, hash, turns, confirmNavigation]);

  return (
    <>
      {!open && (
        <Button
          onClick={onToggle}
          sx={{
            position: "fixed",
            right: 16,
            bottom: 16,
            zIndex: (theme) => theme.zIndex.drawer + 1,
          }}
        >
          Open voice conversation
        </Button>
      )}
      <ConciergeVoiceDock
        voice={voice}
        conversationOpen={open}
        toggleConversation={onToggle}
        debug
        disabled={chat.isStreaming}
      />
      <ConciergeDrawer
        streamingCountry={streamingCountry}
        onStreamingCountryChange={onStreamingCountryChange}
        chat={chat}
        signedIn={accountId !== null}
        voice={voice}
        onClose={onClose}
        open={open}
      />
    </>
  );
}
