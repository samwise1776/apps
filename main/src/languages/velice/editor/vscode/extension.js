const vscode = require('vscode');
const { spawn } = require('child_process');

function getCliCommand() {
  const cfg = vscode.workspace.getConfiguration('velice');
  return cfg.get('executablePath', 'velice');
}

function runCli(args, env) {
  return new Promise((resolve, reject) => {
    const cmd = getCliCommand();
    const child = spawn(cmd, args, { shell: true, cwd: vscode.workspace.workspaceFolders?.[0]?.uri?.fsPath, env });
    let out = '';
    let err = '';
    child.stdout.on('data', d => { out += d; });
    child.stderr.on('data', d => { err += d; });
    child.on('close', code => {
      if (code !== 0) {
        resolve({ ok: false, out, err });
      } else {
        resolve({ ok: true, out, err });
      }
    });
    child.on('error', reject);
  });
}

async function runCurrentFile() {
  const editor = vscode.window.activeTextEditor;
  if (!editor) return;
  if (editor.document.languageId !== 'velice') {
    vscode.window.showWarningMessage('Not a Velice file.');
    return;
  }
  const filePath = editor.document.uri.fsPath;
  await vscode.window.withProgress(
    { location: vscode.ProgressLocation.Notification, title: 'Running Velice...' },
    async () => {
      const env = { ...process.env };
      if (vscode.workspace.getConfiguration('velice').get('guiHeadless', false)) {
        env.VELICE_GUI = 'none';
      }
      const result = await runCli(['run', filePath], env);
      const channel = vscode.window.createOutputChannel('Velice');
      channel.clear();
      channel.appendLine(result.ok ? '--- output ---' : '--- error ---');
      channel.append(result.ok ? result.out : (result.err || result.out));
      channel.show();
      if (!result.ok) {
        vscode.window.showErrorMessage('Velice run failed — see output channel.');
      }
    }
  );
}

async function openRepl() {
  const terminal = vscode.window.createTerminal({ name: 'Velice REPL', iconPath: vscode.Uri.file(__dirname + '/icons/velice-icon.svg') });
  terminal.sendText(`${getCliCommand()} repl`);
  terminal.show();
}

function activate(context) {
  context.subscriptions.push(
    vscode.commands.registerCommand('velice.runFile', runCurrentFile),
    vscode.commands.registerCommand('velice.openRepl', openRepl),
    vscode.workspace.onDidChangeConfiguration(e => { /* config is read on demand */ })
  );
}

function deactivate() {}

module.exports = { activate, deactivate };
