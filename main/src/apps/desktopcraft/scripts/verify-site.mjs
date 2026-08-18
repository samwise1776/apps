import { access, readFile, readdir } from "node:fs/promises";
import { dirname, extname, join, resolve } from "node:path";
import { spawnSync } from "node:child_process";

const root = resolve(process.argv[2] || "dist");
const requiredFiles = [
  "index.html", "login.html", "tutorials.html", "forum.html", "appmaker.html", "make.html", "view.html", "leaderboard.html", "customization.html", "helper.html", "projects.html", "feedback.html", "404.html", "review.html",
  "manifest.webmanifest", "robots.txt", "assets/desktopcraft-icon.svg",
  "downloads/Desktopcraft.jar", "downloads/Desktopcraft-desktop.zip", "downloads/Desktopcraft-source.zip"
];
const missing = [];
for (const file of requiredFiles) {
  try { await access(join(root, file)); } catch { missing.push(file); }
}
if (missing.length) throw new Error(`Missing publication files: ${missing.join(", ")}`);

async function collect(folder, extension) {
  const result = [];
  for (const entry of await readdir(folder, { withFileTypes: true })) {
    const path = join(folder, entry.name);
    if (entry.isDirectory()) result.push(...await collect(path, extension));
    else if (extname(entry.name) === extension) result.push(path);
  }
  return result;
}

const htmlFiles = await collect(root, ".html");
const brokenLinks = [];
const forbiddenLabels = /private by design|no server|no data upload|posts stay in this browser|saved on this device|stay on this device|private to the device/i;
for (const htmlFile of htmlFiles) {
  const source = await readFile(htmlFile, "utf8");
  if (forbiddenLabels.test(source)) brokenLinks.push(`${htmlFile}: contains a development privacy label`);
  for (const match of source.matchAll(/(?:href|src)="([^"]+)"/g)) {
    const reference = match[1];
    if (!reference || reference.startsWith("#") || /^(?:https?:|mailto:|data:|javascript:)/.test(reference)) continue;
    const clean = decodeURIComponent(reference.split(/[?#]/)[0]);
    const target = clean.startsWith("/") ? join(root, clean) : resolve(dirname(htmlFile), clean);
    try { await access(target); } catch { brokenLinks.push(`${htmlFile}: ${reference}`); }
  }
}
if (brokenLinks.length) throw new Error(`Publication validation failed:\n${brokenLinks.join("\n")}`);

const scripts = await collect(root, ".js");
for (const script of scripts) {
  const result = spawnSync(process.execPath, ["--check", script], { encoding: "utf8" });
  if (result.status !== 0) throw new Error(`JavaScript syntax check failed for ${script}:\n${result.stderr}`);
}

JSON.parse(await readFile(join(root, "manifest.webmanifest"), "utf8"));
console.log(`Verified ${htmlFiles.length} HTML pages, ${scripts.length} scripts, all internal links, and all desktop downloads.`);
