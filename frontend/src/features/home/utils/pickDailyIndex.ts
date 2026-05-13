export const pickDailyIndex = (
  collectionLength: number,
  date = new Date(),
): number | null => {
  if (collectionLength <= 0) {
    return null;
  }

  const dayKey = date.toISOString().slice(0, 10);
  let hash = 0;
  for (const character of dayKey) {
    hash = ((hash << 5) - hash + character.charCodeAt(0)) | 0;
  }

  return Math.abs(hash) % collectionLength;
};
