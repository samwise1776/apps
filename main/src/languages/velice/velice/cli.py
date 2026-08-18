"""Velice CLI – compiler, runtime, and package manager entry point."""
import sys, os
from velice.lexer import Lexer
from velice.parser import Parser
from velice.interpreter import Interpreter

def run_file(path, project_root=None, args=()):
    if not os.path.exists(path):
        print(f"Error: file not found: {path}", file=sys.stderr); sys.exit(1)
    with open(path, encoding="utf-8") as f: source = f.read()
    try:
        from velice.moduleloader import ModuleLoader
        tokens = Lexer(source, path).tokenize()
        ast = Parser(tokens, source).parse()
        interp = Interpreter(module_loader=ModuleLoader(project_root=project_root, source_path=path), argv=list(args))
        interp.source_path = os.path.abspath(path)
        result = interp.run(ast)
    except Exception as e:
        print(f"\033[91mError: {e}\033[0m", file=sys.stderr); sys.exit(1)

def _find_project_root(path):
    cur = os.path.dirname(os.path.abspath(path))
    while True:
        if os.path.exists(os.path.join(cur, "velice.toml")):
            return cur
        parent = os.path.dirname(cur)
        if parent == cur: return None
        cur = parent

def run_string(code, project_root=None):
    from velice.moduleloader import ModuleLoader
    tokens = Lexer(code).tokenize()
    ast = Parser(tokens, code).parse()
    interp = Interpreter(module_loader=ModuleLoader(project_root=project_root))
    return interp.run(ast)

def main():
    args = sys.argv[1:]
    if not args:
        from velice.repl import run_repl
        run_repl()
        return

    cmd = args[0]
    rest = args[1:]

    # package manager + project commands
    try:
        import velice.packagemanager as pm
        from velice.project import Project, ManifestError, scaffold_project
    except ImportError:
        pm = None

    if pm is None:
        print("Error: package manager unavailable", file=sys.stderr); sys.exit(1)

    def project():
        p = Project.find()
        if p is None:
            print("Error: not inside a Velice project (no velice.toml)", file=sys.stderr)
            print("Run `velice init` or `velice new <name>` to create one.", file=sys.stderr)
            sys.exit(1)
        return p

    if cmd == "new" or cmd == "create":
        if not rest:
            print("Usage: velice new <name>", file=sys.stderr); sys.exit(1)
        root = rest[0]
        p = scaffold_project(root, name=root)
        print(f"Created Velice project at {p.root}")
        print(f"  velice run      run the app")
        print(f"  velice build    build it")
        print(f"  velice test     run tests")
    elif cmd == "init":
        p = scaffold_project(os.getcwd())
        print(f"Initialized Velice project in {os.getcwd()}")
    elif cmd == "install" or cmd == "add":
        if not rest:
            print("Usage: velice install <package>[@version]", file=sys.stderr); sys.exit(1)
        p = project(); p.load()
        for spec in rest:
            try:
                src = pm.install(p, spec)
            except pm.PkgError as e:
                print(f"Error: {e}", file=sys.stderr); sys.exit(1)
        print(f"Installed {len(rest)} package(s)")
    elif cmd == "uninstall" or cmd == "remove":
        if not rest:
            print("Usage: velice uninstall <package>", file=sys.stderr); sys.exit(1)
        p = project()
        pm.uninstall(p, rest[0])
    elif cmd == "update":
        p = project(); p.load()
        updated = pm.update(p)
        if updated:
            for name, ver in updated: print(f"  updated {name} -> {ver}")
        else:
            print("  everything is up to date (or no registry reachable)")
    elif cmd == "upgrade":
        from velice import __version__
        print(f"  Velice SDK is already at v{__version__} (latest)")
    elif cmd == "list":
        p = project()
        pkgs = pm.list_packages(p)
        if not pkgs:
            print("  no packages installed")
        for name in pkgs: print(f"  {name}")
    elif cmd == "search":
        q = " ".join(rest) if rest else ""
        print("  Searching registry (packages.velice.org)…")
        results = pm.registry_search(q) if q else None
        if results is None:
            print("  registry unreachable; offline cache search:")
            if os.path.isdir(pm.cache_root()):
                for d in sorted(os.listdir(pm.cache_root())):
                    if q.lower() in d.lower(): print(f"  {d}")
        else:
            for pkg in results.get("results", []):
                print(f"  {pkg.get('name')}@{pkg.get('version', 'latest')} — {pkg.get('description', '')}")
    elif cmd == "run":
        if rest:
            run_file(rest[0], args=rest[1:])
        else:
            p = project()
            try:
                pm.run_project(p)
            except pm.PkgError as e:
                print(f"Error: {e}", file=sys.stderr); sys.exit(1)
    elif cmd == "debug":
        p = project()
        pm.build(p, release=False)
    elif cmd == "build":
        p = project()
        release = "--release" in rest or "-r" in rest
        try:
            pm.build(p, release=release)
        except pm.PkgError as e:
            print(f"Error: {e}", file=sys.stderr); sys.exit(1)
    elif cmd == "package":
        p = project()
        try:
            pm.package(p)
        except pm.PkgError as e:
            print(f"Error: {e}", file=sys.stderr); sys.exit(1)
    elif cmd == "publish":
        p = project()
        try:
            pm.publish(p)
        except pm.PkgError as e:
            print(f"Error: {e}", file=sys.stderr); sys.exit(1)
    elif cmd == "test":
        p = project()
        failed = pm.run_tests(p)
        if failed:
            sys.exit(1)
    elif cmd == "clean":
        p = project()
        for removed in pm.clean(p):
            print(f"  removed {removed}")
    elif cmd == "format":
        p = project()
        check = "--check" in rest
        pm.format_project(p, check=check)
    elif cmd == "lint":
        p = project()
        issues = pm.lint(p)
        if issues:
            for path, msg in issues:
                print(f"  {os.path.relpath(path)}: {msg}")
            print(f"  {len(issues)} issue(s)")
        else:
            print("  no issues found")
    elif cmd == "docs":
        p = project()
        pm.gen_docs(p)
    elif cmd == "install-dir":
        print(pm.CACHE_DIR)
    elif cmd == "eval":
        if rest:
            try:
                result = run_string(" ".join(rest))
                if result is not None:
                    print(Interpreter()._to_str(result))
            except Exception as error:
                print(f"Error: {error}", file=sys.stderr)
                sys.exit(1)
        else:
            print("Usage: velice eval <code>", file=sys.stderr); sys.exit(1)
    elif cmd == "repl":
        from velice.repl import run_repl; run_repl()
    elif cmd == "version" or cmd == "--version" or cmd == "-v":
        from velice import __version__; print(f"Velice v{__version__}")
    elif cmd == "help" or cmd == "--help" or cmd == "-h":
        print("Velice — language, toolchain, and package manager")
        print()
        print("Project commands:")
        print("  new <name>        Create a new project")
        print("  init              Initialize the current directory")
        print("  run               Run the project entry")
        print("  run <file>        Run a .velice file")
        print("  build [--release] Build the project")
        print("  debug             Build a debug binary")
        print("  test              Run tests in tests/")
        print("  docs              Generate documentation")
        print("  clean             Remove build artifacts")
        print("  format [--check]  Format source files")
        print("  lint              Lint source files")
        print()
        print("Package commands:")
        print("  install <pkg>[@ver]   Install a package (registry/git/path)")
        print("  uninstall <pkg>       Remove a package")
        print("  update                Update packages")
        print("  upgrade               Upgrade the SDK")
        print("  search <q>            Search packages")
        print("  list                  List installed packages")
        print("  package               Build a .vpkg archive")
        print("  publish               Validate, test, build, and publish")
        print()
        print("Other:")
        print("  repl              Start the REPL")
        print("  eval <code>       Evaluate an expression")
        print("  version           Show version")
        print("  help              Show this help")
    elif cmd.endswith(".velice"):
        run_file(cmd, args=rest)
    else:
        print(f"Unknown command: {cmd}", file=sys.stderr); sys.exit(1)

if __name__ == "__main__":
    main()
