const courseCatalog = [
  { id: "java-swing", title: "Java Swing", total: 500 },
  { id: "python-tkinter", title: "Python Tkinter", total: 500 },
  { id: "csharp-winforms", title: "C# WinForms", total: 500 },
  { id: "cpp-qt", title: "C++ Qt Widgets", total: 500 },
  { id: "javascript-electron", title: "JavaScript Electron", total: 500 }
];

function readStats() {
  try {
    const stats = JSON.parse(localStorage.getItem("desktopcraft-stats-v1") || "{}");
    const legacy = JSON.parse(localStorage.getItem("swingcraft-progress-v1") || "null");
    if (!stats["java-swing"] && Array.isArray(legacy?.completed)) {
      stats["java-swing"] = { completed: legacy.completed.length, total: 500, xp: legacy.completed.length * 100 };
    }
    return stats;
  } catch {
    return {};
  }
}

const escapeHtml = (value) => String(value)
  .replaceAll("&", "&amp;")
  .replaceAll("<", "&lt;")
  .replaceAll(">", "&gt;")
  .replaceAll('"', "&quot;")
  .replaceAll("'", "&#039;");

function completedFor(stats, courseId) {
  return Math.max(0, Math.min(500, Number(stats?.[courseId]?.completed) || 0));
}

function builderFromAccount(account, signedInUser) {
  const completed = courseCatalog.reduce((total, course) => total + completedFor(account.courses, course.id), 0);
  const strongest = [...courseCatalog].sort((left, right) => completedFor(account.courses, right.id) - completedFor(account.courses, left.id))[0];
  return {
    name: account.name,
    username: account.username,
    initials: window.DesktopcraftAuth.initials(account.name),
    focus: completed ? strongest.title : "Start a course",
    completed,
    xp: completed * 100,
    updatedAt: account.updatedAt || 0,
    you: signedInUser?.username === account.username
  };
}

async function renderLeaderboard() {
  const stats = readStats();
  await window.DesktopcraftAuth?.ready?.();
  const signedInUser = window.DesktopcraftAuth?.currentUser();
  if (signedInUser) window.DesktopcraftAuth.recordProgress(stats);
  const sharedAccounts = await window.DesktopcraftAuth.databaseLeaderboard();
  const accounts = sharedAccounts ?? await window.DesktopcraftAuth.leaderboardEntries();
  const rankings = accounts
    .map((account) => builderFromAccount(account, signedInUser))
    .sort((left, right) => right.xp - left.xp || right.updatedAt - left.updatedAt || left.username.localeCompare(right.username));

  const totalCompleted = courseCatalog.reduce((total, course) => total + completedFor(stats, course.id), 0);
  const totalXp = totalCompleted * 100;
  const activeCourses = courseCatalog.filter((course) => completedFor(stats, course.id) > 0).length;
  const playerRankIndex = rankings.findIndex((builder) => builder.you);
  document.querySelector("#totalXp").textContent = totalXp.toLocaleString();
  document.querySelector("#totalCompleted").textContent = totalCompleted;
  document.querySelector("#playerRank").textContent = playerRankIndex >= 0 ? `#${playerRankIndex + 1}` : "—";
  document.querySelector("#activeCourses").textContent = activeCourses;

  document.querySelector("#rankingList").innerHTML = rankings.length
    ? rankings.map((builder, index) => `
        <li class="ranking-row${builder.you ? " you" : ""}">
          <span class="rank-number${index < 3 ? " medal" : ""}">${index + 1}</span>
          <div class="builder-cell">
            <span class="builder-avatar">${escapeHtml(builder.initials)}</span>
            <div><strong>${escapeHtml(builder.name)}${builder.you ? " · current learner" : ""}</strong><span>@${escapeHtml(builder.username)} · ${builder.completed} lessons complete</span></div>
          </div>
          <span class="focus-cell">${escapeHtml(builder.focus)}</span>
          <span class="xp-cell">${builder.xp.toLocaleString()} <span>XP</span></span>
        </li>`).join("")
    : `<li class="leaderboard-empty"><strong>No ranked accounts yet</strong><span>Create a Desktopcraft account and complete a lesson to claim the first spot.</span><a href="login.html?switch=1&mode=create&next=leaderboard.html">Create account</a></li>`;

  document.querySelector("#courseProgressList").innerHTML = courseCatalog
    .map((course) => {
      const completed = completedFor(stats, course.id);
      const percent = Math.round((completed / course.total) * 100);
      return `
        <div class="course-progress-item">
          <div class="course-progress-copy"><strong>${course.title}</strong><span>${completed} / ${course.total}</span></div>
          <div class="course-progress-track" aria-label="${course.title}: ${percent}% complete"><span style="width:${percent}%"></span></div>
        </div>`;
    })
    .join("");
}

renderLeaderboard().catch(() => {
  document.querySelector("#rankingList").innerHTML = `<li class="leaderboard-empty"><strong>Leaderboard unavailable</strong><span>Your progress is safe. Reload this page to try again.</span></li>`;
});
