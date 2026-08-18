"""Module loading for Velice: resolve `import` statements to source files."""
import os
import sys

from velice.lexer import Lexer
from velice.parser import Parser
from velice import ast_nodes as A
from velice.project import Project, ManifestError
from velice.packagemanager import CACHE_DIR, is_cached, cache_pkg_path
from velice.interpreter import Env

BUILTIN_MODULES = ("gui", "stdlib", "json", "math", "time", "random", "io", "net")


class ModuleNotFoundError(Exception):
    pass


class ModuleLoader:
    def __init__(self, project_root=None, source_path=None):
        self.project_root = os.path.abspath(project_root) if project_root else None
        self.source_path = os.path.abspath(source_path) if source_path else None
        self.loaded = {}

    def _search_roots(self):
        roots = []
        if self.project_root:
            p = Project(self.project_root)
            roots.append(os.path.join(self.project_root, "src"))
            roots.append(p.packages_dir())
        roots.append(os.path.join(CACHE_DIR))
        if self.source_path:
            roots.append(os.path.dirname(self.source_path))
        return roots

    def resolve(self, module_path, is_file=False):
        """Return an absolute .velice path for `module_path`, or raise."""
        if is_file or module_path.endswith(".velice"):
            path = module_path
            if not os.path.isabs(path) and self.source_path:
                path = os.path.join(os.path.dirname(self.source_path), path)
            path = os.path.abspath(path)
            if os.path.exists(path):
                return path
            raise ModuleNotFoundError(f"module file not found: {module_path}")

        parts = module_path.split(".")
        top = parts[0].lower()
        rest = parts[1:]
        for root in self._search_roots():
            found = self._find_in(root, top, rest)
            if found:
                return found
        raise ModuleNotFoundError(
            f"module '{module_path}' not found (searched project, packages/, ~/.velice/packages)")

    def _find_in(self, root, top, rest):
        if not root or not os.path.isdir(root):
            return None
        base = os.path.join(root, top)
        if not os.path.isdir(base):
            # case-insensitive match against <name>[-<version>] dirs
            base = self._case_insensitive_match(root, top)
            if base is None:
                if rest:
                    return None
                file = os.path.join(root, top + ".velice")
                if os.path.exists(file):
                    return file
                lower = self._case_insensitive_file(root, top + ".velice")
                return lower
        for candidate in (
            os.path.join(base, "src", "main.velice"),
            os.path.join(base, "src", (rest[-1] if rest else top) + ".velice"),
            os.path.join(base, (rest[-1] if rest else "main") + ".velice"),
        ):
            if os.path.exists(candidate):
                return candidate
        return None

    @staticmethod
    def _case_insensitive_match(root, top):
        if os.path.isdir(root):
            for d in os.listdir(root):
                if d.lower().split("-")[0] == top.lower():
                    return os.path.join(root, d)
        return None

    @staticmethod
    def _case_insensitive_file(root, filename):
        if os.path.isdir(root):
            for f in os.listdir(root):
                if f.lower() == filename.lower():
                    return os.path.join(root, f)
        return None

    def load(self, module_path, is_file=False):
        path = self.resolve(module_path, is_file=is_file)
        if path in self.loaded:
            return self.loaded[path]
        with open(path, encoding="utf-8") as f:
            source = f.read()
        tokens = Lexer(source, path).tokenize()
        ast = Parser(tokens, source).parse()
        entry = {"ast": ast, "globals": {}, "executed": False}
        self.loaded[path] = entry
        return entry

    def execute(self, entry, interp, parent_env):
        """Run a module exactly once, caching its resulting globals."""
        if entry.get("executed"):
            return entry["globals"]
        mod_env = Env(parent_env)
        mod_env.vars.update(entry["globals"])
        sub = type(interp)(module_loader=self)
        sub.globals = mod_env
        sub.module_loader = self
        sub._setup_builtins()
        sub.run(entry["ast"], env=mod_env, raise_errors=True)
        entry["globals"] = {k: (v[0] if isinstance(v, tuple) and len(v) == 2 else v)
                            for k, v in mod_env.vars.items()}
        entry["executed"] = True
        return entry["globals"]
