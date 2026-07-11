const YOUTUBE_VIDEO_KEY_PATTERN = /^[A-Za-z0-9_-]{11}$/;

export const getValidYouTubeVideoKey = (
  value: string | null | undefined,
): string | null => {
  const normalizedValue = value?.trim();
  return normalizedValue && YOUTUBE_VIDEO_KEY_PATTERN.test(normalizedValue)
    ? normalizedValue
    : null;
};

export const getYouTubeNoCookieEmbedUrl = (
  value: string | null | undefined,
): string | null => {
  const videoKey = getValidYouTubeVideoKey(value);
  if (!videoKey) {
    return null;
  }

  const url = new URL(`https://www.youtube-nocookie.com/embed/${videoKey}`);
  url.searchParams.set("autoplay", "1");
  url.searchParams.set("playsinline", "1");
  url.searchParams.set("rel", "0");
  return url.toString();
};
