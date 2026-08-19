(function () {
  const supportedTypes = new Set([
    "JPanel",
    "JLabel",
    "JButton",
    "JTextField",
    "JPasswordField",
    "JTextArea",
    "JCheckBox",
    "JRadioButton",
    "JComboBox",
    "JSlider",
    "JProgressBar",
    "JSpinner",
    "JList",
    "JTable",
    "JTree",
    "JSeparator",
    "JToolBar"
  ]);

  const stringValues = (source) =>
    [...String(source).matchAll(/"((?:\\.|[^"\\])*)"/g)].map((match) =>
      match[1].replaceAll('\\"', '"').replaceAll("\\n", "\n").replaceAll("\\\\", "\\")
    );

  const numberValues = (source) => [...String(source).matchAll(/-?\d+(?:\.\d+)?/g)].map((match) => Number(match[0]));

  function detectLayout(source) {
    if (/BorderLayout/.test(source)) return { type: "border", columns: 1 };
    if (/GridLayout/.test(source)) {
      const values = numberValues(source.match(/GridLayout\s*\(([^)]*)\)/)?.[1] || "");
      return { type: "grid", columns: Math.max(1, Math.min(values[1] || values[0] || 2, 4)) };
    }
    if (/BoxLayout\.X_AXIS/.test(source)) return { type: "flow", columns: 1 };
    if (/BoxLayout|GridBagLayout|SpringLayout/.test(source)) return { type: "stack", columns: 1 };
    if (/CardLayout/.test(source)) return { type: "card", columns: 1 };
    return { type: "flow", columns: 1 };
  }

  function parse(code) {
    const title = stringValues(code.match(/new\s+JFrame\s*\(([^;]*)\)/)?.[1] || "")[0] || "Swing Preview";
    const declarations = new Map();
    const models = new Map();
    const declarationPattern = /(?:^|\n)\s*(?:final\s+)?[\w$.]+(?:\s*<[^;=]+>)?\s+(\w+)\s*=\s*new\s+([\w$.]+)(?:\s*<[^>]*>)?\s*\(([^;]*)\)\s*;/g;
    let match;

    while ((match = declarationPattern.exec(code))) {
      const [, name, qualifiedType, args] = match;
      const type = qualifiedType.split(".").pop();
      if (type === "JFrame") continue;
      if (type.includes("Model")) {
        models.set(name, { name, type, items: [] });
        continue;
      }
      if (!supportedTypes.has(type)) continue;
      declarations.set(name, {
        name,
        type,
        args,
        layout: type === "JPanel" || type === "JToolBar" ? detectLayout(args) : null,
        children: [],
        parent: null,
        region: null
      });
    }

    const setupCode = code.split(/\.addActionListener\s*\(/)[0];
    for (const [modelName, model] of models) {
      const itemPattern = new RegExp(`${modelName}\\.addElement\\s*\\(\\s*"([^"]*)"\\s*\\)`, "g");
      let itemMatch;
      while ((itemMatch = itemPattern.exec(setupCode))) model.items.push(itemMatch[1]);
    }

    for (const component of declarations.values()) {
      if (component.type !== "JPanel") continue;
      const setLayout = code.match(new RegExp(`${component.name}\\.setLayout\\s*\\(([^;]+)\\)`));
      if (setLayout) component.layout = detectLayout(setLayout[1]);
    }

    const inline = [];
    const inlinePattern = /(\w+)\.add\(\s*new\s+(JButton|JLabel|JTextField|JCheckBox|JRadioButton)\s*\(([^;)]*)\)\s*\)\s*;/g;
    while ((match = inlinePattern.exec(code))) {
      const [, parent, type, args] = match;
      const name = `inline_${inline.length}`;
      const component = { name, type, args, layout: null, children: [], parent, region: null };
      declarations.set(name, component);
      inline.push(component);
    }

    const relationships = [];
    const addPattern = /(\w+)(?:\.getContentPane\(\))?\.add\(\s*(?:new\s+JScrollPane\s*\(\s*)?(\w+)\s*\)?\s*(?:,\s*BorderLayout\.(\w+))?\s*\)\s*;/g;
    while ((match = addPattern.exec(code))) {
      const [, parent, childName, region] = match;
      const child = declarations.get(childName);
      if (!child) continue;
      relationships.push({ parent, child: childName, region: region || null });
      child.parent = parent;
      child.region = region || null;
      if (declarations.has(parent)) declarations.get(parent).children.push(childName);
    }

    for (const component of inline) {
      if (declarations.has(component.parent) && !declarations.get(component.parent).children.includes(component.name)) {
        declarations.get(component.parent).children.push(component.name);
      }
    }

    const rootChildren = relationships.filter((item) => /^(frame|window|app)$/i.test(item.parent)).map((item) => item.child);
    for (const component of declarations.values()) {
      if (/^(frame|window|app)$/i.test(component.parent || "")) rootChildren.push(component.name);
    }
    if (!rootChildren.length) {
      for (const component of declarations.values()) {
        if (!component.parent || !declarations.has(component.parent)) rootChildren.push(component.name);
      }
    }

    return { title, code, declarations, models, rootChildren: [...new Set(rootChildren)], layout: detectLayout(code) };
  }

  function applyStaticProperties(element, component, code) {
    const name = component.name;
    const disabled = new RegExp(`${name}\\.setEnabled\\s*\\(\\s*false\\s*\\)`).test(code);
    const selected = new RegExp(`${name}\\.setSelected\\s*\\(\\s*true\\s*\\)`).test(code);
    const tooltip = stringValues(code.match(new RegExp(`${name}\\.setToolTipText\\s*\\(([^;]*)\\)`))?.[1] || "")[0];
    if ("disabled" in element) element.disabled = disabled;
    if ("checked" in element) element.checked = selected || element.checked;
    if (tooltip) element.title = tooltip;

    if (new RegExp(`${name}\\.setBackground`).test(code)) element.classList.add("sim-themed");
    if (new RegExp(`${name}\\.setForeground`).test(code)) element.classList.add("sim-accent-text");
    if (new RegExp(`${name}\\.setFont`).test(code)) element.classList.add("sim-emphasis");
    if (new RegExp(`${name}\\.setBorder`).test(code)) element.classList.add("sim-bordered");
  }

  function createComponent(component, parsed, refs) {
    const { type, args, name } = component;
    const strings = stringValues(args);
    const numbers = numberValues(args);
    let element;

    if (type === "JPanel" || type === "JToolBar") {
      element = document.createElement("div");
      element.className = `sim-container ${type === "JToolBar" ? "sim-toolbar" : ""}`;
      applyLayout(element, component.layout || { type: "flow", columns: 1 });
    } else if (type === "JLabel") {
      element = document.createElement("div");
      element.className = "swing-label";
      element.textContent = strings[0] || "Label";
    } else if (type === "JButton") {
      element = document.createElement("button");
      element.type = "button";
      element.className = "swing-button";
      element.textContent = strings[0] || "Button";
    } else if (type === "JTextField" || type === "JPasswordField") {
      element = document.createElement("input");
      element.className = "swing-input";
      element.type = type === "JPasswordField" ? "password" : "text";
      element.value = strings[0] || "";
      element.placeholder = type === "JPasswordField" ? "Password" : "Type here";
    } else if (type === "JTextArea") {
      element = document.createElement("textarea");
      element.className = "swing-input sim-textarea";
      element.value = strings[0] || "";
      element.rows = Math.min(numbers[0] || 3, 5);
    } else if (type === "JCheckBox" || type === "JRadioButton") {
      element = document.createElement("label");
      element.className = "sim-choice";
      const input = document.createElement("input");
      input.type = type === "JCheckBox" ? "checkbox" : "radio";
      input.name = type === "JRadioButton" ? "sim-radio-group" : "";
      input.checked = /true/.test(args);
      const copy = document.createElement("span");
      copy.textContent = strings[0] || (type === "JCheckBox" ? "Check box" : "Radio choice");
      element.append(input, copy);
      element.control = input;
    } else if (type === "JComboBox") {
      element = document.createElement("select");
      element.className = "swing-input sim-select";
      (strings.length ? strings : ["Option one", "Option two"]).forEach((option) => {
        const node = document.createElement("option");
        node.textContent = option;
        element.appendChild(node);
      });
    } else if (type === "JSlider") {
      element = document.createElement("label");
      element.className = "sim-range-wrap";
      const input = document.createElement("input");
      input.type = "range";
      input.min = numbers[0] ?? 0;
      input.max = numbers[1] ?? 100;
      input.value = numbers[2] ?? 50;
      const output = document.createElement("output");
      output.textContent = input.value;
      input.addEventListener("input", () => (output.textContent = input.value));
      element.append(input, output);
      element.control = input;
    } else if (type === "JProgressBar") {
      element = document.createElement("div");
      element.className = "sim-progress";
      const valueMatch = parsed.code.match(new RegExp(`${name}\\.setValue\\s*\\(\\s*(\\d+)\\s*\\)`));
      const value = Number(valueMatch?.[1] || numbers[2] || 0);
      element.dataset.value = String(value);
      element.innerHTML = `<span style="width:${Math.max(0, Math.min(value, 100))}%"></span><strong>${value}%</strong>`;
    } else if (type === "JSpinner") {
      element = document.createElement("input");
      element.className = "swing-input sim-spinner";
      element.type = "number";
      element.value = numbers[0] ?? 0;
    } else if (type === "JList") {
      element = document.createElement("ul");
      element.className = "sim-list";
      const model = parsed.models.get(args.trim());
      const items = model?.items.length ? model.items : strings.length ? strings : ["List item"];
      items.forEach((item) => appendListItem(element, item));
      if (model) model.element = element;
    } else if (type === "JTable") {
      element = createTable(parsed.code);
    } else if (type === "JTree") {
      element = document.createElement("ul");
      element.className = "sim-tree";
      element.innerHTML = "<li>▾ Root<ul><li>Document</li><li>Pictures</li><li>Projects</li></ul></li>";
    } else if (type === "JSeparator") {
      element = document.createElement("hr");
      element.className = "sim-separator";
    } else {
      element = document.createElement("div");
      element.className = "sim-unknown";
      element.textContent = type;
    }

    element.dataset.simName = name;
    applyStaticProperties(element.control || element, component, parsed.code);
    refs.set(name, element);
    return element;
  }

  function createTable(code) {
    const table = document.createElement("table");
    table.className = "sim-table";
    const columnsSource = code.match(/String\s*\[\]\s+columns\s*=\s*\{([^;]+)\}/)?.[1] || "";
    const columns = stringValues(columnsSource);
    const rowSource = code.match(/Object\s*\[\]\s*\[\]\s+rows\s*=\s*\{([\s\S]*?)\};/)?.[1] || "";
    const rows = [...rowSource.matchAll(/\{([^{}]+)\}/g)].map((match) => stringValues(match[1]));
    const headers = columns.length ? columns : ["Column A", "Column B"];
    const data = rows.length ? rows : [["Alpha", "Ready"], ["Beta", "Learning"]];
    table.innerHTML = `<thead><tr>${headers.map((header) => `<th>${escapeHtml(header)}</th>`).join("")}</tr></thead><tbody>${data
      .map((row) => `<tr>${row.map((cell) => `<td>${escapeHtml(cell)}</td>`).join("")}</tr>`)
      .join("")}</tbody>`;
    return table;
  }

  function appendListItem(list, value) {
    const item = document.createElement("li");
    item.textContent = value;
    item.tabIndex = 0;
    item.addEventListener("click", () => {
      [...list.children].forEach((node) => node.classList.remove("selected"));
      item.classList.add("selected");
    });
    list.appendChild(item);
  }

  function escapeHtml(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function applyLayout(element, layout) {
    element.classList.remove("sim-flow", "sim-grid-layout", "sim-border-layout", "sim-stack-layout", "sim-card-layout");
    const className = {
      flow: "sim-flow",
      grid: "sim-grid-layout",
      border: "sim-border-layout",
      stack: "sim-stack-layout",
      card: "sim-card-layout"
    }[layout.type] || "sim-flow";
    element.classList.add(className);
    if (layout.type === "grid") element.style.setProperty("--sim-columns", layout.columns);
  }

  function evaluateText(expression, refs, counters, body) {
    const ternary = expression.match(/\?\s*"([^"]*)"\s*:\s*"([^"]*)"/);
    if (ternary) {
      const fieldName = body.match(/String\s+\w+\s*=\s*(\w+)\.getText/)?.[1];
      const value = fieldName ? getControlValue(refs.get(fieldName)) : "";
      return value.trim() ? ternary[2] : ternary[1];
    }

    return expression
      .split(/\s*\+\s*/)
      .map((part) => {
        const literal = part.match(/^"([\s\S]*)"$/);
        if (literal) return literal[1].replaceAll("\\n", "\n");
        const field = part.match(/(\w+)\.getText\(\)(?:\.trim\(\))?/);
        if (field) return getControlValue(refs.get(field[1]));
        const counter = part.match(/(?:String\.valueOf\()?\s*(\w+)\s*\[\s*0\s*\]/);
        if (counter) return String(counters[counter[1]] ?? 0);
        const variable = body.match(new RegExp(`String\\s+${part.trim()}\\s*=\\s*(\\w+)\\.getText`));
        if (variable) return getControlValue(refs.get(variable[1])).trim();
        return "";
      })
      .join("");
  }

  function getControlValue(element) {
    if (!element) return "";
    const control = element.control || element;
    return "value" in control ? String(control.value) : control.textContent || "";
  }

  function findListenerBody(code, name) {
    const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const block = code.match(new RegExp(`${escaped}\\.addActionListener\\s*\\(\\s*\\w+\\s*->\\s*\\{([\\s\\S]*?)\\}\\s*\\)\\s*;`));
    if (block) return block[1];
    const single = code.match(new RegExp(`${escaped}\\.addActionListener\\s*\\(\\s*\\w+\\s*->\\s*([^;]+;)\\s*\\)`));
    return single?.[1] || "";
  }

  function wireEvents(parsed, refs, onConsole, mount) {
    const counters = {};
    for (const match of parsed.code.matchAll(/int\s*\[\]\s*(\w+)\s*=\s*\{\s*(-?\d+)\s*\}/g)) {
      counters[match[1]] = Number(match[2]);
    }

    for (const [name, element] of refs) {
      const component = parsed.declarations.get(name);
      if (!component || !["JButton", "JTextField"].includes(component.type)) continue;
      const body = findListenerBody(parsed.code, name);
      const trigger = () => {
        if (!body) {
          onConsole(`${name} fired an event, but no ActionListener was found.`);
          return;
        }

        for (const increment of body.matchAll(/(\w+)\s*\[\s*0\s*\]\s*(\+\+|\+=\s*(-?\d+))/g)) {
          counters[increment[1]] = (counters[increment[1]] ?? 0) + (increment[2] === "++" ? 1 : Number(increment[3]));
        }

        for (const setter of body.matchAll(/(\w+)\.setText\s*\(([\s\S]*?)\)\s*;/g)) {
          const target = refs.get(setter[1]);
          if (!target) continue;
          const value = evaluateText(setter[2], refs, counters, body);
          if ("value" in target) target.value = value;
          else target.textContent = value;
        }

        for (const add of body.matchAll(/(\w+)\.addElement\s*\(([^;]+)\)\s*;/g)) {
          const model = parsed.models.get(add[1]);
          if (!model?.element) continue;
          const value = evaluateText(add[2], refs, counters, body) || stringValues(add[2])[0] || "New item";
          appendListItem(model.element, value);
        }

        for (const clear of body.matchAll(/(\w+)\.setText\s*\(\s*""\s*\)/g)) {
          const target = refs.get(clear[1]);
          if (target && "value" in target) target.value = "";
        }

        for (const progress of body.matchAll(/(\w+)\.setValue\s*\(\s*\1\.getValue\(\)\s*\+\s*(\d+)\s*\)/g)) {
          const target = refs.get(progress[1]);
          if (!target) continue;
          const value = Math.min(100, Number(target.dataset.value || 0) + Number(progress[2]));
          target.dataset.value = String(value);
          target.querySelector("span").style.width = `${value}%`;
          target.querySelector("strong").textContent = `${value}%`;
        }

        const dialog = body.match(/JOptionPane\.show\w+Dialog\s*\([^,]+,\s*"([^"]*)"/);
        if (dialog) showDialog(mount, dialog[1]);

        const printed = body.match(/System\.out\.println\s*\(([^;]+)\)/);
        onConsole(printed ? `System.out: ${evaluateText(printed[1], refs, counters, body)}` : `ActionEvent handled by ${name}.`);
      };

      if (component.type === "JButton") element.addEventListener("click", trigger);
      else element.addEventListener("keydown", (event) => event.key === "Enter" && trigger());
    }
  }

  function showDialog(mount, message) {
    mount.querySelector(".sim-dialog-overlay")?.remove();
    const overlay = document.createElement("div");
    overlay.className = "sim-dialog-overlay";
    overlay.innerHTML = `<div class="sim-dialog"><strong>Message</strong><p>${escapeHtml(message)}</p><button type="button" class="swing-button primary">OK</button></div>`;
    overlay.querySelector("button").addEventListener("click", () => overlay.remove());
    mount.appendChild(overlay);
  }

  function renderNode(name, parsed, refs, stack = new Set()) {
    if (stack.has(name)) return document.createTextNode("");
    const component = parsed.declarations.get(name);
    if (!component) return document.createTextNode("");
    const nextStack = new Set(stack).add(name);
    const element = createComponent(component, parsed, refs);
    if (component.children.length && (component.type === "JPanel" || component.type === "JToolBar")) {
      component.children.forEach((childName) => {
        const child = parsed.declarations.get(childName);
        const childElement = renderNode(childName, parsed, refs, nextStack);
        if (child?.region) childElement.classList?.add(`sim-region-${child.region.toLowerCase()}`);
        element.appendChild(childElement);
      });
    }
    return element;
  }

  function render(code, mount, options = {}) {
    const parsed = parse(code);
    const refs = new Map();
    const windowNode = document.createElement("div");
    windowNode.className = "swing-window sim-window-large";
    windowNode.innerHTML = `<div class="swing-titlebar"><span class="window-controls"><span></span><span></span><span></span></span>${escapeHtml(parsed.title)}</div>`;
    const body = document.createElement("div");
    body.className = "swing-body sim-container";
    applyLayout(body, parsed.layout.type === "flow" ? { type: "border", columns: 1 } : parsed.layout);

    parsed.rootChildren.forEach((name) => {
      const component = parsed.declarations.get(name);
      const element = renderNode(name, parsed, refs);
      if (component?.region) element.classList?.add(`sim-region-${component.region.toLowerCase()}`);
      body.appendChild(element);
    });

    if (/paintComponent\s*\(/.test(code)) {
      const painted = document.createElement("div");
      painted.className = "sim-painted-panel";
      painted.innerHTML = "<span></span><strong>Graphics2D canvas</strong>";
      body.appendChild(painted);
    }

    if (!body.children.length) {
      body.innerHTML = `<div class="sim-empty"><strong>No supported component declarations yet</strong><span>Try creating a JLabel, JButton, JTextField, JPanel, JTable, JTree, JList, or JProgressBar.</span></div>`;
    }

    windowNode.appendChild(body);
    mount.replaceChildren(windowNode);
    wireEvents(parsed, refs, options.onConsole || (() => {}), mount);
    return {
      title: parsed.title,
      components: refs.size,
      listeners: [...refs.keys()].filter((name) => findListenerBody(code, name)).length
    };
  }

  window.SwingSimulator = { render, parse };
})();
