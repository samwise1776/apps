"""Velice Interpreter – tree-walking evaluator with environments and builtins."""
from __future__ import annotations
import math, time, random, sys, os, json, hashlib, datetime, uuid, tempfile
import subprocess
from typing import Any, Optional
from velice import ast_nodes as A
from velice.lexer import Lexer
from velice.parser import Parser

class VeliceError(Exception):
    def __init__(self, msg, traceback=None):
        super().__init__(msg)
        self.vl_traceback = traceback or []

class ReturnSignal(Exception):
    def __init__(self, val): self.val = val

class BreakSignal(Exception):
    def __init__(self, val=None): self.val = val

class ContinueSignal(Exception):
    pass

# ── Environment ──────────────────────────────────────────────────────────
class Env:
    def __init__(self, parent=None):
        self.vars: dict[str, Any] = {}; self.parent = parent

    def get(self, name):
        if name in self.vars:
            val = self.vars[name]
            return val[0] if isinstance(val, tuple) else val
        if self.parent: return self.parent.get(name)
        raise VeliceError(f"Undefined variable '{name}'")

    def set(self, name, val, mutable=True):
        if name in self.vars and not mutable:
            raise VeliceError(f"Cannot reassign immutable variable '{name}'")
        self.vars[name] = val

    def define(self, name, val, mutable=True):
        self.vars[name] = (val, mutable)

    def update(self, name, val):
        if name in self.vars:
            v, m = self.vars[name]
            if not m: raise VeliceError(f"Cannot reassign immutable variable '{name}'")
            self.vars[name] = (val, True); return
        if self.parent: self.parent.update(name, val); return
        raise VeliceError(f"Undefined variable '{name}'")

    def resolve(self, name):
        if name in self.vars: return self
        if self.parent: return self.parent.resolve(name)
        return None

# ── Callable ─────────────────────────────────────────────────────────────
class VLFunction:
    def __init__(self, name, params, body, closure, is_method=False, is_native=False, native_fn=None):
        self.name = name; self.params = params; self.body = body
        self.closure = closure; self.is_method = is_method; self.is_native = is_native; self.native_fn = native_fn

    def call(self, interp, args, kwargs=None):
        if self.is_native: return self.native_fn(interp, args, kwargs or {})
        env = Env(self.closure)
        if hasattr(self, '_bound_to'):
            env.define("self", self._bound_to)
        for i, p in enumerate(self.params):
            pname = p.name if hasattr(p, 'name') else p
            if i < len(args): env.define(pname, args[i])
            elif pname in (kwargs or {}): env.define(pname, kwargs[pname])
            elif hasattr(p, 'value') and p.value is not None:
                env.define(pname, interp.eval(p.value, env))
            else: raise VeliceError(f"Missing argument '{pname}'")
        try: interp.exec(self.body, env)
        except ReturnSignal as r: return r.val
        return None

    def __repr__(self): return f"<fn {self.name}>"

class VLClass:
    def __init__(self, name, methods=None, superclass=None):
        self.name = name; self.methods = methods or {}; self.superclass = superclass

    def _find_method(self, name):
        if name in self.methods: return self.methods[name]
        if self.superclass: return self.superclass._find_method(name)
        return None

    def call(self, interp, args, kwargs=None):
        obj = VLInstance(self)
        init = self._find_method("init")
        if init:
            bound = VLFunction(init.name, init.params, init.body, init.closure, is_method=True, is_native=init.is_native, native_fn=init.native_fn)
            bound._bound_to = obj
            bound.call(interp, args, kwargs or {})
        return obj
    def __repr__(self): return f"<class {self.name}>"

class VLInstance:
    def __init__(self, klass):
        self.klass = klass; self.fields = {}

    def get(self, name):
        if name in self.fields: return self.fields[name]
        method = self.klass._find_method(name)
        if method: return method
        raise VeliceError(f"Undefined property '{name}' on {self.klass.name}")

    def set(self, name, val): self.fields[name] = val

    def __repr__(self): return f"<{self.klass.name} instance>"

class _ModuleProxy:
    """Namespace object exposing a module's globals via dot access."""
    def __init__(self, namespace):
        self.__namespace = namespace
    def get(self, name):
        if name in self.__namespace:
            return self.__namespace[name]
        raise VeliceError(f"module has no member '{name}'")
    def names(self):
        return list(self.__namespace.keys())
    def __repr__(self): return "<module>"


class _ArguProxy:
    """`argu` — command-line arguments passed to the script.

        argu.0            -> first argument
        argu.1            -> second argument
        argu.len          -> number of arguments
        argu              -> array of all arguments
        for x in argu     -> iterate over arguments
    """
    def __init__(self, argv):
        self._argv = argv

    def get(self, name):
        if name == "len":
            return len(self._argv)
        try:
            idx = int(name)
        except ValueError:
            raise VeliceError(f"argu: no member '{name}'")
        if idx < 0 or idx >= len(self._argv):
            raise VeliceError(f"argu: index {idx} out of range (0..{len(self._argv) - 1})")
        return self._argv[idx]

    def __iter__(self): return iter(self._argv)
    def __getitem__(self, idx): return self._argv[idx]
    def __len__(self): return len(self._argv)
    def __str__(self): return str(self._argv)
    def __repr__(self): return f"<argu {self._argv}>"


class _UserProxy:
    """`User` — information about the current operating-system user.

        "${User.name}"   -> login name (e.g. "ray")
        User.home        -> home directory
        User.id          -> numeric uid
        User.gid         -> numeric gid
        User.group       -> primary group name
        User.shell       -> login shell
        User.full        -> full (display) name
    """
    def __init__(self):
        import getpass
        self._name = getpass.getuser()
        try:
            import pwd
            pw = pwd.getpwuid(os.getuid())
            self._home, self._uid, self._gid = pw.pw_dir, pw.pw_uid, pw.pw_gid
            self._shell, self._full = pw.pw_shell, pw.pw_gecos
            try:
                import grp
                self._group = grp.getgrgid(pw.pw_gid).gr_name
            except Exception:
                self._group = str(pw.pw_gid)
        except Exception:
            self._home = os.path.expanduser("~")
            self._uid, self._gid = os.getuid(), os.getgid()
            self._shell = os.environ.get("SHELL", "")
            self._full = ""

    def get(self, name):
        mapping = {
            "name": self._name,
            "home": self._home,
            "id": self._uid,
            "gid": self._gid,
            "group": self._group,
            "shell": self._shell,
            "full": self._full,
        }
        if name in mapping:
            return mapping[name]
        raise VeliceError(f"User: no such member '{name}'")

    def __str__(self): return self._name
    def __repr__(self): return f"<User {self._name}>"


def _vl_inbetween(s, delims):
    """Extract the substring between the first and last delimiter characters.

        "print(\"Hello\")".inbetween("\"\"")  -> "Hello"
        "foo(bar)baz".inbetween("()")         -> "bar"
    """
    if not delims:
        return s
    start = delims[0]
    end = delims[-1]
    i = s.find(start)
    if i < 0:
        return ""
    j = s.rfind(end)
    if j <= i + len(start) - 1:
        return ""
    return s[i + len(start):j]


class _FileLines:
    """Line reader returned by ``readFileLines``.

        lines = readFileLines("code.txt")
        while lines.hasMore() {
            line = lines.currentLine()   # next line (nil when exhausted)
            ...
        }
    """
    def __init__(self, lines):
        self._lines = list(lines)
        self._pos = 0

    def current(self):
        """The line at the cursor, without advancing (nil past the end)."""
        if self._pos < len(self._lines):
            return self._lines[self._pos]
        return None

    def currentLine(self):
        """Return the next line and advance the cursor (nil when exhausted)."""
        if self._pos < len(self._lines):
            line = self._lines[self._pos]
            self._pos += 1
            return line
        return None

    def hasMore(self):
        return self._pos < len(self._lines)

    def reset(self):
        self._pos = 0

    def position(self):
        return self._pos

    def __len__(self): return len(self._lines)
    def __getitem__(self, idx): return self._lines[idx]
    def __iter__(self): return iter(self._lines)
    def __str__(self): return str(self._lines)
    def __repr__(self): return f"<filelines {len(self._lines)} lines>"


def _vl_read_file(args):
    if not args:
        return None
    path = str(args[0])
    if not os.path.isfile(path):
        return None
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            return f.read()
    except Exception:
        return None


def _vl_write_file(args):
    if not args:
        return False
    path = str(args[0])
    content = str(args[1]) if len(args) > 1 else ""
    try:
        d = os.path.dirname(os.path.abspath(path))
        if d and not os.path.isdir(d):
            os.makedirs(d, exist_ok=True)
        tmp = path + ".tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            f.write(content)
        os.replace(tmp, path)
        return True
    except Exception:
        return False


def _vl_append_file(args):
    if not args:
        return False
    path = str(args[0])
    content = str(args[1]) if len(args) > 1 else ""
    try:
        with open(path, "a", encoding="utf-8") as f:
            f.write(content)
        return True
    except Exception:
        return False


def _vl_list_dir(args):
    if not args or not os.path.isdir(str(args[0])):
        return []
    try:
        return sorted(os.listdir(str(args[0])))
    except Exception:
        return []


def _vl_make_dir(args):
    if not args:
        return False
    try:
        os.makedirs(str(args[0]), exist_ok=True)
        return True
    except Exception:
        return False


def _vl_remove_file(args):
    if not args:
        return False
    path = str(args[0])
    try:
        if os.path.isdir(path) and not os.path.islink(path):
            os.rmdir(path)
        elif os.path.exists(path):
            os.remove(path)
        else:
            return False
        return True
    except Exception:
        return False


def _vl_copy_file(args):
    if len(args) < 2:
        return False
    try:
        import shutil
        shutil.copyfile(str(args[0]), str(args[1]))
        return True
    except Exception:
        return False


def _vl_join_path(args):
    return os.path.join(*[str(a) for a in args])


def _vl_read_json(args):
    text = _vl_read_file(args)
    if text is None:
        return None
    try:
        return json.loads(text)
    except Exception:
        return None


def _vl_write_json(args):
    if not args:
        return False
    return _vl_write_file([str(args[0]), json.dumps(args[1] if len(args) > 1 else {},
                                                     indent=2, ensure_ascii=False)])


def _vl_run_shell(interp, args, kwargs):
    if not args:
        return {"code": -2, "stdout": "", "stderr": "no command"}
    cmd = interp._to_str(args[0])
    timeout = float(args[1]) if len(args) > 1 else 60.0
    env_map = args[2] if len(args) > 2 and isinstance(args[2], dict) else None
    env = dict(os.environ)
    if env_map:
        for k, v in env_map.items():
            env[str(k)] = interp._to_str(v)
    try:
        r = subprocess.run(cmd, shell=True, capture_output=True, text=True,
                           timeout=timeout, env=env)
        return {"code": r.returncode, "stdout": r.stdout, "stderr": r.stderr}
    except subprocess.TimeoutExpired:
        return {"code": -1, "stdout": "", "stderr": f"timeout after {timeout}s"}
    except Exception as e:
        return {"code": -2, "stdout": "", "stderr": str(e)}


def _vl_http_request(interp, args, kwargs):
    import urllib.request
    import urllib.error
    if not args:
        return {"status": 0, "headers": {}, "body": "no url"}
    method = str(args[0]).upper()
    url = interp._to_str(args[1])
    headers = args[2] if len(args) > 2 and isinstance(args[2], dict) else {}
    body = interp._to_str(args[3]) if len(args) > 3 and args[3] is not None else None
    timeout = float(args[4]) if len(args) > 4 else 30.0
    req = urllib.request.Request(url, method=method)
    for k, v in headers.items():
        req.add_header(str(k), interp._to_str(v))
    if body is not None:
        req.data = body.encode("utf-8")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return {"status": r.status,
                    "headers": {k: v for k, v in r.headers.items()},
                    "body": r.read().decode("utf-8", "replace")}
    except urllib.error.HTTPError as e:
        return {"status": e.code,
                "headers": {k: v for k, v in e.headers.items()},
                "body": e.read().decode("utf-8", "replace")}
    except Exception as e:
        return {"status": 0, "headers": {}, "body": str(e)}


def _vl_parse_source(args):
    from velice.astdump import parse_source as _parse
    from velice.parser import ParseError
    if not args:
        return {"kind": "program", "statements": []}
    try:
        return _parse(str(args[0]))
    except ParseError as e:
        raise VeliceError(str(e))


def _read_file_lines(path):
    if not path:
        raise VeliceError("readFileLines: expected a file path")
    if not os.path.exists(path):
        raise VeliceError(f"readFileLines: file not found: {path}")
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        content = f.read()
    lines = content.split("\n")
    if lines and lines[-1] == "":
        lines.pop()
    return _FileLines(l.rstrip("\r") for l in lines)


def _vl_rand(args):
    """Unified random helper for the ``rand`` builtin.

        rand()              -> float in [0, 1)
        rand(n)             -> int in [0, n]         (n is an int)
                               float in [0, n]       (n is a float)
                               a random element      (n is a list or string)
        rand(a, b)          -> int in [a, b]         (a and b ints, inclusive)
                               float in [a, b]       (a or b is a float)
                               random.choice([a, b]) (anything else,
                                                      e.g. rand(true, false))
    """
    if not args:
        return random.random()
    if len(args) == 1:
        x = args[0]
        if isinstance(x, (list, str)):
            return random.choice(x) if len(x) else None
        if isinstance(x, bool):
            return random.choice([True, False])
        if isinstance(x, int):
            return random.randint(0, x)
        if isinstance(x, float):
            return random.uniform(0.0, x)
        return random.choice([x])
    a, b = args[0], args[1]
    if isinstance(a, bool) and isinstance(b, bool):
        return random.choice([a, b])
    if isinstance(a, int) and isinstance(b, int):
        return random.randint(a, b)
    if isinstance(a, (int, float)) and isinstance(b, (int, float)):
        return random.uniform(a, b)
    return random.choice([a, b])


def _vl_reduce(interp, args):
    if len(args) < 2 or not isinstance(args[1], VLFunction):
        raise VeliceError("reduce: expected an array and a function")
    values = list(args[0])
    if len(args) >= 3:
        accumulator = args[2]
    elif values:
        accumulator = values.pop(0)
    else:
        raise VeliceError("reduce: empty array needs an initial value")
    for value in values:
        accumulator = args[1].call(interp, [accumulator, value])
    return accumulator

# ── Interpreter ──────────────────────────────────────────────────────────
class Interpreter:
    def __init__(self, module_loader=None, argv=None):
        self.globals = Env()
        self.module_loader = module_loader
        self.argv = list(argv or [])
        self.source_path = None
        self._discard_result = False
        self._pending_discard = None
        self._setup_builtins()

    def _setup_builtins(self):
        builtins = {
            "print": lambda interp, a, kw: print(*[interp._to_str(x) for x in a], **({"end": kw.get("end", "\n")})),
            "println": lambda interp, a, kw: print(*[interp._to_str(x) for x in a]),
            "len": lambda interp, a, kw: len(a[0]) if a else 0,
            "str": lambda interp, a, kw: interp._to_str(a[0]) if a else "",
            "int": lambda interp, a, kw: int(a[0]) if a else 0,
            "float": lambda interp, a, kw: float(a[0]) if a else 0.0,
            "bool": lambda interp, a, kw: bool(a[0]) if a else False,
            "abs": lambda interp, a, kw: abs(a[0]) if a else 0,
            "min": lambda interp, a, kw: min(a[0]) if a and isinstance(a[0], list) else (min(a) if a else 0),
            "max": lambda interp, a, kw: max(a[0]) if a and isinstance(a[0], list) else (max(a) if a else 0),
            "sum": lambda interp, a, kw: sum(a[0]) if a else 0,
            "range": lambda interp, a, kw: list(range(*[int(x) for x in a])),
            "enumerate": lambda interp, a, kw: list(enumerate(a[0])) if a else [],
            "zip": lambda interp, a, kw: list(zip(*[x for x in a])),
            "map": lambda interp, a, kw: list(map(lambda x: a[1].call(interp, [x]) if isinstance(a[1], VLFunction) else x, a[0])) if len(a) >= 2 else [],
            "filter": lambda interp, a, kw: [x for x in a[0] if a[1].call(interp, [x])] if len(a) >= 2 else [],
            "reduce": lambda interp, a, kw: _vl_reduce(interp, a),
            "sorted": lambda interp, a, kw: sorted(a[0], key=lambda x: a[1].call(interp, [x]) if len(a) > 1 and isinstance(a[1], VLFunction) else x) if a else [],
            "reversed": lambda interp, a, kw: list(reversed(a[0])) if a else [],
            "type": lambda interp, a, kw: type(a[0]).__name__ if a else "none",
            "typeof": lambda interp, a, kw: interp._vl_type(a[0]) if a else "none",
            "input": lambda interp, a, kw: input(interp._to_str(a[0]) if a else ""),
            "assert": lambda interp, a, kw: None if (a and a[0]) else (_ for _ in ()).throw(VeliceError("Assertion failed" + (f": {interp._to_str(a[1])}" if len(a) > 1 else ""))),
            "panic": lambda interp, a, kw: (_ for _ in ()).throw(VeliceError(interp._to_str(a[0]) if a else "panic")),
            "chr": lambda interp, a, kw: chr(int(a[0])) if a else "",
            "ord": lambda interp, a, kw: ord(str(a[0])) if a else 0,
            "hex": lambda interp, a, kw: hex(int(a[0])) if a else "0x0",
            "oct": lambda interp, a, kw: oct(int(a[0])) if a else "0o0",
            "bin": lambda interp, a, kw: bin(int(a[0])) if a else "0b0",
            "append": lambda interp, a, kw: (a[0].append(a[1]) if len(a) >= 2 else None) or a[0],
            "push": lambda interp, a, kw: (a[0].append(a[1]) if len(a) >= 2 else None),
            "pop": lambda interp, a, kw: a[0].pop() if a else None,
            "insert": lambda interp, a, kw: a[0].insert(int(a[1]), a[2]) if len(a) >= 3 else None,
            "remove": lambda interp, a, kw: a[0].remove(a[1]) if len(a) >= 2 else None,
            "contains": lambda interp, a, kw: a[1] in a[0] if len(a) >= 2 else False,
            "keys": lambda interp, a, kw: list(a[0].keys()) if a and isinstance(a[0], dict) else [],
            "values": lambda interp, a, kw: list(a[0].values()) if a and isinstance(a[0], dict) else [],
            "items": lambda interp, a, kw: list(a[0].items()) if a and isinstance(a[0], dict) else [],
            "join": lambda interp, a, kw: interp._to_str(a[1]).join([interp._to_str(x) for x in a[0]]) if len(a) >= 2 else "",
            "split": lambda interp, a, kw: interp._to_str(a[0]).split(interp._to_str(a[1]) if len(a) > 1 else " ") if a else [],
            "replace": lambda interp, a, kw: interp._to_str(a[0]).replace(interp._to_str(a[1]), interp._to_str(a[2])) if len(a) >= 3 else "",
            "trim": lambda interp, a, kw: interp._to_str(a[0]).strip() if a else "",
            "lower": lambda interp, a, kw: interp._to_str(a[0]).lower() if a else "",
            "upper": lambda interp, a, kw: interp._to_str(a[0]).upper() if a else "",
            "parse_int": lambda interp, a, kw: int(interp._to_str(a[0])) if a else 0,
            "parse_float": lambda interp, a, kw: float(interp._to_str(a[0])) if a else 0.0,
            "to_json": lambda interp, a, kw: json.dumps(a[0]) if a else "null",
            "from_json": lambda interp, a, kw: json.loads(interp._to_str(a[0])) if a else None,
            "json_stringify": lambda interp, a, kw: json.dumps(a[0]) if a else "null",
            "json_parse": lambda interp, a, kw: json.loads(interp._to_str(a[0])) if a else None,
            "floor": lambda interp, a, kw: math.floor(a[0]) if a else 0,
            "ceil": lambda interp, a, kw: math.ceil(a[0]) if a else 0,
            "round": lambda interp, a, kw: round(a[0], int(a[1])) if len(a) > 1 else round(a[0]) if a else 0,
            "time": lambda interp, a, kw: time.time(),
            "sleep": lambda interp, a, kw: time.sleep(float(a[0]) if a else 0),
            "random": lambda interp, a, kw: random.random(),
            "rand": lambda interp, a, kw: _vl_rand(a),
            "rand_int": lambda interp, a, kw: random.randint(int(a[0]), int(a[1])) if len(a) >= 2 else random.randint(0, 100),
            "sha256": lambda interp, a, kw: hashlib.sha256(interp._to_str(a[0]).encode()).hexdigest() if a else "",
            "now": lambda interp, a, kw: datetime.datetime.now().isoformat(),
            "exit": lambda interp, a, kw: sys.exit(int(a[0]) if a else 0),
            "clone": lambda interp, a, kw: list(a[0]) if a and isinstance(a[0], list) else dict(a[0]) if a and isinstance(a[0], dict) else a[0],
            "readFileLines": lambda interp, a, kw: _read_file_lines(interp._to_str(a[0]) if a else ""),
            "argu": _ArguProxy(self.argv),
            "nil": None,
            "User": _UserProxy(),
            # ── file system ─────────────────────────────────────────────
            "read_file": lambda interp, a, kw: _vl_read_file(a),
            "write_file": lambda interp, a, kw: _vl_write_file(a),
            "append_file": lambda interp, a, kw: _vl_append_file(a),
            "file_exists": lambda interp, a, kw: bool(a) and os.path.exists(str(a[0])),
            "is_dir": lambda interp, a, kw: bool(a) and os.path.isdir(str(a[0])),
            "make_dir": lambda interp, a, kw: _vl_make_dir(a),
            "list_dir": lambda interp, a, kw: _vl_list_dir(a),
            "remove_file": lambda interp, a, kw: _vl_remove_file(a),
            "copy_file": lambda interp, a, kw: _vl_copy_file(a),
            "join_path": lambda interp, a, kw: _vl_join_path(a),
            "dirname": lambda interp, a, kw: os.path.dirname(str(a[0])) if a else "",
            "basename": lambda interp, a, kw: os.path.basename(str(a[0])) if a else "",
            "extname": lambda interp, a, kw: os.path.splitext(str(a[0]))[1] if a else "",
            "absolute_path": lambda interp, a, kw: os.path.abspath(str(a[0])) if a else "",
            "current_dir": lambda interp, a, kw: os.getcwd(),
            "home_dir": lambda interp, a, kw: os.path.expanduser("~"),
            "env": lambda interp, a, kw: os.environ.get(interp._to_str(a[0])) if a else None,
            "random_id": lambda interp, a, kw: (interp._to_str(a[0]) if a else "id") + "_" + uuid.uuid4().hex[:10],
            "python_path": lambda interp, a, kw: sys.executable,
            "temp_dir": lambda interp, a, kw: tempfile.gettempdir(),
            "script_dir": lambda interp, a, kw: (
                os.path.dirname(os.path.abspath(getattr(interp, "source_path", None)))
                if getattr(interp, "source_path", None) else os.getcwd()),
            # ── JSON helpers ────────────────────────────────────────────
            "json_pretty": lambda interp, a, kw: json.dumps(a[0], indent=2, ensure_ascii=False) if a else "null",
            "read_json": lambda interp, a, kw: _vl_read_json(a),
            "write_json": lambda interp, a, kw: _vl_write_json(a),
            # ── process / network / parsing ─────────────────────────────
            "run_shell": lambda interp, a, kw: _vl_run_shell(interp, a, kw),
            "http_request": lambda interp, a, kw: _vl_http_request(interp, a, kw),
            "parse_source": lambda interp, a, kw: _vl_parse_source(a),
        }
        for name, val in builtins.items():
            if callable(val) and not isinstance(val, VLFunction):
                self.globals.define(name, VLFunction(name, [], None, self.globals, is_native=True, native_fn=val))
            else:
                self.globals.define(name, val)

    def _eval_interp_str(self, s, env):
        parts = []
        i = 0
        while i < len(s):
            if s[i:i+2] == "${":
                depth = 1; j = i + 2; expr = ""
                while j < len(s) and depth > 0:
                    if s[j:j+2] == "${": depth += 1
                    elif s[j] == "}": depth -= 1
                    if depth > 0: expr += s[j]
                    j += 1
                try:
                    toks = Lexer(expr).tokenize()
                    ast = Parser(toks, expr).parse()
                    val = self.eval(ast, env)
                    parts.append(self._to_str(val))
                except Exception as e:
                    parts.append(str(e))
                i = j
            else:
                parts.append(s[i]); i += 1
        return "".join(parts)

    def _to_str(self, val):
        if val is None: return "nil"
        if isinstance(val, bool): return "true" if val else "false"
        if isinstance(val, list): return "[" + ", ".join(self._to_str(x) for x in val) + "]"
        if isinstance(val, dict): return "{" + ", ".join(f"{self._to_str(k)}: {self._to_str(v)}" for k, v in val.items()) + "}"
        if isinstance(val, _ArguProxy): return "[" + ", ".join(self._to_str(x) for x in val) + "]"
        if isinstance(val, _FileLines): return "[" + ", ".join(self._to_str(x) for x in val._lines) + "]"
        return str(val)

    def _vl_type(self, val):
        if val is None: return "nil"
        if isinstance(val, bool): return "bool"
        if isinstance(val, int): return "int"
        if isinstance(val, float): return "float"
        if isinstance(val, str): return "string"
        if isinstance(val, list): return "array"
        if isinstance(val, dict): return "map"
        if isinstance(val, VLFunction): return "function"
        if isinstance(val, VLClass): return "class"
        if isinstance(val, VLInstance): return val.klass.name
        if isinstance(val, _ArguProxy): return "argu"
        if isinstance(val, _FileLines): return "filelines"
        return "unknown"

    def run(self, ast_node: A.Node, env: Optional[Env] = None, raise_errors: bool = False):
        if env is None: env = self.globals
        try:
            return self.eval(ast_node, env)
        except VeliceError as e:
            if raise_errors: raise
            print(f"\033[91mRuntime Error: {e}\033[0m", file=sys.stderr)
            return None
        except ReturnSignal as r:
            return r.val

    def eval(self, node, env):
        if isinstance(node, A.ParenExpr): return self.eval(node.inner, env)
        if isinstance(node, A.ThunkExpr): return self._eval_thunk(node, env)
        if isinstance(node, A.Literal):
            if isinstance(node.value, str) and "${" in node.value and node.kind == "string":
                return self._eval_interp_str(node.value, env)
            return node.value
        if isinstance(node, A.Identifier): return env.get(node.name)
        if isinstance(node, A.BinaryOp): return self._eval_binop(node, env)
        if isinstance(node, A.UnaryOp): return self._eval_unaryop(node, env)
        if isinstance(node, A.Assignment): return self._eval_assignment(node, env)
        if isinstance(node, A.Call): return self._eval_call(node, env)
        if isinstance(node, A.DotAccess): return self._eval_dot(node, env)
        if isinstance(node, A.BlockCall): return self._eval_block_call(node, env)
        if isinstance(node, A.IndexAccess): return self._eval_index(node, env)
        if isinstance(node, A.SliceAccess): return self._eval_slice(node, env)
        if isinstance(node, A.TernaryExpr): return self.eval(node.then, env) if self.eval(node.cond, env) else self.eval(node.else_, env)
        if isinstance(node, A.NullCoalesce):
            l = self.eval(node.left, env); return l if l is not None else self.eval(node.right, env)
        if isinstance(node, A.ArrayLit): return [self.eval(e, env) for e in node.elems]
        if isinstance(node, A.MapLit): return {self.eval(k, env): self.eval(v, env) for k, v in zip(node.keys, node.vals)}
        if isinstance(node, A.TupleLit): return tuple(self.eval(e, env) for e in node.elems)
        if isinstance(node, A.InterpString): return self._eval_interp(node, env)
        if isinstance(node, A.PipeExpr): return self._eval_pipe(node, env)
        if isinstance(node, A.LambdaExpr): return self._eval_lambda(node, env)
        if isinstance(node, A.LetStmt): return self._eval_let(node, env)
        if isinstance(node, A.ConstStmt): return self._eval_const(node, env)
        if isinstance(node, A.ExprStmt):
            expr = node.expr
            if isinstance(expr, A.Call):
                self._pending_discard = expr
            try:
                return self.eval(expr, env)
            finally:
                self._pending_discard = None
        if isinstance(node, A.ReturnStmt): raise ReturnSignal(self.eval(node.value, env) if node.value else None)
        if isinstance(node, A.BreakStmt): raise BreakSignal(self.eval(node.value, env) if node.value else None)
        if isinstance(node, A.ContinueStmt): raise ContinueSignal()
        if isinstance(node, A.Block): return self._eval_block(node, env)
        if isinstance(node, A.IfStmt): return self._eval_if(node, env)
        if isinstance(node, A.WhileStmt): return self._eval_while(node, env)
        if isinstance(node, A.ForInStmt): return self._eval_for_in(node, env)
        if isinstance(node, A.LoopStmt): return self._eval_loop(node, env)
        if isinstance(node, A.MatchStmt): return self._eval_match(node, env)
        if isinstance(node, A.DeferStmt):
            env._defers = getattr(env, '_defers', []); env._defers.append(node.body); return None
        if isinstance(node, A.ThrowStmt): raise VeliceError(self._to_str(self.eval(node.expr, env)))
        if isinstance(node, A.TryStmt): return self._eval_try(node, env)
        if isinstance(node, A.AssertStmt): return self._eval_assert(node, env)
        if isinstance(node, A.ImportStmt): return self._eval_import(node, env)
        if isinstance(node, A.WindowDecl): return self._eval_window_decl(node, env)
        if isinstance(node, A.RunStmt): return self._eval_run_stmt(node, env)
        if isinstance(node, (A.FnDecl, A.LambdaExpr)): return self._eval_fn_decl(node, env)
        if isinstance(node, A.ClassDecl): return self._eval_class(node, env)
        if isinstance(node, A.StructDecl): return self._eval_struct(node, env)
        if isinstance(node, A.EnumDecl): return self._eval_enum(node, env)
        if isinstance(node, A.ImplDecl): return self._eval_impl(node, env)
        if isinstance(node, A.Program):
            result = None
            for s in node.stmts: result = self.eval(s, env)
            return result
        return None

    def _eval_binop(self, n, env):
        if n.op == "and":
            l = self.eval(n.left, env); return self.eval(n.right, env) if l else l
        if n.op == "or":
            l = self.eval(n.left, env); return l if l else self.eval(n.right, env)
        l = self.eval(n.left, env); r = self.eval(n.right, env)
        ops = {"+":lambda a,b:a+b,"-":lambda a,b:a-b,"*":lambda a,b:a*b,"%":lambda a,b:a%b,"**":lambda a,b:a**b,
            "==":lambda a,b:a==b,"!=":lambda a,b:a!=b,"<":lambda a,b:a<b,">":lambda a,b:a>b,"<=":lambda a,b:a<=b,">=":lambda a,b:a>=b,
            "&":lambda a,b:a&b,"|":lambda a,b:a|b,"^":lambda a,b:a^b}
        if n.op == "/": return l / r if isinstance(r, float) or isinstance(l, float) else l // r if r != 0 else (_ for _ in ()).throw(VeliceError("Division by zero"))
        if n.op in ops: return ops[n.op](l, r)
        if n.op == "..": return list(range(int(l), int(r) + 1))
        raise VeliceError(f"Unknown operator '{n.op}'")

    def _eval_unaryop(self, n, env):
        val = self.eval(n.operand, env)
        if n.op == "-": return -val
        if n.op == "+": return +val
        if n.op == "!": return not val
        if n.op == "not": return not val
        return val

    def _eval_assignment(self, n, env):
        val = self.eval(n.value, env)
        if isinstance(n.target, A.Identifier):
            if n.op: val = self._binop_val(env.get(n.target.name), val, n.op)
            if env.resolve(n.target.name) is None:
                env.define(n.target.name, val, mutable=True)
            else:
                env.update(n.target.name, val)
            return val
        if isinstance(n.target, A.DotAccess):
            obj = self.eval(n.target.obj, env)
            if isinstance(obj, VLInstance): obj.set(n.target.prop, val)
            elif getattr(obj, "_velice_widget", False): obj.set(n.target.prop, val)
            elif isinstance(obj, dict): obj[n.target.prop] = val
            elif hasattr(obj, "set") and not isinstance(obj, (list, str, tuple, int, float, bool)):
                obj.set(n.target.prop, val)
            return val
        if isinstance(n.target, A.IndexAccess):
            obj = self.eval(n.target.obj, env); idx = self.eval(n.target.index, env)
            if isinstance(obj, list): obj[int(idx)] = val
            elif isinstance(obj, dict): obj[idx] = val
            return val
        return val

    def _binop_val(self, old, val, op):
        if op == "+=": return old + val
        if op == "-=": return old - val
        if op == "*=": return old * val
        if op == "/=": return old / val
        if op == "%=": return old % val
        return val

    def _eval_call(self, n, env):
        if isinstance(n.func, A.Identifier) and n.func.name in ("Some", "Some"):
            return self.eval(n.args[0], env) if n.args else None
        self._discard_result = n is getattr(self, "_pending_discard", None)
        try:
            callee = self.eval(n.func, env)
            args = [self.eval(a, env) for a in n.args]
            kwargs = {k: self.eval(v, env) for k, v in n.kwargs.items()}
            if isinstance(callee, VLFunction): return callee.call(self, args, kwargs)
            if isinstance(callee, VLClass): return callee.call(self, args, kwargs)
            if callable(callee): return callee(*args, **kwargs)
            if isinstance(callee, (list, str)):
                if args: return callee[int(args[0])]
                return callee
            raise VeliceError(f"Cannot call {type(callee).__name__}")
        finally:
            self._discard_result = False

    def _eval_dot(self, n, env):
        obj = self.eval(n.obj, env)
        if isinstance(obj, VLInstance):
            val = obj.get(n.prop)
            if isinstance(val, VLFunction):
                bound = VLFunction(val.name, val.params, val.body, val.closure, is_method=val.is_method, is_native=val.is_native, native_fn=val.native_fn)
                bound._bound_to = obj
                return bound
            return val
        if isinstance(obj, _ArguProxy): return obj.get(n.prop)
        if isinstance(obj, _ModuleProxy): return obj.get(n.prop)
        if isinstance(obj, _UserProxy): return obj.get(n.prop)
        if isinstance(obj, dict): return obj.get(n.prop)
        if isinstance(obj, _FileLines):
            if n.prop == "len": return len(obj)
            methods = {
                "contains": lambda interp, a, kw: any(interp._to_str(a[0]) in line for line in obj) if a else False,
                "currentLine": lambda interp, a, kw: obj.currentLine(),
                "current": lambda interp, a, kw: obj.current(),
                "hasMore": lambda interp, a, kw: obj.hasMore(),
                "reset": lambda interp, a, kw: obj.reset(),
                "position": lambda interp, a, kw: obj.position(),
            }
            if n.prop in methods:
                return VLFunction(n.prop, [], None, env, is_native=True, native_fn=methods[n.prop])
            raise VeliceError(f"filelines: no member '{n.prop}'")
        if isinstance(obj, str):
            if n.prop == "len": return len(obj)
            methods = {
                "contains": lambda interp, a, kw: interp._to_str(a[0]) in obj if a else False,
                "starts": lambda interp, a, kw: obj.startswith(interp._to_str(a[0])) if a else False,
                "ends": lambda interp, a, kw: obj.endswith(interp._to_str(a[0])) if a else False,
                "inbetween": lambda interp, a, kw: _vl_inbetween(obj, interp._to_str(a[0])) if a else "",
            }
            if n.prop in methods:
                return VLFunction(n.prop, [], None, env, is_native=True, native_fn=methods[n.prop])
            return getattr(obj, n.prop, None)
        if isinstance(obj, (list, tuple)):
            if n.prop == "len": return len(obj)
            if n.prop == "push" or n.prop == "append":
                return VLFunction(n.prop, [A.LetStmt(0,0,"item")], None, env, is_native=True,
                    native_fn=lambda i,a,k: (obj.append(a[0]) if a else None) or obj)
        if isinstance(obj, VLClass) and hasattr(obj, n.prop): return getattr(obj, n.prop)
        if getattr(obj, "_velice_widget", False):
            if hasattr(obj, n.prop):
                return getattr(obj, n.prop)
            return obj.get(n.prop)
        if hasattr(obj, n.prop): return getattr(obj, n.prop)
        raise VeliceError(f"Undefined property '{n.prop}'")

    def _eval_block_call(self, n, env):
        """`obj.prop { ... }` — bind a block as a widget event handler."""
        obj = self.eval(n.obj, env)
        binder = getattr(obj, "bind", None)
        if binder is None:
            raise VeliceError(f"Cannot bind event '{n.prop}' on {type(obj).__name__}")
        body = n.body if isinstance(n.body, A.Block) else A.Block(n.line, n.col, [n.body])

        def cb(info=None):
            handler_env = Env(env)
            handler_env.define("event", info or {})
            return self.exec(body, handler_env)

        binder(n.prop, cb)
        return obj

    def _eval_window_decl(self, n, env):
        """Declarative GUI DSL: `window Name { ... }`."""
        try:
            from velice.gui import bridge
        except ImportError as e:
            raise VeliceError(f"GUI framework unavailable: {e}")
        builder = bridge.Builder(self, env)
        return builder.build_window(n)

    def _eval_run_stmt(self, n, env):
        """`run Name` — show a declared window."""
        try:
            from velice.gui.runtime import app as gui_app
        except ImportError as e:
            raise VeliceError(f"GUI framework unavailable: {e}")
        win = gui_app.get_window(n.name)
        if win is None:
            try:
                win = env.get(n.name)
            except VeliceError:
                win = None
        if win is None:
            raise VeliceError(f"GUI: no window named '{n.name}'")
        show = getattr(win, "show", None)
        if show is not None:
            show()
        return win

    def _eval_index(self, n, env):
        obj = self.eval(n.obj, env); idx = self.eval(n.index, env)
        if isinstance(obj, _ArguProxy): return obj[int(idx)]
        if isinstance(obj, (list, tuple, _FileLines)): return obj[int(idx)]
        if isinstance(obj, dict): return obj.get(idx)
        if isinstance(obj, str): return obj[int(idx)]
        raise VeliceError("Cannot index this value")

    def _eval_slice(self, n, env):
        obj = self.eval(n.obj, env)
        s = self.eval(n.start, env) if n.start else None
        e = self.eval(n.end, env) if n.end else None
        if not isinstance(obj, (list, str)):
            return obj
        start = int(s) if s is not None else 0
        end = int(e) if e is not None else len(obj)
        return obj[start:end]

    def _eval_interp(self, n, env):
        parts = []
        for p in n.parts:
            if isinstance(p, str): parts.append(p)
            else: parts.append(self._to_str(self.eval(p, env)))
        return "".join(parts)

    def _eval_pipe(self, n, env):
        val = self.eval(n.left, env)
        if isinstance(n.right, A.Call):
            args = [val] + [self.eval(a, env) for a in n.right.args[1:]]
            callee = self.eval(n.right.func, env)
            if isinstance(callee, VLFunction): return callee.call(self, args)
            if callable(callee): return callee(*args)
        if isinstance(n.right, A.Identifier):
            callee = self.eval(n.right, env)
            if isinstance(callee, VLFunction): return callee.call(self, [val])
        if isinstance(n.right, A.LambdaExpr):
            fn = self._eval_lambda(n.right, env)
            return fn.call(self, [val])
        return val

    def _eval_lambda(self, n, env):
        return VLFunction("<lambda>", n.params, n.body, env)

    def _eval_thunk(self, n, env):
        body = A.Block(n.line, n.col, [A.ReturnStmt(n.line, n.col, n.expr)])
        return VLFunction("", [], body, env)

    def _eval_let(self, n, env):
        val = self.eval(n.value, env) if n.value else None
        env.define(n.name, val, mutable=n.mutable); return val

    def _eval_const(self, n, env):
        val = self.eval(n.value, env) if n.value else None
        env.define(n.name, val, mutable=False); return val

    def _eval_block(self, n, env):
        block_env = Env(env); result = None
        for s in n.stmts:
            result = self.eval(s, block_env)
        defers = getattr(block_env, '_defers', [])
        for d in reversed(defers): self.exec(d, env)
        return result

    def _eval_if(self, n, env):
        if self.eval(n.cond, env): return self.exec(n.then, env)
        for cond, body in n.elifs:
            if self.eval(cond, env): return self.exec(body, env)
        if n.else_: return self.exec(n.else_, env)
        return None

    def _eval_while(self, n, env):
        result = None
        while self.eval(n.cond, env):
            try: result = self.exec(n.body, env)
            except BreakSignal: break
            except ContinueSignal: continue
        return result

    def _eval_for_in(self, n, env):
        iterable = self.eval(n.iterable, env)
        result = None
        for item in iterable:
            env.define(n.var, item, mutable=n.mutable)
            try: result = self.exec(n.body, env)
            except BreakSignal: break
            except ContinueSignal: continue
        return result

    def _eval_loop(self, n, env):
        while True:
            try: self.exec(n.body, env)
            except BreakSignal: break
            except ContinueSignal: continue
        return None

    def _eval_match(self, n, env):
        val = self.eval(n.expr, env)
        for arm in n.arms:
            bindings = {}
            if self._match_pattern(arm.pattern, val, env, bindings):
                if arm.guard:
                    guard_env = Env(env)
                    for k, v in bindings.items(): guard_env.define(k, v)
                    if not self.eval(arm.guard, guard_env): continue
                match_env = Env(env)
                for k, v in bindings.items(): match_env.define(k, v)
                return self.eval(arm.body, match_env)
        raise VeliceError("No matching pattern in match expression")

    def _match_pattern(self, pat, val, env, bindings):
        if isinstance(pat, A.WildcardPattern): return True
        if isinstance(pat, A.LitPattern): return pat.value == val
        if isinstance(pat, A.IdentPattern): bindings[pat.name] = val; return True
        if isinstance(pat, A.OrPattern): return any(self._match_pattern(a, val, env, bindings) for a in pat.alts)
        if isinstance(pat, A.ArrayPattern) and isinstance(val, list):
            if len(pat.elems) != len(val): return False
            return all(self._match_pattern(p, v, env, bindings) for p, v in zip(pat.elems, val))
        if isinstance(pat, A.TuplePattern) and isinstance(val, tuple):
            if len(pat.elems) != len(val): return False
            return all(self._match_pattern(p, v, env, bindings) for p, v in zip(pat.elems, val))
        return False

    def _eval_try(self, n, env):
        try:
            return self.exec(n.body, env)
        except VeliceError as e:
            for catch in n.catches:
                catch_env = Env(env)
                if catch.name: catch_env.define(catch.name, str(e))
                return self.exec(catch.body, catch_env)
        finally:
            if n.finally_: self.exec(n.finally_, env)

    def _eval_assert(self, n, env):
        val = self.eval(n.expr, env)
        if not val:
            msg = self._to_str(self.eval(n.msg, env)) if n.msg else "Assertion failed"
            raise VeliceError(msg)

    def _eval_import(self, n, env):
        if n.path == "gui":
            return self._import_gui(n, env)
        if n.path == "cmd":
            return self._import_cmd(n, env)
        loader = self.module_loader
        if loader is None:
            # attempt a default loader based on the current file, if known
            from velice.moduleloader import ModuleLoader
            loader = self.module_loader = ModuleLoader()
        is_file = n.path.endswith(".velice") or n.module_path.endswith(".velice")
        target = n.path if is_file else n.module_path
        mod = loader.load(target, is_file=is_file)
        exposed = loader.execute(mod, self, self.globals)
        exposed = dict(mod["globals"])
        name = n.alias or (target.split(".")[-1].split("/")[-1].replace(".velice", ""))
        if n.items:
            for item in n.items:
                if item in exposed:
                    env.define(item, exposed[item])
            return None
        if n.wildcard:
            for k, v in exposed.items():
                env.define(k, v)
            return None
        env.define(name, _ModuleProxy(exposed))
        return None

    def _import_cmd(self, n, env):
        try:
            from velice import cmd
        except ImportError as e:
            raise VeliceError(f"Command module unavailable: {e}")
        namespace = {}
        for name, obj in cmd.EXPORTS.items():
            namespace[name] = cmd.make_builtin(self, name, obj)
        if n.alias:
            env.define(n.alias, _ModuleProxy(namespace))
        elif n.items:
            for item in n.items:
                if item in namespace:
                    env.define(item, namespace[item])
                else:
                    raise VeliceError(f"cmd: no such member '{item}'")
        elif n.wildcard:
            for name, obj in namespace.items():
                env.define(name, obj)
        else:
            env.define("cmd", _ModuleProxy(namespace))

    def _import_gui(self, n, env):
        try:
            from velice import gui
        except ImportError as e:
            raise VeliceError(f"GUI framework unavailable: {e}")
        namespace = {}
        for name, obj in gui.EXPORTS.items():
            namespace[name] = gui.make_builtin(self, name, obj)
        if n.alias:
            env.define(n.alias, _ModuleProxy(namespace))
        elif n.items:
            for item in n.items:
                if item in namespace:
                    env.define(item, namespace[item])
                else:
                    raise VeliceError(f"GUI: no such member '{item}'")
        elif n.wildcard:
            for name, obj in namespace.items():
                env.define(name, obj)
        else:
            # `import gui` exposes both the module (gui.run) and all
            # constructors/functions directly (Window, Button, ...).
            env.define("gui", _ModuleProxy(namespace))
            for name, obj in namespace.items():
                env.define(name, obj)

    def _eval_fn_decl(self, n, env):
        fn = VLFunction(n.name, n.params, n.body, env)
        env.define(n.name, fn); return fn

    def _eval_class(self, n, env):
        methods = {}
        for m in n.members:
            if isinstance(m, A.FnDecl):
                fn = VLFunction(m.name, m.params, m.body, env, is_method=True)
                methods[m.name] = fn
        sup = None
        if n.superclass:
            name = None
            if isinstance(n.superclass, A.Identifier): name = n.superclass.name
            elif isinstance(n.superclass, A.TypeName): name = n.superclass.name
            if name: sup = env.get(name)
            if not isinstance(sup, VLClass): sup = None
        klass = VLClass(n.name, methods, sup)
        env.define(n.name, klass); return klass

    def _eval_struct(self, n, env):
        methods = {}
        for m in n.methods:
            if isinstance(m, A.FnDecl):
                fn = VLFunction(m.name, m.params, m.body, env, is_method=True)
                methods[m.name] = fn
        def struct_init(interp, args, kwargs):
            obj = VLInstance(VLClass(n.name, methods))
            for i, f in enumerate(n.fields):
                fname = f.name if hasattr(f, 'name') else f
                if i < len(args): obj.fields[fname] = args[i]
            for k, v in (kwargs or {}).items(): obj.fields[k] = v
            return obj
        klass = VLClass(n.name, methods)
        init_fn = VLFunction(n.name, n.fields, None, env, is_native=True, native_fn=struct_init)
        env.define(n.name, init_fn); return init_fn

    def _eval_enum(self, n, env):
        variants = {}
        for v in n.variants:
            variants[v.name] = len(variants)
        env.define(n.name, {"variants": variants, "type": "enum"}); return variants

    def _eval_impl(self, n, env):
        target = env.get(n.target.name) if isinstance(n.target, A.Identifier) else None
        if isinstance(target, VLClass):
            for m in n.methods:
                fn = VLFunction(m.name, m.params, m.body, env, is_method=True)
                target.methods[m.name] = fn
        return None

    def exec(self, node, env):
        if isinstance(node, A.Block): return self._eval_block(node, env)
        return self.eval(node, env)
