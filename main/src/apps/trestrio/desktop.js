const path = require('path');

// Some terminals and IDEs export this flag for their own Electron helpers. It
// turns the Electron executable into plain Node, where the `app` API does not
// exist. Relaunch once with a clean environment so `npm run desktop` works
// from those terminals too.
if (process.env.ELECTRON_RUN_AS_NODE) {
  const { spawn } = require('child_process');
  const environment = { ...process.env };
  delete environment.ELECTRON_RUN_AS_NODE;
  const child = spawn(process.execPath, process.argv.slice(1), {
    env: environment,
    stdio: 'inherit'
  });
  child.once('error', error => {
    console.error('Could not launch Trestrio:', error);
    process.exitCode = 1;
  });
  child.once('exit', code => process.exit(code ?? 0));
  return;
}

const { app, BrowserWindow, shell } = require('electron');

let server;
let mainWindow;

async function createWindow() {
  process.env.TRESTRIO_DATA_FILE = path.join(app.getPath('userData'), 'data.json');
  if (!server) server = require('./server');
  if (!server.listening) {
    await new Promise((resolve, reject) => {
      server.once('error', reject);
      server.listen(0, '127.0.0.1', resolve);
    });
  }

  const port = server.address().port;
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 760,
    minHeight: 600,
    title: 'Trestrio',
    backgroundColor: '#eef3ff',
    autoHideMenuBar: true,
    webPreferences: {
      contextIsolation: true,
      sandbox: true,
      nodeIntegration: false
    }
  });

  mainWindow.on('closed', () => { mainWindow = null; });
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });
  mainWindow.webContents.on('will-navigate', (event, url) => {
    if (new URL(url).origin !== `http://127.0.0.1:${port}`) {
      event.preventDefault();
      if (/^https?:/.test(url)) shell.openExternal(url);
    }
  });
  await mainWindow.loadURL(`http://127.0.0.1:${port}`);
}

app.whenReady().then(createWindow).catch(error => {
  console.error(error);
  app.quit();
});
app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow().catch(error => console.error('Could not open Trestrio:', error));
  }
});
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
app.on('before-quit', () => {
  if (server?.listening) server.close();
});
