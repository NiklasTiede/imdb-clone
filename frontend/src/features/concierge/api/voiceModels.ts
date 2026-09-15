import { useQuery } from "@tanstack/react-query";
import { z } from "zod";

export const voiceModelSchema = z.enum(["grok", "gpt-live-1"]);
export type VoiceModel = z.infer<typeof voiceModelSchema>;
export const voiceModelLabels: Record<VoiceModel, string> = {
  grok: "Grok",
  "gpt-live-1": "GPT-Live 1",
};

export const useVoiceModels = () =>
  useQuery({
    queryKey: ["concierge", "voice-models"],
    queryFn: async ({ signal }): Promise<VoiceModel[]> => {
      const base =
        import.meta.env.VITE_IMDB_CLONE_CONCIERGE_ADDRESS ?? "/concierge-api";
      const response = await fetch(`${base}/v1/voice/models`, { signal });
      if (!response.ok) throw new Error("Voice models are unavailable");
      return z
        .object({ models: z.array(voiceModelSchema).max(2) })
        .parse(await response.json()).models;
    },
    staleTime: 30_000,
    retry: false,
    refetchOnWindowFocus: false,
  });
