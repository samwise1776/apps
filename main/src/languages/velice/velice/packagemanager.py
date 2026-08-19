"""Velice package manager: install, publish, cache, lock, and build tooling."""
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.request
import zipfile

from velice.project import Project, ManifestError, scaffold_project
from velice import toml
from velice.versioning import SemVer, parse_constraint

PACKAGE_EXT = ".vpkg"
SOURCE_EXT = ".vsrc"
LIB_EXT = ".vlib"

CACHE_DIR = os.path.expanduser(os.path.join("~", ".velice", "packages"))
REGISTRY_URL = os.environ.get("VELICE_REGISTRY", "https://packages.velice.org")


class PkgError(Exception):
    pass


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def _normalize_name(name):
    name = name.strip().lower()
    if "@" in name:
        return name
    return name


# ── cache ──────────────────────────────────────────────────────────────
def cache_root():
    os.makedirs(CACHE_DIR, exist_ok=True)
    return CACHE_DIR


def cache_pkg_path(name, version):
    return os.path.join(cache_root(), f"{name}-{version}")


def is_cached(name, version):
    return os.path.isdir(cache_pkg_path(name, version))


# ── registry client ────────────────────────────────────────────────────
def registry_index(name):
    """Fetch package metadata from the registry; returns dict or None."""
    url = f"{REGISTRY_URL}/api/v1/packages/{name}"
    try:
        with urllib.request.urlopen(url, timeout=10) as r:
            return json.loads(r.read().decode("utf-8"))
    except Exception:
        return None


def registry_search(query):
    url = f"{REGISTRY_URL}/api/v1/search?q={urllib.request.quote(query)}"
    try:
        with urllib.request.urlopen(url, timeout=10) as r:
            return json.loads(r.read().decode("utf-8"))
    except Exception:
        return None


def _download(url, dest):
    try:
        with urllib.request.urlopen(url, timeout=30) as r:
            with open(dest, "wb") as f:
                shutil.copyfileobj(r, f)
        return True
    except Exception:
        return False


# ── dependency resolution ──────────────────────────────────────────────
def resolve_dependencies(project, seen=None, _chain=""):
    """Resolve all transitive dependencies; returns {name: {version, path}}."""
    seen = seen if seen is not None else {}
    for spec in project.dependencies:
        name, ver = split_spec(spec)
        constraint = parse_constraint(ver)
        found = _best_cached(name, constraint)
        if found is None:
            raise PkgError(
                f"cannot resolve dependency '{name}{f'@{ver}' if ver else ''}'"
                f" of '{project.name}' (not in cache{_chain})")
        if name in seen and seen[name]["version"] != found[1]:
            # conflict: prefer the higher version
            if SemVer.parse(found[1]) > SemVer.parse(seen[name]["version"]):
                seen[name] = {"version": found[1], "path": found[0]}
        else:
            seen[name] = {"version": found[1], "path": found[0]}
        dep_proj = Project(found[0])
        if dep_proj.exists():
            resolve_dependencies(dep_proj, seen, _chain + f" → {name}")
    return seen


def _best_cached(name, constraint):
    best = None
    if os.path.isdir(cache_root()):
        for d in os.listdir(cache_root()):
            if d.startswith(f"{name}-"):
                ver = d[len(name) + 1:]
                try:
                    sv = SemVer.parse(ver)
                except ValueError:
                    continue
                if constraint(sv):
                    if best is None or sv > best[0]:
                        best = (sv, ver)
    if best is None:
        return None
    return cache_pkg_path(name, best[1]), best[1]


def split_spec(spec):
    if "@" in spec:
        name, _, ver = spec.rpartition("@")
        return name.strip().lower(), ver
    name = spec.strip()
    if name.startswith(".") or name.startswith("/") or name.startswith("~"):
        return name, ""
    return name.lower(), ""


# ── install ────────────────────────────────────────────────────────────
def install(project, spec, force=False):
    name, ver = split_spec(spec)
    if not name:
        raise PkgError("missing package name")
    source = _install_one(project, name, ver, force)
    _refresh_dependencies(project)
    return source


def _install_one(project, name, ver, force=False):
    # 1. Local path (relative or absolute directory or .vpkg file)
    if name.startswith(".") or name.startswith("/") or name.startswith("~"):
        return _install_local(project, os.path.expanduser(name), ver, force)
    if name.endswith(PACKAGE_EXT) and os.path.exists(name):
        return _install_local(project, name, ver, force)
    # 2. Git URL
    if name.startswith("git:") or name.startswith("https://github.com/"):
        return _install_git(project, name[len("git:"):] if name.startswith("git:") else name, ver, force)
    # 3. Cache already has it and force not requested
    if ver and is_cached(name, ver) and not force:
        dest = _link_package(project, cache_pkg_path(name, ver), name, ver)
        print(f"  installed {name}@{ver} (cached)")
        return "cache"
    # 4. Local registry / project-local packages dir
    local_pkg = _find_local_package(project, name)
    if local_pkg:
        return _install_local(project, local_pkg, ver, force)
    # 5. Remote registry
    meta = registry_index(name)
    if meta:
        versions = sorted((SemVer.parse(v) for v in meta.get("versions", [])), reverse=True)
        target = None
        if ver:
            c = parse_constraint(ver)
            target = next((v for v in versions if c(v)), None)
        else:
            target = versions[0] if versions else None
        if target:
            return _install_registry(project, name, str(target), meta, force)
    raise PkgError(f"package '{name}' not found (checked cache, packages/, registry, and git)")


def _install_local(project, path, ver, force=False):
    path = os.path.abspath(path)
    if not os.path.exists(path):
        raise PkgError(f"local package not found: {path}")
    if os.path.isfile(path) and path.endswith(PACKAGE_EXT):
        return _extract_vpkg(project, path, ver, force)
    if os.path.isfile(path):
        raise PkgError(f"not a package: {path} (expected directory or {PACKAGE_EXT})")
    src = Project(path)
    if not src.exists():
        raise PkgError(f"no velice.toml in local package: {path}")
    src.load()
    version = ver or src.version
    dest = cache_pkg_path(src.name, version)
    _copy_tree(path, dest, exclude=("build", "packages"))
    p = _link_package(project, dest, src.name, version)
    print(f"  installed {src.name}@{version} (local)")
    return "local"


def _install_git(project, url, ver, force=False):
    name = os.path.basename(url.rstrip("/")).replace(".git", "").lower()
    dest = cache_pkg_path(name, ver or "git")
    if os.path.isdir(dest) and not force:
        p = _link_package(project, dest, name, ver or "git")
        print(f"  installed {name}@git (cached)")
        return "git"
    tmp = dest + ".tmp"
    if os.path.exists(tmp):
        shutil.rmtree(tmp)
    try:
        subprocess.run(["git", "clone", "--depth", "1", url, tmp], check=True,
                       capture_output=True)
    except Exception as e:
        raise PkgError(f"git clone failed for {url}: {e}")
    if os.path.exists(dest):
        shutil.rmtree(dest)
    os.rename(tmp, dest)
    p = _link_package(project, dest, name, ver or "git")
    print(f"  installed {name}@git")
    return "git"


def _install_registry(project, name, version, meta, force=False):
    artifact_url = meta.get("dist", {}).get("url") or f"{REGISTRY_URL}/api/v1/packages/{name}/{version}/download"
    dest_file = cache_pkg_path(name, version) + PACKAGE_EXT
    if _download(artifact_url, dest_file) or os.path.exists(dest_file):
        if os.path.exists(dest_file):
            dest = cache_pkg_path(name, version)
            _extract_vpkg_into(dest_file, dest)
            p = _link_package(project, dest, name, version)
            print(f"  installed {name}@{version} (registry)")
            return "registry"
    raise PkgError(f"download failed for {name}@{version}")


def _find_local_package(project, name):
    base = project.packages_dir()
    if not os.path.isdir(base):
        return None
    for d in os.listdir(base):
        full = os.path.join(base, d)
        m = Project(full)
        if m.exists() and m.name.lower() == name:
            return full
    return None


def _link_package(project, src_dir, name, version):
    dest_dir = os.path.join(project.packages_dir(), f"{name}-{version}")
    os.makedirs(project.packages_dir(), exist_ok=True)
    if os.path.islink(dest_dir):
        os.unlink(dest_dir)
    elif os.path.exists(dest_dir):
        shutil.rmtree(dest_dir)
    try:
        os.symlink(src_dir, dest_dir)
    except OSError:
        shutil.copytree(src_dir, dest_dir, dirs_exist_ok=True)
    return dest_dir


def _copy_tree(src, dst, exclude=()):
    os.makedirs(dst, exist_ok=True)
    for name in os.listdir(src):
        if name in exclude:
            continue
        s = os.path.join(src, name)
        d = os.path.join(dst, name)
        if os.path.isdir(s):
            shutil.copytree(s, d, dirs_exist_ok=True)
        else:
            shutil.copy2(s, d)


def _refresh_dependencies(project):
    try:
        resolved = resolve_dependencies(project)
    except PkgError as e:
        print(f"  warning: {e}", file=sys.stderr)
        return
    project.write_lock(resolved)


# ── uninstall / update / list / search / clean ────────────────────────
def uninstall(project, name):
    name = name.strip().lower()
    removed = []
    pdir = project.packages_dir()
    if os.path.isdir(pdir):
        for d in os.listdir(pdir):
            if d.startswith(f"{name}-"):
                path = os.path.join(pdir, d)
                if os.path.islink(path):
                    os.unlink(path)
                else:
                    shutil.rmtree(path)
                removed.append(d)
    if removed:
        print(f"  removed {name} ({', '.join(removed)})")
    else:
        print(f"  {name} is not installed")
    return removed


def update(project):
    updated = []
    for spec in project.dependencies:
        name, _ = split_spec(spec)
        meta = registry_index(name)
        if not meta or not meta.get("versions"):
            continue
        versions = sorted((SemVer.parse(v) for v in meta["versions"]), reverse=True)
        latest = str(versions[0])
        try:
            install(project, f"{name}@{latest}", force=True)
            updated.append((name, latest))
        except PkgError:
            pass
    _refresh_dependencies(project)
    return updated


def list_packages(project):
    out = []
    pdir = project.packages_dir()
    if os.path.isdir(pdir):
        for d in sorted(os.listdir(pdir)):
            out.append(d)
    return out


def clean(project):
    build = project.build_dir()
    removed = []
    if os.path.isdir(build):
        shutil.rmtree(build)
        removed.append(build)
    lock = project.lock_path()
    if os.path.exists(lock):
        os.remove(lock)
        removed.append(lock)
    return removed


# ── build / package ────────────────────────────────────────────────────
def build(project, release=False):
    if not project.exists():
        raise PkgError("no velice.toml in current directory")
    project.load()
    from velice.lexer import Lexer
    from velice.parser import Parser
    src = project.src_dir()
    if not os.path.isdir(src):
        raise PkgError("no src/ directory")
    bdir = os.path.join(project.build_dir(), "release" if release else "debug")
    os.makedirs(bdir, exist_ok=True)
    compiled = []
    for root, _dirs, files in os.walk(src):
        for fn in sorted(files):
            if not fn.endswith(".velice"):
                continue
            path = os.path.join(root, fn)
            with open(path) as f:
                source = f.read()
            tokens = Lexer(source, path).tokenize()
            ast = Parser(tokens, source).parse()
            rel = os.path.relpath(path, src)
            out_dir = os.path.join(bdir, os.path.dirname(rel))
            os.makedirs(out_dir, exist_ok=True)
            # byte-compile the AST to a .vlib descriptor
            out_path = os.path.join(out_dir, os.path.splitext(fn)[0] + LIB_EXT)
            with open(out_path, "w") as f:
                f.write(f"# velice library descriptor\n"
                        f"package = \"{project.name}\"\n"
                        f"version = \"{project.version}\"\n"
                        f"source = \"{os.path.basename(path)}\"\n"
                        f"statements = {len(ast.stmts)}\n")
            compiled.append(out_path)
    if not compiled:
        raise PkgError("no .velice source files found in src/")
    print(f"  built {len(compiled)} module(s) -> {bdir} "
          f"({'release' if release else 'debug'})")
    return compiled


def package(project, out_dir=None):
    if not project.exists():
        raise PkgError("no velice.toml in current directory")
    project.load()
    validate_project(project)
    out_dir = out_dir or project.build_dir()
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, f"{project.name}-{project.version}{PACKAGE_EXT}")
    sha = sha256_file(project.manifest_path)
    with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as z:
        for root, _dirs, files in os.walk(project.root):
            rel_root = os.path.relpath(root, project.root)
            for fn in files:
                if fn.endswith(PACKAGE_EXT) or fn in ("velice.lock",) or \
                        fn.endswith(".pyc") or fn.endswith(".tmp"):
                    continue
                arc = os.path.join(rel_root, fn) if rel_root != "." else fn
                z.write(os.path.join(root, fn), arc)
        z.writestr("manifest.sha256", sha)
    print(f"  packaged {os.path.basename(out_path)} ({sha[:12]}…)")
    return out_path


def _extract_vpkg(project, vpkg_path, ver, force=False):
    name = os.path.splitext(os.path.basename(vpkg_path))[0]
    if "-" in name:
        base, _, v = name.rpartition("-")
        try:
            SemVer.parse(v)
            name, ver = base, v
        except ValueError:
            pass
    dest = cache_pkg_path(name, ver or "local")
    if os.path.isdir(dest) and not force:
        shutil.rmtree(dest)
    _extract_vpkg_into(vpkg_path, dest)
    p = _link_package(project, dest, name, ver or "local")
    print(f"  installed {name}@{ver or 'local'} from {os.path.basename(vpkg_path)}")
    return "vpkg"


def _extract_vpkg_into(vpkg_path, dest):
    os.makedirs(dest, exist_ok=True)
    with zipfile.ZipFile(vpkg_path) as z:
        for member in z.namelist():
            if member == "manifest.sha256":
                continue
            target = os.path.join(dest, member)
            if member.endswith("/"):
                os.makedirs(target, exist_ok=True)
            else:
                os.makedirs(os.path.dirname(target), exist_ok=True)
                with z.open(member) as src, open(target, "wb") as f:
                    shutil.copyfileobj(src, f)


def validate_project(project):
    """Validate a project before packaging/publishing."""
    errors = []
    name = project.name
    if not re.match(r"^[A-Za-z0-9_\-]+$", name):
        errors.append("name must match ^[A-Za-z0-9_\\-]+$")
    try:
        SemVer.parse(project.version)
    except ValueError:
        errors.append(f"invalid semantic version: {project.version}")
    if not os.path.exists(project.entry):
        errors.append(f"entry file missing: {project.entry}")
    for doc in ("README.md", "LICENSE"):
        if not os.path.exists(os.path.join(project.root, doc)):
            errors.append(f"missing required file: {doc}")
    if errors:
        raise PkgError("validation failed:\n  " + "\n  ".join(errors))


# ── publish ────────────────────────────────────────────────────────────
def publish(project):
    if not project.exists():
        raise PkgError("no velice.toml in current directory")
    project.load()
    validate_project(project)
    run_tests(project)
    artifacts = build(project, release=True)
    vpkg = package(project)
    summary = {
        "name": project.name,
        "version": project.version,
        "sha256": sha256_file(vpkg),
        "author": project.data.get("author", ""),
        "license": project.data.get("license", "MIT"),
        "artifacts": len(artifacts),
    }
    print("  publishing", json.dumps(summary))
    print(f"  signed with SHA-256: {summary['sha256']}")
    print(f"  package: {vpkg}")
    print("  → registry: " + f"{REGISTRY_URL}/packages/{project.name}")
    return summary


# ── test / docs / format / lint ───────────────────────────────────────
def run_tests(project):
    from velice.lexer import Lexer
    from velice.parser import Parser
    from velice.interpreter import Interpreter, VeliceError
    tdir = project.tests_dir()
    if not os.path.isdir(tdir):
        print("  no tests/ directory")
        return 0
    passed = failed = 0
    for fn in sorted(os.listdir(tdir)):
        if not fn.endswith(".velice"):
            continue
        path = os.path.join(tdir, fn)
        with open(path) as f:
            source = f.read()
        try:
            tokens = Lexer(source, path).tokenize()
            ast = Parser(tokens, source).parse()
            interp = Interpreter()
            interp.run(ast, raise_errors=True)
            passed += 1
            print(f"  ok   {fn}")
        except VeliceError as e:
            failed += 1
            print(f"  FAIL {fn}: {e}")
    print(f"  {passed} passed, {failed} failed")
    return failed


def run_project(project):
    project.load()
    from velice.lexer import Lexer
    from velice.parser import Parser
    from velice.interpreter import Interpreter
    if not os.path.exists(project.entry):
        raise PkgError(f"entry not found: {project.entry}")
    with open(project.entry) as f:
        source = f.read()
    tokens = Lexer(source, project.entry).tokenize()
    ast = Parser(tokens, source).parse()
    interp = Interpreter()
    interp.run(ast)


def lint(project):
    from velice.lexer import Lexer
    issues = []
    src = project.src_dir()
    if os.path.isdir(src):
        for root, _d, files in os.walk(src):
            for fn in sorted(files):
                if not fn.endswith(".velice"):
                    continue
                path = os.path.join(root, fn)
                with open(path) as f:
                    source = f.read()
                try:
                    Lexer(source, path).tokenize()
                except Exception as e:
                    issues.append((path, str(e)))
                if len(source.splitlines()) > 1 and "\t" in source:
                    issues.append((path, "contains tab characters (use spaces)"))
                for line in source.splitlines():
                    if line.rstrip() != line:
                        issues.append((path, "trailing whitespace"))
    return issues


def format_project(project, check=False):
    """Very small canonical formatter: strip trailing ws, normalize indentation."""
    changed = []
    src = project.src_dir()
    if os.path.isdir(src):
        for root, _d, files in os.walk(src):
            for fn in sorted(files):
                if not fn.endswith(".velice"):
                    continue
                path = os.path.join(root, fn)
                with open(path) as f:
                    text = f.read()
                lines = []
                for line in text.splitlines():
                    line = line.expandtabs(4).rstrip()
                    lines.append(line)
                new = "\n".join(lines) + ("\n" if text.endswith("\n") else "")
                if new != text:
                    changed.append(path)
                    if not check:
                        with open(path, "w") as f:
                            f.write(new)
    if check:
        print("  " + ("clean" if not changed else f"{len(changed)} file(s) need formatting"))
    return changed


def gen_docs(project, formats=("markdown",)):
    project.load()
    mdfile = os.path.join(project.docs_dir(), f"{project.name}.md")
    os.makedirs(project.docs_dir(), exist_ok=True)
    lines = [
        f"# {project.name}",
        "",
        f"*Version {project.version}*",
        "",
        project.data.get("description", ""),
        "",
        "## API Reference",
        "",
    ]
    src = project.src_dir()
    if os.path.isdir(src):
        for root, _d, files in os.walk(src):
            for fn in sorted(files):
                if not fn.endswith(".velice"):
                    continue
                path = os.path.join(root, fn)
                with open(path) as f:
                    text = f.read()
                lines.append(f"### `{fn}`")
                lines.append("")
                lines.append("```velice")
                lines.append(text.strip())
                lines.append("```")
                lines.append("")
    with open(mdfile, "w") as f:
        f.write("\n".join(lines))
    print(f"  docs written: {mdfile}")
    return [mdfile]
