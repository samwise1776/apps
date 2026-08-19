const viewElements = {
  grid: document.getElementById("communityAppGrid"),
  search: document.getElementById("communitySearch"),
  toolkit: document.getElementById("communityToolkitFilter"),
  sort: document.getElementById("communitySort"),
  resultCopy: document.getElementById("libraryResultCopy"),
  appCount: document.getElementById("communityAppCount"),
  creatorCount: document.getElementById("communityCreatorCount"),
  downloadCount: document.getElementById("communityDownloadCount"),
};

let communityApps = [];
const highlightedAppId = Number(new URLSearchParams(window.location.search).get("published")) || null;

function escapeCommunityText(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function initials(name) {
  return window.DesktopcraftAuth?.initials?.(name) || String(name || "Builder").split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase();
}

function relativeTime(timestamp) {
  const seconds = Math.max(1, Math.floor((Date.now() - Number(timestamp)) / 1000));
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(timestamp).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

function formatBytes(bytes) {
  const count = Number(bytes) || 0;
  return count < 1024 ? `${count} B` : `${(count / 1024).toFixed(count < 10240 ? 1 : 0)} KB`;
}

function visibleApps() {
  const query = viewElements.search.value.trim().toLowerCase();
  const toolkit = viewElements.toolkit.value;
  const apps = communityApps.filter((app) => {
    const matchesToolkit = toolkit === "all" || app.toolkit === toolkit;
    const searchable = `${app.title} ${app.description} ${app.creator.name} ${app.creator.username} ${app.toolkitLabel}`.toLowerCase();
    return matchesToolkit && (!query || searchable.includes(query));
  });
  if (viewElements.sort.value === "downloads") return apps.sort((a, b) => b.downloads - a.downloads || b.createdAt - a.createdAt);
  if (viewElements.sort.value === "title") return apps.sort((a, b) => a.title.localeCompare(b.title));
  return apps.sort((a, b) => b.createdAt - a.createdAt);
}

function renderStats() {
  viewElements.appCount.textContent = communityApps.length.toLocaleString();
  viewElements.creatorCount.textContent = new Set(communityApps.map((app) => app.creator.username)).size.toLocaleString();
  viewElements.downloadCount.textContent = communityApps.reduce((total, app) => total + Number(app.downloads || 0), 0).toLocaleString();
}

function renderApps() {
  const apps = visibleApps();
  viewElements.resultCopy.textContent = `${apps.length} free app${apps.length === 1 ? "" : "s"} shown · every download costs $0`;
  if (!apps.length) {
    const hasAnyApps = communityApps.length > 0;
    viewElements.grid.innerHTML = `<div class="community-empty">
      <strong>${hasAnyApps ? "No apps match that search" : "Be the first app creator"}</strong>
      <span>${hasAnyApps ? "Try another phrase or toolkit." : "Publish source code and give the community its first free download."}</span>
      <a href="make.html">${hasAnyApps ? "Publish a different app" : "Make and publish an app"} →</a>
    </div>`;
    return;
  }

  viewElements.grid.innerHTML = apps.map((app) => `<article class="community-app-card${app.id === highlightedAppId ? " highlighted" : ""}" data-app-id="${app.id}">
    <div class="app-card-top"><span class="toolkit-chip">${escapeCommunityText(app.toolkitLabel)}</span><span class="free-chip">${app.package ? "INSTALLABLE .DEB" : "SOURCE"} · FREE</span></div>
    <h3>${escapeCommunityText(app.title)}</h3>
    <p class="community-app-description">${escapeCommunityText(app.description)}</p>
    <div class="app-card-meta"><span>${escapeCommunityText(app.fileName)}</span><span>·</span><span>${formatBytes(app.sourceBytes)}</span><span>·</span><span>${relativeTime(app.createdAt)}</span></div>
    <div class="app-card-author"><span class="community-avatar">${escapeCommunityText(initials(app.creator.name))}</span><div><strong>${escapeCommunityText(app.creator.name)}</strong><small>@${escapeCommunityText(app.creator.username)}</small></div></div>
    <div class="app-card-footer">
      <a class="app-download-button" data-download-app="${app.id}" href="${escapeCommunityText(app.downloadUrl)}" download="${escapeCommunityText(app.fileName)}">Download ${app.package ? ".deb" : "source"} free · <span data-download-count>${Number(app.downloads).toLocaleString()}</span></a>
      ${app.isOwner ? `<button class="delete-app-button" data-delete-app="${app.id}" type="button">Remove</button>` : ""}
    </div>
  </article>`).join("");

  if (highlightedAppId) {
    window.requestAnimationFrame(() => document.querySelector(`[data-app-id="${highlightedAppId}"]`)?.scrollIntoView({ behavior: "smooth", block: "center" }));
  }
}

async function deleteApp(appId) {
  const app = communityApps.find((candidate) => candidate.id === appId);
  if (!app || !window.confirm(`Remove “${app.title}” from the free app library?`)) return;
  try {
    const response = await fetch(`/api/apps/${appId}`, { method: "DELETE", credentials: "same-origin" });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || "The app could not be removed.");
    communityApps = communityApps.filter((candidate) => candidate.id !== appId);
    renderStats();
    renderApps();
  } catch (error) {
    window.alert(error.message || "The app could not be removed.");
  }
}

async function loadCommunityApps() {
  viewElements.grid.innerHTML = '<div class="community-empty"><strong>Loading free apps…</strong><span>Connecting to the community library.</span></div>';
  try {
    const response = await fetch("/api/apps", { credentials: "same-origin" });
    const contentType = response.headers.get("content-type") || "";
    if (!contentType.includes("application/json")) throw new Error("The community server is unavailable.");
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || "The community library could not be loaded.");
    communityApps = Array.isArray(payload.apps) ? payload.apps.filter((app) => app.isFree && Number(app.price) === 0) : [];
    renderStats();
    renderApps();
  } catch (error) {
    viewElements.resultCopy.textContent = "Community library unavailable";
    viewElements.grid.innerHTML = `<div class="community-empty"><strong>Start the community server</strong><span>${escapeCommunityText(error.message || "Run this site with npm start to share apps between people.")}</span><a href="make.html">Open the publishing guide →</a></div>`;
  }
}

viewElements.search.addEventListener("input", renderApps);
viewElements.toolkit.addEventListener("change", renderApps);
viewElements.sort.addEventListener("change", renderApps);
viewElements.grid.addEventListener("click", (event) => {
  const deleteButton = event.target.closest("[data-delete-app]");
  if (deleteButton) {
    void deleteApp(Number(deleteButton.dataset.deleteApp));
    return;
  }
  const download = event.target.closest("[data-download-app]");
  if (!download) return;
  const app = communityApps.find((candidate) => candidate.id === Number(download.dataset.downloadApp));
  if (!app) return;
  app.downloads += 1;
  download.querySelector("[data-download-count]").textContent = app.downloads.toLocaleString();
  renderStats();
});

const communityAuthReady = window.DesktopcraftAuth?.ready?.();
if (communityAuthReady?.then) void communityAuthReady.then(loadCommunityApps);
else void loadCommunityApps();
