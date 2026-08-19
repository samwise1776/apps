#!/usr/bin/env python3
"""Validate the static Datacenter site and the complete Velice guide."""
from __future__ import annotations

import subprocess
import sys
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[2]
WEB = Path(sys.argv[1]).resolve() if len(sys.argv) == 2 else ROOT / "main" / "src" / "public" / "web" / "main"


class Page(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.ids: list[str] = []
        self.refs: list[str] = []
        self.lessons = 0
        self.code_blocks = 0
        self.unlabelled_code_blocks = 0

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        values = dict(attrs)
        if values.get("id"):
            self.ids.append(values["id"] or "")
        if tag in {"a", "link"} and values.get("href"):
            self.refs.append(values["href"] or "")
        if tag in {"script", "img"} and values.get("src"):
            self.refs.append(values["src"] or "")
        classes = set((values.get("class") or "").split())
        if tag == "article" and "guide-lesson" in classes:
            self.lessons += 1
        if tag == "pre":
            self.code_blocks += 1
            if not values.get("data-language"):
                self.unlabelled_code_blocks += 1


def parse(path: Path) -> Page:
    page = Page()
    page.feed(path.read_text(encoding="utf-8"))
    page.close()
    return page


def fail(message: str) -> None:
    print(f"[FAIL] {message}", file=sys.stderr)
    raise SystemExit(1)


def main() -> int:
    required = {
        "index.html", "style.css", "app.js", "velice.html",
        "velice-guide.css", "guide.js", "assets/velice-logo.svg",
    }
    missing = sorted(name for name in required if not (WEB / name).is_file())
    if missing:
        fail("missing website files: " + ", ".join(missing))

    pages = {path.resolve(): parse(path) for path in WEB.glob("*.html")}
    for path, page in pages.items():
        duplicates = sorted({value for value in page.ids if page.ids.count(value) > 1})
        if duplicates:
            fail(f"duplicate IDs in {path.name}: {', '.join(duplicates)}")
        for ref in page.refs:
            parts = urlsplit(ref)
            if parts.scheme or parts.netloc or ref.startswith(("mailto:", "tel:")):
                continue
            target = path if not parts.path else (path.parent / unquote(parts.path)).resolve()
            if not target.exists():
                fail(f"broken link in {path.name}: {ref}")
            if parts.fragment and target.suffix.lower() == ".html":
                target_page = pages.get(target) or parse(target)
                if parts.fragment not in target_page.ids:
                    fail(f"missing anchor in {path.name}: {ref}")

    home_text = (WEB / "index.html").read_text(encoding="utf-8")
    if "velice.html" not in home_text or "apps/velice/velice.zip" not in home_text:
        fail("homepage does not expose both the Velice guide and download")

    guide = pages[(WEB / "velice.html").resolve()]
    if guide.lessons != 24:
        fail(f"expected 24 Velice chapters, found {guide.lessons}")
    if guide.code_blocks < 24 or guide.unlabelled_code_blocks:
        fail("every guide chapter must include labelled, highlightable code")

    guide_script = (WEB / "guide.js").read_text(encoding="utf-8")
    for feature in ("copy", "localStorage", "guideSearch", "highlight"):
        if feature not in guide_script:
            fail(f"guide script is missing {feature} support")

    for script in (WEB / "app.js", WEB / "guide.js"):
        result = subprocess.run(["node", "--check", str(script)], capture_output=True, text=True)
        if result.returncode:
            fail(f"JavaScript syntax error in {script.name}: {result.stderr.strip()}")

    examples = subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "test" / "velice-guide.py"), str(WEB)],
        capture_output=True, text=True,
    )
    if examples.returncode:
        fail((examples.stderr or examples.stdout).strip())

    print(f"[PASS] Datacenter website: {len(pages)} pages, {guide.lessons} Velice chapters, {guide.code_blocks} code blocks")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
