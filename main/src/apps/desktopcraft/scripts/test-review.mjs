import assert from "node:assert/strict";

const listeners = {};
globalThis.document = {
  addEventListener(event, callback) { listeners[event] = callback; }
};
globalThis.localStorage = {
  getItem: () => null,
  setItem: () => {}
};

await import(`../review.js?test=${Date.now()}`);
const { analyze, detectLanguage, sampleLines } = globalThis.DesktopcraftReview;

const titles = (code, language) => analyze(code, language).findings.map((finding) => finding.title);
const counts = (code, language) => analyze(code, language).counts;

assert.equal(detectLanguage(sampleLines.java), "java");
assert.equal(detectLanguage(sampleLines.python), "python");
assert.equal(detectLanguage(sampleLines.csharp), "csharp");
assert.equal(detectLanguage(sampleLines.cpp), "cpp");
assert.equal(detectLanguage(sampleLines.electron), "electron");
assert.equal(detectLanguage("print('hello')"), null);

const java = analyze(sampleLines.java, "auto");
assert.equal(java.languageKey, "java");
assert.equal(java.components >= 3, true);
assert.ok(titles(sampleLines.java, "auto").includes("Avoid absolute layout"));

const brokenJava = [
  "import javax.swing.*;",
  "public class Main {",
  "  public static void main(String[] a) {",
  "    JFrame frame = new JFrame(\"x\");",
  "    frame.setLayout(null);",
  "    frame.add(new JButton(\"ok\"));",
  "  }",
  "}"
].join("\n");
assert.ok(titles(brokenJava, "auto").includes("Window is never shown"));
assert.equal(counts(brokenJava, "auto").issue, 1);
assert.equal(analyze(brokenJava, "auto").verdict, "Almost there");

const python = [
  "import tkinter as tk",
  "root = tk.Tk()",
  "def go(): pass",
  "b = tk.Button(root, text='go', command=go())",
  "root.mainloop()"
].join("\n");
assert.ok(titles(python, "auto").includes("Callback runs immediately"));

const pythonMissingLoop = "import tkinter as tk\nroot = tk.Tk()";
assert.ok(titles(pythonMissingLoop, "python").includes("Event loop never starts"));

const csharp = "using System.Windows.Forms;\npublic class F : Form { public static void Main() { } }";
assert.ok(titles(csharp, "auto").includes("App never runs the form"));
assert.ok(titles(csharp, "auto").includes("Add [STAThread] to Main"));

const cpp = ["#include <QApplication>", "int main(int argc, char** argv) {", "  QApplication app(argc, argv);", "  QWidget w;", "  w.show();", "}"].join("\n");
assert.ok(titles(cpp, "auto").includes("Event loop never starts"));

const electron = "const { ipcRenderer } = require('electron');\ndocument.querySelector('#x');";
assert.ok(titles(electron, "auto").includes("Expose IPC through a preload"));

const nodeIntegration = "BrowserWindow({ webPreferences: { nodeIntegration: true } });";
assert.ok(titles(nodeIntegration, "electron").includes("nodeIntegration is a security risk"));

const coverage = "import javax.swing.*;\nJFrame f = new JFrame();\nf.setVisible(true);\nSwingWorker worker;";
assert.equal(counts(coverage, "java").coverage, 1);

const cleanJava = [
  "import javax.swing.*;",
  "public class Main {",
  "  public static void main(String[] a) {",
  "    SwingUtilities.invokeLater(() -> {",
  "      JFrame f = new JFrame(\"ok\");",
  "      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);",
  "      f.setVisible(true);",
  "    });",
  "  }",
  "}"
].join("\n");
assert.equal(counts(cleanJava, "auto").issue, 0);

assert.equal(analyze("", "auto").verdict, "Looks clean");
assert.equal(analyze("", "auto").languageLabel, "Unknown toolkit");

console.log("Verified code reviewer detection, correctness rules, style rules, and simulator coverage warnings.");
