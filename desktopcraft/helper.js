const helperHistoryKey = "desktopcraft-helper-history-v1";
const activeCourseKey = "desktopcraft-active-course-v1";

const helperCourses = {
  "java-swing": {
    name: "Java Swing",
    summary: "Build Java desktop windows with JFrame, Swing controls, layout managers, listeners, models, and the Swing UI thread.",
    window: "Create the interface inside SwingUtilities.invokeLater, configure a JFrame, add controls, choose a layout, and call setVisible(true).",
    event: "Swing listeners turn user actions into callbacks. For a button, add an ActionListener and keep its callback short.",
    layout: "Use FlowLayout for simple rows, BorderLayout for five main regions, GridLayout for equal cells, or nested panels for larger forms.",
    freeze: "Slow work blocks Swing's Event Dispatch Thread. Move long I/O or calculations into SwingWorker, then update controls in done().",
    input: "Read JTextField.getText() inside the action that needs it, validate it, update your state, then show clear feedback.",
    next: "Start with JFrame and JLabel, then learn JButton events, layout managers, text input, lists, tables, and SwingWorker."
  },
  "python-tkinter": {
    name: "Python Tkinter",
    summary: "Build Python desktop tools with Tk, ttk controls, geometry managers, command callbacks, variables, and the Tk event loop.",
    window: "Create one tk.Tk root, set its title, add widgets, and call root.mainloop() after the interface is ready.",
    event: "Set a button's command to a function without calling it immediately: ttk.Button(..., command=save).",
    layout: "Use pack for simple stacks, grid for row-and-column forms, and place only for intentional overlays. Do not mix pack and grid in the same parent.",
    freeze: "A long callback blocks Tk's event loop. Break work into after() steps or run slow I/O away from the UI thread and return results safely.",
    input: "Read an Entry with entry.get() when the command runs. Use StringVar when several widgets need to share or observe the same value.",
    next: "Start with Tk and ttk.Label, then learn Button commands, Entry, pack, grid, Tk variables, lists, Treeview, and after()."
  },
  "csharp-winforms": {
    name: "C# WinForms",
    summary: "Build Windows desktop interfaces with Form, controls, events, layout panels, validation, data binding, and async work.",
    window: "Create a Form, configure its controls, and start it with Application.Run from an STAThread entry point.",
    event: "Subscribe to events such as button.Click with a named handler or a short lambda. Read current values inside that handler.",
    layout: "Use FlowLayoutPanel for wrapping rows and TableLayoutPanel for structured, resizable forms instead of fixed coordinates.",
    freeze: "Long work blocks the WinForms UI thread. Await asynchronous I/O and update controls after the await resumes on the UI context.",
    input: "Read TextBox.Text, validate it, show field-level errors with ErrorProvider, and update the model only after validation succeeds.",
    next: "Start with Form and Label, then learn Click events, TextBox, layout panels, validation, lists, DataGridView, and async/await."
  },
  "cpp-qt": {
    name: "C++ Qt Widgets",
    summary: "Build cross-platform C++ interfaces with QApplication, widgets, layouts, signals, slots, models, and Qt's event loop.",
    window: "Create QApplication first, construct a QWidget or QMainWindow, assign a layout, call show(), then return app.exec().",
    event: "Use QObject::connect to connect a signal such as QPushButton::clicked to a receiver, slot, or carefully captured lambda.",
    layout: "Use QVBoxLayout or QHBoxLayout for stacks and rows, QGridLayout for forms, and nested layouts for larger interfaces.",
    freeze: "Slow work blocks Qt's GUI thread. Move a worker object to QThread and return results through signals, or schedule small work with QTimer.",
    input: "Read QLineEdit::text() when the action runs and use a QValidator when input must follow a strict format.",
    next: "Start with QApplication and QWidget, then learn QLabel, QPushButton signals, layouts, input controls, model/view, QTimer, and QThread."
  },
  "javascript-electron": {
    name: "JavaScript Electron",
    summary: "Build cross-platform desktop apps with a main process, BrowserWindow, a renderer interface, DOM events, preload bridges, and IPC.",
    window: "Create BrowserWindow in the main process, load the renderer HTML, and keep the window reference alive for as long as it is needed.",
    event: "Use addEventListener in the renderer for DOM controls. For privileged work, call a narrow preload function that uses ipcRenderer.invoke.",
    layout: "Use semantic HTML with CSS Grid for structured forms and Flexbox for rows, toolbars, and one-dimensional groups.",
    freeze: "Heavy synchronous work blocks the renderer. Use asynchronous APIs, keep file access in the main process, and return only the data the renderer needs.",
    input: "Read the input element's value in the event handler, validate it in the renderer, and validate again in the main process before privileged work.",
    next: "Start with BrowserWindow and renderer HTML, then learn DOM events, contextBridge, IPC, native dialogs, file access, menus, and packaging."
  }
};

const helperElements = {
  form: document.querySelector("#helperForm"),
  question: document.querySelector("#helperQuestion"),
  send: document.querySelector("#helperSend"),
  count: document.querySelector("#helperCharacterCount"),
  messages: document.querySelector("#helperMessages"),
  course: document.querySelector("#helperCourse"),
  context: document.querySelector("#helperContextCopy"),
  statusDot: document.querySelector("#helperStatusDot"),
  statusTitle: document.querySelector("#helperStatusTitle"),
  statusText: document.querySelector("#helperStatusText"),
  signIn: document.querySelector("#helperSignInLink")
};

let helperHistory = readHelperHistory();
let helperMode = "local";
let helperBusy = false;

function readHelperHistory() {
  try {
    const stored = JSON.parse(localStorage.getItem(helperHistoryKey) || "[]");
    if (!Array.isArray(stored)) return [];
    return stored.filter((item) => ["user", "assistant"].includes(item?.role) && typeof item.text === "string").slice(-20);
  } catch {
    return [];
  }
}

function saveHelperHistory() {
  try { localStorage.setItem(helperHistoryKey, JSON.stringify(helperHistory.slice(-20))); } catch { /* Keep the current chat in memory. */ }
}

function selectedCourse() {
  return helperCourses[helperElements.course.value] || helperCourses["java-swing"];
}

function explanationLevel() {
  return window.DesktopcraftCustomization?.getSettings().explanationLevel || "balanced";
}

function localCourseAnswer(question) {
  const course = selectedCourse();
  const query = question.toLowerCase();
  let answer;
  if (/which|choose|compare|best language|best toolkit/.test(query)) {
    answer = "Choose by target and comfort:\n\n• Java Swing — portable Java tools and strong event-driven fundamentals.\n• Python Tkinter — quick utilities with Python's standard library.\n• C# WinForms — productive Windows-focused business tools.\n• C++ Qt — cross-platform native C++ applications.\n• Electron — cross-platform apps built with HTML, CSS, and JavaScript.";
  } else if (/freeze|frozen|slow|thread|responsive|hang/.test(query)) {
    answer = course.freeze;
  } else if (/layout|grid|pack|flow|border|position|resize|align/.test(query)) {
    answer = course.layout;
  } else if (/click|event|listener|signal|slot|callback|command/.test(query)) {
    answer = course.event;
  } else if (/input|textbox|entry|field|validate|value/.test(query)) {
    answer = course.input;
  } else if (/window|start|first|open|frame|form|root|browserwindow|qwidget/.test(query)) {
    answer = course.window;
  } else if (/next|order|roadmap|learn/.test(query)) {
    answer = course.next;
  } else if (/debug|error|does not|doesn't|not work|broken/.test(query)) {
    answer = `Debug ${course.name} in small checks:\n\n1. Read the first error and its line number.\n2. Confirm the window starts before testing the control.\n3. Add visible feedback at the beginning of the event callback.\n4. Inspect the current input and state.\n5. Reduce the code to one window, one control, and one action.\n\nPaste the smallest relevant error and code section for a more focused check.`;
  } else {
    answer = `${course.summary}\n\nA good next step is: ${course.next}`;
  }

  if (explanationLevel() === "concise") return answer.split("\n\n")[0];
  if (explanationLevel() === "detailed") {
    return `${answer}\n\nUse this check after each change: identify the control, identify the event, describe the state change, then confirm what visible feedback proves it worked.`;
  }
  return answer;
}

function addMessage(role, text, source = "", tools = []) {
  helperHistory.push({ role, text: String(text).slice(0, 8000), source, tools: Array.isArray(tools) ? tools.slice(0, 6) : [], createdAt: Date.now() });
  helperHistory = helperHistory.slice(-20);
  saveHelperHistory();
  renderMessages();
}

function messageElement(item) {
  const article = document.createElement("article");
  article.className = `helper-message ${item.role}`;
  const meta = document.createElement("span");
  meta.className = "helper-message-meta";
  meta.textContent = item.role === "user" ? "You" : "Desktopcraft Agent";
  const bubble = document.createElement("div");
  bubble.className = "helper-message-bubble";
  bubble.textContent = item.text;
  article.append(meta, bubble);
  if (item.source) {
    const source = document.createElement("span");
    source.className = "helper-message-source";
    const toolNames = { lookup_course_reference: "course reference", analyze_code: "static code analysis", create_learning_plan: "learning planner" };
    const used = Array.isArray(item.tools) ? item.tools.map((name) => toolNames[name] || name).join(", ") : "";
    source.textContent = item.source === "agent" ? `AI agent${used ? ` · used ${used}` : ""}` : "Built-in course guide";
    article.appendChild(source);
  }
  return article;
}

function renderMessages() {
  helperElements.messages.replaceChildren();
  if (!helperHistory.length) {
    helperElements.messages.appendChild(messageElement({
      role: "assistant",
      text: "Ask me about a lesson, an error, a layout, or the next concept to learn. I’ll use the selected Desktopcraft course as context."
    }));
  } else {
    helperHistory.forEach((item) => helperElements.messages.appendChild(messageElement(item)));
  }
  helperElements.messages.scrollTop = helperElements.messages.scrollHeight;
}

function showThinking() {
  const article = document.createElement("article");
  article.className = "helper-message assistant helper-thinking";
  article.id = "helperThinking";
  article.innerHTML = '<span class="helper-message-meta">Desktopcraft Agent</span><div class="helper-message-bubble"><i></i><i></i><i></i></div>';
  helperElements.messages.appendChild(article);
  helperElements.messages.scrollTop = helperElements.messages.scrollHeight;
}

async function fetchJson(path, options = {}) {
  const { timeout = 35000, ...requestOptions } = options;
  const controller = window.AbortController ? new AbortController() : null;
  const timer = controller ? window.setTimeout(() => controller.abort(), timeout) : null;
  try {
    const response = await fetch(path, {
      credentials: "same-origin",
      ...requestOptions,
      ...(controller ? { signal: controller.signal } : {})
    });
    const type = response.headers.get("content-type") || "";
    if (!type.includes("application/json")) return { available: false, status: response.status };
    return { available: true, status: response.status, data: await response.json() };
  } catch {
    return { available: false, status: 0 };
  } finally {
    if (timer) window.clearTimeout(timer);
  }
}

async function detectHelperMode() {
  const result = await fetchJson("/api/helper/status", { timeout: 2500 });
  const status = result.available ? result.data : null;
  if (status?.available && status?.signedIn) {
    helperMode = "ai";
    helperElements.statusDot.classList.add("online");
    helperElements.statusTitle.textContent = "AI coding agent connected";
    helperElements.statusText.textContent = `${status.model || "OpenAI"} can reason, inspect pasted code, consult course references, and build learning plans.`;
    helperElements.signIn.hidden = true;
  } else if (status?.available) {
    helperMode = "local";
    helperElements.statusTitle.textContent = "Course guide mode";
    helperElements.statusText.textContent = "Sign in to use the connected AI. Built-in guidance is ready now.";
    helperElements.signIn.hidden = false;
  } else {
    helperMode = "local";
    helperElements.statusTitle.textContent = "Course guide mode";
    helperElements.statusText.textContent = "Built-in Desktopcraft guidance is available without an AI server.";
    helperElements.signIn.hidden = Boolean(window.DesktopcraftAuth?.currentUser());
  }
}

async function askHelper(question) {
  if (helperMode !== "ai") return { text: localCourseAnswer(question), source: "local" };
  const previous = helperHistory.slice(0, -1).slice(-8).map(({ role, text }) => ({ role, text }));
  const result = await fetchJson("/api/helper", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      message: question,
      courseId: helperElements.course.value,
      explanationLevel: explanationLevel(),
      history: previous
    })
  });
  if (result.available && result.status === 200 && result.data?.answer) {
    return { text: result.data.answer, source: "agent", tools: result.data.toolsUsed || [] };
  }
  if (result.status === 401) helperElements.signIn.hidden = false;
  helperMode = "local";
  helperElements.statusDot.classList.remove("online");
  helperElements.statusTitle.textContent = "Course guide mode";
  helperElements.statusText.textContent = "The connected AI is unavailable, so this answer uses built-in course guidance.";
  return { text: localCourseAnswer(question), source: "local" };
}

async function submitQuestion(question) {
  const cleanQuestion = String(question || "").trim();
  if (!cleanQuestion || helperBusy) return;
  helperBusy = true;
  addMessage("user", cleanQuestion);
  helperElements.question.value = "";
  helperElements.count.textContent = "0";
  helperElements.send.disabled = true;
  showThinking();
  const response = await askHelper(cleanQuestion);
  document.querySelector("#helperThinking")?.remove();
  addMessage("assistant", response.text, response.source, response.tools);
  helperElements.send.disabled = false;
  helperElements.question.focus();
  helperBusy = false;
}

function applyPrompt(prompt) {
  helperElements.question.value = prompt;
  helperElements.count.textContent = String(prompt.length);
  helperElements.question.focus();
}

function updateCourseContext() {
  const course = selectedCourse();
  helperElements.context.textContent = course.summary;
  try { localStorage.setItem(activeCourseKey, helperElements.course.value); } catch { /* Keep selection for this page view. */ }
}

try {
  const preferredCourse = localStorage.getItem(activeCourseKey);
  if (helperCourses[preferredCourse]) helperElements.course.value = preferredCourse;
} catch { /* Use Java Swing as the default. */ }

helperElements.form.addEventListener("submit", (event) => {
  event.preventDefault();
  void submitQuestion(helperElements.question.value);
});
helperElements.question.addEventListener("input", () => {
  helperElements.count.textContent = String(helperElements.question.value.length);
  helperElements.question.style.height = "auto";
  helperElements.question.style.height = `${Math.min(190, helperElements.question.scrollHeight)}px`;
});
helperElements.question.addEventListener("keydown", (event) => {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault();
    helperElements.form.requestSubmit();
  }
});
helperElements.course.addEventListener("change", updateCourseContext);
document.addEventListener("click", (event) => {
  const promptButton = event.target.closest("[data-prompt]");
  if (promptButton) applyPrompt(promptButton.dataset.prompt);
});
document.querySelector("#clearHelperChat").addEventListener("click", () => {
  helperHistory = [];
  try { localStorage.removeItem(helperHistoryKey); } catch { /* The visible conversation is still cleared. */ }
  renderMessages();
  helperElements.question.focus();
});

const queryQuestion = new URLSearchParams(window.location.search).get("question");
if (queryQuestion) applyPrompt(queryQuestion.slice(0, 2000));
updateCourseContext();
renderMessages();
const helperAuthReady = window.DesktopcraftAuth?.ready?.();
if (helperAuthReady) void helperAuthReady.then(detectHelperMode);
else void detectHelperMode();
