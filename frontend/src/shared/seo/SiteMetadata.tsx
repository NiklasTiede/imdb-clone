import { useEffect } from "react";
import { useLocation } from "react-router";
import { getPageMetadata, siteDescription, siteName } from "./metadata";

/** Route metadata must follow SPA navigation without publishing tokens or search queries. */
export default function SiteMetadata() {
  const { pathname, search } = useLocation();

  useEffect(() => {
    const { canonical, robots } = getPageMetadata(
      window.location.origin,
      pathname,
      search,
    );
    const elements: HTMLElement[] = [];
    const meta = (name: string, content: string, property = false) => {
      const element = document.createElement("meta");
      element.setAttribute(property ? "property" : "name", name);
      element.content = content;
      document.head.append(element);
      elements.push(element);
    };

    meta("robots", robots);
    if (canonical) {
      const link = document.createElement("link");
      link.rel = "canonical";
      link.href = canonical;
      document.head.append(link);
      elements.push(link);
      meta("og:url", canonical, true);
    }

    if (pathname === "/") {
      const structuredData = document.createElement("script");
      structuredData.type = "application/ld+json";
      structuredData.textContent = JSON.stringify({
        "@context": "https://schema.org",
        "@type": "WebSite",
        name: siteName,
        url: `${window.location.origin}/`,
        description: siteDescription,
      });
      document.head.append(structuredData);
      elements.push(structuredData);
    }

    return () => elements.forEach((element) => element.remove());
  }, [pathname, search]);

  return null;
}
