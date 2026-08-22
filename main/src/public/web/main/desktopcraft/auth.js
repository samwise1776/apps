(function () {
  const accountsKey = "desktopcraft-accounts-v2";
  const sessionKey = "desktopcraft-session-v2";
  const lastAccountKey = "desktopcraft-last-account-v1";
  const signedOutKey = "desktopcraft-explicitly-signed-out-v1";
  const overflowFallbackKey = "desktopcraft-accounts-overflow-v2";
  const overflowDatabaseName = "desktopcraft-account-overflow-v1";
  const leaderboardKey = "desktopcraft-real-leaderboard-v1";
  const primaryAccountLimit = 100;
  const passwordIterations = 210000;
  const databaseTimeoutMs = 2500;
  const authenticationTimeoutMs = 15000;
  let storagePersistencePromise;

  const normalizeUsername = (username) => String(username || "").trim().toLowerCase();

  async function databaseRequest(path, options = {}) {
    const controller = window.AbortController ? new AbortController() : null;
    const { headers = {}, timeoutMs = databaseTimeoutMs, ...requestOptions } = options;
    const timeout = controller ? window.setTimeout(() => controller.abort(), timeoutMs) : null;
    try {
      const response = await window.fetch(path, {
        credentials: "same-origin",
        headers: { "Content-Type": "application/json", ...headers },
        ...requestOptions,
        ...(controller ? { signal: controller.signal } : {})
      });
      const contentType = response.headers.get("content-type") || "";
      if (!contentType.includes("application/json")) return { available: false };
      const data = await response.json();
      if (!response.ok) {
        const error = new Error(data.error || "Desktopcraft could not complete that database request.");
        error.databaseResponse = true;
        error.status = response.status;
        throw error;
      }
      return { available: true, data };
    } catch (error) {
      if (error.databaseResponse) throw error;
      return { available: false };
    } finally {
      if (timeout) window.clearTimeout(timeout);
    }
  }

  function readJson(key, fallback) {
    try {
      return JSON.parse(localStorage.getItem(key) || JSON.stringify(fallback));
    } catch {
      return fallback;
    }
  }

  function writeJson(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
  }

  function requestPersistentStorage() {
    if (!storagePersistencePromise) {
      storagePersistencePromise = (async () => {
        const storage = window.navigator?.storage;
        if (!storage?.persist) return false;
        try {
          if (storage.persisted && await storage.persisted()) return true;
          return Boolean(await storage.persist());
        } catch {
          return false;
        }
      })();
    }
    return storagePersistencePromise;
  }

  function openOverflowDatabase() {
    return new Promise((resolve, reject) => {
      if (!window.indexedDB) {
        reject(new Error("IndexedDB is unavailable"));
        return;
      }
      const request = window.indexedDB.open(overflowDatabaseName, 1);
      request.onupgradeneeded = () => {
        if (!request.result.objectStoreNames.contains("accounts")) request.result.createObjectStore("accounts", { keyPath: "username" });
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error || new Error("Could not open overflow storage"));
    });
  }

  async function readOverflowAccounts() {
    try {
      const database = await openOverflowDatabase();
      const accounts = await new Promise((resolve, reject) => {
        const request = database.transaction("accounts", "readonly").objectStore("accounts").getAll();
        request.onsuccess = () => resolve(request.result || []);
        request.onerror = () => reject(request.error || new Error("Could not read overflow accounts"));
      });
      database.close();
      const fallback = readJson(overflowFallbackKey, []);
      return [...accounts, ...fallback.filter((account) => !accounts.some((stored) => stored.username === account.username))];
    } catch {
      return readJson(overflowFallbackKey, []);
    }
  }

  async function writeOverflowAccount(account) {
    try {
      const database = await openOverflowDatabase();
      await new Promise((resolve, reject) => {
        const request = database.transaction("accounts", "readwrite").objectStore("accounts").put(account);
        request.onsuccess = () => resolve();
        request.onerror = () => reject(request.error || new Error("Could not write overflow account"));
      });
      database.close();
    } catch {
      const overflow = readJson(overflowFallbackKey, []);
      const existingIndex = overflow.findIndex((stored) => stored.username === account.username);
      if (existingIndex >= 0) overflow[existingIndex] = account;
      else overflow.push(account);
      writeJson(overflowFallbackKey, overflow);
    }
  }

  async function readAccountTiers() {
    let primary = readJson(accountsKey, []);
    if (primary.length > primaryAccountLimit) {
      const spillover = primary.slice(primaryAccountLimit);
      primary = primary.slice(0, primaryAccountLimit);
      writeJson(accountsKey, primary);
      for (const account of spillover) await writeOverflowAccount({ ...account, storageTier: "browser-overflow" });
    }
    const overflow = await readOverflowAccounts();
    return {
      primary,
      overflow: overflow.filter((account) => !primary.some((stored) => stored.username === account.username))
    };
  }

  async function storeLocalAccount(account) {
    const { primary, overflow } = await readAccountTiers();
    const primaryIndex = primary.findIndex((stored) => stored.username === account.username);
    if (primaryIndex >= 0) {
      primary[primaryIndex] = account;
      writeJson(accountsKey, primary);
      await writeOverflowAccount(account);
      return;
    }
    if (overflow.some((stored) => stored.username === account.username)) {
      await writeOverflowAccount(account);
      return;
    }
    if (primary.length < primaryAccountLimit) {
      primary.push(account);
      writeJson(accountsKey, primary);
      await writeOverflowAccount(account);
    } else {
      await writeOverflowAccount(account);
    }
  }

  async function confirmStoredAccount(username) {
    const { primary, overflow } = await readAccountTiers();
    const stored = [...primary, ...overflow].find((account) => account.username === username);
    if (!stored?.passwordVerifier) {
      throw new Error("Desktopcraft could not save this account in browser storage. Allow site storage and try again.");
    }
    return stored;
  }

  const bytesToHex = (bytes) => [...bytes].map((value) => value.toString(16).padStart(2, "0")).join("");

  async function legacyPasswordHash(password) {
    if (window.crypto?.subtle && window.TextEncoder) {
      const bytes = new TextEncoder().encode(`desktopcraft-local-v2:${password}`);
      const digest = await window.crypto.subtle.digest("SHA-256", bytes);
      return bytesToHex(new Uint8Array(digest));
    }

    let hash = 2166136261;
    for (const character of `desktopcraft-local-v2:${password}`) {
      hash ^= character.charCodeAt(0);
      hash = Math.imul(hash, 16777619);
    }
    return `fallback-${(hash >>> 0).toString(16)}`;
  }

  async function derivePasswordVerifier(password, salt, iterations = passwordIterations) {
    if (!window.crypto?.subtle || !window.TextEncoder) {
      throw new Error("Secure browser password storage is unavailable. Update your browser and try again.");
    }
    const passwordKey = await window.crypto.subtle.importKey(
      "raw",
      new TextEncoder().encode(String(password)),
      "PBKDF2",
      false,
      ["deriveBits"]
    );
    const digest = await window.crypto.subtle.deriveBits(
      { name: "PBKDF2", hash: "SHA-256", salt: new TextEncoder().encode(salt), iterations },
      passwordKey,
      256
    );
    return bytesToHex(new Uint8Array(digest));
  }

  async function createPasswordVerifier(password) {
    if (!window.crypto?.getRandomValues) {
      throw new Error("Secure browser password storage is unavailable. Update your browser and try again.");
    }
    const salt = bytesToHex(window.crypto.getRandomValues(new Uint8Array(16)));
    const digest = await derivePasswordVerifier(password, salt);
    return `pbkdf2-sha256:${passwordIterations}:${salt}:${digest}`;
  }

  async function verifyLocalPassword(account, password) {
    if (account?.passwordVerifier) {
      const [algorithm, iterationText, salt, expected] = String(account.passwordVerifier).split(":");
      const iterations = Number(iterationText);
      if (algorithm !== "pbkdf2-sha256" || iterations !== passwordIterations || !/^[a-f0-9]{32}$/.test(salt) || !/^[a-f0-9]{64}$/.test(expected)) {
        return false;
      }
      return await derivePasswordVerifier(password, salt, iterations) === expected;
    }
    return Boolean(account?.passwordHash) && account.passwordHash === await legacyPasswordHash(password);
  }

  function currentUser() {
    const session = readJson(sessionKey, null);
    return session?.username && session?.name ? session : null;
  }

  function explicitlySignedOut() {
    try {
      return localStorage.getItem(signedOutKey) === "true";
    } catch {
      return false;
    }
  }

  function rememberLogin(username) {
    try {
      localStorage.setItem(lastAccountKey, normalizeUsername(username));
      localStorage.removeItem(signedOutKey);
    } catch {
      // The active session still works if preference storage is restricted.
    }
  }

  function sessionForAccount(account, automatic = false) {
    if (!account?.username || !account?.name) return null;
    const session = {
      name: account.name,
      username: account.username,
      createdAt: Number(account.createdAt) || Date.now(),
      storageTier: account.storageTier || "browser-primary",
      signedInAt: Date.now(),
      ...(automatic ? { autoSignedIn: true } : {})
    };
    writeJson(sessionKey, session);
    rememberLogin(session.username);
    rememberLeaderboardAccount(session);
    return session;
  }

  function initials(name) {
    const parts = String(name || "Guest").trim().split(/\s+/).filter(Boolean);
    return parts.slice(0, 2).map((part) => part[0].toUpperCase()).join("") || "GU";
  }

  function safeCourseStats(stats) {
    const result = {};
    Object.entries(stats && typeof stats === "object" ? stats : {}).forEach(([courseId, course]) => {
      const total = Math.max(0, Math.min(500, Number(course?.total) || 500));
      const completed = Math.max(0, Math.min(total, Number(course?.completed) || 0));
      result[courseId] = { completed, total, updatedAt: Number(course?.updatedAt) || Date.now() };
    });
    return result;
  }

  function rememberLeaderboardAccount(account, stats) {
    if (!account?.username || !account?.name) return;
    const records = readJson(leaderboardKey, []);
    const index = records.findIndex((record) => record.username === account.username);
    const previous = index >= 0 ? records[index] : {};
    const record = {
      name: account.name,
      username: account.username,
      createdAt: Number(account.createdAt) || Number(previous.createdAt) || Date.now(),
      updatedAt: Date.now(),
      courses: stats ? safeCourseStats(stats) : safeCourseStats(previous.courses)
    };
    if (index >= 0) records[index] = record;
    else records.push(record);
    writeJson(leaderboardKey, records);
  }

  function recordProgress(stats, progressSnapshot) {
    const user = currentUser();
    if (!user) return false;
    try {
      rememberLeaderboardAccount(user, stats);
      if (progressSnapshot?.courseId && Array.isArray(progressSnapshot.completed)) {
        void databaseRequest("/api/progress", {
          method: "PUT",
          body: JSON.stringify(progressSnapshot)
        }).catch(() => {});
      }
      return true;
    } catch {
      return false;
    }
  }

  function saveQuizAttempt(attempt) {
    if (!currentUser()) return false;
    void databaseRequest("/api/quiz-attempts", {
      method: "POST",
      body: JSON.stringify(attempt)
    }).catch(() => {});
    return true;
  }

  async function loadDatabaseProgress() {
    try {
      const response = await databaseRequest("/api/progress");
      return response.available ? response.data.courses || [] : null;
    } catch {
      return null;
    }
  }

  async function databaseLeaderboard() {
    try {
      const response = await databaseRequest("/api/leaderboard");
      return response.available ? response.data.entries || [] : null;
    } catch {
      return null;
    }
  }

  async function restoreDatabaseSession() {
    if (explicitlySignedOut()) return null;
    try {
      const response = await databaseRequest("/api/auth/session");
      const user = response.available ? response.data.user : null;
      if (!user) return null;
      const session = { ...user, signedInAt: Date.now() };
      writeJson(sessionKey, session);
      rememberLogin(session.username);
      rememberLeaderboardAccount(session);
      return session;
    } catch {
      return null;
    }
  }

  async function autoSignIn() {
    const existing = currentUser();
    if (existing) return existing;
    if (explicitlySignedOut()) return null;

    const databaseSession = await restoreDatabaseSession();
    if (databaseSession) return databaseSession;

    const primary = readJson(accountsKey, []);
    let lastUsername = "";
    try { lastUsername = normalizeUsername(localStorage.getItem(lastAccountKey)); } catch { /* Use a single saved account when possible. */ }
    let account = primary.find((candidate) => candidate.username === lastUsername);
    if (!account && primary.length === 1) account = primary[0];
    if (account) return sessionForAccount(account, true);

    const overflow = await readOverflowAccounts();
    account = overflow.find((candidate) => candidate.username === lastUsername);
    if (!account && !primary.length && overflow.length === 1) account = overflow[0];
    return account ? sessionForAccount(account, true) : null;
  }

  let readyPromise;
  function ready() {
    if (!readyPromise) readyPromise = autoSignIn();
    return readyPromise;
  }

  function renderSharedAccountStatus(user = currentUser()) {
    if (typeof document === "undefined" || !document.body || !window.location) return;
    if (/(?:^|\/)login\.html$/.test(window.location.pathname) || /(?:^|\/)index\.html$/.test(window.location.pathname)) return;
    let status = document.querySelector("#desktopcraftSharedAccount");
    if (!status) {
      status = document.createElement("aside");
      status.id = "desktopcraftSharedAccount";
      status.className = "shared-account-status";
      status.setAttribute("aria-live", "polite");
      document.body.append(status);
    }
    status.replaceChildren();
    status.classList.toggle("signed-in", Boolean(user));
    if (user) {
      const avatar = document.createElement("button");
      avatar.type = "button";
      avatar.className = "shared-account-avatar";
      avatar.textContent = initials(user.name);
      avatar.setAttribute("aria-label", "Open account menu");
      avatar.setAttribute("aria-expanded", "false");
      const menu = document.createElement("div");
      menu.className = "shared-account-menu";
      menu.hidden = true;
      const signOutButton = document.createElement("button");
      signOutButton.type = "button";
      signOutButton.textContent = "Sign Out";
      signOutButton.addEventListener("click", () => { signOut(); renderSharedAccountStatus(null); });
      avatar.addEventListener("click", () => {
        menu.hidden = !menu.hidden;
        avatar.setAttribute("aria-expanded", String(!menu.hidden));
      });
      menu.append(signOutButton);
      status.append(avatar, menu);
    } else {
      const link = document.createElement("a");
      link.href = "login.html?switch=1";
      link.textContent = "Sign in to Desktopcraft";
      status.append(link);
    }
  }

  async function leaderboardEntries() {
    const { primary, overflow } = await readAccountTiers();
    const current = currentUser();
    const accounts = [...primary, ...overflow];
    if (current && !accounts.some((account) => account.username === current.username)) accounts.push(current);
    const records = readJson(leaderboardKey, []);
    return accounts
      .filter((account, index, all) => account.username && all.findIndex((candidate) => candidate.username === account.username) === index)
      .map((account) => {
        const record = records.find((candidate) => candidate.username === account.username);
        return {
          name: account.name,
          username: account.username,
          createdAt: Number(account.createdAt) || Date.now(),
          updatedAt: Number(record?.updatedAt) || 0,
          courses: safeCourseStats(record?.courses)
        };
      });
  }

  async function signUp({ name, username, password }) {
    const cleanName = String(name || "").trim();
    const cleanUsername = normalizeUsername(username);
    if (cleanName.length < 2) throw new Error("Enter a name with at least two characters.");
    if (!/^[a-z0-9_.-]{3,24}$/.test(cleanUsername)) {
      throw new Error("Use 3–24 letters, numbers, dots, dashes, or underscores for your username.");
    }
    if (String(password).length < 8) throw new Error("Use at least eight characters for your Desktopcraft password.");

    await requestPersistentStorage();
    const { primary, overflow } = await readAccountTiers();
    if ([...primary, ...overflow].some((account) => account.username === cleanUsername)) throw new Error("That Desktopcraft username is already taken.");
    const database = await databaseRequest("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ name: cleanName, username: cleanUsername, password }),
      timeoutMs: authenticationTimeoutMs
    });
    const databaseUser = database.available ? database.data.user : null;
    const storageTier = databaseUser?.storageTier || (primary.length < primaryAccountLimit ? "browser-primary" : "browser-overflow");
    const account = {
      name: databaseUser?.name || cleanName,
      username: databaseUser?.username || cleanUsername,
      passwordVerifier: await createPasswordVerifier(password),
      createdAt: Number(databaseUser?.createdAt) || Date.now(),
      storageTier
    };
    await storeLocalAccount(account);
    await confirmStoredAccount(account.username);
    const session = { name: account.name, username: account.username, createdAt: account.createdAt, storageTier, signedInAt: Date.now() };
    writeJson(sessionKey, session);
    rememberLogin(session.username);
    rememberLeaderboardAccount(session, {});
    return session;
  }

  async function signIn({ username, password }) {
    await requestPersistentStorage();
    const cleanUsername = normalizeUsername(username);
    const { primary, overflow } = await readAccountTiers();
    const account = [...primary, ...overflow].find((candidate) => candidate.username === cleanUsername);
    const localPasswordMatches = account ? await verifyLocalPassword(account, password) : false;
    let database;
    try {
      database = await databaseRequest("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ username: cleanUsername, password }),
        timeoutMs: authenticationTimeoutMs
      });
    } catch (error) {
      if (error.status !== 401 || !localPasswordMatches) throw error;
      try {
        database = await databaseRequest("/api/auth/register", {
          method: "POST",
          body: JSON.stringify({ name: account.name, username: cleanUsername, password }),
          timeoutMs: authenticationTimeoutMs
        });
      } catch (migrationError) {
        if (migrationError.status === 409) {
          throw new Error("That username already belongs to a database account. Use its Desktopcraft password.");
        }
        throw migrationError;
      }
    }

    if (database?.available) {
      const user = database.data.user;
      const databaseAccount = {
        name: user.name,
        username: user.username,
        passwordVerifier: await createPasswordVerifier(password),
        createdAt: Number(user.createdAt) || Date.now(),
        storageTier: "database"
      };
      await storeLocalAccount(databaseAccount);
      const session = { name: databaseAccount.name, username: databaseAccount.username, createdAt: databaseAccount.createdAt, storageTier: "database", signedInAt: Date.now() };
      writeJson(sessionKey, session);
      rememberLogin(session.username);
      rememberLeaderboardAccount(session);
      return session;
    }

    if (!account || !localPasswordMatches) {
      throw new Error("Desktopcraft username or password is incorrect.");
    }
    if (!account.passwordVerifier) {
      const upgradedAccount = { ...account, passwordVerifier: await createPasswordVerifier(password) };
      delete upgradedAccount.passwordHash;
      await storeLocalAccount(upgradedAccount);
    }
    const session = { name: account.name, username: account.username, createdAt: account.createdAt, storageTier: account.storageTier || "browser-primary", signedInAt: Date.now() };
    writeJson(sessionKey, session);
    rememberLogin(session.username);
    rememberLeaderboardAccount(session);
    return session;
  }

  function signOut() {
    try {
      localStorage.setItem(signedOutKey, "true");
      localStorage.removeItem(sessionKey);
      localStorage.removeItem("desktopcraft-session-v1");
    } catch {
      // Continue with server logout if browser storage is restricted.
    }
    void databaseRequest("/api/auth/logout", { method: "POST", body: "{}", keepalive: true }).catch(() => {});
  }

  window.DesktopcraftAuth = {
    currentUser,
    initials,
    signUp,
    signIn,
    signOut,
    recordProgress,
    leaderboardEntries,
    saveQuizAttempt,
    loadDatabaseProgress,
    databaseLeaderboard,
    restoreDatabaseSession,
    autoSignIn,
    ready
  };
  void ready().then(renderSharedAccountStatus);
  window.addEventListener?.("storage", (event) => {
    if (event.key === sessionKey || event.key === signedOutKey) renderSharedAccountStatus();
  });
})();
