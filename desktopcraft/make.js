const communityToolkits = {
  java: { label: "Java Swing", extension: "java" },
  python: { label: "Python Tkinter", extension: "py" },
  csharp: { label: "C# WinForms", extension: "cs" },
  cpp: { label: "C++ Qt Widgets", extension: "cpp" },
  electron: { label: "JavaScript Electron", extension: "js" },
};

const makeElements = {
  form: document.getElementById("communityMakerForm"),
  title: document.getElementById("communityAppTitle"),
  toolkit: document.getElementById("communityToolkit"),
  description: document.getElementById("communityAppDescription"),
  source: document.getElementById("communitySourceCode"),
  agreement: document.getElementById("freeAgreement"),
  publish: document.getElementById("publishCommunityApp"),
  status: document.getElementById("communityFormStatus"),
  fileName: document.getElementById("communityFileName"),
  lineCount: document.getElementById("sourceLineCount"),
  byteCount: document.getElementById("sourceByteCount"),
  descriptionCount: document.getElementById("descriptionCount"),
  accountName: document.getElementById("makerAccountName"),
  accountHandle: document.getElementById("makerAccountHandle"),
  accountAvatar: document.getElementById("makerAvatar"),
  signIn: document.getElementById("makerSignIn"),
  publishedResult: document.getElementById("publishedResult"),
  viewPublished: document.getElementById("viewPublishedApp"),
};

let serverUser = null;
let sourceWasEdited = false;

function classNameFor(value) {
  const words = String(value || "Desktopcraft App").match(/[A-Za-z0-9]+/g) || ["Desktopcraft", "App"];
  let name = words.map((word) => word[0].toUpperCase() + word.slice(1)).join("").slice(0, 60) || "DesktopcraftApp";
  if (/^[0-9]/.test(name)) name = `App${name}`;
  return name;
}

function draftFileName() {
  return `${classNameFor(makeElements.title.value)}.${communityToolkits[makeElements.toolkit.value].extension}`;
}

function literal(value) {
  return JSON.stringify(String(value || "Desktopcraft App"));
}

function starterSource(toolkit, title) {
  const appName = String(title || "Desktopcraft App").trim() || "Desktopcraft App";
  const className = classNameFor(appName);
  const name = literal(appName);
  const starters = {
    java: `import javax.swing.*;
import java.awt.*;

public class ${className} {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame(${name});
            JLabel message = new JLabel("Ready to build.", SwingConstants.CENTER);
            JButton action = new JButton("Run action");
            action.addActionListener(event -> message.setText("The app is working!"));

            window.add(message, BorderLayout.CENTER);
            window.add(action, BorderLayout.SOUTH);
            window.setSize(520, 320);
            window.setLocationRelativeTo(null);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setVisible(true);
        });
    }
}`,
    python: `import tkinter as tk
from tkinter import ttk

root = tk.Tk()
root.title(${name})
root.geometry("520x320")

message = ttk.Label(root, text="Ready to build.")
message.pack(expand=True)

def run_action():
    message.config(text="The app is working!")

ttk.Button(root, text="Run action", command=run_action).pack(pady=24)
root.mainloop()`,
    csharp: `using System;
using System.Drawing;
using System.Windows.Forms;

public class ${className} : Form
{
    public ${className}()
    {
        Text = ${name};
        ClientSize = new Size(520, 320);
        var message = new Label { Text = "Ready to build.", AutoSize = true };
        var action = new Button { Text = "Run action", AutoSize = true };
        action.Click += (_, _) => message.Text = "The app is working!";
        var layout = new FlowLayoutPanel { Dock = DockStyle.Fill, Padding = new Padding(32) };
        layout.Controls.AddRange(new Control[] { message, action });
        Controls.Add(layout);
    }

    [STAThread]
    public static void Main()
    {
        ApplicationConfiguration.Initialize();
        Application.Run(new ${className}());
    }
}`,
    cpp: `#include <QApplication>
#include <QLabel>
#include <QPushButton>
#include <QVBoxLayout>
#include <QWidget>

int main(int argc, char *argv[]) {
    QApplication app(argc, argv);
    QWidget window;
    window.setWindowTitle(${name});
    window.resize(520, 320);

    auto *message = new QLabel("Ready to build.");
    auto *action = new QPushButton("Run action");
    auto *layout = new QVBoxLayout(&window);
    layout->addWidget(message);
    layout->addWidget(action);
    QObject::connect(action, &QPushButton::clicked, [message]() {
        message->setText("The app is working!");
    });

    window.show();
    return app.exec();
}`,
    electron: `const { app, BrowserWindow } = require("electron");

function createWindow() {
    const window = new BrowserWindow({ width: 720, height: 480, title: ${name} });
    const page = \`<!doctype html>
      <html><body style="font-family:system-ui;display:grid;place-items:center;height:100vh;margin:0">
      <main><h1>${appName.replaceAll("<", "&lt;").replaceAll(">", "&gt;")}</h1><p id="message">Ready to build.</p>
      <button id="action">Run action</button></main>
      <script>document.querySelector("#action").onclick=()=>document.querySelector("#message").textContent="The app is working!";<\/script>
      </body></html>\`;
    window.loadURL("data:text/html;charset=utf-8," + encodeURIComponent(page));
}

app.whenReady().then(createWindow);
app.on("window-all-closed", () => { if (process.platform !== "darwin") app.quit(); });`,
  };
  return starters[toolkit];
}

function showFormStatus(message, success = false) {
  makeElements.status.textContent = message;
  makeElements.status.className = `community-form-status${success ? " success" : ""}`;
  makeElements.status.hidden = false;
}

function updateEditorDetails() {
  const source = makeElements.source.value;
  makeElements.fileName.textContent = draftFileName();
  makeElements.lineCount.textContent = source ? String(source.split("\n").length) : "0";
  makeElements.byteCount.textContent = String(new TextEncoder().encode(source).length);
  makeElements.descriptionCount.textContent = String(makeElements.description.value.length);
}

function loadStarter(force = false) {
  if (!force && sourceWasEdited && makeElements.source.value.trim() && !window.confirm("Replace the current source with a new starter?")) return;
  makeElements.source.value = starterSource(makeElements.toolkit.value, makeElements.title.value);
  sourceWasEdited = false;
  updateEditorDetails();
}

function downloadDraft() {
  const blob = new Blob([makeElements.source.value], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = draftFileName();
  link.click();
  URL.revokeObjectURL(url);
}

async function loadPublishingAccount() {
  await window.DesktopcraftAuth?.ready?.();
  try {
    const response = await fetch("/api/auth/session", { credentials: "same-origin" });
    const contentType = response.headers.get("content-type") || "";
    if (!contentType.includes("application/json")) throw new Error("server unavailable");
    const payload = await response.json();
    serverUser = response.ok ? payload.user : null;
  } catch {
    serverUser = null;
  }

  if (serverUser) {
    makeElements.accountName.textContent = serverUser.name;
    makeElements.accountHandle.textContent = `@${serverUser.username} · ready to publish`;
    makeElements.accountAvatar.textContent = window.DesktopcraftAuth.initials(serverUser.name);
    makeElements.signIn.hidden = true;
    makeElements.publish.disabled = false;
  } else {
    const localUser = window.DesktopcraftAuth?.currentUser?.();
    makeElements.accountName.textContent = localUser ? "Connect your server account" : "Sign in to publish";
    makeElements.accountHandle.textContent = "Run the database-backed site and sign in first.";
    makeElements.accountAvatar.textContent = "GU";
    makeElements.signIn.hidden = false;
    makeElements.publish.disabled = true;
  }
}

async function publishApp(event) {
  event.preventDefault();
  if (!makeElements.form.reportValidity()) return;
  if (!serverUser) {
    showFormStatus("Sign in with the database-backed site before publishing.");
    return;
  }
  const sourceBytes = new TextEncoder().encode(makeElements.source.value).length;
  if (sourceBytes > 100000) {
    showFormStatus("App source must be 100 KB or smaller.");
    return;
  }

  makeElements.publish.disabled = true;
  makeElements.publish.textContent = "Publishing…";
  makeElements.status.hidden = true;
  makeElements.publishedResult.hidden = true;
  try {
    const response = await fetch("/api/apps", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        title: makeElements.title.value,
        description: makeElements.description.value,
        toolkit: makeElements.toolkit.value,
        sourceCode: makeElements.source.value,
      }),
    });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || "The app could not be published.");
    showFormStatus("Published successfully. Your app is available as a free download.", true);
    makeElements.viewPublished.href = `view.html?published=${encodeURIComponent(payload.app.id)}`;
    makeElements.publishedResult.hidden = false;
  } catch (error) {
    showFormStatus(error.message || "The app could not be published.");
  } finally {
    makeElements.publish.disabled = !serverUser;
    makeElements.publish.textContent = "Publish free app";
  }
}

makeElements.title.addEventListener("input", updateEditorDetails);
makeElements.toolkit.addEventListener("change", updateEditorDetails);
makeElements.description.addEventListener("input", updateEditorDetails);
makeElements.source.addEventListener("input", () => {
  sourceWasEdited = true;
  updateEditorDetails();
});
makeElements.source.addEventListener("keydown", (event) => {
  if (event.key !== "Tab") return;
  event.preventDefault();
  const start = makeElements.source.selectionStart;
  const end = makeElements.source.selectionEnd;
  makeElements.source.setRangeText("    ", start, end, "end");
  sourceWasEdited = true;
  updateEditorDetails();
});
document.getElementById("loadStarterSource").addEventListener("click", () => loadStarter());
document.getElementById("downloadDraft").addEventListener("click", downloadDraft);
makeElements.form.addEventListener("submit", publishApp);

loadStarter(true);
void loadPublishingAccount();
