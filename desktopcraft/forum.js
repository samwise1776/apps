const forumStorageKey = "desktopcraft-forum-topics-v1";
const forumCategories = [
  { id: "all", label: "All topics", short: "ALL" },
  { id: "java-swing", label: "Java Swing", short: "SW" },
  { id: "python-tkinter", label: "Python Tkinter", short: "PY" },
  { id: "csharp-winforms", label: "C# WinForms", short: "C#" },
  { id: "cpp-qt", label: "C++ Qt", short: "QT" },
  { id: "javascript-electron", label: "Electron", short: "JS" },
  { id: "app-maker", label: "App Maker", short: "AM" },
  { id: "general", label: "General", short: "GEN" }
];

const legacySampleTopicIds = new Set([
  "welcome-swing-table",
  "tkinter-layout-choice",
  "qt-signal-lambda",
  "app-maker-template",
  "winforms-dpi",
  "electron-preload"
]);

const forumElements = {
  profile: document.querySelector("#forumProfile"),
  avatar: document.querySelector("#forumAvatar"),
  profileName: document.querySelector("#forumProfileName"),
  profileHandle: document.querySelector("#forumProfileHandle"),
  newTopicButton: document.querySelector("#newTopicButton"),
  topicCount: document.querySelector("#forumTopicCount"),
  replyCount: document.querySelector("#forumReplyCount"),
  memberCount: document.querySelector("#forumMemberCount"),
  categories: document.querySelector("#forumCategories"),
  search: document.querySelector("#forumSearch"),
  sort: document.querySelector("#forumSort"),
  listTitle: document.querySelector("#forumListTitle"),
  resultCount: document.querySelector("#forumResultCount"),
  topicList: document.querySelector("#forumTopicList"),
  discussion: document.querySelector("#forumDiscussion"),
  dialog: document.querySelector("#newTopicDialog"),
  form: document.querySelector("#newTopicForm"),
  title: document.querySelector("#topicTitle"),
  category: document.querySelector("#topicCategory"),
  body: document.querySelector("#topicBody"),
  code: document.querySelector("#topicCode"),
  formError: document.querySelector("#topicFormError"),
  toast: document.querySelector("#forumToast")
};

let signedInForumUser = window.DesktopcraftAuth?.currentUser();
let forumTopics = loadForumTopics();
let selectedCategory = "all";
let selectedTopicId = forumTopics[0]?.id || null;
let forumToastTimeout;

function loadForumTopics() {
  try {
    const stored = JSON.parse(localStorage.getItem(forumStorageKey) || "null");
    if (Array.isArray(stored)) {
      const realTopics = stored.filter((topic) => !legacySampleTopicIds.has(topic?.id));
      if (realTopics.length !== stored.length) localStorage.setItem(forumStorageKey, JSON.stringify(realTopics));
      return realTopics;
    }
    localStorage.setItem(forumStorageKey, "[]");
  } catch {
    // The forum can remain empty if browser storage is unavailable.
  }
  return [];
}

function saveForumTopics() {
  try {
    localStorage.setItem(forumStorageKey, JSON.stringify(forumTopics));
  } catch {
    showForumToast("Changes are available for this session; browser storage is unavailable");
  }
}

function forumEscape(value) {
  return String(value || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function forumInitials(name) {
  return window.DesktopcraftAuth?.initials(name) || String(name || "Guest").split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase();
}

function relativeForumTime(timestamp) {
  const seconds = Math.max(1, Math.floor((Date.now() - Number(timestamp)) / 1000));
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

function categoryFor(id) {
  return forumCategories.find((category) => category.id === id) || forumCategories.at(-1);
}

function forumContainsProfanity(...values) {
  const pattern = /(?:^|[^a-z0-9])(?:f[\W_]*u[\W_]*c[\W_]*k(?:er|ing|ed|s)?|s[\W_]*h[\W_]*i[\W_]*t(?:ty|s)?|bitch(?:es|ing)?|ass(?:hole|holes|es)?|bastard(?:s)?|cunt(?:s)?|whore(?:s)?|slut(?:s)?)(?=$|[^a-z0-9])/i;
  return values.some((value) => pattern.test(String(value).toLowerCase()));
}

function renderForumProfile() {
  forumElements.avatar.textContent = forumInitials(signedInForumUser?.name || "Guest learner");
  forumElements.profileName.textContent = signedInForumUser?.name || "Guest learner";
  forumElements.profileHandle.textContent = signedInForumUser?.username ? `@${signedInForumUser.username}` : "Sign in to post";
  if (!signedInForumUser) {
    forumElements.profile.setAttribute("role", "link");
    forumElements.profile.tabIndex = 0;
    forumElements.profile.title = "Sign in to join the forum";
  } else {
    forumElements.profile.removeAttribute("role");
    forumElements.profile.removeAttribute("tabindex");
    forumElements.profile.removeAttribute("title");
  }
}

function renderForumStats() {
  const replies = forumTopics.reduce((total, topic) => total + topic.replies.length, 0);
  const members = new Set(forumTopics.flatMap((topic) => [topic.author.username, ...topic.replies.map((reply) => reply.author.username)])).size;
  forumElements.topicCount.textContent = forumTopics.length;
  forumElements.replyCount.textContent = replies;
  forumElements.memberCount.textContent = members;
}

function renderForumCategories() {
  forumElements.categories.innerHTML = forumCategories.map((category) => {
    const count = category.id === "all" ? forumTopics.length : forumTopics.filter((topic) => topic.category === category.id).length;
    return `<button class="forum-category-button${selectedCategory === category.id ? " active" : ""}" data-category="${category.id}">
      <span class="forum-category-icon">${forumEscape(category.short)}</span>
      <span>${forumEscape(category.label)}</span>
      <small>${count}</small>
    </button>`;
  }).join("");
}

function visibleForumTopics() {
  const query = forumElements.search.value.trim().toLowerCase();
  const filtered = forumTopics.filter((topic) => {
    const inCategory = selectedCategory === "all" || topic.category === selectedCategory;
    const searchable = `${topic.title} ${topic.body} ${topic.code} ${topic.author.name} ${topic.author.username}`.toLowerCase();
    return inCategory && (!query || searchable.includes(query));
  });
  if (forumElements.sort.value === "popular") return filtered.sort((a, b) => b.likedBy.length - a.likedBy.length || b.createdAt - a.createdAt);
  if (forumElements.sort.value === "unanswered") return filtered.sort((a, b) => a.replies.length - b.replies.length || b.createdAt - a.createdAt);
  return filtered.sort((a, b) => b.createdAt - a.createdAt);
}

function renderForumTopics() {
  const topics = visibleForumTopics();
  if (!topics.some((topic) => topic.id === selectedTopicId)) selectedTopicId = topics[0]?.id || null;
  const category = categoryFor(selectedCategory);
  forumElements.listTitle.textContent = selectedCategory === "all" ? "All discussions" : category.label;
  forumElements.resultCount.textContent = `${topics.length} topic${topics.length === 1 ? "" : "s"}`;
  forumElements.topicList.innerHTML = topics.length ? topics.map((topic) => {
    const topicCategory = categoryFor(topic.category);
    return `<button class="topic-card${topic.id === selectedTopicId ? " active" : ""}" data-topic="${forumEscape(topic.id)}">
      <span class="topic-card-top"><span class="topic-category">${forumEscape(topicCategory.label)}</span><time>${relativeForumTime(topic.createdAt)}</time></span>
      <h3>${forumEscape(topic.title)}</h3>
      <p>${forumEscape(topic.body)}</p>
      <span class="topic-card-meta"><span>@${forumEscape(topic.author.username)}</span><span class="topic-card-stats"><span>♥ ${topic.likedBy.length}</span><span>↩ ${topic.replies.length}</span></span></span>
    </button>`;
  }).join("") : '<div class="forum-empty">No discussions match this view.<br>Try another category or start a new topic.</div>';
  renderForumDiscussion();
}

function renderForumDiscussion() {
  const topic = forumTopics.find((candidate) => candidate.id === selectedTopicId);
  if (!topic) {
    forumElements.discussion.innerHTML = forumTopics.length
      ? '<div class="forum-empty">Choose a discussion to read it here.</div>'
      : '<div class="forum-empty">No discussions yet. Sign in and start the first real conversation.</div>';
    return;
  }
  const category = categoryFor(topic.category);
  const liked = Boolean(signedInForumUser && topic.likedBy.includes(signedInForumUser.username));
  forumElements.discussion.innerHTML = `
    <header class="discussion-header">
      <div class="discussion-eyebrow"><span class="topic-category">${forumEscape(category.label)}</span><time>${relativeForumTime(topic.createdAt)}</time></div>
      <h2>${forumEscape(topic.title)}</h2>
      <div class="discussion-author"><span class="discussion-avatar">${forumEscape(forumInitials(topic.author.name))}</span><div><strong>${forumEscape(topic.author.name)}</strong><small>@${forumEscape(topic.author.username)} · topic author</small></div></div>
    </header>
    <div class="discussion-body">${forumEscape(topic.body)}</div>
    ${topic.code ? `<pre class="discussion-code"><code>${forumEscape(topic.code)}</code></pre>` : ""}
    <div class="discussion-actions"><button class="like-button${liked ? " liked" : ""}" type="button" data-like-topic="${forumEscape(topic.id)}">${liked ? "♥ Liked" : "♡ Like"} · ${topic.likedBy.length}</button><span class="topic-category">${topic.replies.length} repl${topic.replies.length === 1 ? "y" : "ies"}</span></div>
    <section class="reply-section">
      <h3>Builder replies</h3>
      <div class="reply-list">${topic.replies.length ? topic.replies.map((reply) => `<article class="reply-card"><div class="reply-author"><span class="discussion-avatar">${forumEscape(forumInitials(reply.author.name))}</span><div><strong>${forumEscape(reply.author.name)}</strong><small>@${forumEscape(reply.author.username)} · ${relativeForumTime(reply.createdAt)}</small></div></div><p>${forumEscape(reply.body)}</p></article>`).join("") : '<div class="forum-empty">No replies yet. Be the first builder to help.</div>'}</div>
      <form class="reply-form" id="replyForm">
        <textarea id="replyBody" minlength="3" maxlength="2000" required placeholder="${signedInForumUser ? "Write a useful reply…" : "Sign in to reply…"}" ${signedInForumUser ? "" : "disabled"}></textarea>
        <div><small>${signedInForumUser ? `Replying as @${forumEscape(signedInForumUser.username)}` : "A Desktopcraft account is required to post."}</small><button class="button button-primary" type="submit">${signedInForumUser ? "Post reply" : "Sign in to reply"}</button></div>
      </form>
    </section>`;
  document.querySelector("#replyForm").addEventListener("submit", submitForumReply);
}

function requireForumLogin() {
  if (signedInForumUser) return true;
  window.location.href = "login.html?switch=1&next=forum.html";
  return false;
}

function openTopicDialog() {
  if (!requireForumLogin()) return;
  forumElements.form.reset();
  forumElements.formError.hidden = true;
  forumElements.category.value = selectedCategory === "all" ? "general" : selectedCategory;
  forumElements.dialog.showModal();
  forumElements.title.focus();
}

function submitNewTopic(event) {
  event.preventDefault();
  if (!requireForumLogin()) return;
  const title = forumElements.title.value.trim();
  const body = forumElements.body.value.trim();
  const code = forumElements.code.value.trim();
  if (title.length < 8 || body.length < 20) {
    forumElements.formError.textContent = "Use at least 8 characters in the title and 20 characters in the discussion.";
    forumElements.formError.hidden = false;
    return;
  }
  if (forumContainsProfanity(title, body, code)) {
    forumElements.formError.textContent = "Keep forum discussions profanity-free. Revise the content before posting.";
    forumElements.formError.hidden = false;
    return;
  }
  const topic = {
    id: `topic-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    category: forumElements.category.value,
    title,
    body,
    code,
    author: { name: signedInForumUser.name, username: signedInForumUser.username },
    createdAt: Date.now(),
    likedBy: [],
    replies: []
  };
  forumTopics.push(topic);
  selectedCategory = "all";
  selectedTopicId = topic.id;
  saveForumTopics();
  forumElements.dialog.close();
  renderForum();
  showForumToast("Discussion published");
}

function submitForumReply(event) {
  event.preventDefault();
  if (!requireForumLogin()) return;
  const field = document.querySelector("#replyBody");
  const body = field.value.trim();
  if (body.length < 3) {
    showForumToast("Write a little more before posting");
    return;
  }
  if (forumContainsProfanity(body)) {
    showForumToast("Keep replies profanity-free");
    return;
  }
  const topic = forumTopics.find((candidate) => candidate.id === selectedTopicId);
  topic.replies.push({ id: `reply-${Date.now()}`, body, author: { name: signedInForumUser.name, username: signedInForumUser.username }, createdAt: Date.now() });
  saveForumTopics();
  renderForum();
  showForumToast("Reply posted");
}

function toggleTopicLike(topicId) {
  if (!requireForumLogin()) return;
  const topic = forumTopics.find((candidate) => candidate.id === topicId);
  if (!topic) return;
  const index = topic.likedBy.indexOf(signedInForumUser.username);
  if (index >= 0) topic.likedBy.splice(index, 1);
  else topic.likedBy.push(signedInForumUser.username);
  saveForumTopics();
  renderForum();
}

function showForumToast(message) {
  window.clearTimeout(forumToastTimeout);
  forumElements.toast.textContent = message;
  forumElements.toast.classList.add("show");
  forumToastTimeout = window.setTimeout(() => forumElements.toast.classList.remove("show"), 2200);
}

function renderForum() {
  renderForumProfile();
  renderForumStats();
  renderForumCategories();
  renderForumTopics();
}

forumElements.category.innerHTML = forumCategories.filter((category) => category.id !== "all").map((category) => `<option value="${category.id}">${forumEscape(category.label)}</option>`).join("");
forumElements.newTopicButton.addEventListener("click", openTopicDialog);
forumElements.profile.addEventListener("click", () => { if (!signedInForumUser) requireForumLogin(); });
forumElements.categories.addEventListener("click", (event) => {
  const button = event.target.closest("[data-category]");
  if (!button) return;
  selectedCategory = button.dataset.category;
  renderForumCategories();
  renderForumTopics();
});
forumElements.topicList.addEventListener("click", (event) => {
  const button = event.target.closest("[data-topic]");
  if (!button) return;
  selectedTopicId = button.dataset.topic;
  renderForumTopics();
});
forumElements.discussion.addEventListener("click", (event) => {
  const button = event.target.closest("[data-like-topic]");
  if (button) toggleTopicLike(button.dataset.likeTopic);
});
forumElements.search.addEventListener("input", renderForumTopics);
forumElements.sort.addEventListener("change", renderForumTopics);
forumElements.form.addEventListener("submit", submitNewTopic);
document.querySelector("#closeTopicDialog").addEventListener("click", () => forumElements.dialog.close());
document.querySelector("#cancelTopicDialog").addEventListener("click", () => forumElements.dialog.close());
forumElements.dialog.addEventListener("click", (event) => { if (event.target === forumElements.dialog) forumElements.dialog.close(); });

renderForum();
void window.DesktopcraftAuth?.ready?.().then(() => {
  signedInForumUser = window.DesktopcraftAuth.currentUser();
  renderForum();
});
