(function () {
  const storageKey = "desktopcraft-customization-v1";
  const defaults = Object.freeze({
    themeColor: "#2e7d5b",
    brightness: 100,
    explanationLevel: "balanced"
  });
  const explanationLevels = new Set(["concise", "balanced", "detailed"]);

  function clamp(value, minimum, maximum) {
    return Math.min(maximum, Math.max(minimum, Number(value) || 0));
  }

  function normalizeSettings(value = {}) {
    const themeColor = /^#[0-9a-f]{6}$/i.test(String(value.themeColor || ""))
      ? String(value.themeColor).toLowerCase()
      : defaults.themeColor;
    const explanationLevel = explanationLevels.has(value.explanationLevel)
      ? value.explanationLevel
      : defaults.explanationLevel;
    return {
      themeColor,
      brightness: clamp(value.brightness || defaults.brightness, 70, 130),
      explanationLevel
    };
  }

  function getSettings() {
    try {
      return normalizeSettings(JSON.parse(localStorage.getItem(storageKey) || "{}"));
    } catch {
      return { ...defaults };
    }
  }

  function hexToHsl(hex) {
    const red = parseInt(hex.slice(1, 3), 16) / 255;
    const green = parseInt(hex.slice(3, 5), 16) / 255;
    const blue = parseInt(hex.slice(5, 7), 16) / 255;
    const maximum = Math.max(red, green, blue);
    const minimum = Math.min(red, green, blue);
    const lightness = (maximum + minimum) / 2;
    const difference = maximum - minimum;
    let hue = 0;
    let saturation = 0;

    if (difference) {
      saturation = difference / (1 - Math.abs(2 * lightness - 1));
      if (maximum === red) hue = 60 * (((green - blue) / difference) % 6);
      else if (maximum === green) hue = 60 * ((blue - red) / difference + 2);
      else hue = 60 * ((red - green) / difference + 4);
    }
    if (hue < 0) hue += 360;
    return { hue: Math.round(hue), saturation: Math.round(saturation * 100), lightness: Math.round(lightness * 100) };
  }

  function applySettings(value, announce = false) {
    const settings = normalizeSettings(value);
    const root = document.documentElement;
    const { hue, saturation, lightness } = hexToHsl(settings.themeColor);
    const calmSaturation = Math.min(32, Math.max(8, Math.round(saturation * 0.28)));
    const navSaturation = Math.min(45, Math.max(18, Math.round(saturation * 0.62)));
    const highlightHue = (hue + 42) % 360;

    root.style.setProperty("--green", settings.themeColor);
    root.style.setProperty("--green-dark", `hsl(${hue} ${Math.max(28, saturation)}% ${Math.max(20, lightness - 12)}%)`);
    root.style.setProperty("--green-soft", `hsl(${hue} ${Math.max(24, Math.round(saturation * 0.52))}% 91%)`);
    root.style.setProperty("--nav", `hsl(${hue} ${navSaturation}% 12%)`);
    root.style.setProperty("--nav-panel", `hsl(${hue} ${navSaturation}% 17%)`);
    root.style.setProperty("--nav-active", `hsl(${hue} ${navSaturation}% 22%)`);
    root.style.setProperty("--nav-muted", `hsl(${hue} ${Math.min(24, navSaturation)}% 67%)`);
    root.style.setProperty("--lime", `hsl(${highlightHue} ${Math.max(58, saturation)}% 70%)`);
    root.style.setProperty("--canvas", `hsl(${hue} ${calmSaturation}% 96%)`);
    root.style.setProperty("--paper", `hsl(${hue} ${Math.max(5, Math.round(calmSaturation * 0.55))}% 99%)`);
    root.style.setProperty("--line", `hsl(${hue} ${Math.max(5, Math.round(calmSaturation * 0.45))}% 86%)`);
    root.style.setProperty("--soft-line", `hsl(${hue} ${Math.max(4, Math.round(calmSaturation * 0.4))}% 91%)`);

    const brightnessDifference = settings.brightness - 100;
    root.style.setProperty("--custom-brightness-overlay", brightnessDifference < 0 ? "#000" : "#fff");
    root.style.setProperty("--custom-brightness-opacity", String(
      brightnessDifference < 0 ? Math.abs(brightnessDifference) / 150 : brightnessDifference / 120
    ));
    root.dataset.explanationLevel = settings.explanationLevel;
    root.dataset.customBrightness = String(settings.brightness);

    const themeMeta = document.querySelector('meta[name="theme-color"]');
    if (themeMeta) themeMeta.setAttribute("content", `hsl(${hue} ${navSaturation}% 12%)`);
    if (announce) document.dispatchEvent(new CustomEvent("desktopcraft:customizationchange", { detail: settings }));
    return settings;
  }

  function saveSettings(value) {
    const settings = applySettings(value, true);
    try {
      localStorage.setItem(storageKey, JSON.stringify(settings));
      return { settings, saved: true };
    } catch {
      return { settings, saved: false };
    }
  }

  function resetSettings() {
    try { localStorage.removeItem(storageKey); } catch { /* Defaults still apply for this session. */ }
    return applySettings(defaults, true);
  }

  function initializeControls() {
    const form = document.querySelector("#customizationForm");
    if (!form) return;
    const colorInput = document.querySelector("#themeColor");
    const colorValue = document.querySelector("#themeColorValue");
    const brightnessInput = document.querySelector("#brightness");
    const brightnessValue = document.querySelector("#brightnessValue");
    const status = document.querySelector("#customizationStatus");
    const previewLevel = document.querySelector("#previewLevelLabel");
    const previewBrightness = document.querySelector("#previewBrightnessLabel");
    const presetButtons = [...document.querySelectorAll("[data-theme-color]")];

    function setForm(settings) {
      colorInput.value = settings.themeColor;
      brightnessInput.value = String(settings.brightness);
      const explanation = form.elements.explanationLevel;
      for (const option of explanation) option.checked = option.value === settings.explanationLevel;
    }

    function readForm() {
      return normalizeSettings({
        themeColor: colorInput.value,
        brightness: brightnessInput.value,
        explanationLevel: form.elements.explanationLevel.value
      });
    }

    function updateLabels(settings) {
      colorValue.value = settings.themeColor.toUpperCase();
      colorValue.textContent = settings.themeColor.toUpperCase();
      brightnessValue.value = `${settings.brightness}%`;
      brightnessValue.textContent = `${settings.brightness}%`;
      previewLevel.textContent = `${settings.explanationLevel.charAt(0).toUpperCase() + settings.explanationLevel.slice(1)} explanations`;
      previewBrightness.textContent = `${settings.brightness}% page brightness`;
      presetButtons.forEach((button) => button.classList.toggle("selected", button.dataset.themeColor === settings.themeColor));
    }

    function persistFromForm(message = "Changes saved automatically in this browser.") {
      const result = saveSettings(readForm());
      updateLabels(result.settings);
      status.textContent = result.saved ? message : "Browser storage is unavailable; changes will last until this tab closes.";
    }

    const initial = getSettings();
    setForm(initial);
    updateLabels(initial);
    colorInput.addEventListener("input", () => persistFromForm());
    brightnessInput.addEventListener("input", () => persistFromForm());
    form.addEventListener("change", (event) => {
      if (event.target.matches('[name="explanationLevel"]')) persistFromForm();
    });
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      persistFromForm("Preferences saved. They now apply across Desktopcraft.");
    });
    presetButtons.forEach((button) => button.addEventListener("click", () => {
      colorInput.value = button.dataset.themeColor;
      persistFromForm();
    }));
    document.querySelector("#resetCustomization").addEventListener("click", () => {
      const settings = resetSettings();
      setForm(settings);
      updateLabels(settings);
      status.textContent = "Default customization restored.";
    });
  }

  function initializePageMenu() {
    if (!document.body || !document.createElement || document.querySelector("#sitePagesMenu")) return;
    const pages = [
      ["Courses", "index.html"],
      ["Tutorials", "tutorials.html"],
      ["Forum", "forum.html"],
      ["App Maker", "appmaker.html"],
      ["Make App", "make.html"],
      ["Free Apps", "view.html"],
      ["Projects", "projects.html"],
      ["Helper AI", "helper.html"],
      ["Customize", "customization.html"],
      ["Leaderboard", "leaderboard.html"],
      ["Feedback", "feedback.html"],
      ["Sign In", "login.html"],
      ["Welcome", "welcomepage.html"]
    ];
    const currentFile = (window.location?.pathname || "").split("/").pop() || "index.html";
    const container = document.createElement("div");
    container.className = "site-pages";
    container.innerHTML = `
      <button class="site-pages-toggle" id="sitePagesToggle" type="button" aria-haspopup="true" aria-expanded="false" aria-controls="sitePagesMenu">
        <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="4" width="6" height="6" rx="1"/><rect x="14" y="4" width="6" height="6" rx="1"/><rect x="4" y="14" width="6" height="6" rx="1"/><rect x="14" y="14" width="6" height="6" rx="1"/></svg>
        <span>All pages</span>
      </button>
      <nav class="site-pages-menu" id="sitePagesMenu" aria-label="All Desktopcraft pages" hidden>
        <header><div><strong>Go to any page</strong><span>Desktopcraft site map</span></div><button type="button" data-close-pages aria-label="Close page menu">×</button></header>
        <div class="site-pages-grid">
          ${pages.map(([label, href]) => `<a href="${href}"${currentFile === href ? ' class="current" aria-current="page"' : ""}>${label}${currentFile === href ? "<small>Current</small>" : ""}</a>`).join("")}
        </div>
      </nav>`;
    document.body.appendChild(container);

    const toggle = container.querySelector("#sitePagesToggle");
    const menu = container.querySelector("#sitePagesMenu");
    const close = (returnFocus = false) => {
      menu.hidden = true;
      toggle.setAttribute("aria-expanded", "false");
      if (returnFocus) toggle.focus();
    };
    const open = () => {
      menu.hidden = false;
      toggle.setAttribute("aria-expanded", "true");
      menu.querySelector("a")?.focus();
    };
    toggle.addEventListener("click", () => menu.hidden ? open() : close());
    menu.querySelector("[data-close-pages]").addEventListener("click", () => close(true));
    document.addEventListener("click", (event) => {
      if (!menu.hidden && !container.contains(event.target)) close();
    });
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape" && !menu.hidden) close(true);
    });
  }

  function initializePageFeatures() {
    initializeControls();
    initializePageMenu();
  }

  window.DesktopcraftCustomization = { defaults, getSettings, applySettings, saveSettings, resetSettings };
  applySettings(getSettings());
  window.addEventListener("storage", (event) => {
    if (event.key === storageKey) applySettings(getSettings(), true);
  });
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", initializePageFeatures);
  else initializePageFeatures();
})();
