import { Link } from "@mui/material";
import type { ReactNode } from "react";

/** Only the fixed TMDB watch-page contract is clickable in model-authored answers. */
export const ConciergeSourceLink = ({
  href,
  children,
}: {
  href?: string | undefined;
  children?: ReactNode;
}) =>
  href &&
  /^https:\/\/www\.themoviedb\.org\/movie\/[1-9][0-9]*\/watch\?locale=[A-Z]{2}$/.test(
    href,
  ) ? (
    <Link href={href} target="_blank" rel="noopener noreferrer">
      {children}
    </Link>
  ) : (
    <span>{children}</span>
  );
