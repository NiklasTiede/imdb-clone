export const sanitizeUrlForTelemetry = (url: string | undefined): string => {
  if (!url) {
    return "unknown";
  }

  try {
    const parsedUrl = new URL(url, window.location.origin);
    const sanitized = `${parsedUrl.origin}${parsedUrl.pathname}`;
    return url.startsWith("/") ? parsedUrl.pathname : sanitized;
  } catch {
    return url.split("?")[0] || "unknown";
  }
};
