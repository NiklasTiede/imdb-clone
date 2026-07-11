import Box from "@mui/material/Box";
import { useEffect, useRef } from "react";

type HomeFeedSentinelProps = {
  disabled?: boolean;
  onVisible: () => void;
};

const HomeFeedSentinel = ({ disabled = false, onVisible }: HomeFeedSentinelProps) => {
  const sentinelRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const element = sentinelRef.current;
    if (!element || disabled) {
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry?.isIntersecting) {
          onVisible();
        }
      },
      { rootMargin: "320px 0px" },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [disabled, onVisible]);

  return <Box ref={sentinelRef} aria-hidden sx={{ height: 1 }} />;
};

export default HomeFeedSentinel;
