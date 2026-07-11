import { useEffect, useRef } from "react";
import {
  getHomeFeedVerticalScrollPosition,
  setHomeFeedVerticalScrollPosition,
} from "../model/homeFeedSession";

export const useHomeFeedRestoration = (
  feedInstanceId: string,
  renderedSectionCount: number,
) => {
  const restoredFeedInstanceId = useRef<string | null>(null);

  useEffect(() => {
    const savePosition = () => setHomeFeedVerticalScrollPosition(window.scrollY);
    window.addEventListener("scroll", savePosition, { passive: true });
    return () => {
      savePosition();
      window.removeEventListener("scroll", savePosition);
    };
  }, []);

  useEffect(() => {
    if (
      renderedSectionCount === 0 ||
      restoredFeedInstanceId.current === feedInstanceId
    ) {
      return;
    }

    const frame = window.requestAnimationFrame(() => {
      window.scrollTo({
        behavior: "auto",
        top: getHomeFeedVerticalScrollPosition(),
      });
      restoredFeedInstanceId.current = feedInstanceId;
    });
    return () => window.cancelAnimationFrame(frame);
  }, [feedInstanceId, renderedSectionCount]);
};
