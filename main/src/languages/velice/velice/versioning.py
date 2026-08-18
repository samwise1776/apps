"""Semantic versioning for the Velice package system."""
import re

_V = re.compile(r"^(?P<major>0|[1-9]\d*)\.(?P<minor>0|[1-9]\d*)\.(?P<patch>0|[1-9]\d*)"
                r"(?:-(?P<pre>[0-9A-Za-z.\-]+))?(?:\+(?P<build>[0-9A-Za-z.\-]+))?$")


class SemVer:
    def __init__(self, major=0, minor=0, patch=0, prerelease="", build=""):
        self.major, self.minor, self.patch = major, minor, patch
        self.prerelease, self.build = prerelease, build

    @classmethod
    def parse(cls, text):
        m = _V.match(text.strip())
        if not m:
            raise ValueError(f"invalid version: {text!r}")
        return cls(int(m.group("major")), int(m.group("minor")), int(m.group("patch")),
                   m.group("pre") or "", m.group("build") or "")

    def _cmp_key(self):
        return (self.major, self.minor, self.patch, self._pre_key())

    def _pre_key(self):
        if not self.prerelease:
            return (1,)          # release > prerelease
        parts = []
        for p in self.prerelease.split("."):
            parts.append((0, int(p)) if p.isdigit() else (1, p))
        return (0, tuple(parts))

    def __eq__(self, other):
        if not isinstance(other, SemVer): return NotImplemented
        return self._cmp_key() == other._cmp_key()

    def __lt__(self, other):
        return self._cmp_key() < other._cmp_key()

    def __le__(self, other): return self < other or self == other
    def __gt__(self, other): return not (self <= other)
    def __ge__(self, other): return not (self < other)

    def __str__(self):
        s = f"{self.major}.{self.minor}.{self.patch}"
        if self.prerelease: s += f"-{self.prerelease}"
        if self.build: s += f"+{self.build}"
        return s

    def is_release(self):
        return not self.prerelease


def parse_constraint(spec):
    """Parse a dependency constraint like '>=2.0.0', '^1.2', '~1.2.3', '1.x', '*', '2.1.0'."""
    spec = (spec or "").strip()
    if not spec or spec == "*":
        return lambda v: True
    spec = spec.replace(" ", "")
    if "||" in spec:
        alts = [parse_constraint(p) for p in spec.split("||")]
        return lambda v: any(f(v) for f in alts)
    if "," in spec:
        parts = [parse_constraint(p) for p in spec.split(",")]
        return lambda v: all(f(v) for f in parts)
    m = re.match(r"^(<=|>=|==|!=|<|>|\^|~)?(.*)$", spec)
    op, rest = m.group(1) or "==", m.group(2)
    try:
        target = SemVer.parse(rest)
    except ValueError:
        parts = rest.split(".")
        if any(p not in ("*", "x", "X") and not p.isdigit() for p in parts):
            raise ValueError(f"invalid constraint: {spec!r}")
        while len(parts) < 3: parts.append("*")
        def _to(target=None):
            def f(v):
                for i, p in enumerate(parts):
                    if p in ("*", "x", "X"): return True
                    if int(p) != [v.major, v.minor, v.patch][i]: return False
                return True
            return f
        return _to()
    def make(op):
        def f(v):
            return {"<=": v <= target, ">=": v >= target, "==": v == target,
                    "!=": v != target, "<": v < target, ">": v > target}[op](v) if op != "==" else v == target
        return f
    if op == "^":
        def caret(v):
            if v < target: return False
            if target.major > 0: return v.major == target.major
            if target.minor > 0: return v.major == target.major and v.minor == target.minor
            return v.major == target.major and v.minor == target.minor and v.patch == target.patch
        return caret
    if op == "~":
        return lambda v: v >= target and v.major == target.major and v.minor == target.minor
    return make(op)
