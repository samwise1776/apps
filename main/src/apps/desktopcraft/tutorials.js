const tutorials = [
  {
    id: "java-swing",
    language: "java",
    languageLabel: "Java",
    toolkit: "Java Swing",
    short: "SW",
    fileName: "Main.java",
    title: "Build your first Swing window",
    description: "Create a JFrame on Swing’s event thread, arrange a label and button, and connect a click to a visible result.",
    tags: ["JFrame", "JButton", "ActionListener"],
    hint: "Try changing the JFrame title or button label.",
    steps: [
      ["Create", "Make the JFrame on the Event Dispatch Thread with SwingUtilities.invokeLater."],
      ["Compose", "Add components to a panel and let a layout manager arrange them."],
      ["React", "Attach an ActionListener and update the label when the button is clicked."]
    ],
    code: `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Swing Quick Start");
            JLabel status = new JLabel("Ready to build", SwingConstants.CENTER);
            JButton action = new JButton("Say hello");

            action.addActionListener(event -> status.setText("Hello from Swing!"));

            JPanel content = new JPanel(new BorderLayout(8, 8));
            content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
            content.add(status, BorderLayout.CENTER);
            content.add(action, BorderLayout.SOUTH);
            frame.setContentPane(content);
            frame.setSize(440, 240);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`
  },
  {
    id: "python-tkinter",
    language: "python",
    languageLabel: "Python",
    toolkit: "Python Tkinter",
    short: "PY",
    fileName: "main.py",
    title: "Open a Tkinter interface",
    description: "Use Python’s built-in Tk toolkit to create a window, place themed controls, and connect a command function.",
    tags: ["Tk", "ttk", "command"],
    hint: "Change the root title or the text passed to status.config.",
    steps: [
      ["Root", "Create one tk.Tk root window and give it a useful title and size."],
      ["Widgets", "Place ttk controls in a padded frame so the interface can resize cleanly."],
      ["Command", "Pass a function to the button’s command option to handle clicks."]
    ],
    code: `import tkinter as tk
from tkinter import ttk

root = tk.Tk()
root.title("Tkinter Quick Start")
root.geometry("440x240")

content = ttk.Frame(root, padding=24)
content.pack(fill="both", expand=True)
status = ttk.Label(content, text="Ready to build")

def say_hello():
    status.config(text="Hello from Tkinter!")

action = ttk.Button(content, text="Say hello", command=say_hello)
status.pack(pady=14)
action.pack(pady=8)
root.mainloop()`
  },
  {
    id: "csharp-winforms",
    language: "csharp",
    languageLabel: "C#",
    toolkit: "C# WinForms",
    short: "C#",
    fileName: "MainForm.cs",
    title: "Compose a WinForms screen",
    description: "Create a Form, add managed controls, and use the Click event to update application state on Windows.",
    tags: ["Form", "Button", "Click event"],
    hint: "Change the Form Text or the message assigned inside Click.",
    steps: [
      ["Form", "Subclass Form and configure its title, size, and start position."],
      ["Controls", "Create labels and buttons, then add them to a layout panel."],
      ["Event", "Subscribe to Click with a lambda and update the status label."]
    ],
    code: `using System;
using System.Drawing;
using System.Windows.Forms;

public class MainForm : Form
{
    public MainForm()
    {
        Text = "WinForms Quick Start";
        ClientSize = new Size(440, 240);
        StartPosition = FormStartPosition.CenterScreen;

        var status = new Label { Text = "Ready to build", AutoSize = true };
        var action = new Button { Text = "Say hello", AutoSize = true };
        action.Click += (_, _) => status.Text = "Hello from WinForms!";

        var content = new FlowLayoutPanel { Dock = DockStyle.Fill, Padding = new Padding(24) };
        content.Controls.Add(status);
        content.Controls.Add(action);
        Controls.Add(content);
    }

    [STAThread]
    public static void Main()
    {
        ApplicationConfiguration.Initialize();
        Application.Run(new MainForm());
    }
}`
  },
  {
    id: "cpp-qt",
    language: "cpp",
    languageLabel: "C++",
    toolkit: "C++ Qt Widgets",
    short: "QT",
    fileName: "main.cpp",
    title: "Connect a Qt signal",
    description: "Build a Qt Widgets window with a vertical layout and connect a QPushButton signal to a small lambda.",
    tags: ["QWidget", "QVBoxLayout", "QObject::connect"],
    hint: "Change setWindowTitle or the string passed to status->setText.",
    steps: [
      ["Application", "Create QApplication first; it owns the native event loop."],
      ["Layout", "Attach QVBoxLayout to the window and add heap-allocated widgets."],
      ["Signal", "Connect QPushButton::clicked to a lambda that changes the label."]
    ],
    code: `#include <QApplication>
#include <QLabel>
#include <QPushButton>
#include <QVBoxLayout>
#include <QWidget>

int main(int argc, char *argv[]) {
    QApplication app(argc, argv);
    QWidget window;
    window.setWindowTitle("Qt Quick Start");
    window.resize(440, 240);

    auto *status = new QLabel("Ready to build");
    auto *action = new QPushButton("Say hello");
    auto *layout = new QVBoxLayout(&window);
    layout->addWidget(status);
    layout->addWidget(action);
    QObject::connect(action, &QPushButton::clicked, [status]() {
        status->setText("Hello from Qt!");
    });

    window.show();
    return app.exec();
}`
  },
  {
    id: "javascript-electron",
    language: "javascript",
    languageLabel: "JavaScript",
    toolkit: "JavaScript Electron",
    short: "JS",
    fileName: "renderer.js",
    title: "Make an Electron renderer react",
    description: "Use familiar HTML controls inside an Electron window and connect a DOM event listener to visible output.",
    tags: ["Electron", "DOM", "addEventListener"],
    hint: "Change document.title or the final status.textContent value.",
    steps: [
      ["Window", "The Electron main process creates BrowserWindow; this renderer supplies its interface."],
      ["Markup", "Render native-looking controls with a small HTML template."],
      ["Listener", "Use addEventListener to update the result when the button is clicked."]
    ],
    code: `document.title = "Electron Quick Start";
const app = document.querySelector("#app");

app.innerHTML = \`
  <label id="heading">Desktopcraft Electron</label>
  <p id="status">Ready to build</p>
  <button id="action">Say hello</button>
\`;

const status = document.querySelector("#status");
document.querySelector("#action").addEventListener("click", () => {
    status.textContent = "Hello from Electron!";
});`
  }
];

const tutorialStorageKey = "desktopcraft-tutorial-progress-v1";
const tutorialCourseKey = "desktopcraft-active-course-v1";
const tutorialElements = {
  list: document.querySelector("#tutorialList"),
  search: document.querySelector("#tutorialSearch"),
  language: document.querySelector("#tutorialLanguage"),
  kicker: document.querySelector("#tutorialKicker"),
  title: document.querySelector("#tutorialTitle"),
  description: document.querySelector("#tutorialDescription"),
  tags: document.querySelector("#tutorialTags"),
  steps: document.querySelector("#tutorialSteps"),
  fileName: document.querySelector("#tutorialFileName"),
  code: document.querySelector("#tutorialCode"),
  hint: document.querySelector("#tutorialCodeHint"),
  preview: document.querySelector("#tutorialPreview"),
  consoleBar: document.querySelector("#tutorialConsoleBar"),
  console: document.querySelector("#tutorialConsole"),
  courseLink: document.querySelector("#tutorialCourseLink"),
  previous: document.querySelector("#previousTutorial"),
  next: document.querySelector("#nextTutorial"),
  complete: document.querySelector("#completeTutorial"),
  progressCount: document.querySelector("#tutorialProgressCount"),
  progressPercent: document.querySelector("#tutorialProgressPercent"),
  progressBar: document.querySelector("#tutorialProgressBar"),
  toast: document.querySelector("#tutorialToast")
};

let activeTutorialIndex = Math.max(0, tutorials.findIndex((tutorial) => `#${tutorial.id}` === window.location.hash));
let completedTutorials = new Set();
let tutorialToastTimeout;

try {
  const stored = JSON.parse(localStorage.getItem(tutorialStorageKey) || "[]");
  if (Array.isArray(stored)) completedTutorials = new Set(stored.filter((id) => tutorials.some((tutorial) => tutorial.id === id)));
} catch {
  completedTutorials = new Set();
}

function escapeTutorialHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function renderTutorialList() {
  const query = tutorialElements.search.value.trim().toLowerCase();
  const visible = tutorials.filter((tutorial) => `${tutorial.toolkit} ${tutorial.languageLabel} ${tutorial.tags.join(" ")}`.toLowerCase().includes(query));
  tutorialElements.list.innerHTML = visible.length
    ? visible.map((tutorial) => {
      const index = tutorials.indexOf(tutorial);
      const complete = completedTutorials.has(tutorial.id);
      return `<button class="tutorial-list-button${index === activeTutorialIndex ? " active" : ""}" data-tutorial="${index}" ${index === activeTutorialIndex ? 'aria-current="page"' : ""}>
        <span class="tutorial-list-icon">${escapeTutorialHtml(tutorial.short)}</span>
        <span class="tutorial-list-copy"><strong>${escapeTutorialHtml(tutorial.toolkit)}</strong><small>${escapeTutorialHtml(tutorial.title)}</small></span>
        <span class="tutorial-list-state" aria-label="${complete ? "Complete" : "Not complete"}">${complete ? "✓" : "○"}</span>
      </button>`;
    }).join("")
    : '<div class="tutorial-empty">No tutorial matches that search.</div>';
}

function updateTutorialProgress() {
  const count = completedTutorials.size;
  const percent = Math.round((count / tutorials.length) * 100);
  tutorialElements.progressCount.textContent = `${count} of ${tutorials.length} complete`;
  tutorialElements.progressPercent.textContent = `${percent}%`;
  tutorialElements.progressBar.style.width = `${percent}%`;
  try {
    localStorage.setItem(tutorialStorageKey, JSON.stringify([...completedTutorials]));
  } catch {
    // Completion still works for this browser session.
  }
}

function runTutorial() {
  const tutorial = tutorials[activeTutorialIndex];
  tutorialElements.consoleBar.classList.remove("error");
  try {
    const metrics = window.DesktopSimulator.render(tutorialElements.code.value, tutorialElements.preview, {
      language: tutorial.language,
      languageLabel: tutorial.toolkit,
      onConsole: (message) => {
        tutorialElements.console.textContent = message;
      }
    });
    tutorialElements.console.textContent = `Preview ready: ${metrics.components} component${metrics.components === 1 ? "" : "s"}, ${metrics.listeners} event connection${metrics.listeners === 1 ? "" : "s"}.`;
  } catch (error) {
    tutorialElements.consoleBar.classList.add("error");
    tutorialElements.console.textContent = `Simulator error: ${error.message}`;
  }
}

function renderTutorial() {
  const tutorial = tutorials[activeTutorialIndex];
  tutorialElements.language.textContent = tutorial.languageLabel.toUpperCase();
  tutorialElements.kicker.textContent = `${tutorial.toolkit.toUpperCase()} QUICK START`;
  tutorialElements.title.textContent = tutorial.title;
  tutorialElements.description.textContent = tutorial.description;
  tutorialElements.tags.innerHTML = tutorial.tags.map((tag) => `<span class="lesson-tag">${escapeTutorialHtml(tag)}</span>`).join("");
  tutorialElements.steps.innerHTML = tutorial.steps.map(([title, copy], index) => `<div class="tutorial-step"><span>STEP ${index + 1}</span><h4>${escapeTutorialHtml(title)}</h4><p>${escapeTutorialHtml(copy)}</p></div>`).join("");
  tutorialElements.fileName.textContent = tutorial.fileName;
  tutorialElements.code.value = tutorial.code;
  tutorialElements.hint.textContent = tutorial.hint;
  tutorialElements.complete.textContent = completedTutorials.has(tutorial.id) ? "Tutorial completed ✓" : "Mark tutorial complete";
  tutorialElements.previous.disabled = activeTutorialIndex === 0;
  tutorialElements.next.disabled = activeTutorialIndex === tutorials.length - 1;
  tutorialElements.courseLink.onclick = () => {
    try { localStorage.setItem(tutorialCourseKey, tutorial.id); } catch { /* Open the default course if storage is unavailable. */ }
  };
  window.history.replaceState(null, "", `#${tutorial.id}`);
  document.title = `${tutorial.toolkit} tutorial — Desktopcraft`;
  renderTutorialList();
  runTutorial();
}

function setTutorial(index) {
  activeTutorialIndex = Math.min(Math.max(index, 0), tutorials.length - 1);
  renderTutorial();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function showTutorialToast(message) {
  window.clearTimeout(tutorialToastTimeout);
  tutorialElements.toast.textContent = message;
  tutorialElements.toast.classList.add("show");
  tutorialToastTimeout = window.setTimeout(() => tutorialElements.toast.classList.remove("show"), 2200);
}

tutorialElements.list.addEventListener("click", (event) => {
  const button = event.target.closest("[data-tutorial]");
  if (button) setTutorial(Number(button.dataset.tutorial));
});

tutorialElements.search.addEventListener("input", renderTutorialList);
tutorialElements.previous.addEventListener("click", () => setTutorial(activeTutorialIndex - 1));
tutorialElements.next.addEventListener("click", () => setTutorial(activeTutorialIndex + 1));
tutorialElements.complete.addEventListener("click", () => {
  const tutorial = tutorials[activeTutorialIndex];
  completedTutorials.add(tutorial.id);
  updateTutorialProgress();
  renderTutorialList();
  tutorialElements.complete.textContent = "Tutorial completed ✓";
  showTutorialToast(`${tutorial.toolkit} tutorial completed`);
});

document.querySelector("#runTutorial").addEventListener("click", runTutorial);
document.querySelector("#resetTutorialCode").addEventListener("click", () => {
  tutorialElements.code.value = tutorials[activeTutorialIndex].code;
  runTutorial();
  showTutorialToast("Starter example restored");
});
document.querySelector("#copyTutorialCode").addEventListener("click", async () => {
  try {
    await navigator.clipboard.writeText(tutorialElements.code.value);
  } catch {
    tutorialElements.code.select();
    document.execCommand("copy");
  }
  showTutorialToast("Tutorial code copied");
});
tutorialElements.code.addEventListener("keydown", (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
    event.preventDefault();
    runTutorial();
  }
  if (event.key === "Tab") {
    event.preventDefault();
    const start = tutorialElements.code.selectionStart;
    const end = tutorialElements.code.selectionEnd;
    tutorialElements.code.value = `${tutorialElements.code.value.slice(0, start)}    ${tutorialElements.code.value.slice(end)}`;
    tutorialElements.code.selectionStart = tutorialElements.code.selectionEnd = start + 4;
  }
});

updateTutorialProgress();
renderTutorial();
