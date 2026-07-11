const relativeTime = new Intl.RelativeTimeFormat("en", { numeric: "auto" });
const absoluteTime = new Intl.DateTimeFormat("en", {
  day: "numeric",
  month: "short",
  year: "numeric",
});

export const formatCommentTime = (
  value?: string,
  now: number = Date.now(),
): string => {
  const timestamp = value ? Date.parse(value) : Number.NaN;
  if (!Number.isFinite(timestamp)) {
    return "Unknown date";
  }

  const elapsedSeconds = Math.max(0, Math.round((now - timestamp) / 1000));
  if (elapsedSeconds < 60) {
    return "Just now";
  }
  const elapsedMinutes = Math.round(elapsedSeconds / 60);
  if (elapsedMinutes < 60) {
    return relativeTime.format(-elapsedMinutes, "minute");
  }
  const elapsedHours = Math.round(elapsedMinutes / 60);
  if (elapsedHours < 24) {
    return relativeTime.format(-elapsedHours, "hour");
  }
  const elapsedDays = Math.round(elapsedHours / 24);
  if (elapsedDays < 7) {
    return relativeTime.format(-elapsedDays, "day");
  }
  return absoluteTime.format(timestamp);
};

export const formatCommentTimeTitle = (value?: string): string => {
  const timestamp = value ? Date.parse(value) : Number.NaN;
  return Number.isFinite(timestamp)
    ? new Date(timestamp).toLocaleString("en")
    : "Unknown date";
};

export const wasCommentEdited = (
  createdAtInUtc?: string,
  modifiedAtInUtc?: string,
): boolean => {
  const createdAt = createdAtInUtc ? Date.parse(createdAtInUtc) : Number.NaN;
  const modifiedAt = modifiedAtInUtc ? Date.parse(modifiedAtInUtc) : Number.NaN;
  return (
    Number.isFinite(createdAt) &&
    Number.isFinite(modifiedAt) &&
    modifiedAt - createdAt > 1000
  );
};
