const makerToolkits = {
  java: { label: "Java Swing", badge: "JAVA SWING", extension: "java" },
  python: { label: "Python Tkinter", badge: "PYTHON TKINTER", extension: "py" },
  csharp: { label: "C# WinForms", badge: "C# WINFORMS", extension: "cs" },
  cpp: { label: "C++ Qt Widgets", badge: "C++ QT WIDGETS", extension: "cpp" },
  electron: { label: "JavaScript Electron", badge: "JAVASCRIPT ELECTRON", extension: "js" }
};

const makerElements = {
  form: document.querySelector("#appMakerForm"),
  toolkit: document.querySelector("#makerToolkit"),
  appName: document.querySelector("#makerAppName"),
  windowTitle: document.querySelector("#makerWindowTitle"),
  message: document.querySelector("#makerMessage"),
  buttonLabel: document.querySelector("#makerButtonLabel"),
  actionMessage: document.querySelector("#makerActionMessage"),
  width: document.querySelector("#makerWidth"),
  height: document.querySelector("#makerHeight"),
  fileName: document.querySelector("#makerFileName"),
  code: document.querySelector("#generatedCode"),
  status: document.querySelector("#makerStatus"),
  previewWindow: document.querySelector("#makerWindow"),
  previewWindowTitle: document.querySelector("#previewWindowTitle"),
  previewToolkit: document.querySelector("#previewToolkit"),
  previewAppName: document.querySelector("#previewAppName"),
  previewMessage: document.querySelector("#previewMessage"),
  previewAction: document.querySelector("#previewAction"),
  toast: document.querySelector("#makerToast")
};

let generatedSource = "";
let generatedFileName = "";
let makerToastTimeout;

function cleanClassName(value) {
  const parts = String(value || "DesktopApp").match(/[A-Za-z0-9]+/g) || ["DesktopApp"];
  const name = parts.map((part) => part[0].toUpperCase() + part.slice(1)).join("").replace(/^[0-9]+/, "");
  return name || "DesktopApp";
}

function quote(value) {
  return String(value).replaceAll("\\", "\\\\").replaceAll('"', '\\"').replaceAll("\r", "").replaceAll("\n", "\\n");
}

function htmlText(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function clampNumber(value, minimum, maximum, fallback) {
  const number = Number(value);
  return Number.isFinite(number) ? Math.min(maximum, Math.max(minimum, Math.round(number))) : fallback;
}

function readBlueprint() {
  return {
    toolkit: makerElements.toolkit.value,
    appName: makerElements.appName.value.trim() || "Desktop App",
    windowTitle: makerElements.windowTitle.value.trim() || "My Desktop App",
    message: makerElements.message.value.trim() || "Ready.",
    buttonLabel: makerElements.buttonLabel.value.trim() || "Run",
    actionMessage: makerElements.actionMessage.value.trim() || "It works!",
    width: clampNumber(makerElements.width.value, 320, 1200, 520),
    height: clampNumber(makerElements.height.value, 220, 900, 320)
  };
}

function buildJava(values) {
  const className = cleanClassName(values.appName);
  return `import javax.swing.*;
import java.awt.*;

public class ${className} {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${quote(values.windowTitle)}");
            JLabel heading = new JLabel("${quote(values.appName)}", SwingConstants.CENTER);
            JLabel message = new JLabel("${quote(values.message)}", SwingConstants.CENTER);
            JButton action = new JButton("${quote(values.buttonLabel)}");

            heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
            action.addActionListener(event -> message.setText("${quote(values.actionMessage)}"));

            JPanel content = new JPanel(new GridLayout(3, 1, 8, 8));
            content.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
            content.add(heading);
            content.add(message);
            content.add(action);

            frame.setContentPane(content);
            frame.setSize(${values.width}, ${values.height});
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`;
}

function buildPython(values) {
  return `import tkinter as tk
from tkinter import ttk

root = tk.Tk()
root.title("${quote(values.windowTitle)}")
root.geometry("${values.width}x${values.height}")

content = ttk.Frame(root, padding=32)
content.pack(fill="both", expand=True)

ttk.Label(content, text="${quote(values.appName)}", font=("TkDefaultFont", 18, "bold")).pack(pady=8)
message = ttk.Label(content, text="${quote(values.message)}")
message.pack(pady=12)

def handle_action():
    message.config(text="${quote(values.actionMessage)}")

ttk.Button(content, text="${quote(values.buttonLabel)}", command=handle_action).pack(pady=8)
root.mainloop()`;
}

function buildCSharp(values) {
  const className = cleanClassName(values.appName);
  return `using System;
using System.Drawing;
using System.Windows.Forms;

public class ${className} : Form
{
    private readonly Label message = new();

    public ${className}()
    {
        Text = "${quote(values.windowTitle)}";
        ClientSize = new Size(${values.width}, ${values.height});
        StartPosition = FormStartPosition.CenterScreen;

        var heading = new Label { Text = "${quote(values.appName)}", Font = new Font(Font, FontStyle.Bold), AutoSize = true };
        message.Text = "${quote(values.message)}";
        message.AutoSize = true;
        var action = new Button { Text = "${quote(values.buttonLabel)}", AutoSize = true };
        action.Click += (_, _) => message.Text = "${quote(values.actionMessage)}";

        var layout = new FlowLayoutPanel { Dock = DockStyle.Fill, FlowDirection = FlowDirection.TopDown, Padding = new Padding(32) };
        layout.Controls.AddRange(new Control[] { heading, message, action });
        Controls.Add(layout);
    }

    [STAThread]
    public static void Main()
    {
        ApplicationConfiguration.Initialize();
        Application.Run(new ${className}());
    }
}`;
}

function buildCpp(values) {
  return `#include <QApplication>
#include <QLabel>
#include <QPushButton>
#include <QVBoxLayout>
#include <QWidget>

int main(int argc, char *argv[]) {
    QApplication app(argc, argv);
    QWidget window;
    window.setWindowTitle("${quote(values.windowTitle)}");
    window.resize(${values.width}, ${values.height});

    auto *heading = new QLabel("${quote(values.appName)}");
    auto *message = new QLabel("${quote(values.message)}");
    auto *action = new QPushButton("${quote(values.buttonLabel)}");
    auto *layout = new QVBoxLayout(&window);
    layout->addWidget(heading);
    layout->addWidget(message);
    layout->addWidget(action);

    QObject::connect(action, &QPushButton::clicked, [message]() {
        message->setText("${quote(values.actionMessage)}");
    });

    window.show();
    return app.exec();
}`;
}

function buildElectron(values) {
  const safeAction = JSON.stringify(values.actionMessage).replace(/<\/script/gi, "<\\/script");
  const body = `<!doctype html><html><body style="font-family:system-ui;display:grid;place-items:center;height:100vh;margin:0"><main style="text-align:center"><h1>${htmlText(values.appName)}</h1><p id="message">${htmlText(values.message)}</p><button id="action">${htmlText(values.buttonLabel)}</button></main><script>document.querySelector("#action").addEventListener("click",()=>document.querySelector("#message").textContent=${safeAction})<\/script></body></html>`;
  return `const { app, BrowserWindow } = require("electron");

function createWindow() {
    const window = new BrowserWindow({
        width: ${values.width},
        height: ${values.height},
        title: "${quote(values.windowTitle)}"
    });

    const page = ${JSON.stringify(body)};
    window.loadURL("data:text/html;charset=utf-8," + encodeURIComponent(page));
}

app.whenReady().then(createWindow);
app.on("window-all-closed", () => {
    if (process.platform !== "darwin") app.quit();
});`;
}

function buildSource(values) {
  return {
    java: buildJava,
    python: buildPython,
    csharp: buildCSharp,
    cpp: buildCpp,
    electron: buildElectron
  }[values.toolkit](values);
}

function updatePreview(values) {
  const toolkit = makerToolkits[values.toolkit];
  makerElements.previewToolkit.textContent = toolkit.badge;
  makerElements.previewAppName.textContent = values.appName;
  makerElements.previewWindowTitle.textContent = values.windowTitle;
  makerElements.previewMessage.textContent = values.message;
  makerElements.previewAction.textContent = values.buttonLabel;
  makerElements.previewWindow.style.maxWidth = `${Math.min(values.width, 700)}px`;
  makerElements.previewWindow.querySelector(".maker-window-body").style.minHeight = `${Math.min(Math.max(values.height - 110, 150), 430)}px`;
}

function generateApp(event) {
  event?.preventDefault();
  const values = readBlueprint();
  const toolkit = makerToolkits[values.toolkit];
  generatedSource = buildSource(values);
  generatedFileName = `${cleanClassName(values.appName)}.${toolkit.extension}`;
  makerElements.fileName.textContent = generatedFileName;
  makerElements.code.textContent = generatedSource;
  makerElements.status.textContent = `${toolkit.label} source ready`;
  updatePreview(values);
}

function showMakerToast(message) {
  window.clearTimeout(makerToastTimeout);
  makerElements.toast.textContent = message;
  makerElements.toast.classList.add("show");
  makerToastTimeout = window.setTimeout(() => makerElements.toast.classList.remove("show"), 2200);
}

makerElements.form.addEventListener("submit", generateApp);
makerElements.form.addEventListener("input", generateApp);
makerElements.previewAction.addEventListener("click", () => {
  const values = readBlueprint();
  makerElements.previewMessage.textContent = values.actionMessage;
  makerElements.status.textContent = "Button event handled";
});

document.querySelector("#copyGeneratedCode").addEventListener("click", async () => {
  try {
    await navigator.clipboard.writeText(generatedSource);
  } catch {
    const selection = window.getSelection();
    const range = document.createRange();
    range.selectNodeContents(makerElements.code);
    selection.removeAllRanges();
    selection.addRange(range);
    document.execCommand("copy");
    selection.removeAllRanges();
  }
  showMakerToast("Generated source copied");
});

document.querySelector("#downloadGeneratedCode").addEventListener("click", () => {
  const blob = new Blob([generatedSource], { type: "text/plain;charset=utf-8" });
  const link = document.createElement("a");
  const url = URL.createObjectURL(blob);
  link.href = url;
  link.download = generatedFileName;
  link.click();
  URL.revokeObjectURL(url);
  showMakerToast(`${generatedFileName} downloaded`);
});

generateApp();
