import { readFile, readdir, stat } from "node:fs/promises";
import { extname, join, resolve } from "node:path";

const root = resolve(process.argv[2] || "dist");
const failures = [];
const htmlFiles = [];
const assetFiles = [];

async function walk(folder) {
  for (const entry of await readdir(folder, { withFileTypes: true })) {
    const path = join(folder, entry.name);
    if (entry.isDirectory()) {
      if (entry.name !== "downloads") await walk(path);
    } else {
      assetFiles.push(path);
      if (extname(path) === ".html") htmlFiles.push(path);
    }
  }
}

await walk(root);
for (const file of htmlFiles) {
  const source = await readFile(file, "utf8");
  const name = file.slice(root.length + 1);
  const require = (condition, message) => { if (!condition) failures.push(`${name}: ${message}`); };
  require(/<html\s[^>]*lang="[a-z]{2}/i.test(source), "missing document language");
  require(/<meta\s[^>]*name="viewport"/i.test(source), "missing responsive viewport");
  require(/<meta\s[^>]*name="description"/i.test(source), "missing description metadata");
  require(/<title>[^<]{8,}<\/title>/i.test(source), "missing descriptive title");
  require(/<main(?:\s|>)/i.test(source), "missing main landmark");
  require(/<h1(?:\s|>)/i.test(source), "missing level-one heading");
  require(/class="skip-link"[^>]*href="#main-content"/i.test(source), "missing keyboard skip link");
  require(/<main\b[^>]*id="main-content"/i.test(source), "main landmark is not a skip-link target");
  const ids = [...source.matchAll(/\sid="([^"]+)"/g)].map((match) => match[1]);
  const duplicates = ids.filter((id, index) => ids.indexOf(id) !== index);
  require(duplicates.length === 0, `duplicate IDs: ${[...new Set(duplicates)].join(", ")}`);
  for (const match of source.matchAll(/<img\b[^>]*>/gi)) require(/\salt="[^"]*"/i.test(match[0]), "image is missing alt text");
  for (const match of source.matchAll(/<button\b([^>]*)>([\s\S]*?)<\/button>/gi)) {
    const text = match[2].replace(/<[^>]+>/g, "").trim();
    require(Boolean(text) || /aria-label="[^"]+"/i.test(match[1]), "button has no accessible name");
  }
  for (const match of source.matchAll(/<a\b[^>]*>([\s\S]*?)<\/a>/gi)) {
    require(!/<button\b/i.test(match[1]), "interactive button is nested inside a link");
  }
}

const budgets = { ".html": 100_000, ".css": 180_000, ".js": 650_000 };
let totalBytes = 0;
for (const file of assetFiles) {
  const size = (await stat(file)).size;
  totalBytes += size;
  const limit = budgets[extname(file)];
  if (limit && size > limit) failures.push(`${file.slice(root.length + 1)}: ${size} bytes exceeds ${limit}-byte budget`);
}
if (totalBytes > 3_000_000) failures.push(`public site is ${totalBytes} bytes, exceeding the 3 MB non-download budget`);

const requiredHeaders = ["X-Content-Type-Options", "Referrer-Policy", "Permissions-Policy", "Content-Security-Policy"];
const netlify = await readFile("netlify.toml", "utf8");
const vercel = await readFile("vercel.json", "utf8");
for (const header of requiredHeaders) {
  if (!netlify.includes(header)) failures.push(`netlify.toml: missing ${header}`);
  if (!vercel.includes(header)) failures.push(`vercel.json: missing ${header}`);
}
if (failures.length) throw new Error(`Quality audit failed:\n${failures.join("\n")}`);
console.log(`Quality audit passed for ${htmlFiles.length} pages; accessibility structure, unique IDs, controls, security headers, and ${totalBytes}-byte performance budget verified.`);
