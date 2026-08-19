(() => {
  const keywords = new Set([
    "let", "mut", "var", "const", "fn", "return", "if", "elif", "else", "while", "for", "in", "loop",
    "break", "continue", "match", "class", "extends", "with", "struct", "enum", "trait", "impl", "try",
    "catch", "finally", "throw", "defer", "assert", "import", "from", "as", "and", "or", "not", "is",
    "self", "super", "async", "await", "spawn", "yield", "pub", "mod", "use", "type", "where", "panic",
    "unsafe", "extern", "inline", "guard", "new", "sizeof", "typeof", "argu", "run"
  ]);
  const literals = new Set(["true", "false", "nil", "none", "some", "ok", "err"]);
  const builtins = new Set([
    "print", "println", "input", "len", "range", "sorted", "reversed", "append", "push", "pop", "insert",
    "remove", "contains", "keys", "values", "items", "enumerate", "zip", "map", "filter", "reduce", "min", "max", "sum", "abs",
    "floor", "ceil", "round", "rand", "rand_int", "random", "str", "int", "float", "bool",
    "json_parse", "json_stringify", "from_json", "to_json", "sha256", "now", "time", "sleep", "typeof", "clone",
    "chr", "ord", "hex", "oct", "bin", "join", "split", "replace", "trim", "lower", "upper", "parse_int", "parse_float",
    "exit", "readFileLines", "argu", "cmd", "User", "Window", "Frame", "Button", "Label", "TextField", "PasswordField",
    "TextArea", "Checkbox", "RadioButton", "ToggleSwitch", "Slider", "Spinner", "ProgressBar", "ComboBox",
    "ListBox", "Table", "Image", "Hyperlink", "MenuBar", "Menu", "MenuItem", "MenuSeparator", "TabView",
    "Tab", "StatusBar", "Canvas"
  ]);
  const tokenPattern = /(\/\*[\s\S]*?\*\/|#[^\n]*|r?"""[\s\S]*?"""|r?"(?:\\.|[^"\\])*"|r?'(?:\\.|[^'\\])*'|\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|0[oO][0-7_]+|\d[\d_]*(?:\.\d[\d_]*)?(?:[eE][+-]?\d+)?)\b|\b[A-Za-z_][A-Za-z0-9_]*\b|\?\?|\|>|===|!==|==|!=|<=|>=|\*\*|\.\.|\+=|-=|\*=|\/=|%=|&&|\|\||::|->|[+\-*\/%=<>!&|^~?:@])/g;

  function escapeHtml(value) {
    return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  }

  function highlight(source, language) {
    let result = "";
    let cursor = 0;
    for (const match of source.matchAll(tokenPattern)) {
      const token = match[0];
      const index = match.index;
      result += escapeHtml(source.slice(cursor, index));
      let className = "";
      if (token.startsWith("#") || token.startsWith("/*")) className = "tok-comment";
      else if (/^r?["']/.test(token)) className = "tok-string";
      else if (/^(?:0[xX]|0[bB]|0[oO]|\d)/.test(token)) className = "tok-number";
      else if (keywords.has(token)) className = "tok-keyword";
      else if (literals.has(token)) className = "tok-literal";
      else if (builtins.has(token)) className = "tok-builtin";
      else if (/^[A-Z][A-Za-z0-9_]*$/.test(token)) className = "tok-type";
      else if (/^[A-Za-z_]/.test(token) && /^\s*\(/.test(source.slice(index + token.length))) className = "tok-function";
      else if (language === "shell" && source.slice(Math.max(0, index - 2), index) === "--") className = "tok-flag";
      else if (/[^A-Za-z0-9_]/.test(token)) className = "tok-operator";
      result += className ? `<span class="${className}">${escapeHtml(token)}</span>` : escapeHtml(token);
      cursor = index + token.length;
    }
    return result + escapeHtml(source.slice(cursor));
  }

  const toast = document.querySelector("#copyToast");
  let toastTimer;
  function showToast(message) {
    toast.textContent = message;
    toast.classList.add("show");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove("show"), 1800);
  }

  async function copyText(text) {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      const area = document.createElement("textarea");
      area.value = text;
      area.setAttribute("readonly", "");
      area.style.position = "fixed";
      area.style.opacity = "0";
      document.body.append(area);
      area.select();
      document.execCommand("copy");
      area.remove();
    }
  }

  document.querySelectorAll("pre[data-language]").forEach((pre) => {
    const code = pre.querySelector("code");
    const source = code.textContent.replace(/^\n|\n$/g, "");
    const language = pre.dataset.language || "text";
    code.dataset.source = source;
    code.innerHTML = highlight(source, language);
    const frame = document.createElement("div");
    frame.className = "code-frame";
    const label = document.createElement("span");
    label.className = "code-label";
    label.textContent = language;
    const button = document.createElement("button");
    button.className = "copy-code";
    button.type = "button";
    button.textContent = "Copy";
    button.setAttribute("aria-label", `Copy ${language} code`);
    button.addEventListener("click", async () => {
      await copyText(source);
      button.textContent = "Copied";
      showToast("Code copied");
      window.setTimeout(() => { button.textContent = "Copy"; }, 1400);
    });
    pre.replaceWith(frame);
    frame.append(label, button, pre);
  });

  const lessons = [...document.querySelectorAll(".guide-lesson")];
  const navLinks = [...document.querySelectorAll(".guide-nav-list a")];
  const storageKey = "velice-guide-progress-v1";
  let completed = new Set();
  try { completed = new Set(JSON.parse(localStorage.getItem(storageKey) || "[]")); } catch { completed = new Set(); }

  function updateProgress() {
    const valid = lessons.filter((lesson) => completed.has(lesson.id));
    document.querySelector("#guideProgressText").textContent = `${valid.length} / ${lessons.length}`;
    document.querySelector("#guideProgressBar").style.width = `${lessons.length ? (valid.length / lessons.length) * 100 : 0}%`;
    navLinks.forEach((link) => link.classList.toggle("complete", completed.has(link.hash.slice(1))));
    try { localStorage.setItem(storageKey, JSON.stringify([...completed])); } catch { /* Progress remains in memory. */ }
  }

  lessons.forEach((lesson) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "lesson-complete";
    const refresh = () => {
      const done = completed.has(lesson.id);
      button.classList.toggle("done", done);
      button.textContent = done ? "✓ Complete" : "Mark complete";
      button.setAttribute("aria-pressed", String(done));
    };
    button.addEventListener("click", () => {
      if (completed.has(lesson.id)) completed.delete(lesson.id); else completed.add(lesson.id);
      refresh();
      updateProgress();
    });
    lesson.querySelector("header").append(button);
    refresh();
  });
  updateProgress();

  const search = document.querySelector("#guideSearch");
  const searchStatus = document.querySelector("#guideSearchStatus");
  const empty = document.querySelector("#guideEmpty");
  search.addEventListener("input", () => {
    const words = search.value.trim().toLowerCase().split(/\s+/).filter(Boolean);
    let visible = 0;
    lessons.forEach((lesson) => {
      const haystack = `${lesson.dataset.title || ""} ${lesson.dataset.keywords || ""} ${lesson.textContent}`.toLowerCase();
      const matches = words.every((word) => haystack.includes(word));
      lesson.hidden = !matches;
      const link = navLinks.find((candidate) => candidate.hash === `#${lesson.id}`);
      if (link) link.closest("li").hidden = !matches;
      if (matches) visible++;
    });
    searchStatus.textContent = `${visible} chapter${visible === 1 ? "" : "s"} ${words.length ? "matched" : "available"}`;
    empty.hidden = visible !== 0;
  });

  if ("IntersectionObserver" in window) {
    const observer = new IntersectionObserver((entries) => {
      const visible = entries.filter((entry) => entry.isIntersecting).sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0];
      if (!visible) return;
      navLinks.forEach((link) => link.classList.toggle("active", link.hash === `#${visible.target.id}`));
    }, { rootMargin: "-18% 0px -65%", threshold: [0.05, 0.25, 0.5] });
    lessons.forEach((lesson) => observer.observe(lesson));
  }
})();
