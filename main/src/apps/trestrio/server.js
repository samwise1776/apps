const http = require('http');
const fs = require('fs/promises');
const path = require('path');
const crypto = require('crypto');

const PORT = Number(process.env.PORT) || 3000;
const HOST = process.env.TRESTRIO_HOST || '127.0.0.1';
const PUBLIC = path.join(__dirname, 'public');
const DATA = process.env.TRESTRIO_DATA_FILE || path.join(__dirname, 'data.json');
const defaults = { tasks: [], notes: '', focusSessions: 0, focusMinutes: 0 };

async function readData() {
  try { return { ...defaults, ...JSON.parse(await fs.readFile(DATA, 'utf8')) }; }
  catch (error) { if (error.code === 'ENOENT') return { ...defaults }; throw error; }
}
let pendingWrite = Promise.resolve();
async function writeData(data) {
  const snapshot = JSON.stringify(data, null, 2);
  pendingWrite = pendingWrite.then(async () => {
    const temporary = `${DATA}.${process.pid}.${crypto.randomUUID()}.tmp`;
    await fs.mkdir(path.dirname(DATA), { recursive: true });
    try {
      await fs.writeFile(temporary, snapshot, { encoding: 'utf8', mode: 0o600 });
      await fs.rename(temporary, DATA);
    } finally {
      await fs.rm(temporary, { force: true });
    }
  });
  return pendingWrite;
}
async function body(req) {
  let raw = '';
  for await (const chunk of req) {
    raw += chunk;
    if (raw.length > 1e6) throw new Error('Payload too large');
  }
  return raw ? JSON.parse(raw) : {};
}
function json(res, status, payload) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(payload));
}

async function api(req, res, url) {
  const data = await readData();
  if (req.method === 'GET' && url.pathname === '/api/state') return json(res, 200, data);
  if (req.method === 'POST' && url.pathname === '/api/tasks') {
    const input = await body(req);
    const title = String(input.title || '').trim().slice(0, 120);
    if (!title) return json(res, 400, { error: 'Task title is required' });
    const task = { id: crypto.randomUUID(), title, completed: false, createdAt: new Date().toISOString() };
    data.tasks.unshift(task); await writeData(data); return json(res, 201, task);
  }
  const taskMatch = url.pathname.match(/^\/api\/tasks\/([^/]+)$/);
  if (taskMatch && req.method === 'PATCH') {
    const task = data.tasks.find(t => t.id === taskMatch[1]);
    if (!task) return json(res, 404, { error: 'Task not found' });
    const input = await body(req);
    if (typeof input.completed === 'boolean') task.completed = input.completed;
    if (typeof input.title === 'string' && input.title.trim()) task.title = input.title.trim().slice(0, 120);
    await writeData(data); return json(res, 200, task);
  }
  if (taskMatch && req.method === 'DELETE') {
    const before = data.tasks.length;
    data.tasks = data.tasks.filter(t => t.id !== taskMatch[1]);
    if (before === data.tasks.length) return json(res, 404, { error: 'Task not found' });
    await writeData(data); return json(res, 200, { ok: true });
  }
  if (req.method === 'PUT' && url.pathname === '/api/notes') {
    const input = await body(req); data.notes = String(input.notes || '').slice(0, 10000);
    await writeData(data); return json(res, 200, { notes: data.notes });
  }
  if (req.method === 'POST' && url.pathname === '/api/focus') {
    const input = await body(req); const minutes = Math.max(1, Math.min(180, Number(input.minutes) || 25));
    data.focusSessions += 1; data.focusMinutes += minutes; await writeData(data);
    return json(res, 200, { focusSessions: data.focusSessions, focusMinutes: data.focusMinutes });
  }
  return json(res, 404, { error: 'Not found' });
}

const types = { '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.svg': 'image/svg+xml' };
const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Referrer-Policy', 'no-referrer');
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
    if (url.pathname.startsWith('/api/') && !['GET', 'HEAD', 'OPTIONS'].includes(req.method)) {
      const origin = req.headers.origin;
      if (origin && origin !== `http://${req.headers.host}`) return json(res, 403, { error: 'Cross-origin request denied' });
    }
    if (url.pathname.startsWith('/api/')) return await api(req, res, url);
    const relative = url.pathname === '/' ? 'index.html' : url.pathname.slice(1);
    const file = path.resolve(PUBLIC, relative);
    if (!file.startsWith(PUBLIC + path.sep) && file !== path.join(PUBLIC, 'index.html')) return json(res, 403, { error: 'Forbidden' });
    const content = await fs.readFile(file);
    res.writeHead(200, { 'Content-Type': types[path.extname(file)] || 'application/octet-stream' }); res.end(content);
  } catch (error) {
    if (error.code === 'ENOENT') return json(res, 404, { error: 'Not found' });
    console.error(error); json(res, error instanceof SyntaxError ? 400 : 500, { error: 'Something went wrong' });
  }
});
if (require.main === module) server.listen(PORT, HOST, () => console.log(`Trestrio is running at http://${HOST}:${PORT}`));
module.exports = server;
