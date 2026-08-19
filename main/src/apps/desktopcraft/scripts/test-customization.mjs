import assert from "node:assert/strict";

const values = new Map();
const properties = new Map();
globalThis.localStorage = {
  getItem: (key) => values.has(key) ? values.get(key) : null,
  setItem: (key, value) => values.set(key, String(value)),
  removeItem: (key) => values.delete(key)
};
globalThis.CustomEvent = class CustomEvent {
  constructor(type, options = {}) {
    this.type = type;
    this.detail = options.detail;
  }
};
globalThis.document = {
  readyState: "complete",
  documentElement: {
    dataset: {},
    style: { setProperty: (name, value) => properties.set(name, String(value)) }
  },
  querySelector: () => null,
  addEventListener: () => {},
  dispatchEvent: () => {}
};
globalThis.window = {
  addEventListener: () => {}
};

await import(`../customization.js?test=${Date.now()}`);
const customization = window.DesktopcraftCustomization;
assert.deepEqual(customization.getSettings(), customization.defaults);

const result = customization.saveSettings({
  themeColor: "#2869a8",
  brightness: 118,
  explanationLevel: "detailed"
});
assert.equal(result.saved, true);
assert.equal(document.documentElement.dataset.explanationLevel, "detailed");
assert.equal(document.documentElement.dataset.customBrightness, "118");
assert.equal(properties.get("--green"), "#2869a8");
assert.equal(properties.get("--custom-brightness-overlay"), "#fff");
assert.equal(JSON.parse(values.get("desktopcraft-customization-v1")).brightness, 118);

const reset = customization.resetSettings();
assert.deepEqual(reset, customization.defaults);
assert.equal(values.has("desktopcraft-customization-v1"), false);

console.log("Verified site-wide customization persistence and theme application.");
