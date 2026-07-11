const createFeedInstanceId = () => {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }

  return `home-${Date.now()}-${Math.random().toString(36).slice(2)}`;
};

let feedInstanceId = createFeedInstanceId();
const carouselScrollPositions = new Map<string, number>();
let verticalScrollPosition = 0;

export const getHomeFeedInstanceId = () => feedInstanceId;

export const startNewHomeFeedSession = () => {
  feedInstanceId = createFeedInstanceId();
  carouselScrollPositions.clear();
  verticalScrollPosition = 0;
  return feedInstanceId;
};

export const getCarouselScrollPosition = (sectionId: string) =>
  carouselScrollPositions.get(sectionId) ?? 0;

export const setCarouselScrollPosition = (sectionId: string, position: number) => {
  carouselScrollPositions.set(sectionId, position);
};

export const getHomeFeedVerticalScrollPosition = () => verticalScrollPosition;

export const setHomeFeedVerticalScrollPosition = (position: number) => {
  verticalScrollPosition = position;
};
