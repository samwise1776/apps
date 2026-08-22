(function (root) {
  const languageDefinitions = [
    ["Plain text", "txt text log"], ["Markdown", "md markdown mdown mkd"], ["Java", "java"], ["Python", "py pyw pyi"],
    ["C#", "cs csx"], ["C++", "cpp cc cxx hpp hh hxx"], ["C", "c h"], ["JavaScript", "js mjs cjs"], ["TypeScript", "ts mts cts"],
    ["JSX", "jsx"], ["TSX", "tsx"], ["HTML", "html htm xhtml"], ["CSS", "css"], ["SCSS", "scss"], ["Sass", "sass"],
    ["Less", "less"], ["JSON", "json jsonc json5"], ["XML", "xml xsd xsl xslt plist"], ["SVG", "svg"], ["YAML", "yaml yml"],
    ["TOML", "toml"], ["INI", "ini cfg conf"], ["Properties", "properties prefs"], ["SQL", "sql"], ["Shell", "sh"],
    ["Bash", "bash"], ["Zsh", "zsh"], ["Fish", "fish"], ["Batch", "bat cmd"], ["PowerShell", "ps1 psm1 psd1"],
    ["Ruby", "rb erb"], ["PHP", "php phtml"], ["Go", "go"], ["Rust", "rs"], ["Swift", "swift"], ["Kotlin", "kt kts"],
    ["Dart", "dart"], ["Lua", "lua"], ["R", "r rmd"], ["Vue", "vue"], ["Svelte", "svelte"], ["Groovy", "groovy gvy gy"],
    ["Scala", "scala sc"], ["Perl", "pl pm t"], ["Haskell", "hs lhs"], ["Elixir", "ex exs"], ["Erlang", "erl hrl"],
    ["Clojure", "clj cljs cljc edn"], ["F#", "fs fsx fsi"], ["Visual Basic", "vb vbs"], ["Objective-C", "m"],
    ["Objective-C++", "mm"], ["Julia", "jl"], ["MATLAB", "matlab"], ["Assembly", "asm s inc"], ["COBOL", "cob cbl"],
    ["Fortran", "f f90 f95 f03 for"], ["Pascal", "pas pp"], ["Ada", "ada adb ads"], ["Common Lisp", "lisp lsp cl"],
    ["Scheme", "scm ss"], ["Racket", "rkt"], ["OCaml", "ml mli"], ["Nim", "nim nims"], ["Crystal", "cr"], ["Zig", "zig"],
    ["Solidity", "sol"], ["Vyper", "vy"], ["Move", "move"], ["Protocol Buffers", "proto"], ["GraphQL", "graphql gql"],
    ["Terraform", "tf tfvars"], ["HCL", "hcl"], ["Nix", "nix"], ["CMake", "cmake"], ["Gradle", "gradle"], ["Meson", "meson"],
    ["Ninja", "ninja"], ["CSV", "csv"], ["TSV", "tsv"], ["LaTeX", "tex sty cls"], ["BibTeX", "bib"],
    ["reStructuredText", "rst"], ["AsciiDoc", "adoc asciidoc"], ["Handlebars", "hbs handlebars"], ["Pug", "pug jade"],
    ["EJS", "ejs"], ["Liquid", "liquid"], ["Mustache", "mustache"], ["Nunjucks", "njk nunjucks"], ["Astro", "astro"],
    ["Razor", "razor cshtml"], ["QML", "qml"], ["GLSL", "glsl vert frag geom"], ["HLSL", "hlsl fx"], ["WGSL", "wgsl"],
    ["CUDA", "cu cuh"], ["OpenCL", "clc"], ["Processing", "pde"], ["Arduino", "ino"], ["ABAP", "abap"], ["Apex", "cls trigger"],
    ["PL/SQL", "pls plsql pkb pks"], ["T-SQL", "tsql"], ["GDScript", "gd"], ["VHDL", "vhd vhdl"],
    ["Verilog", "v vh"], ["SystemVerilog", "sv svh"], ["Elm", "elm"], ["PureScript", "purs"], ["Reason", "re rei"],
    ["ReScript", "res resi"], ["Prolog", "pro prolog"], ["Smalltalk", "st"], ["Tcl", "tcl"], ["Awk", "awk"], ["Sed", "sed"],
    ["Prisma", "prisma"], ["Dhall", "dhall"], ["Janet", "janet"], ["Odin", "odin"], ["V", "vlang"], ["Mojo", "mojo"],
    ["Bicep", "bicep"], ["CUE", "cue"], ["Starlark", "bzl star"], ["Chapel", "chpl"], ["Raku", "raku rakumod"],
    ["D", "d di"], ["Delphi", "dpr dfm"], ["ActionScript", "as"], ["ColdFusion", "cfm cfc"], ["Velocity", "vm vtl"],
    ["Twig", "twig"], ["Jinja", "jinja jinja2"], ["Apache config", "htaccess"], ["Nginx config", "nginx"],
    ["Docker Compose", "compose"], ["Earthfile", "earth"], ["WebAssembly text", "wat wast"], ["CoffeeScript", "coffee"],
    ["LiveScript", "ls"], ["Stylus", "styl"], ["PostCSS", "pcss postcss"], ["MDX", "mdx"], ["Ember", "gjs gts"],
    ["SparQL", "rq sparql"], ["Cypher", "cyp cypher"], ["PromQL", "promql"], ["Jsonnet", "jsonnet libsonnet"],
    ["D2", "d2"], ["Mermaid", "mmd mermaid"], ["PlantUML", "puml plantuml"], ["Org", "org"], ["Typst", "typ"],
    ["Lean", "lean"], ["Coq", "vcoq"], ["Agda", "agda"], ["Idris", "idr lidr"], ["Gleam", "gleam"], ["Pony", "pony"],
    ["Hack", "hack hhvm"], ["Ballerina", "bal"], ["SAS", "sas"], ["Stata", "do ado"], ["Maple", "mpl"],
    ["Wolfram", "wl wls nb"], ["Robot Framework", "robot resource"], ["Gherkin", "feature"], ["Cucumber", "story"],
    ["OpenAPI", "openapi"], ["RAML", "raml"], ["Avro", "avsc avdl"], ["Thrift", "thrift"], ["Cap'n Proto", "capnp"]
  ].map(([name, extensions]) => ({ name, extensions: extensions.split(" ") }));

  const specialLanguageNames = new Map([
    ["dockerfile", "Dockerfile"], ["makefile", "Makefile"], ["gnumakefile", "Makefile"], ["cmakelists.txt", "CMake"],
    ["readme", "Plain text"], ["license", "Plain text"], ["notice", "Plain text"], ["procfile", "Procfile"],
    ["gemfile", "Ruby"], ["rakefile", "Ruby"], ["podfile", "Ruby"], ["gradlew", "Shell"], ["pom.xml", "Maven POM"],
    ["build.xml", "Ant"], [".gitignore", "Git ignore"], [".dockerignore", "Docker ignore"], [".editorconfig", "EditorConfig"],
    [".env", "Environment"], ["nginx.conf", "Nginx config"], ["earthfile", "Earthfile"]
  ]);
  const extensionLanguages = new Map();
  for (const language of languageDefinitions) {
    for (const extension of language.extensions) if (!extensionLanguages.has(extension)) extensionLanguages.set(extension, language.name);
  }
  const textExtensions = new Set(extensionLanguages.keys());
  const textNames = new Set(specialLanguageNames.keys());

  function normalizePath(value) {
    return String(value || "").replaceAll("\\", "/").replace(/^\.\//, "").split("/").filter((part) => part && part !== ".").join("/");
  }

  function supportedFile(path, size = 0) {
    const clean = normalizePath(path);
    const name = clean.split("/").pop()?.toLowerCase() || "";
    const extension = name.includes(".") ? name.split(".").pop() : name;
    return Boolean(clean) && Number(size) <= 2_000_000 && !clean.split("/").some((part) => [".git", "node_modules", ".idea", ".vscode", "__pycache__"].includes(part))
      && (textExtensions.has(extension) || textNames.has(name));
  }

  function detectLanguage(path) {
    const name = normalizePath(path).split("/").pop()?.toLowerCase() || "";
    if (specialLanguageNames.has(name)) return specialLanguageNames.get(name);
    const extension = name.includes(".") ? name.split(".").pop() : name;
    return extensionLanguages.get(extension) || "Plain text";
  }

  function languageCatalog() {
    return languageDefinitions.map((language) => language.name);
  }

  function projectSummary(workspace) {
    const files = Array.isArray(workspace?.files) ? workspace.files : [];
    const languages = new Map();
    let lines = 0;
    let characters = 0;
    for (const file of files) {
      const content = String(file?.content || "");
      const language = detectLanguage(file?.path || "");
      languages.set(language, (languages.get(language) || 0) + 1);
      lines += content ? content.split("\n").length : 0;
      characters += content.length;
    }
    return { files: files.length, lines, characters, languages: [...languages.entries()].sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0])) };
  }

  function starterForPath(path) {
    const language = detectLanguage(path);
    const fileName = normalizePath(path).split("/").pop() || "file";
    const starters = {
      "JavaScript": "const message = \"Hello from Desktopcraft!\";\nconsole.log(message);\n",
      "TypeScript": "const message: string = \"Hello from Desktopcraft!\";\nconsole.log(message);\n",
      "Python": "def main():\n    print(\"Hello from Desktopcraft!\")\n\n\nif __name__ == \"__main__\":\n    main()\n",
      "Java": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello from Desktopcraft!\");\n    }\n}\n",
      "C#": "using System;\n\nclass Program\n{\n    static void Main()\n    {\n        Console.WriteLine(\"Hello from Desktopcraft!\");\n    }\n}\n",
      "C++": "#include <iostream>\n\nint main() {\n    std::cout << \"Hello from Desktopcraft!\\n\";\n    return 0;\n}\n",
      "C": "#include <stdio.h>\n\nint main(void) {\n    puts(\"Hello from Desktopcraft!\");\n    return 0;\n}\n",
      "Rust": "fn main() {\n    println!(\"Hello from Desktopcraft!\");\n}\n",
      "Go": "package main\n\nimport \"fmt\"\n\nfunc main() {\n\tfmt.Println(\"Hello from Desktopcraft!\")\n}\n",
      "Ruby": "puts \"Hello from Desktopcraft!\"\n",
      "PHP": "<?php\necho \"Hello from Desktopcraft!\\n\";\n",
      "HTML": "<!doctype html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\" />\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n  <title>My project</title>\n</head>\n<body>\n  <h1>Hello from Desktopcraft!</h1>\n</body>\n</html>\n",
      "CSS": ":root {\n  color-scheme: light dark;\n  font-family: system-ui, sans-serif;\n}\n\nbody {\n  margin: 0;\n}\n",
      "Markdown": `# ${fileName.replace(/\.[^.]+$/, "")}\n\nStart writing here.\n`,
      "JSON": "{\n  \"name\": \"desktopcraft-project\",\n  \"version\": \"1.0.0\"\n}\n",
      "Shell": "#!/usr/bin/env sh\n\nprintf '%s\\n' \"Hello from Desktopcraft!\"\n",
      "SQL": "SELECT 'Hello from Desktopcraft!' AS message;\n"
    };
    if (language === "Java") {
      const className = (fileName.replace(/\.[^.]+$/, "").replace(/[^A-Za-z0-9_$]/g, "") || "Main").replace(/^[0-9]/, "_$&");
      return starters.Java.replace("class Main", `class ${className}`);
    }
    return starters[language] || "";
  }

  function terminalTokens(value) {
    const tokens = [];
    String(value || "").replace(/"([^"]*)"|'([^']*)'|(\S+)/g, (_match, doubleQuoted, singleQuoted, bare) => {
      tokens.push(doubleQuoted ?? singleQuoted ?? bare);
      return "";
    });
    return tokens;
  }

  function terminalFile(workspace, path) {
    const clean = normalizePath(path);
    return workspace.files.find((file) => file.path === clean)
      || workspace.files.find((file) => file.path.toLowerCase() === clean.toLowerCase())
      || null;
  }

  function formatSize(characters) {
    if (characters < 1000) return `${characters} B`;
    if (characters < 1_000_000) return `${(characters / 1000).toFixed(1)} KB`;
    return `${(characters / 1_000_000).toFixed(1)} MB`;
  }

  function executeTerminalCommand(value, workspaceValue) {
    const workspace = cleanWorkspace(workspaceValue);
    const tokens = terminalTokens(value);
    const command = (tokens.shift() || "").toLowerCase();
    const files = [...workspace.files].sort((left, right) => left.path.localeCompare(right.path));
    if (!command) return { kind: "output", lines: [] };
    if (command === "clear" || command === "cls") return { kind: "clear", lines: [] };
    if (command === "help" || command === "?") return { kind: "output", lines: [
      "Workspace terminal commands", "help  clear  pwd  ls [folder]  tree [folder]", "cat <file>  head <file> [lines]  tail <file> [lines]  wc <file>",
      "find <name>  grep <text> [folder]  file <path>  open <path>", "touch <path>  languages [search]  stats  echo <text>  date  whoami  version",
      "Tip: quote names that contain spaces. This safe terminal works only with the files in this browser workspace."
    ] };
    if (command === "pwd") return { kind: "output", lines: [`/${workspace.name.replaceAll("/", "-")}`] };
    if (command === "echo") return { kind: "output", lines: [tokens.join(" ")] };
    if (command === "date") return { kind: "output", lines: [new Date().toLocaleString()] };
    if (command === "whoami") return { kind: "output", lines: ["desktopcraft-builder"] };
    if (command === "version" || command === "about") return { kind: "output", lines: ["Desktopcraft Workspace Terminal 2.0", `${languageDefinitions.length} recognized languages · local, private, and safe`] };
    if (command === "stats") {
      const summary = projectSummary(workspace);
      return { kind: "output", lines: [
        `${summary.files} files · ${summary.lines.toLocaleString()} lines · ${formatSize(summary.characters)}`,
        summary.languages.length ? summary.languages.map(([name, count]) => `${name} (${count})`).join(", ") : "No languages detected yet."
      ] };
    }
    if (command === "languages" || command === "langs") {
      const query = tokens.join(" ").toLowerCase();
      const matches = languageDefinitions.map((language) => language.name).filter((name) => name.toLowerCase().includes(query));
      return { kind: "output", lines: matches.length ? [`${matches.length} recognized language${matches.length === 1 ? "" : "s"}`, ...matches] : ["No recognized languages match that search."] };
    }
    if (command === "ls") {
      const prefix = normalizePath(tokens[0] || "").replace(/\/$/, "");
      const names = new Set();
      for (const file of files) {
        if (prefix && file.path !== prefix && !file.path.startsWith(`${prefix}/`)) continue;
        const remainder = prefix ? file.path.slice(prefix.length).replace(/^\//, "") : file.path;
        const [name, ...rest] = remainder.split("/");
        if (name) names.add(rest.length ? `${name}/` : name);
      }
      return { kind: "output", lines: names.size ? [...names].sort() : [prefix ? `Folder not found or empty: ${prefix}` : "Project is empty."] };
    }
    if (command === "tree") {
      const prefix = normalizePath(tokens[0] || "").replace(/\/$/, "");
      const matches = files.filter((file) => !prefix || file.path === prefix || file.path.startsWith(`${prefix}/`));
      return { kind: "output", lines: matches.length ? matches.slice(0, 250).map((file) => `${"  ".repeat(Math.max(0, file.path.split("/").length - 1))}└─ ${file.path.split("/").pop()}`) : ["No files found."] };
    }
    if (command === "find") {
      const query = tokens.join(" ").toLowerCase();
      if (!query) return { kind: "error", lines: ["Usage: find <part of a file name>"] };
      const matches = files.filter((file) => file.path.toLowerCase().includes(query));
      return { kind: "output", lines: matches.length ? matches.map((file) => file.path) : ["No matching files."] };
    }
    if (command === "grep") {
      const query = (tokens.shift() || "").toLowerCase();
      const prefix = normalizePath(tokens.join(" "));
      if (!query) return { kind: "error", lines: ["Usage: grep <text> [folder]"] };
      const matches = [];
      for (const file of files) {
        if (prefix && !file.path.startsWith(prefix)) continue;
        String(file.content).split("\n").forEach((line, index) => {
          if (line.toLowerCase().includes(query) && matches.length < 200) matches.push(`${file.path}:${index + 1}: ${line.trim()}`);
        });
      }
      return { kind: "output", lines: matches.length ? matches : ["No matching text."] };
    }
    if (["cat", "head", "tail", "wc", "file", "open"].includes(command)) {
      const path = normalizePath(tokens.shift() || "");
      const file = terminalFile(workspace, path);
      if (!path) return { kind: "error", lines: [`Usage: ${command} <file>${["head", "tail"].includes(command) ? " [lines]" : ""}`] };
      if (!file) return { kind: "error", lines: [`File not found: ${path}`] };
      const contentLines = String(file.content).split("\n");
      if (command === "open") return { kind: "open", path: file.path, lines: [`Opened ${file.path}`] };
      if (command === "file") return { kind: "output", lines: [`${file.path}: ${detectLanguage(file.path)}, ${contentLines.length} lines, ${formatSize(file.content.length)}`] };
      if (command === "wc") return { kind: "output", lines: [`${contentLines.length} lines · ${(file.content.match(/\S+/g) || []).length} words · ${file.content.length} characters · ${file.path}`] };
      const limit = Math.max(1, Math.min(200, Number(tokens[0]) || (command === "cat" ? 200 : 10)));
      if (command === "head") return { kind: "output", lines: contentLines.slice(0, limit) };
      if (command === "tail") return { kind: "output", lines: contentLines.slice(-limit) };
      return { kind: "output", lines: contentLines.slice(0, limit).concat(contentLines.length > limit ? [`… ${contentLines.length - limit} more lines`] : []) };
    }
    if (command === "touch" || command === "new") {
      const path = normalizePath(tokens.join(" "));
      if (!path || path.includes("..") || !supportedFile(path, 0)) return { kind: "error", lines: ["Use: touch <safe supported-file path>"] };
      if (terminalFile(workspace, path)) return { kind: "error", lines: [`File already exists: ${path}`] };
      return { kind: "create", path, content: starterForPath(path), lines: [`Created ${path}`] };
    }
    return { kind: "error", lines: [`Command not found: ${command}`, "Type help to see the available workspace commands."] };
  }

  function simpleLineDiff(before, after) {
    const oldLines = String(before).split("\n");
    const newLines = String(after).split("\n");
    const rows = [];
    let added = 0;
    let removed = 0;
    let changed = 0;
    const count = Math.max(oldLines.length, newLines.length);
    for (let index = 0; index < count; index++) {
      if (index >= oldLines.length) { rows.push({ type: "added", text: newLines[index], line: index + 1 }); added++; }
      else if (index >= newLines.length) { rows.push({ type: "removed", text: oldLines[index], line: index + 1 }); removed++; }
      else if (oldLines[index] !== newLines[index]) { rows.push({ type: "changed", before: oldLines[index], text: newLines[index], line: index + 1 }); changed++; }
    }
    return { rows, added, removed, changed };
  }

  function calculateLineDiff(before, after) {
    const oldLines = String(before).split("\n");
    const newLines = String(after).split("\n");
    if (oldLines.length > 320 || newLines.length > 320) return simpleLineDiff(before, after);
    const table = Array.from({ length: oldLines.length + 1 }, () => new Uint16Array(newLines.length + 1));
    for (let oldIndex = oldLines.length - 1; oldIndex >= 0; oldIndex--) {
      for (let newIndex = newLines.length - 1; newIndex >= 0; newIndex--) {
        table[oldIndex][newIndex] = oldLines[oldIndex] === newLines[newIndex]
          ? table[oldIndex + 1][newIndex + 1] + 1
          : Math.max(table[oldIndex + 1][newIndex], table[oldIndex][newIndex + 1]);
      }
    }
    const raw = [];
    let oldIndex = 0;
    let newIndex = 0;
    while (oldIndex < oldLines.length || newIndex < newLines.length) {
      if (oldIndex < oldLines.length && newIndex < newLines.length && oldLines[oldIndex] === newLines[newIndex]) {
        oldIndex++; newIndex++; continue;
      }
      if (newIndex < newLines.length && (oldIndex === oldLines.length || table[oldIndex][newIndex + 1] >= table[oldIndex + 1][newIndex])) {
        raw.push({ type: "added", text: newLines[newIndex], line: newIndex + 1 }); newIndex++;
      } else {
        raw.push({ type: "removed", text: oldLines[oldIndex], line: oldIndex + 1 }); oldIndex++;
      }
    }
    const rows = [];
    let changed = 0;
    for (let index = 0; index < raw.length; index++) {
      if (raw[index + 1] && raw[index].type !== raw[index + 1].type) {
        const removedRow = raw[index].type === "removed" ? raw[index] : raw[index + 1];
        const addedRow = raw[index].type === "added" ? raw[index] : raw[index + 1];
        rows.push({ type: "changed", before: removedRow.text, text: addedRow.text, line: addedRow.line });
        changed++; index++;
      } else rows.push(raw[index]);
    }
    return {
      rows,
      added: rows.filter((row) => row.type === "added").length,
      removed: rows.filter((row) => row.type === "removed").length,
      changed
    };
  }

  function cleanWorkspace(value) {
    const files = Array.isArray(value?.files) ? value.files.filter((file) => file && typeof file.path === "string").map((file) => ({
      id: String(file.id || file.path),
      path: normalizePath(file.path),
      content: String(file.content || ""),
      updatedAt: Number(file.updatedAt) || Date.now(),
      versions: Array.isArray(file.versions) ? file.versions.filter((version) => version && typeof version.content === "string").map((version) => ({
        id: String(version.id || version.createdAt || Date.now()),
        name: String(version.name || "Saved version").slice(0, 70),
        content: version.content,
        createdAt: Number(version.createdAt) || Date.now()
      })).slice(-40) : []
    })).filter((file, index, all) => file.path && all.findIndex((candidate) => candidate.path === file.path) === index) : [];
    return { name: String(value?.name || "Browser project").slice(0, 80), activeId: String(value?.activeId || files[0]?.id || ""), files };
  }

  root.DesktopcraftProjectsCore = {
    normalizePath, supportedFile, detectLanguage, languageCatalog, projectSummary, starterForPath,
    executeTerminalCommand, calculateLineDiff, cleanWorkspace
  };
})(typeof window === "undefined" ? globalThis : window);
