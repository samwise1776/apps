(function () {
  const catalogs = {
    java: {
      modules: ["Component mastery", "Layouts and composition", "Events and state", "Models and data", "Polish and accessibility", "Projects and architecture"],
      apis: ["JFrame", "JPanel", "JLabel", "JButton", "JTextField", "JTextArea", "JCheckBox", "JRadioButton", "JComboBox", "JList", "JTable", "JTree", "JTabbedPane", "JSplitPane", "JScrollPane", "BorderLayout", "GridBagLayout", "BoxLayout", "Action", "KeyStroke", "DocumentListener", "SwingWorker", "Timer", "TableModel", "ListModel"],
      code: javaCode
    },
    python: {
      modules: ["Widget mastery", "Geometry and composition", "Events and state", "Data-driven interfaces", "Polish and accessibility", "Projects and architecture"],
      apis: ["Tk", "ttk.Frame", "ttk.Label", "ttk.Button", "ttk.Entry", "Text", "ttk.Checkbutton", "ttk.Radiobutton", "ttk.Combobox", "Listbox", "ttk.Treeview", "ttk.Notebook", "ttk.Panedwindow", "Canvas", "pack", "grid", "place", "StringVar", "bind", "after", "messagebox", "filedialog", "ttk.Progressbar", "Menu", "Toplevel"],
      code: pythonCode
    },
    csharp: {
      modules: ["Control mastery", "Layout and composition", "Events and state", "Data-bound interfaces", "Polish and accessibility", "Projects and architecture"],
      apis: ["Form", "Panel", "Label", "Button", "TextBox", "RichTextBox", "CheckBox", "RadioButton", "ComboBox", "ListBox", "DataGridView", "TreeView", "TabControl", "SplitContainer", "FlowLayoutPanel", "TableLayoutPanel", "Click", "KeyDown", "ErrorProvider", "BindingSource", "Timer", "async / await", "MenuStrip", "ProgressBar", "UserControl"],
      code: csharpCode
    },
    cpp: {
      modules: ["Widget mastery", "Layout and composition", "Signals and state", "Model/view interfaces", "Polish and accessibility", "Projects and architecture"],
      apis: ["QApplication", "QWidget", "QLabel", "QPushButton", "QLineEdit", "QTextEdit", "QCheckBox", "QRadioButton", "QComboBox", "QListWidget", "QTableView", "QTreeView", "QTabWidget", "QSplitter", "QVBoxLayout", "QGridLayout", "signals and slots", "QAction", "QValidator", "QStringListModel", "QTimer", "QThread", "QMenuBar", "QProgressBar", "QDialog"],
      code: cppCode
    },
    javascript: {
      modules: ["Renderer mastery", "Layout and composition", "Events and state", "Secure desktop data", "Polish and accessibility", "Projects and architecture"],
      apis: ["BrowserWindow", "HTMLElement", "HTMLButtonElement", "HTMLInputElement", "HTMLTextAreaElement", "checkbox input", "radio input", "select element", "dialog element", "CSS Grid", "Flexbox", "addEventListener", "CustomEvent", "FormData", "contextBridge", "ipcRenderer.invoke", "ipcMain.handle", "Menu", "Tray", "Notification", "globalShortcut", "fs/promises", "localStorage", "ARIA", "Electron Forge"],
      code: javascriptCode
    }
  };

  const projects = ["profile editor", "task board", "notes desk", "budget panel", "timer dashboard", "contact book", "inventory tool", "study planner", "music queue", "weather console"];
  const skills = ["create", "configure", "connect", "validate", "update", "organize", "test", "refactor", "reuse", "ship"];

  const escapeCode = (value) => String(value).replaceAll("\\", "\\\\").replaceAll('"', '\\"').replaceAll("\n", " ");

  function javaCode(title, api, starter) {
    return `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${escapeCode(title)}");
            JTextField input = new JTextField("Desktop app", 14);
            JButton action = new JButton("Try ${escapeCode(api)}");
            JLabel status = new JLabel("Ready");
            action.addActionListener(event -> status.setText("${escapeCode(starter)}"));
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 18));
            panel.add(input);
            panel.add(action);
            panel.add(status);
            frame.add(panel);
            frame.setSize(440, 230);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}`;
  }

  function pythonCode(title, api, starter) {
    return `import tkinter as tk
from tkinter import ttk

root = tk.Tk()
root.title("${escapeCode(title)}")
panel = ttk.Frame(root, padding=18)
panel.pack(fill="both", expand=True)
entry = ttk.Entry(panel)
entry.insert(0, "Desktop app")
status = ttk.Label(panel, text="Ready")

def run_demo():
    status.config(text="${escapeCode(starter)}")

action = ttk.Button(panel, text="Try ${escapeCode(api)}", command=run_demo)
entry.pack(fill="x", pady=6)
action.pack(pady=6)
status.pack(pady=6)
root.mainloop()`;
  }

  function csharpCode(title, api, starter) {
    return `using System;
using System.Windows.Forms;

public class MainForm : Form
{
    public MainForm()
    {
        Text = "${escapeCode(title)}";
        var panel = new FlowLayoutPanel { Dock = DockStyle.Fill, Padding = new Padding(18) };
        var input = new TextBox { Text = "Desktop app", Width = 180 };
        var action = new Button { Text = "Try ${escapeCode(api)}", AutoSize = true };
        var status = new Label { Text = "Ready", AutoSize = true };
        action.Click += (sender, args) => status.Text = "${escapeCode(starter)}";
        panel.Controls.Add(input);
        panel.Controls.Add(action);
        panel.Controls.Add(status);
        Controls.Add(panel);
    }
    [STAThread] public static void Main() => Application.Run(new MainForm());
}`;
  }

  function cppCode(title, api, starter) {
    return `#include <QApplication>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>
#include <QVBoxLayout>
#include <QWidget>

int main(int argc, char *argv[]) {
    QApplication app(argc, argv);
    QWidget window;
    window.setWindowTitle("${escapeCode(title)}");
    auto *layout = new QVBoxLayout(&window);
    auto *input = new QLineEdit("Desktop app");
    auto *action = new QPushButton("Try ${escapeCode(api)}");
    auto *status = new QLabel("Ready");
    layout->addWidget(input);
    layout->addWidget(action);
    layout->addWidget(status);
    QObject::connect(action, &QPushButton::clicked, [=]() { status->setText("${escapeCode(starter)}"); });
    window.show();
    return app.exec();
}`;
  }

  function javascriptCode(title, api, starter) {
    return `document.title = "${escapeCode(title)}";
const app = document.querySelector("#app");
app.innerHTML = \`
  <main class="desktop-panel">
    <input id="input" value="Desktop app" />
    <button id="action">Try ${escapeCode(api)}</button>
    <p id="status">Ready</p>
  </main>
\`;
const status = document.querySelector("#status");
document.querySelector("#action").addEventListener("click", () => {
  status.textContent = "${escapeCode(starter)}";
});`;
  }

  function generatedLesson(course, lessonNumber) {
    const catalog = catalogs[course.language];
    const offset = lessonNumber - 1;
    const api = catalog.apis[offset % catalog.apis.length];
    const moduleIndex = Math.floor(offset / Math.ceil(500 / catalog.modules.length)) % catalog.modules.length;
    const module = catalog.modules[moduleIndex];
    const project = projects[(offset * 3 + moduleIndex) % projects.length];
    const skill = skills[(offset + moduleIndex) % skills.length];
    const starter = `Lab ${lessonNumber} ready`;
    const goal = `${api} lab ${lessonNumber} complete`;
    const title = `${skill[0].toUpperCase()}${skill.slice(1)} ${api} in a ${project}`;
    const detail = `Practice how ${api} supports a ${project}, then connect that control to one clear piece of interface state.`;
    return {
      module,
      moduleIndex: moduleIndex + 10,
      navTitle: `${title} · ${lessonNumber}`,
      navSubtitle: api,
      time: `${6 + (offset % 5)} MIN`,
      title: `${title} — Lab ${lessonNumber}`,
      description: `${detail} This guided lab builds on earlier ${course.shortTitle} work with a focused edit and an interactive preview.`,
      tags: [api, course.languageLabel, project],
      conceptTitle: `${api} has one clear job in the interface`,
      conceptBody: [
        `${api} is easiest to learn when you identify the data it displays or changes and the event that activates it.`,
        `Trace the sample from input to callback to visible feedback. Then change the response text and run the simulator to verify the ${project}.`
      ],
      mentorNote: `Keep the ${course.languageLabel} callback small: read current input, update state, and show useful feedback immediately.`,
      points: [
        ["Purpose", `Explain why the ${project} uses ${api}.`],
        ["Event", "Locate the callback that responds to the learner's action."],
        ["Feedback", "Confirm the new state in the simulated desktop window."]
      ],
      challengeTitle: `Complete ${api} lab ${lessonNumber}`,
      challengeText: `Replace <code>${starter}</code> with <code>${goal}</code>, then run the program.`,
      challengeTest: (code) => code.includes(goal),
      challengeSolution: (code) => code.replaceAll(starter, goal),
      code: catalog.code(title, api, starter)
    };
  }

  window.expandDesktopcraftCourses = function (courses, target = 500) {
    courses.forEach((course) => {
      const catalog = catalogs[course.language];
      if (!catalog) return;
      if (course.lessons.length > target) course.lessons.length = target;
      while (course.lessons.length < target) {
        course.lessons.push(generatedLesson(course, course.lessons.length + 1));
      }
    });
    return courses;
  };
})();
