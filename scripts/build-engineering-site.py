#!/usr/bin/env python3
"""Validate and package only the public engineering site, using the Python standard library."""

import hashlib
import json
from html.parser import HTMLParser
from pathlib import Path
import shutil
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "engineering/site"
OUTPUT = ROOT / "engineering/dist"
PUBLIC_FILES = (
    "index.html",
    "architecture/index.html",
    "architecture/application/index.html",
    "architecture/observability/index.html",
    "assets/site.css",
    "assets/mark.svg",
    "assets/favicon.svg",
    "assets/fonts/bebas-neue-latin-400-normal.woff2",
    "assets/fonts/bebas-neue-LICENSE.txt",
    "assets/fonts/manrope-latin-wght-normal.woff2",
    "assets/fonts/manrope-LICENSE.txt",
    "assets/application.png",
    "assets/observability.png",
    "404.html",
    "robots.txt",
    "sitemap.xml",
    "CNAME",
    ".nojekyll",
)


class References(HTMLParser):
    def __init__(self):
        super().__init__()
        self.links = []
        self.ids = set()

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if "id" in attrs:
            self.ids.add(attrs["id"])
        for name in ("href", "src"):
            if attrs.get(name):
                self.links.append(attrs[name])


def check_site():
    for relative in PUBLIC_FILES:
        if not (SOURCE / relative).is_file():
            raise ValueError(f"Missing public file: {relative}")
    receipts = json.loads((ROOT / "engineering/diagrams/receipts.json").read_text())
    for name, receipt in receipts.items():
        for kind, path in (
            ("specification", ROOT / f"engineering/diagrams/{name}.json"),
            ("artifact", SOURCE / f"architecture/{name}/index.html"),
        ):
            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            if digest != receipt[f"{kind}_sha256"]:
                raise ValueError(f"Stale {kind} receipt for {name}; regenerate and verify the diagram")
        if receipt["validation"] != "9/9 showcase, 0 errors, 0 warnings":
            raise ValueError(f"Missing showcase validation for {name}")

    # Authored pages use ordinary links; Archify's standalone viewers also use
    # dynamic fragment state, so their behaviour is covered by browser evidence.
    pages = {}
    for relative in ("index.html", "architecture/index.html", "404.html"):
        parsed = References()
        parsed.feed((SOURCE / relative).read_text())
        pages[(SOURCE / relative).resolve()] = parsed
    for page, parsed in pages.items():
        for link in parsed.links:
            url = urlsplit(link)
            if url.scheme or url.netloc:
                continue
            if not url.path:
                target = page
            elif url.path.startswith("/"):
                target = (SOURCE / unquote(url.path.lstrip("/"))).resolve()
            else:
                target = (page.parent / unquote(url.path)).resolve()
            if target.is_dir():
                target /= "index.html"
            if not target.is_relative_to(SOURCE.resolve()):
                raise ValueError(f"Link escapes public site: {page.name}: {link}")
            if target.relative_to(SOURCE.resolve()).as_posix() not in PUBLIC_FILES:
                raise ValueError(f"Link targets an unpublished file: {page.name}: {link}")
            if url.fragment and target in pages and unquote(url.fragment) not in pages[target].ids:
                raise ValueError(f"Broken fragment: {page.name}: {link}")


def main():
    check_site()
    # This is generated output only. Allowlisting keeps browser receipts, local
    # screenshots and internal repository documentation out of the deployment.
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    for relative in PUBLIC_FILES:
        target = OUTPUT / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(SOURCE / relative, target)
    print(f"Validated links and diagram receipts; packaged {len(PUBLIC_FILES)} public files in {OUTPUT}")


if __name__ == "__main__":
    main()
