(function () {
  const courseDefinitions = [
    {
      id: "python-tkinter",
      title: "Python Tkinter Desktop Apps",
      shortTitle: "Tkinter",
      language: "python",
      languageLabel: "Python",
      fileName: "app.py",
      description: "Build approachable native desktop tools with Python's standard GUI toolkit.",
      modules: [
        ["Tkinter foundations", [
          ["Open your first window", "Tk", "Create the root window, give it a title, and enter Tk's event loop.", "Window ready", "Python window launched"],
          ["Explain with labels", "Label", "Use Label widgets for short, non-editable interface copy.", "Label ready", "Helpful label displayed"],
          ["Trigger commands", "Button", "Connect a Button command to a focused Python function.", "Button pressed", "Python command handled"],
          ["Read short input", "Entry", "Read the current Entry value only when an action needs it.", "Entry read", "Typed value captured"],
          ["Create longer notes", "Text", "Use Text for multi-line content and explicit line-and-column indexes.", "Text changed", "Multi-line note updated"]
        ]],
        ["Layout and controls", [
          ["Stack with pack", "pack", "Use pack for simple edge-based rows and columns.", "Packed widgets", "Pack layout balanced"],
          ["Build forms with grid", "grid", "Align labels and inputs on a row-and-column grid.", "Grid created", "Form grid aligned"],
          ["Place with restraint", "place", "Use relative placement for overlays and intentionally positioned details.", "Widget placed", "Relative position applied"],
          ["Toggle preferences", "Checkbutton", "Bind independent Boolean choices to Tkinter variables.", "Choice toggled", "Preference stored"],
          ["Choose one option", "Radiobutton", "Share one variable between radio buttons to enforce a single choice.", "Option selected", "Single choice confirmed"]
        ]],
        ["Data and events", [
          ["Offer compact choices", "ttk.Combobox", "Present a constrained set of values in a themed combo box.", "Combo changed", "Selection applied"],
          ["Display collections", "Listbox", "Insert, select, and remove visible collection items.", "Item selected", "List interaction complete"],
          ["Present rows", "ttk.Treeview", "Use Treeview columns for structured rows and hierarchical data.", "Rows loaded", "Treeview populated"],
          ["Handle keyboard events", "bind", "Bind named key events to callbacks that accept an event object.", "Key received", "Keyboard binding active"],
          ["Schedule updates", "after", "Use after instead of blocking sleeps to keep Tk responsive.", "Timer scheduled", "Non-blocking update complete"]
        ]],
        ["Polish and projects", [
          ["Use themed widgets", "ttk.Style", "Configure reusable ttk styles while respecting platform themes.", "Style loaded", "Tkinter theme polished"],
          ["Open useful dialogs", "messagebox", "Use message, confirmation, and file dialogs for brief decisions.", "Dialog opened", "Dialog result handled"],
          ["Build a calculator", "Calculator", "Compose buttons, state, and a display into a complete numeric loop.", "Calculation ready", "Calculator result shown"],
          ["Build a notes editor", "Notes editor", "Combine Text, file commands, and dirty-state feedback.", "Note drafted", "Desktop note saved"],
          ["Build a task tracker", "Task tracker", "Connect Entry, Listbox, and commands through a small task model.", "Task drafted", "Python task added"]
        ]]
      ]
    },
    {
      id: "csharp-winforms",
      title: "C# Windows Forms Apps",
      shortTitle: "WinForms",
      language: "csharp",
      languageLabel: "C#",
      fileName: "MainForm.cs",
      description: "Create productive Windows desktop interfaces with C#, events, controls, and data binding.",
      modules: [
        ["WinForms foundations", [
          ["Create a Form", "Form", "Configure the top-level window and start it through Application.Run.", "Form ready", "C# window launched"],
          ["Display clear text", "Label", "Use Label for concise instructions and live status.", "Label ready", "WinForms label updated"],
          ["Handle button clicks", "Button.Click", "Subscribe to Click with a short event handler.", "Button clicked", "C# event handled"],
          ["Capture text", "TextBox", "Read TextBox.Text inside the action that consumes it.", "Text captured", "C# input accepted"],
          ["Edit longer content", "RichTextBox", "Use RichTextBox for multi-line editing and styled selections.", "Document changed", "Rich text updated"]
        ]],
        ["Layout and choices", [
          ["Flow responsive controls", "FlowLayoutPanel", "Let a flow panel position and wrap child controls.", "Flow panel ready", "Controls flowed correctly"],
          ["Align structured forms", "TableLayoutPanel", "Use percentage and autosized rows and columns for forms.", "Table layout ready", "WinForms grid aligned"],
          ["Toggle settings", "CheckBox", "Represent independent Boolean choices with checked state.", "Setting toggled", "Checkbox preference saved"],
          ["Choose one value", "RadioButton", "Group radio buttons within a shared container.", "Choice selected", "WinForms choice confirmed"],
          ["Select from lists", "ComboBox", "Bind or add compact choices and read SelectedItem safely.", "Item selected", "ComboBox value applied"]
        ]],
        ["Data and commands", [
          ["Show visible lists", "ListBox", "Display objects with selection separate from stored data.", "List loaded", "ListBox selection handled"],
          ["Present data tables", "DataGridView", "Bind tabular data and configure columns, edits, and selection.", "Grid loaded", "DataGridView populated"],
          ["Organize commands", "MenuStrip", "Place infrequent commands in familiar application menus.", "Menu ready", "Menu command connected"],
          ["Validate fields", "ErrorProvider", "Show recoverable validation messages beside the relevant control.", "Input checked", "WinForms form validated"],
          ["Run scheduled work", "Timer", "Use a UI timer for small periodic updates without blocking input.", "Timer ticked", "WinForms timer running"]
        ]],
        ["Architecture and projects", [
          ["Keep work responsive", "async / await", "Await I/O work and return naturally to the UI synchronization context.", "Work started", "Async operation completed"],
          ["Remember settings", "ApplicationSettingsBase", "Persist lightweight user preferences between sessions.", "Setting written", "User preference restored"],
          ["Build a calculator", "Calculator", "Model numeric operations separately from button labels.", "Calculation ready", "WinForms result shown"],
          ["Build a contacts app", "Contacts", "Coordinate a list, edit form, validation, and record model.", "Contact drafted", "C# contact saved"],
          ["Build inventory", "Inventory", "Combine DataGridView, commands, async loading, and settings.", "Item drafted", "Inventory record added"]
        ]]
      ]
    },
    {
      id: "cpp-qt",
      title: "C++ Qt Widgets Apps",
      shortTitle: "Qt Widgets",
      language: "cpp",
      languageLabel: "C++",
      fileName: "main.cpp",
      description: "Build cross-platform native desktop software with Qt Widgets, layouts, signals, and models.",
      modules: [
        ["Qt foundations", [
          ["Start QApplication", "QApplication", "Create one application object before constructing widgets.", "Application ready", "Qt event loop started"],
          ["Show a QWidget", "QWidget", "Use QWidget as a window or a reusable region inside another widget.", "Widget ready", "Qt window displayed"],
          ["Explain with QLabel", "QLabel", "Display text or pixmaps and configure useful alignment.", "Label ready", "Qt label updated"],
          ["Connect QPushButton", "QPushButton", "Connect the clicked signal to a lambda or receiver slot.", "Button clicked", "Qt signal received"],
          ["Capture QLineEdit", "QLineEdit", "Read current text and use validators for constrained input.", "Text captured", "Qt input accepted"]
        ]],
        ["Layouts and controls", [
          ["Stack vertically", "QVBoxLayout", "Place widgets in a vertical sequence with stretch and spacing.", "Vertical layout ready", "Qt stack balanced"],
          ["Arrange horizontally", "QHBoxLayout", "Build tool rows and paired controls in a horizontal layout.", "Horizontal layout ready", "Qt row aligned"],
          ["Build form grids", "QGridLayout", "Place widgets by row and column with spans where needed.", "Grid ready", "Qt form grid complete"],
          ["Toggle options", "QCheckBox", "Use checked state and stateChanged signals for independent choices.", "Option toggled", "Qt preference updated"],
          ["Offer choices", "QComboBox", "Store visible labels and optional user data in compact selections.", "Choice selected", "Qt combo value applied"]
        ]],
        ["Models and commands", [
          ["Display lists", "QListWidget", "Use item widgets for simple lists and model/view for richer data.", "List loaded", "Qt list selection handled"],
          ["Use model/view tables", "QTableView", "Separate tabular data into a model consumed by the view.", "Table ready", "Qt table model connected"],
          ["Build application menus", "QMenuBar", "Create QAction objects that can be shared by menus and toolbars.", "Menu ready", "Qt action connected"],
          ["Show dialogs", "QMessageBox", "Use standard dialogs for brief messages and decisions.", "Dialog opened", "Qt dialog handled"],
          ["Validate input", "QValidator", "Constrain acceptable text without scattering parsing logic.", "Input checked", "Qt value validated"]
        ]],
        ["Performance and projects", [
          ["Schedule updates", "QTimer", "Deliver periodic timeout signals through the event loop.", "Timer started", "Qt timer update complete"],
          ["Move slow work", "QThread", "Move worker objects off the GUI thread and return results with signals.", "Worker started", "Qt background task finished"],
          ["Paint a custom widget", "QPainter", "Draw inside paintEvent with scoped painter state.", "Canvas ready", "Qt custom painting visible"],
          ["Build a notes app", "Notes", "Combine QTextEdit, file actions, and unsaved-state feedback.", "Note drafted", "Qt note saved"],
          ["Build a media queue", "Media queue", "Connect a list model, playback actions, and background metadata loading.", "Track queued", "Qt media queue updated"]
        ]]
      ]
    },
    {
      id: "javascript-electron",
      title: "JavaScript Electron Desktop Apps",
      shortTitle: "Electron",
      language: "javascript",
      languageLabel: "JavaScript",
      fileName: "renderer.js",
      description: "Turn web skills into secure cross-platform desktop applications with Electron.",
      modules: [
        ["Electron foundations", [
          ["Understand the processes", "Main and renderer", "Separate privileged application lifecycle work from rendered interface code.", "Processes ready", "Electron architecture understood"],
          ["Open BrowserWindow", "BrowserWindow", "Configure and show a native window from the main process.", "Window ready", "Electron window launched"],
          ["Build the interface", "HTML and DOM", "Use semantic HTML controls as the renderer's visual layer.", "Interface ready", "Desktop DOM rendered"],
          ["Handle button actions", "addEventListener", "Connect DOM events to focused renderer behavior.", "Button clicked", "Electron action handled"],
          ["Capture form values", "HTMLInputElement", "Read current input values at the moment an action needs them.", "Input captured", "Renderer value accepted"]
        ]],
        ["Secure platform access", [
          ["Expose a safe bridge", "contextBridge", "Publish a narrow preload API instead of exposing Node directly.", "Bridge ready", "Secure API exposed"],
          ["Send IPC requests", "ipcRenderer.invoke", "Use request-response IPC for renderer-to-main operations.", "Request sent", "IPC result received"],
          ["Handle IPC safely", "ipcMain.handle", "Validate arguments and keep privileged operations in the main process.", "Handler ready", "Main process request handled"],
          ["Open native dialogs", "dialog", "Request file and message dialogs through the secure bridge.", "Dialog requested", "Native dialog handled"],
          ["Read files carefully", "fs/promises", "Keep file access in the privileged layer and return only needed data.", "File requested", "Desktop file loaded safely"]
        ]],
        ["Desktop capabilities", [
          ["Create native menus", "Menu", "Build platform-aware menu templates and command roles.", "Menu ready", "Electron menu installed"],
          ["Use the system tray", "Tray", "Keep a persistent tray object and attach a concise context menu.", "Tray ready", "Tray action connected"],
          ["Send notifications", "Notification", "Use native notifications for timely background results.", "Notification ready", "Desktop notification sent"],
          ["Register shortcuts", "globalShortcut", "Register global shortcuts sparingly and release them on shutdown.", "Shortcut ready", "Global shortcut active"],
          ["Persist settings", "electron-store", "Store small preferences outside renderer presentation code.", "Setting written", "Electron preference restored"]
        ]],
        ["Quality and projects", [
          ["Design accessible controls", "Accessibility", "Preserve semantic controls, focus order, names, and keyboard operation.", "Keyboard ready", "Electron interface accessible"],
          ["Package the app", "Electron Forge", "Configure distributable builds, icons, signing, and platform metadata.", "Build configured", "Desktop package prepared"],
          ["Build a calculator", "Calculator", "Keep calculation state separate from DOM rendering.", "Calculation ready", "Electron result shown"],
          ["Build a notes app", "Notes", "Connect an editor to safe file commands through preload APIs.", "Note drafted", "Electron note saved"],
          ["Build a task board", "Task board", "Model tasks, render columns, and persist changes through a narrow data API.", "Task drafted", "Electron task added"]
        ]]
      ]
    }
  ];

  const escapeText = (value) => value.replaceAll("\\", "\\\\").replaceAll('"', '\\"');

  function pythonCode(title, api, starter) {
    return `import tkinter as tk
from tkinter import ttk, messagebox

root = tk.Tk()
root.title("${escapeText(title)}")
root.geometry("430x230")

panel = ttk.Frame(root, padding=18)
panel.pack(fill="both", expand=True)
status = ttk.Label(panel, text="Ready to explore ${escapeText(api)}")
entry = ttk.Entry(panel)
entry.insert(0, "Desktop app")

def run_demo():
    status.config(text="${escapeText(starter)}")

action = ttk.Button(panel, text="Try ${escapeText(api)}", command=run_demo)
entry.pack(fill="x", pady=6)
action.pack(pady=6)
status.pack(pady=6)
root.mainloop()`;
  }

  function csharpCode(title, api, starter) {
    return `using System;
using System.Drawing;
using System.Windows.Forms;

public class MainForm : Form
{
    private readonly Label status = new Label();

    public MainForm()
    {
        Text = "${escapeText(title)}";
        Size = new Size(460, 240);
        var panel = new FlowLayoutPanel { Dock = DockStyle.Fill, Padding = new Padding(18) };
        var input = new TextBox { Text = "Desktop app", Width = 180 };
        var action = new Button { Text = "Try ${escapeText(api)}", AutoSize = true };
        status.Text = "Ready";
        status.AutoSize = true;

        action.Click += (sender, args) => status.Text = "${escapeText(starter)}";
        panel.Controls.Add(input);
        panel.Controls.Add(action);
        panel.Controls.Add(status);
        Controls.Add(panel);
    }

    [STAThread]
    public static void Main()
    {
        ApplicationConfiguration.Initialize();
        Application.Run(new MainForm());
    }
}`;
  }

  function cppCode(title, api, starter) {
    return `#include <QApplication>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>
#include <QVBoxLayout>
#include <QWidget>

int main(int argc, char *argv[])
{
    QApplication app(argc, argv);
    QWidget window;
    window.setWindowTitle("${escapeText(title)}");
    window.resize(440, 230);

    auto *layout = new QVBoxLayout(&window);
    auto *input = new QLineEdit("Desktop app");
    auto *action = new QPushButton("Try ${escapeText(api)}");
    auto *status = new QLabel("Ready");
    layout->addWidget(input);
    layout->addWidget(action);
    layout->addWidget(status);

    QObject::connect(action, &QPushButton::clicked, [=]() {
        status->setText("${escapeText(starter)}");
    });

    window.show();
    return app.exec();
}`;
  }

  function javascriptCode(title, api, starter) {
    return `// Renderer-side lesson for ${api}
document.title = "${escapeText(title)}";

const app = document.querySelector("#app");
app.innerHTML = ` + "`" + `
  <main class="desktop-panel">
    <input id="input" value="Desktop app" />
    <button id="action">Try ${escapeText(api)}</button>
    <p id="status">Ready</p>
  </main>
` + "`" + `;

const status = document.querySelector("#status");
document.querySelector("#action").addEventListener("click", () => {
  status.textContent = "${escapeText(starter)}";
});`;
  }

  const codeFactories = {
    python: pythonCode,
    csharp: csharpCode,
    cpp: cppCode,
    javascript: javascriptCode
  };

  function buildLesson(course, moduleName, moduleIndex, spec, lessonIndex) {
    const [title, api, detail, starter, goal] = spec;
    return {
      module: moduleName,
      moduleIndex: moduleIndex + 1,
      navTitle: title,
      navSubtitle: api,
      time: `${6 + ((moduleIndex + lessonIndex) % 5)} MIN`,
      title,
      description: `${detail} Edit the example, run it, and interact with the simulated ${course.shortTitle} window.`,
      tags: [api, course.languageLabel, "Desktop"],
      conceptTitle: `${api}: ${title}`,
      conceptBody: [
        detail,
        `Start by tracing how the window, controls, and event callback connect. Then make one focused edit and use the preview to explain what <code>${api}</code> contributes.`
      ],
      mentorNote: `Keep ${course.languageLabel} interface callbacks short. Read current input, update application state, reflect the result, and move slow work away from the UI thread.`,
      points: [
        ["Widget", `Identify the visual responsibility owned by ${api}.`],
        ["Event", "Find the callback that turns a user action into behavior."],
        ["Feedback", "Run one small change at a time and inspect the result."]
      ],
      challengeTitle: `Update the ${api} response`,
      challengeText: `Replace <code>${starter}</code> with <code>${goal}</code>, then run the program.`,
      challengeTest: (code) => code.includes(goal),
      challengeSolution: (code) => code.replaceAll(starter, goal),
      code: codeFactories[course.language](title, api, starter)
    };
  }

  window.buildDesktopCourses = function () {
    return courseDefinitions.map((course) => ({
      id: course.id,
      title: course.title,
      shortTitle: course.shortTitle,
      language: course.language,
      languageLabel: course.languageLabel,
      fileName: course.fileName,
      description: course.description,
      lessons: course.modules.flatMap(([moduleName, specs], moduleIndex) =>
        specs.map((spec, lessonIndex) => buildLesson(course, moduleName, moduleIndex, spec, lessonIndex))
      )
    }));
  };
})();
