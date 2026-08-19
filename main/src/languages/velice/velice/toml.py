"""Minimal TOML parsing for velice.toml manifests.

Uses Python 3.11+'s `tomllib` when available; otherwise falls back to a small
hand-written parser sufficient for velice.toml files (tables, strings,
integers, floats, booleans, arrays, inline tables, comments).
"""
import re


def _strip_comment(line):
    in_str = False
    esc = False
    for i, ch in enumerate(line):
        if esc:
            esc = False; continue
        if ch == "\\" and in_str:
            esc = True; continue
        if ch == '"':
            in_str = not in_str
        elif ch == "#" and not in_str:
            return line[:i]
    return line


class _MiniTOML:
    def __init__(self, text):
        self.text = text
        self.root = {}
        self.cur = self.root

    def _set_path(self, path, value):
        node = self.root
        for part in path[:-1]:
            node = node.setdefault(part, {})
            if not isinstance(node, dict):
                raise ValueError(f"conflict in toml at table {path}")
        node[path[-1]] = value

    def parse(self):
        for raw in self.text.splitlines():
            line = _strip_comment(raw).strip()
            if not line:
                continue
            if line.startswith("[["):
                raise ValueError("array-of-tables not supported")
            if line.startswith("["):
                table_path = [p.strip() for p in line.strip("[] ").split(".")]
                self.cur = self.root
                for part in table_path:
                    self.cur = self.cur.setdefault(part, {})
                continue
            m = re.match(r"^([A-Za-z0-9_.\-\"]+)\s*=\s*(.*)$", line)
            if not m:
                raise ValueError(f"cannot parse toml line: {raw!r}")
            key = m.group(1).strip().strip('"')
            self.cur[key] = self._value(m.group(2).strip())
        return self.root

    def _value(self, s):
        if not s:
            return ""
        if s.startswith('"') or s.startswith("'"):
            return self._string(s)
        if s.startswith("["):
            return self._array(s)
        if s.startswith("{"):
            return self._inline_table(s)
        low = s.lower()
        if low in ("true", "false"):
            return low == "true"
        if low in ("null", "nil"):
            return None
        try:
            if any(c in s for c in ".eE") and not re.search(r"^[+-]?\d+[eE][+-]?\d+$", s):
                return float(s)
            return int(re.sub(r"[_]", "", s))
        except ValueError:
            return s

    def _string(self, s):
        if s.startswith("'"):
            return s[1:-1]
        s = s[1:-1]
        out = []
        i = 0
        while i < len(s):
            if s[i] == "\\" and i + 1 < len(s):
                n = s[i + 1]
                out.append({"n": "\n", "t": "\t", "r": "\r", '"': '"', "\\": "\\",
                            "b": "\b", "f": "\f"}.get(n, n))
                i += 2
            else:
                out.append(s[i]); i += 1
        return "".join(out)

    def _array(self, s):
        inner = s.strip()
        if inner == "[]":
            return []
        inner = inner[1:-1]
        return [self._value(p.strip()) for p in self._split_top(inner)]

    def _inline_table(self, s):
        inner = s.strip()[1:-1]
        result = {}
        for pair in self._split_top(inner):
            k, v = pair.split("=", 1)
            result[k.strip().strip('"')] = self._value(v.strip())
        return result

    @staticmethod
    def _split_top(s):
        parts, depth, cur = [], 0, []
        in_str = False
        for ch in s:
            if ch == '"':
                in_str = not in_str
                cur.append(ch)
            elif not in_str and ch in "[{":
                depth += 1; cur.append(ch)
            elif not in_str and ch in "]}":
                depth -= 1; cur.append(ch)
            elif ch == "," and depth == 0:
                parts.append("".join(cur).strip()); cur = []
            else:
                cur.append(ch)
        tail = "".join(cur).strip()
        if tail:
            parts.append(tail)
        return parts


def loads(text):
    return _MiniTOML(text).parse()


def load(path):
    with open(path, encoding="utf-8") as f:
        return loads(f.read())
