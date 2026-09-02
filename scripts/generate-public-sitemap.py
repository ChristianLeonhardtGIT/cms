#!/usr/bin/env python3
"""Build sitemap.xml exclusively from reachable, indexable public pages."""

from __future__ import annotations

import argparse
import os
import re
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from collections import deque
from datetime import date
from html import escape
from html.parser import HTMLParser
from pathlib import Path


DEFAULT_BASE_URL = "https://cleonhardt.de"
USER_AGENT = "cleonhardt-sitemap/1.0 (+https://cleonhardt.de/)"
MAX_PAGES = 500
MIN_PUBLIC_PAGES = 5
SKIPPED_PREFIXES = (
    "/.magnolia",
    "/.resources",
    "/magnoliaAuthor",
    "/magnoliaPublic",
)
SKIPPED_EXTENSIONS = re.compile(
    r"\.(?:avif|css|csv|gif|ico|jpe?g|js|json|mp4|pdf|png|svg|txt|webmanifest|webp|xml|zip)$",
    re.IGNORECASE,
)


class PageSignals(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.links: list[str] = []
        self.canonical = ""
        self.noindex = False

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attributes = {key.lower(): (value or "") for key, value in attrs}
        if tag.lower() == "a" and attributes.get("href"):
            self.links.append(attributes["href"])
        if tag.lower() == "link" and "canonical" in attributes.get("rel", "").lower().split():
            self.canonical = attributes.get("href", "")
        if tag.lower() == "meta" and attributes.get("name", "").lower() == "robots":
            self.noindex = "noindex" in attributes.get("content", "").lower()


def normalize_public_url(candidate: str, base_url: str) -> str | None:
    absolute = urllib.parse.urljoin(base_url + "/", candidate)
    parsed = urllib.parse.urlsplit(absolute)
    base = urllib.parse.urlsplit(base_url)
    if parsed.scheme not in {"http", "https"} or parsed.netloc != base.netloc:
        return None
    path = urllib.parse.unquote(parsed.path or "/")
    if path != "/":
        path = path.rstrip("/")
    if any(path.startswith(prefix) for prefix in SKIPPED_PREFIXES):
        return None
    if SKIPPED_EXTENSIONS.search(path):
        return None
    return urllib.parse.urlunsplit((base.scheme, base.netloc, path, "", ""))


def existing_urls(output: Path, base_url: str) -> list[str]:
    if not output.exists():
        return []
    try:
        root = ET.parse(output).getroot()
    except (ET.ParseError, OSError):
        return []
    urls: list[str] = []
    for element in root.iter():
        if element.tag.endswith("loc") and element.text:
            normalized = normalize_public_url(element.text.strip(), base_url)
            if normalized:
                urls.append(normalized)
    return urls


def fetch_page(url: str) -> tuple[int, str, str, str, str]:
    for attempt in range(2):
        request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "text/html"})
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                body = response.read(2_000_000)
                charset = response.headers.get_content_charset() or "utf-8"
                return (
                    response.status,
                    response.geturl(),
                    response.headers.get("Content-Type", ""),
                    body.decode(charset, "replace"),
                    response.headers.get("X-Robots-Tag", ""),
                )
        except urllib.error.HTTPError as error:
            return error.code, url, error.headers.get("Content-Type", ""), "", error.headers.get("X-Robots-Tag", "")
        except (urllib.error.URLError, TimeoutError):
            if attempt == 0:
                time.sleep(1)
    return 0, url, "", "", ""


def extract_last_modified(html: str) -> str | None:
    candidates = re.findall(r'"dateModified"\s*:\s*"(\d{4}-\d{2}-\d{2})"', html)
    valid: list[str] = []
    for candidate in candidates:
        try:
            date.fromisoformat(candidate)
        except ValueError:
            continue
        valid.append(candidate)
    return max(valid) if valid else None


def build_sitemap(base_url: str, output: Path) -> list[tuple[str, str | None]]:
    start_url = normalize_public_url("/", base_url)
    assert start_url
    queue = deque([start_url, *existing_urls(output, base_url)])
    queued = set(queue)
    visited: set[str] = set()
    indexable: set[str] = set()
    last_modified: dict[str, str] = {}

    while queue and len(visited) < MAX_PAGES:
        url = queue.popleft()
        if url in visited:
            continue
        visited.add(url)
        status, final_url, content_type, html, robots_header = fetch_page(url)
        if status != 200 or "text/html" not in content_type.lower() or "noindex" in robots_header.lower():
            continue

        final_normalized = normalize_public_url(final_url, base_url)
        if not final_normalized:
            continue

        parser = PageSignals()
        parser.feed(html)
        if parser.noindex:
            continue

        canonical = normalize_public_url(parser.canonical or final_normalized, base_url)
        if not canonical:
            continue
        canonical_status, canonical_final, canonical_type, canonical_html, canonical_robots = (
            status,
            final_url,
            content_type,
            html,
            robots_header,
        )
        if canonical != final_normalized:
            canonical_status, canonical_final, canonical_type, canonical_html, canonical_robots = fetch_page(canonical)
        if (
            canonical_status != 200
            or "text/html" not in canonical_type.lower()
            or "noindex" in canonical_robots.lower()
        ):
            continue

        indexable.add(canonical)
        modified = extract_last_modified(canonical_html)
        if modified:
            last_modified[canonical] = max(modified, last_modified.get(canonical, modified))
        for href in parser.links:
            linked = normalize_public_url(href, base_url)
            if linked and linked not in visited and linked not in queued:
                queue.append(linked)
                queued.add(linked)

    if start_url not in indexable or len(indexable) < MIN_PUBLIC_PAGES:
        raise RuntimeError(
            f"Safety stop: only {len(indexable)} indexable pages found; existing sitemap was not replaced"
        )

    urls = sorted(indexable, key=lambda url: (url != start_url, urllib.parse.urlsplit(url).path))
    return [(url, last_modified.get(url)) for url in urls]


def write_atomically(urls: list[tuple[str, str | None]], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    lines = ['<?xml version="1.0" encoding="UTF-8"?>', '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">']
    for url, modified in urls:
        lastmod = f"<lastmod>{modified}</lastmod>" if modified else ""
        lines.append(f"  <url><loc>{escape(url)}</loc>{lastmod}</url>")
    lines.append("</urlset>")
    payload = "\n".join(lines) + "\n"
    descriptor, temporary_name = tempfile.mkstemp(prefix="sitemap.", suffix=".xml", dir=output.parent)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as temporary:
            temporary.write(payload)
            temporary.flush()
            os.fsync(temporary.fileno())
        os.replace(temporary_name, output)
    finally:
        if os.path.exists(temporary_name):
            os.unlink(temporary_name)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--output", type=Path, required=True)
    arguments = parser.parse_args()
    base_url = arguments.base_url.rstrip("/")
    urls = build_sitemap(base_url, arguments.output)
    write_atomically(urls, arguments.output)
    print(f"Wrote {len(urls)} verified public URLs to {arguments.output}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"Sitemap generation failed: {error}", file=sys.stderr)
        raise SystemExit(1)
