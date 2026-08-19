import { createReadStream } from "node:fs";
import { access, readFile, stat } from "node:fs/promises";
import { createServer } from "node:http";
import { dirname, extname, join, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = dirname(dirname(fileURLToPath(import.meta.url)));
const siteRoot = resolve(projectRoot, process.argv[2] || "dist");
const port = Number(process.env.PORT) || 4173;
const types = {
  ".html": "text/html; charset=utf-8", ".css": "text/css; charset=utf-8", ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8", ".webmanifest": "application/manifest+json; charset=utf-8", ".svg": "image/svg+xml",
  ".jar": "application/java-archive", ".zip": "application/zip", ".txt": "text/plain; charset=utf-8"
};

await access(siteRoot);
const server = createServer(async (request, response) => {
  try {
    const url = new URL(request.url || "/", "http://localhost");
    let pathname = decodeURIComponent(url.pathname);
    if (pathname === "/") pathname = "/index.html";
    let target = resolve(siteRoot, `.${pathname}`);
    if (target !== siteRoot && !target.startsWith(`${siteRoot}${sep}`)) throw new Error("Invalid path");
    if ((await stat(target)).isDirectory()) target = join(target, "index.html");
    response.writeHead(200, {
      "Content-Type": types[extname(target).toLowerCase()] || "application/octet-stream",
      "X-Content-Type-Options": "nosniff",
      "Referrer-Policy": "strict-origin-when-cross-origin"
    });
    createReadStream(target).pipe(response);
  } catch {
    const notFound = join(siteRoot, "404.html");
    try {
      response.writeHead(404, { "Content-Type": "text/html; charset=utf-8" });
      response.end(await readFile(notFound));
    } catch {
      response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Not found");
    }
  }
});

server.listen(port, () => console.log(`Desktopcraft preview: http://localhost:${port}`));
