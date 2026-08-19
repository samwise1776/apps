import assert from "node:assert/strict";
import { webcrypto } from "node:crypto";

const values = new Map();
globalThis.localStorage = {
  getItem: (key) => values.has(key) ? values.get(key) : null,
  setItem: (key, value) => values.set(key, String(value)),
  removeItem: (key) => values.delete(key)
};
Object.defineProperty(globalThis, "crypto", { value: webcrypto });
globalThis.window = {
  crypto: webcrypto,
  TextEncoder,
  setTimeout,
  clearTimeout
};

await import(`../auth.js?test=${Date.now()}`);
const auth = window.DesktopcraftAuth;
await auth.ready();
const created = await auth.signUp({ name: "Local Builder", username: "local_builder", password: "desktop-pass" });
assert.equal(created.username, "local_builder");
assert.equal(auth.currentUser().name, "Local Builder");
assert.equal(JSON.parse(values.get("desktopcraft-session-v2")).username, "local_builder");
const storedAccounts = JSON.parse(values.get("desktopcraft-accounts-v2"));
assert.match(storedAccounts[0].passwordVerifier, /^pbkdf2-sha256:210000:[a-f0-9]{32}:[a-f0-9]{64}$/);
assert.equal("passwordHash" in storedAccounts[0], false);
const backupAccounts = JSON.parse(values.get("desktopcraft-accounts-overflow-v2"));
assert.equal(backupAccounts[0].username, "local_builder");

auth.signOut();
assert.equal(auth.currentUser(), null);
const signedIn = await auth.signIn({ username: "LOCAL_BUILDER", password: "desktop-pass" });
assert.equal(signedIn.username, "local_builder");
assert.equal(JSON.parse(values.get("desktopcraft-session-v2")).username, "local_builder");
assert.equal(typeof auth.downloadAccountFile, "undefined");
assert.equal(typeof auth.readAccountFile, "undefined");
assert.equal([...values.values()].some((value) => value.includes("desktop-pass")), false);

values.delete("desktopcraft-session-v2");
values.delete("desktopcraft-explicitly-signed-out-v1");
const restored = await auth.autoSignIn();
assert.equal(restored.username, "local_builder");
assert.equal(restored.autoSignedIn, true);

values.delete("desktopcraft-session-v2");
values.delete("desktopcraft-accounts-v2");
const restoredFromBackup = await auth.autoSignIn();
assert.equal(restoredFromBackup.username, "local_builder");
assert.equal(restoredFromBackup.autoSignedIn, true);

auth.signOut();
assert.equal(await auth.autoSignIn(), null);
assert.equal(auth.currentUser(), null);
await auth.signIn({ username: "local_builder", password: "desktop-pass" });

assert.equal(auth.recordProgress({ "java-swing": { completed: 2, total: 500 } }), true);
const leaderboard = await auth.leaderboardEntries();
assert.equal(leaderboard.length, 1);
assert.equal(leaderboard[0].courses["java-swing"].completed, 2);

console.log("Verified browser-storage account fallback and local leaderboard.");
