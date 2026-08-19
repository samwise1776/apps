(function () {
  const $ = (id) => document.getElementById(id);

  const LANGUAGE_LABELS = {
    java: "Java Swing",
    python: "Python Tkinter",
    csharp: "C# WinForms",
    cpp: "C++ Qt Widgets",
    electron: "JavaScript Electron"
  };

  const sampleLines = {
    java: [
      'import javax.swing.*;',
      'import java.awt.*;',
      '',
      'public class Main {',
      '    public static void main(String[] args) {',
      '        SwingUtilities.invokeLater(() -> {',
      '            JFrame frame = new JFrame("Counter");',
      '            JLabel count = new JLabel("0", SwingConstants.CENTER);',
      '            JButton bump = new JButton("Count");',
      '            bump.addActionListener(e -> count.setText(String.valueOf(count.getText().equals("10") ? 0 : Integer.parseInt(count.getText()) + 1)));',
      '            JPanel panel = new JPanel(new BorderLayout());',
      '            panel.add(count, BorderLayout.CENTER);',
      '            panel.add(bump, BorderLayout.SOUTH);',
      '            frame.setLayout(null);',
      '            frame.add(panel);',
      '            frame.setSize(320, 180);',
      '            frame.setVisible(true);',
      '        });',
      '    }',
      '}'
    ].join("\n"),
    python: [
      'import tkinter as tk',
      'from tkinter import ttk',
      '',
      'root = tk.Tk()',
      'root.title("Task List")',
      '',
      'task = ttk.Entry(root)',
      'task.pack(fill="x", padx=10, pady=6)',
      '',
      'def add_task():',
      '    print(task.get())',
      '',
      'add = ttk.Button(root, text="Add", command=add_task())',
      'add.pack(pady=4)',
      '',
      'root.geometry("300x200")'
    ].join("\n"),
    csharp: [
      'using System;',
      'using System.Windows.Forms;',
      '',
      'public class MainForm : Form',
      '{',
      '    public MainForm()',
      '    {',
      '        Text = "Notes";',
      '        var input = new TextBox { Dock = DockStyle.Top };',
      '        var save = new Button { Text = "Save", Dock = DockStyle.Bottom };',
      '        save.Click += (s, e) => MessageBox.Show("Saved");',
      '        Controls.Add(input);',
      '        Controls.Add(save);',
      '    }',
      '    public static void Main()',
      '    {',
      '        new MainForm();',
      '    }',
      '}'
    ].join("\n"),
    cpp: [
      '#include <QApplication>',
      '#include <QPushButton>',
      '#include <QLabel>',
      '#include <QVBoxLayout>',
      '',
      'int main(int argc, char *argv[]) {',
      '    QApplication app(argc, argv);',
      '    QWidget window;',
      '    window.setWindowTitle("Hello");',
      '    auto *label = new QLabel("Ready");',
      '    auto *action = new QPushButton("Go");',
      '    QObject::connect(action, SIGNAL(clicked()), label, SLOT(clear()));',
      '    auto *layout = new QVBoxLayout(&window);',
      '    layout->addWidget(label);',
      '    layout->addWidget(action);',
      '    window.show();',
      '}'
    ].join("\n"),
    electron: [
      "const { ipcRenderer } = require('electron');",
      'const status = document.querySelector("#status");',
      'document.querySelector("#action").addEventListener("click", async () => {',
      '  const result = await ipcRenderer.invoke("save", { text: status.textContent });',
      '  status.textContent = result;',
      '});'
    ].join("\n")
  };

  const componentPatterns = {
    java: /\bnew\s+(JButton|JLabel|JTextField|JTextArea|JCheckBox|JRadioButton|JComboBox|JList|JTable|JTree|JPanel|JTabbedPane|JProgressBar)\b/g,
    python: /\b(?:ttk|tk)\.(Frame|Label|Button|Entry|Text|Checkbutton|Radiobutton|Combobox|Listbox|Treeview|Scale|Progressbar)\b/g,
    csharp: /\bnew\s+(Form|Button|Label|TextBox|RichTextBox|CheckBox|RadioButton|ComboBox|ListBox|DataGridView|TreeView|ProgressBar|Panel|FlowLayoutPanel)\b/g,
    cpp: /\bnew\s+Q(Label|PushButton|LineEdit|TextEdit|CheckBox|RadioButton|ComboBox|ListWidget|TableView|TreeView|ProgressBar)\b/g,
    electron: /<(input|button|p|label|textarea|select|div|span)(\s|>)/g
  };

  const listenerPatterns = {
    java: /\badd(Action|Change|Document|Mouse|Focus|Window)Listener\b/g,
    python: /command\s*=\s*\w|\b\.bind\s*\(/g,
    csharp: /\.[A-Za-z]+\s*\+=\s*|\.\w+\s*\+=\s*[A-Za-z(]/g,
    cpp: /QObject::connect\s*\(/g,
    electron: /\.addEventListener\s*\(/g
  };

  function countMatches(code, pattern) {
    return (code.match(pattern) || []).length;
  }

  function detectLanguage(code) {
    if (/\bJFrame\b|import\s+java\.\w+\.\*|javax\.swing/.test(code)) return "java";
    if (/import\s+tkinter|from\s+tkinter|tk\.Tk\s*\(|ttk\./.test(code)) return "python";
    if (/using\s+System\.Windows\.Forms|Application\.Run|namespace\s+\w+\s*[\{;]/.test(code)) return "csharp";
    if (/#include\s*<Q\w+>|QApplication|new\s+Q\w+/.test(code)) return "cpp";
    if (/require\(\s*['"]electron|from\s+['"]electron|ipcMain|BrowserWindow/.test(code)) return "electron";
    return null;
  }

  function reviewJava(code) {
    const findings = [];
    if (/\bJFrame\b/.test(code) && !/\bsetVisible\s*\(\s*true\s*\)/.test(code)) {
      findings.push({ level: "issue", title: "Window is never shown", detail: "After configuring the frame, call frame.setVisible(true). Until then the window stays hidden." });
    }
    if (/\bJFrame\b/.test(code) && !/setDefaultCloseOperation\b/.test(code)) {
      findings.push({ level: "tip", title: "Add a close behavior", detail: "setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE) makes the window quit the app when closed." });
    }
    if (/\bsetLayout\s*\(\s*null\s*\)/.test(code)) {
      findings.push({ level: "style", title: "Avoid absolute layout", detail: "setLayout(null) fixes component positions, so resizing breaks the interface. Use a layout manager or nested JPanels instead." });
    }
    if (/\bsetBounds\s*\(/.test(code)) {
      findings.push({ level: "style", title: "Prefer layouts over setBounds", detail: "setBounds fights the layout manager. A BorderLayout or GridBagLayout keeps the window flexible at any size." });
    }
    if (/\bJFrame\b/.test(code) && /\bsetSize\s*\(/.test(code) && /\.pack\s*\(\)/.test(code)) {
      findings.push({ level: "style", title: "Pick pack() over setSize", detail: "pack() sizes the frame to fit its components, so nothing gets cut off when content changes." });
    }
    if (/\bmain\s*\(String\[\]/.test(code) && /\bJFrame\b/.test(code) && !/SwingUtilities\.invokeLater/.test(code)) {
      findings.push({ level: "style", title: "Build UI on the Event Dispatch Thread", detail: "Wrap UI setup in SwingUtilities.invokeLater(...) so Swing is used safely from the main thread." });
    }
    if (/\bSwingWorker\b/.test(code)) {
      findings.push({ level: "coverage", title: "SwingWorker is not simulated", detail: "The browser preview won't run background threads. It works in a real Java environment though." });
    }
    if (/\bJFileChooser\b|\bJOptionPane\b/.test(code)) {
      findings.push({ level: "coverage", title: "Dialogs are not simulated", detail: "JFileChooser and JOptionPane are recognized but the preview shows a plain window instead of real dialogs." });
    }
    if (/\bImageIO\b|\bGraphics2D\b/.test(code)) {
      findings.push({ level: "coverage", title: "Rendering is not simulated", detail: "Images and custom painting won't appear in the preview, but run fine in Java." });
    }
    return findings;
  }

  function reviewPython(code) {
    const findings = [];
    if (!/\.mainloop\s*\(/.test(code)) {
      findings.push({ level: "issue", title: "Event loop never starts", detail: "Add root.mainloop() at the end so Tk can process clicks and keep the window alive." });
    }
    if (/command\s*=\s*[A-Za-z_]\w*\s*\(/.test(code)) {
      findings.push({ level: "issue", title: "Callback runs immediately", detail: "command=add_task() calls the function at setup time. Pass the function itself: command=add_task." });
    }
    if (/\.pack\s*\(/.test(code) && /\.grid\s*\(/.test(code)) {
      findings.push({ level: "style", title: "Don't mix pack and grid", detail: "A widget's parent only supports one geometry manager. Choose pack or grid for each container." });
    }
    if (/from\s+tkinter\s+import\s+\*/.test(code)) {
      findings.push({ level: "style", title: "Avoid wildcard imports", detail: "from tkinter import * pulls every name into scope. Prefer import tkinter as tk and ttk imports." });
    }
    if (/tk\.Tk\s*\(/.test(code) && !/\.title\s*\(/.test(code)) {
      findings.push({ level: "tip", title: "Give the window a title", detail: "root.title(\"...\") makes the taskbar and title bar show something meaningful." });
    }
    if (/\b(Canvas|messagebox|filedialog|Toplevel|Menu|Panedwindow)\b/.test(code)) {
      findings.push({ level: "coverage", title: "Some Tk features are not simulated", detail: "Canvas, dialogs, menus, and extra windows are recognized but not recreated in the browser preview." });
    }
    return findings;
  }

  function reviewCSharp(code) {
    const findings = [];
    if (/\bForm\b/.test(code) && !/Application\.Run/.test(code)) {
      findings.push({ level: "issue", title: "App never runs the form", detail: "Call Application.Run(new MainForm()) in Main to start the Windows message loop." });
    }
    if (/\bstatic\s+void\s+Main\b/.test(code) && !/\[STAThread\]/.test(code)) {
      findings.push({ level: "style", title: "Add [STAThread] to Main", detail: "WinForms needs a single-threaded apartment. Mark the entry point with [STAThread]." });
    }
    if (/\bForm\b/.test(code) && !/\.\w+\s*\+=\s*/.test(code)) {
      findings.push({ level: "tip", title: "Wire up events", detail: "Use button.Click += (s, e) => ... to make controls respond. Without a handler, nothing reacts." });
    }
    if (/\bTimer\b|\bOpenFileDialog\b|\bSaveFileDialog\b/.test(code)) {
      findings.push({ level: "coverage", title: "Timers and dialogs are not simulated", detail: "Timer and file dialogs are recognized but won't run in the browser preview." });
    }
    if (/\bDataGridView\b/.test(code)) {
      findings.push({ level: "coverage", title: "DataGridView is not simulated", detail: "The preview does not render DataGridView rows. A ListBox or ListView gives closer preview behavior." });
    }
    return findings;
  }

  function reviewCpp(code) {
    const findings = [];
    if (/\bQApplication\b/.test(code) && !/\bapp\.exec\s*\(/.test(code)) {
      findings.push({ level: "issue", title: "Event loop never starts", detail: "End main() with return app.exec() so Qt can deliver signals and keep the window alive." });
    }
    if (/QObject::connect\s*\([^)]*SIGNAL\s*\(/.test(code)) {
      findings.push({ level: "style", title: "Use the modern connect syntax", detail: "Prefer QObject::connect(sender, &Sender::clicked, receiver, &Receiver::slot) over the old SIGNAL()/SLOT() macros — the compiler checks the connection." });
    }
    if (/new\s+Q(Label|PushButton|LineEdit)\s*\(\s*"/.test(code) && !/layout->addWidget/.test(code)) {
      findings.push({ level: "tip", title: "Give widgets a home", detail: "Widgets created with new need a parent or a layout (layout->addWidget(...)) so Qt owns and cleans them up." });
    }
    if (/\bQThread\b/.test(code)) {
      findings.push({ level: "coverage", title: "QThread is not simulated", detail: "Threads won't run in the browser preview, but work in a real Qt build." });
    }
    return findings;
  }

  function reviewElectron(code) {
    const findings = [];
    if (/\bipcMain\b/.test(code) && /document\.\w+/.test(code)) {
      findings.push({ level: "issue", title: "Main-process API in the renderer", detail: "ipcMain belongs in the main process file. The renderer talks to it through ipcRenderer.invoke / ipcRenderer.send." });
    }
    if (/\bipcRenderer\b/.test(code) && !/\bcontextBridge\b/.test(code)) {
      findings.push({ level: "style", title: "Expose IPC through a preload", detail: "Use contextBridge.exposeInMainWorld in a preload script so the renderer gets a small, safe API instead of raw ipcRenderer." });
    }
    if (/nodeIntegration\s*:\s*true/.test(code)) {
      findings.push({ level: "issue", title: "nodeIntegration is a security risk", detail: "Turning on nodeIntegration gives renderer pages full Node access. Prefer a contextBridge preload with contextIsolation enabled." });
    }
    if (/\b(BrowserWindow|dialog|Tray|Notification|globalShortcut)\b/.test(code)) {
      findings.push({ level: "coverage", title: "Electron APIs are not simulated", detail: "BrowserWindow, dialogs, Tray, and notifications are recognized but only the renderer DOM is recreated in the preview." });
    }
    return findings;
  }

  const reviewers = { java: reviewJava, python: reviewPython, csharp: reviewCSharp, cpp: reviewCpp, electron: reviewElectron };

  function recognizedApis(code, language) {
    const pattern = componentPatterns[language];
    const seen = [...new Set((code.match(pattern) || []).map((m) => m.replace(/new\s+|\W/g, "").replace(/^(tk|ttk)\./, "")))];
    return seen.slice(0, 10);
  }

  function analyze(code, requestedLanguage = "auto") {
    const language = requestedLanguage !== "auto" ? requestedLanguage : detectLanguage(code);
    let languageKey = null;
    let languageLabel = "Unknown toolkit";
    let findings = [];
    if (language) {
      languageKey = language;
      languageLabel = LANGUAGE_LABELS[language];
      findings = reviewers[language](code);
    } else {
      findings = [{ level: "tip", title: "No desktop toolkit detected", detail: "Paste a Swing, Tkinter, WinForms, Qt, or Electron snippet so the review can check it properly." }];
    }
    const counts = { issue: 0, style: 0, tip: 0, coverage: 0 };
    findings.forEach((finding) => { counts[finding.level] += 1; });
    return {
      languageKey,
      languageLabel,
      findings,
      lines: code.split("\n").length,
      components: languageKey ? countMatches(code, componentPatterns[languageKey]) : 0,
      listeners: languageKey ? countMatches(code, listenerPatterns[languageKey]) : 0,
      apis: languageKey ? recognizedApis(code, languageKey) : [],
      counts,
      verdict: counts.issue > 0 ? "Almost there" : counts.style > 0 ? "Solid, with polish to add" : "Looks clean"
    };
  }

  function escapeHtml(value) {
    return String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  }

  function renderFinding(finding) {
    return `<div class="review-finding review-${finding.level}">
      <span class="review-finding-dot" aria-hidden="true"></span>
      <div><strong>${escapeHtml(finding.title)}</strong><p>${escapeHtml(finding.detail)}</p></div>
    </div>`;
  }

  function runReview() {
    const code = $("reviewCode").value;
    const results = $("reviewResults");
    const resultsTitle = $("reviewResultsTitle");

    if (!code.trim()) {
      resultsTitle.textContent = "Nothing to review";
      results.innerHTML = '<div class="review-empty"><strong>Paste some code first.</strong><p>Add a Swing, Tkinter, WinForms, Qt, or Electron snippet, then run the review.</p></div>';
      $("reviewSummary").hidden = true;
      return;
    }

    const result = analyze(code, $("reviewLanguage").value);

    $("reviewVerdict").textContent = result.verdict;
    $("reviewDetected").textContent = result.languageLabel;
    $("reviewStats").textContent = `${result.lines} lines · ${result.components} components · ${result.listeners} listeners`;

    const chips = $("reviewChips");
    if (result.apis.length) {
      chips.innerHTML = result.apis.map((api) => `<span>${escapeHtml(api)}</span>`).join("");
      chips.hidden = false;
    } else {
      chips.hidden = true;
    }

    const summary = [
      [result.counts.issue, "issue", "review-issue"],
      [result.counts.style, "style", "review-style"],
      [result.counts.tip, "tip", "review-tip"],
      [result.counts.coverage, "not simulated", "review-coverage"]
    ].filter((entry) => entry[0] > 0).map((entry) => `<span class="review-count review-${entry[2]}"><strong>${entry[0]}</strong> ${entry[1]}</span>`).join("") ||
      '<span class="review-count review-clean"><strong>0</strong> findings</span>';
    $("reviewSummaryCounts").innerHTML = summary;

    const sectionFor = {
      issue: ["Fix this first", "Changes that keep the app from working as intended."],
      style: ["Style & structure", "Small habits that make the code easier to keep."],
      tip: ["Good to know", "Quick wins and plain-language guidance."],
      coverage: ["Simulator coverage", "What works in a real environment but not in the browser preview."]
    };

    resultsTitle.textContent = `${result.findings.length} finding${result.findings.length === 1 ? "" : "s"} · ${result.languageLabel}`;
    let html = "";
    for (const level of Object.keys(sectionFor)) {
      const list = result.findings.filter((finding) => finding.level === level);
      if (!list.length) continue;
      const [title, subtitle] = sectionFor[level];
      html += `<section class="review-section" data-level="${level}">
        <header><div><h2>${title}</h2><p>${subtitle}</p></div><span class="review-section-count">${list.length}</span></header>
        <div>${list.map(renderFinding).join("")}</div>
      </section>`;
    }
    results.innerHTML = html;
    $("reviewSummary").hidden = false;
    $("reviewResults").scrollIntoView({ behavior: "smooth", block: "nearest" });
  }

  function loadSample() {
    const requested = $("reviewLanguage").value;
    const key = requested !== "auto" ? requested : "java";
    $("reviewCode").value = sampleLines[key];
    $("reviewFileName").textContent = {
      java: "Main.java", python: "main.py", csharp: "MainForm.cs", cpp: "main.cpp", electron: "main.js"
    }[key];
    $("reviewCode").dispatchEvent(new Event("input"));
    runReview();
  }

  globalThis.DesktopcraftReview = { analyze, detectLanguage, sampleLines };

  document.addEventListener("DOMContentLoaded", () => {
    $("reviewCode").value = localStorage.getItem("desktopcraft-review-draft") || "";
    $("reviewFileName").textContent = "Main.java";
    $("reviewCode").addEventListener("input", () => {
      localStorage.setItem("desktopcraft-review-draft", $("reviewCode").value);
      $("reviewLineCount").textContent = String($("reviewCode").value.split("\n").length);
      $("reviewByteCount").textContent = String($("reviewCode").value.length);
    });
    $("reviewForm").addEventListener("submit", (event) => {
      event.preventDefault();
      runReview();
    });
    $("loadReviewSample").addEventListener("click", loadSample);
    $("reviewCode").addEventListener("keydown", (event) => {
      if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
        event.preventDefault();
        runReview();
      }
    });
    if (!$("reviewCode").value.trim()) loadSample();
  });
})();
