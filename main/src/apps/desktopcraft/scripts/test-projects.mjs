import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

await import(`../projects-core.js?test=${Date.now()}`);
const core = globalThis.DesktopcraftProjectsCore;

assert.equal(core.normalizePath("./src\\Main.java"), "src/Main.java");
assert.equal(core.supportedFile("src/Main.java", 1200), true);
assert.equal(core.supportedFile("node_modules/tool.js", 1200), false);
assert.equal(core.supportedFile("assets/photo.png", 1200), false);
assert.equal(core.supportedFile("src/large.js", 3_000_000), false);
assert.equal(core.detectLanguage("src/server.py"), "Python");
assert.equal(core.detectLanguage("components/App.svelte"), "Svelte");
assert.equal(core.detectLanguage("Dockerfile"), "Dockerfile");
assert.ok(core.languageCatalog().length >= 100, "Projects should recognize at least 100 languages");
assert.match(core.starterForPath("src/main.py"), /def main/);
assert.match(core.starterForPath("src/HelloWorld.java"), /class HelloWorld/);

const added = core.calculateLineDiff("one\ntwo", "one\ntwo\nthree");
assert.equal(added.added, 1);
assert.equal(added.removed, 0);

const removed = core.calculateLineDiff("one\ntwo\nthree", "one\nthree");
assert.equal(removed.removed, 1);

const changed = core.calculateLineDiff("one\ntwo", "one\nupdated");
assert.equal(changed.changed, 1);
assert.equal(changed.rows[0].before, "two");
assert.equal(changed.rows[0].text, "updated");

const workspace = core.cleanWorkspace({
  name: "Demo",
  activeId: "src/Main.java",
  files: [
    { id: "src/Main.java", path: "./src\\Main.java", content: "class Main {}", versions: [{ id: "v1", name: "First", content: "class Main {}", createdAt: 1 }] },
    { id: "duplicate", path: "src/Main.java", content: "duplicate" }
  ]
});
assert.equal(workspace.files.length, 1);
assert.equal(workspace.files[0].path, "src/Main.java");
assert.equal(workspace.files[0].versions[0].name, "First");

const summary = core.projectSummary(workspace);
assert.equal(summary.files, 1);
assert.equal(summary.languages[0][0], "Java");

const terminalWorkspace = core.cleanWorkspace({
  name: "Terminal demo",
  files: [
    { path: "src/main.py", content: "print('hello')\nprint('world')" },
    { path: "README.md", content: "# Demo" }
  ]
});
assert.match(core.executeTerminalCommand("help", terminalWorkspace).lines.join("\n"), /Workspace terminal commands/);
assert.deepEqual(core.executeTerminalCommand("ls src", terminalWorkspace).lines, ["main.py"]);
assert.equal(core.executeTerminalCommand("open src/main.py", terminalWorkspace).kind, "open");
assert.match(core.executeTerminalCommand("grep world", terminalWorkspace).lines[0], /src\/main\.py:2/);
assert.equal(core.executeTerminalCommand("touch src/app.rs", terminalWorkspace).kind, "create");
assert.equal(core.executeTerminalCommand("rm src/main.py", terminalWorkspace).kind, "error");

const page = await readFile(new URL("../projects.html", import.meta.url), "utf8");
const implementation = await readFile(new URL("../projects.js", import.meta.url), "utf8");
for (const id of ["addFileButton", "projectEditor", "addVersionButton", "compareVersion", "changePreview", "versionList", "projectTerminal", "terminalInput", "overviewLanguages", "exportProjectButton"]) {
  assert.match(page, new RegExp(`id=["']${id}["']`));
}
assert.match(implementation, /showDirectoryPicker/);
assert.match(implementation, /createWritable/);
assert.match(implementation, /persistProjectWorkspace/);
assert.match(implementation, /executeTerminalCommand/);

console.log(`Verified project import, editing, versions, export, terminal commands, statistics, and ${core.languageCatalog().length} recognized languages.`);
