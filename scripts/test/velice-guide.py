#!/usr/bin/env python3
"""Execute every runnable Velice example embedded in the website guide."""
from __future__ import annotations

import os
import subprocess
import sys
import tempfile
from html.parser import HTMLParser
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WEB = Path(sys.argv[1]).resolve() if len(sys.argv) == 2 else ROOT / "public" / "web" / "main"
VELICE = ROOT / "main" / "src" / "languages" / "velice"


class GuideCodeParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.blocks: list[tuple[str, str]] = []
        self._mode: str | None = None
        self._inside_code = False
        self._text: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        values = dict(attrs)
        if tag == "pre" and values.get("data-language") == "velice":
            # Keep an empty marker so an unclassified Velice block fails the
            # audit instead of silently escaping execution.
            self._mode = values.get("data-validate") or ""
            self._text = []
        elif tag == "code" and self._mode is not None:
            self._inside_code = True

    def handle_data(self, data: str) -> None:
        if self._inside_code:
            self._text.append(data)

    def handle_endtag(self, tag: str) -> None:
        if tag == "code":
            self._inside_code = False
        elif tag == "pre" and self._mode is not None:
            self.blocks.append((self._mode, "".join(self._text).strip() + "\n"))
            self._mode = None
            self._text = []


def run_file(path: Path, *args: str) -> None:
    env = dict(os.environ, VELICE_GUI="none", PYTHONPATH=str(VELICE))
    result = subprocess.run(
        [sys.executable, "-m", "velice", "run", str(path), *args],
        cwd=VELICE, env=env, capture_output=True, text=True,
    )
    if result.returncode:
        detail = (result.stderr or result.stdout).strip()
        raise RuntimeError(f"{path.name} failed: {detail}")


def main() -> int:
    parser = GuideCodeParser()
    parser.feed((WEB / "velice.html").read_text(encoding="utf-8"))
    if not parser.blocks:
        raise RuntimeError("the guide contains no validated Velice blocks")

    allowed = {"run", "headless", "args", "module-source", "module-main", "cmd"}
    invalid = sorted({mode for mode, _ in parser.blocks if mode not in allowed})
    if invalid:
        raise RuntimeError("unknown or missing data-validate mode: " + ", ".join(invalid))

    with tempfile.TemporaryDirectory(prefix="velice-guide-") as temp_name:
        temp = Path(temp_name)
        module_source = None
        executed = 0
        for index, (mode, source) in enumerate(parser.blocks, start=1):
            if mode == "cmd":
                continue
            if mode == "module-source":
                module_source = temp / "math.velice"
                module_source.write_text(source, encoding="utf-8")
                run_file(module_source)
                executed += 1
                continue
            if mode == "module-main":
                if module_source is None:
                    raise RuntimeError("module main appears before its source block")
                path = temp / "main.velice"
            else:
                path = temp / f"guide-{index:02d}.velice"
            path.write_text(source, encoding="utf-8")
            if mode == "args":
                argument = temp / "source.velice"
                argument.write_text('print("guide validation")\n', encoding="utf-8")
                run_file(path, str(argument))
            else:
                run_file(path)
            executed += 1

    print(f"[PASS] Velice guide examples: {executed}/{len(parser.blocks)} executable blocks")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as error:
        print(f"[FAIL] {error}", file=sys.stderr)
        raise SystemExit(1)
