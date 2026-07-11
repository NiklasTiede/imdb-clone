type MovieShareResult = "cancelled" | "copied" | "shared";

type ShareMovieInput = {
  movieId: number;
  title: string;
};

type ShareMovieApis = {
  copyText?: (value: string) => Promise<void>;
  origin?: string;
  share?: (data: ShareData) => Promise<void>;
};

export const buildMovieShareUrl = (
  movieId: number,
  origin = window.location.origin,
): string => {
  const url = new URL("/movie", origin);
  url.searchParams.set("id", String(movieId));
  return url.toString();
};

export const shareMovie = async (
  { movieId, title }: ShareMovieInput,
  apis: ShareMovieApis = {},
): Promise<MovieShareResult> => {
  const share = apis.share ?? navigator.share?.bind(navigator);
  const copyText =
    apis.copyText ?? navigator.clipboard?.writeText.bind(navigator.clipboard);
  const url = buildMovieShareUrl(movieId, apis.origin);

  if (share) {
    try {
      await share({ title, url });
      return "shared";
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") {
        return "cancelled";
      }
      throw error;
    }
  }

  if (!copyText) {
    throw new Error("Sharing is not supported by this browser.");
  }

  await copyText(url);
  return "copied";
};
