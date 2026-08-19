(function () {
  const strings = (source) =>
    [...String(source).matchAll(/"((?:\\.|[^"\\])*)"/g)].map((match) =>
      match[1].replaceAll('\\"', '"').replaceAll("\\n", "\n").replaceAll("\\\\", "\\")
    );

  const escapeHtml = (value) =>
    String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");

  function parsePython(code) {
    const title = code.match(/\.title\s*\(\s*"([^"]*)"/)?.[1] || "Tkinter Preview";
    const components = [];
    const pattern = /(\w+)\s*=\s*(?:ttk|tk)\.(Frame|Label|Button|Entry|Text|Checkbutton|Radiobutton|Combobox|Listbox|Treeview|Scale|Progressbar)\s*\(([^\n]*)\)/g;
    let match;
    while ((match = pattern.exec(code))) {
      const [, name, type, args] = match;
      const text = args.match(/text\s*=\s*"([^"]*)"/)?.[1] || "";
      components.push({ name, type, text, value: "" });
    }
    const inserted = [...code.matchAll(/(\w+)\.insert\s*\([^,]+,\s*"([^"]*)"\)/g)];
    inserted.forEach((entry) => {
      const component = components.find((item) => item.name === entry[1]);
      if (component) component.value = entry[2];
    });
    const result = [...code.matchAll(/\.config\s*\(\s*text\s*=\s*"([^"]*)"/g)].at(-1)?.[1] || "Command handled";
    return { title, components, result, listeners: (code.match(/command\s*=|\.bind\s*\(/g) || []).length };
  }

  function parseCSharp(code) {
    const title = [...code.matchAll(/(?:^|\s)Text\s*=\s*"([^"]*)"/gm)][0]?.[1] || "WinForms Preview";
    const components = [];
    const pattern = /(?:var|[A-Za-z][\w<>]*)\s+(\w+)\s*=\s*new\s+(Form|Label|Button|TextBox|RichTextBox|CheckBox|RadioButton|ComboBox|ListBox|DataGridView|TreeView|ProgressBar|FlowLayoutPanel|TableLayoutPanel)\s*(?:\([^;]*?\))?\s*(?:\{([^}]*)\})?/g;
    let match;
    while ((match = pattern.exec(code))) {
      const [, name, type, initializer = ""] = match;
      const text = initializer.match(/Text\s*=\s*"([^"]*)"/)?.[1] || "";
      components.push({ name, type, text, value: type.includes("TextBox") ? text : "" });
    }
    if (!components.some((item) => item.name === "status") && /Label\s+status\s*=\s*new\s+Label/.test(code)) {
      components.push({ name: "status", type: "Label", text: "Ready", value: "" });
    }
    const results = [...code.matchAll(/status\.Text\s*=\s*"([^"]*)"/g)];
    const result = results.at(-1)?.[1] || "Event handled";
    return { title, components, result, listeners: (code.match(/\.Click\s*\+=/g) || []).length };
  }

  function parseCpp(code) {
    const title = code.match(/setWindowTitle\s*\(\s*"([^"]*)"/)?.[1] || "Qt Preview";
    const components = [];
    const pattern = /auto\s*\*\s*(\w+)\s*=\s*new\s+Q(Label|PushButton|LineEdit|TextEdit|CheckBox|RadioButton|ComboBox|ListWidget|TableView|TreeView|ProgressBar)\s*\(([^;]*)\)\s*;/g;
    let match;
    while ((match = pattern.exec(code))) {
      const [, name, qtType, args] = match;
      const typeMap = { PushButton: "Button", LineEdit: "TextBox", TextEdit: "RichTextBox" };
      const type = typeMap[qtType] || qtType;
      const text = strings(args)[0] || "";
      components.push({ name, type, text, value: qtType.includes("Edit") ? text : "" });
    }
    const result = [...code.matchAll(/status->setText\s*\(\s*"([^"]*)"/g)].at(-1)?.[1] || "Signal received";
    return { title, components, result, listeners: (code.match(/QObject::connect\s*\(/g) || []).length };
  }

  function parseJavascript(code) {
    const title = code.match(/document\.title\s*=\s*"([^"]*)"/)?.[1] || "Electron Preview";
    const components = [];
    const html = code.match(/innerHTML\s*=\s*`([\s\S]*?)`/)?.[1] || code;
    const pattern = /<(input|button|p|label|textarea|select)([^>]*)>([^<]*)/g;
    let match;
    let index = 0;
    while ((match = pattern.exec(html))) {
      const [, tag, attrs, content] = match;
      const id = attrs.match(/id="([^"]*)"/)?.[1] || `${tag}_${index++}`;
      const value = attrs.match(/value="([^"]*)"/)?.[1] || "";
      const types = { input: "TextBox", button: "Button", p: "Label", label: "Label", textarea: "RichTextBox", select: "ComboBox" };
      components.push({ name: id, type: types[tag], text: content.trim(), value });
    }
    const result = [...code.matchAll(/status\.textContent\s*=\s*"([^"]*)"/g)].at(-1)?.[1] || "DOM event handled";
    return { title, components, result, listeners: (code.match(/\.addEventListener\s*\(/g) || []).length };
  }

  function makeControl(component) {
    const type = component.type;
    let element;
    if (/Button|PushButton/.test(type)) {
      element = document.createElement("button");
      element.type = "button";
      element.className = "swing-button primary";
      element.textContent = component.text || "Run action";
    } else if (/Entry|TextBox|LineEdit/.test(type) && !/Rich|Text$/.test(type)) {
      element = document.createElement("input");
      element.className = "swing-input";
      element.value = component.value || component.text || "";
      element.placeholder = "Type here";
    } else if (/RichTextBox|Text$|TextEdit/.test(type)) {
      element = document.createElement("textarea");
      element.className = "swing-input sim-textarea";
      element.value = component.value || component.text || "";
    } else if (/Check/.test(type) || /Radio/.test(type)) {
      element = document.createElement("label");
      element.className = "sim-choice";
      element.innerHTML = `<input type="${/Radio/.test(type) ? "radio" : "checkbox"}" /> <span>${escapeHtml(component.text || type)}</span>`;
    } else if (/Combo/.test(type)) {
      element = document.createElement("select");
      element.className = "swing-input sim-select";
      element.innerHTML = "<option>First choice</option><option>Second choice</option>";
    } else if (/List|Tree|Grid|Table/.test(type)) {
      element = document.createElement("ul");
      element.className = /Tree/.test(type) ? "sim-tree" : "sim-list";
      element.innerHTML = "<li>Desktop item one</li><li>Desktop item two</li>";
    } else if (/Progress/.test(type)) {
      element = document.createElement("div");
      element.className = "sim-progress";
      element.innerHTML = '<span style="width:45%"></span><strong>45%</strong>';
    } else if (/Frame|Panel|QWidget/.test(type)) {
      element = document.createElement("div");
      element.className = "sim-language-panel";
    } else {
      element = document.createElement("div");
      element.className = "swing-label";
      element.textContent = component.text || "Ready";
    }
    element.dataset.simName = component.name;
    return element;
  }

  function renderOther(parsed, mount, options) {
    const windowNode = document.createElement("div");
    windowNode.className = "swing-window sim-window-large";
    windowNode.innerHTML = `<div class="swing-titlebar"><span class="window-controls"><span></span><span></span><span></span></span>${escapeHtml(parsed.title)}</div>`;
    const body = document.createElement("div");
    body.className = "swing-body sim-container sim-flow sim-language-body";
    const refs = new Map();
    parsed.components.filter((component) => !/Frame|Panel/.test(component.type)).forEach((component) => {
      const element = makeControl(component);
      refs.set(component.name, element);
      body.appendChild(element);
    });

    if (!body.children.length) {
      body.innerHTML = '<div class="sim-empty"><strong>No supported desktop controls found</strong><span>Add a label, button, input, list, table, tree, or progress control.</span></div>';
    }

    windowNode.appendChild(body);
    mount.replaceChildren(windowNode);
    const buttons = [...refs.entries()].filter(([, element]) => element.tagName === "BUTTON");
    const status = refs.get("status") || [...refs.entries()].find(([name]) => /status|message|result/i.test(name))?.[1];
    buttons.forEach(([name, button]) =>
      button.addEventListener("click", () => {
        if (status) status.textContent = parsed.result;
        options.onConsole?.(`${name} handled the ${options.languageLabel} desktop event.`);
      })
    );
    return { components: refs.size, listeners: parsed.listeners, title: parsed.title };
  }

  function render(code, mount, options = {}) {
    if (options.language === "java" || !options.language) {
      return window.SwingSimulator.render(code, mount, options);
    }
    const parser = {
      python: parsePython,
      csharp: parseCSharp,
      cpp: parseCpp,
      javascript: parseJavascript
    }[options.language];
    return renderOther(parser(code), mount, options);
  }

  function parse(code, language) {
    if (language === "java") return window.SwingSimulator.parse(code);
    return { python: parsePython, csharp: parseCSharp, cpp: parseCpp, javascript: parseJavascript }[language](code);
  }

  window.DesktopSimulator = { render, parse };
})();
