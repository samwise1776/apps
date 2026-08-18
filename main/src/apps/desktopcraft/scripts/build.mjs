import { copyFile, cp, mkdir, rm } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = dirname(dirname(fileURLToPath(import.meta.url)));
const outputRoot = join(projectRoot, "dist");
const publicFiles = [
  "index.html", "login.html", "tutorials.html", "forum.html", "appmaker.html", "make.html", "view.html", "leaderboard.html", "customization.html", "helper.html", "projects.html", "feedback.html", "welcomepage.html", "404.html", "review.html",
  "styles.css", "login.css", "tutorials.css", "forum.css", "appmaker.css", "community.css", "leaderboard.css", "customization.css", "helper.css", "projects.css", "feedback.css", "review.css",
  "auth.js", "app.js", "login.js", "tutorials.js", "forum.js", "appmaker.js", "make.js", "view.js", "leaderboard.js", "customization.js", "helper.js", "projects-core.js", "projects.js", "feedback.js", "review.js",
  "lessons-extra.js", "desktop-courses.js", "language-courses.js", "curriculum-expansion.js", "simulator.js", "desktop-simulator.js",
  "manifest.webmanifest", "robots.txt"
];

await rm(outputRoot, { recursive: true, force: true });
await mkdir(outputRoot, { recursive: true });
await Promise.all(publicFiles.map((file) => copyFile(join(projectRoot, file), join(outputRoot, file))));
await Promise.all([
  cp(join(projectRoot, "assets"), join(outputRoot, "assets"), { recursive: true }),
  cp(join(projectRoot, "downloads"), join(outputRoot, "downloads"), { recursive: true })
]);

console.log(`Desktopcraft production build created at ${outputRoot}`);
