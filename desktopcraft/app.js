const swingLessons = [
  {
    navTitle: "Windows & frames",
    navSubtitle: "Meet JFrame",
    time: "6 MIN",
    title: "Give your app a window",
    description:
      "Every Swing app starts with a frame. Build one, place a label inside it, and learn the tiny ritual that makes a desktop interface appear.",
    tags: ["JFrame", "JLabel", "setVisible()"],
    conceptTitle: "A JFrame is your app's outer shell",
    conceptBody: [
      "Think of <code>JFrame</code> as the actual operating-system window: it owns the title bar, borders, size, and close behavior.",
      "Components such as <code>JLabel</code> live inside that shell. After adding them, call <code>pack()</code> to size the window around its content, then <code>setVisible(true)</code> to show it."
    ],
    mentorNote:
      "Swing should be created on the Event Dispatch Thread. <code>SwingUtilities.invokeLater</code> schedules your UI safely on that thread.",
    points: [
      ["Frame", "The top-level window that holds your interface."],
      ["Components", "Labels, buttons, fields, and other visible building blocks."],
      ["Lifecycle", "Configure first, pack second, reveal last."]
    ],
    challengeTitle: "Make it yours",
    challengeText:
      "Change the window title to <code>My Swing App</code>, then press Run code.",
    challengeTest: (code) => /new\s+JFrame\s*\(\s*["']My Swing App["']\s*\)/.test(code),
    code: `import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hello, Swing!");
            JLabel label = new JLabel("My first desktop app", SwingConstants.CENTER);

            frame.add(label);
            frame.setSize(360, 180);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}`
  },
  {
    navTitle: "Layout managers",
    navSubtitle: "Arrange components",
    time: "7 MIN",
    title: "Let layouts do the measuring",
    description:
      "Pixel-perfect coordinates break as windows resize. Let Swing's layout managers arrange components so your interface stays flexible.",
    tags: ["GridLayout", "FlowLayout", "BorderLayout"],
    conceptTitle: "Layouts are rules, not coordinates",
    conceptBody: [
      "A layout manager decides where components go and how much room they receive. <code>FlowLayout</code> makes a row, <code>GridLayout</code> makes equal cells, and <code>BorderLayout</code> divides space into five regions.",
      "The container does the geometry every time its size changes. Your job is to describe the relationship between components."
    ],
    mentorNote:
      "Nested panels are normal in Swing. Give each <code>JPanel</code> one simple layout, then combine panels to build a more capable screen.",
    points: [
      ["Flow", "Places components left-to-right and wraps when needed."],
      ["Grid", "Gives every component an equal-sized cell."],
      ["Border", "Uses NORTH, SOUTH, EAST, WEST, and CENTER regions."]
    ],
    challengeTitle: "Add another column",
    challengeText:
      "Change <code>new GridLayout(2, 2, 8, 8)</code> to a grid with <strong>3 columns</strong>, then run it.",
    challengeTest: (code) => /new\s+GridLayout\s*\(\s*2\s*,\s*3\s*,/.test(code),
    code: `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Layout Lab");
            JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));

            panel.add(new JButton("One"));
            panel.add(new JButton("Two"));
            panel.add(new JButton("Three"));
            panel.add(new JButton("Four"));

            frame.add(panel);
            frame.setSize(360, 220);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`
  },
  {
    navTitle: "Events & listeners",
    navSubtitle: "Respond to clicks",
    time: "8 MIN",
    title: "Make the interface respond",
    description:
      "A button is only decoration until it reacts. Connect user actions to your code with listeners and make a tiny working counter.",
    tags: ["ActionListener", "Lambda", "setText()"],
    conceptTitle: "Listeners turn actions into behavior",
    conceptBody: [
      "Swing components publish events when something happens. A <code>JButton</code> publishes an action event when it is clicked; an <code>ActionListener</code> receives it.",
      "Lambdas make simple handlers concise: <code>button.addActionListener(e -&gt; ...)</code>. Inside the handler, update your state and then update the component."
    ],
    mentorNote:
      "Keep slow work out of a listener. The Event Dispatch Thread also paints the interface, so long-running work there makes the app feel frozen.",
    points: [
      ["Source", "The component that publishes an event."],
      ["Listener", "The callback that waits for that event."],
      ["Update", "Change state, then reflect it in the interface."]
    ],
    challengeTitle: "Count by two",
    challengeText:
      "Change <code>count[0]++</code> to <code>count[0] += 2</code>, run the code, then click the preview button.",
    challengeTest: (code) => /count\s*\[\s*0\s*\]\s*\+=\s*2/.test(code),
    code: `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Click Counter");
            JLabel countLabel = new JLabel("0", SwingConstants.CENTER);
            JButton addButton = new JButton("Add one");
            int[] count = {0};

            addButton.addActionListener(e -> {
                count[0]++;
                countLabel.setText(String.valueOf(count[0]));
            });

            frame.add(countLabel, BorderLayout.CENTER);
            frame.add(addButton, BorderLayout.SOUTH);
            frame.setSize(320, 180);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`
  },
  {
    navTitle: "Inputs & state",
    navSubtitle: "Read user input",
    time: "7 MIN",
    title: "Turn input into an answer",
    description:
      "Read text from a field, use it in your program, and send a friendly result back to the interface.",
    tags: ["JTextField", "getText()", "ActionEvent"],
    conceptTitle: "Components can hold and reveal state",
    conceptBody: [
      "A <code>JTextField</code> stores the text typed by the user. Call <code>getText()</code> inside the event listener—not before—to read the value at click time.",
      "Then update a label with <code>setText()</code>. This input → event → output loop is the heartbeat of many desktop interfaces."
    ],
    mentorNote:
      "Trim user input before checking it. A string containing only spaces looks non-empty to Java unless you call <code>trim()</code> or <code>isBlank()</code>.",
    points: [
      ["Capture", "Read the newest value inside the handler."],
      ["Validate", "Check empty or invalid input before using it."],
      ["Respond", "Show a useful result close to the action."]
    ],
    challengeTitle: "Use a warmer greeting",
    challengeText:
      "Change the greeting from <code>Hello,</code> to <code>Welcome,</code>, then run and try your name in the preview.",
    challengeTest: (code) => /setText\s*\(\s*["']Welcome,\s*["']/.test(code),
    code: `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Greeter");
            JTextField nameField = new JTextField(16);
            JButton greetButton = new JButton("Say hello");
            JLabel message = new JLabel("Type your name above");

            greetButton.addActionListener(e -> {
                String name = nameField.getText().trim();
                message.setText("Hello, " + name + "!");
            });

            JPanel panel = new JPanel(new FlowLayout());
            panel.add(nameField);
            panel.add(greetButton);
            panel.add(message);
            frame.add(panel);
            frame.setSize(350, 180);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`
  },
  {
    navTitle: "Mini project",
    navSubtitle: "Build a task list",
    time: "10 MIN",
    title: "Ship a tiny task list",
    description:
      "Bring frames, layouts, input, and events together in one small app—then check your understanding and earn your course completion.",
    tags: ["DefaultListModel", "JList", "Composition"],
    conceptTitle: "Small pieces compose into real apps",
    conceptBody: [
      "The frame is still just a shell. The interesting work comes from combining a model, a list, an input field, and a couple of focused event handlers.",
      "Here, <code>DefaultListModel</code> owns the data while <code>JList</code> displays it. Keeping those roles separate makes updates predictable."
    ],
    mentorNote:
      "Production Swing apps usually separate view code from application logic. This small example stays together so you can clearly see the entire event loop.",
    points: [
      ["Model", "Stores the actual task strings."],
      ["View", "The JList renders what the model contains."],
      ["Actions", "Buttons add to or remove from the model."]
    ],
    challengeTitle: "Personalize the empty state",
    challengeText:
      "Change the starter task <code>Learn Swing</code> to <code>Build my first app</code>, then run it.",
    challengeTest: (code) => /addElement\s*\(\s*["']Build my first app["']\s*\)/.test(code),
    quiz: true,
    code: `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("My Tasks");
            DefaultListModel<String> model = new DefaultListModel<>();
            model.addElement("Learn Swing");

            JList<String> taskList = new JList<>(model);
            JTextField input = new JTextField();
            JButton add = new JButton("Add task");

            add.addActionListener(e -> {
                String task = input.getText().trim();
                if (!task.isEmpty()) {
                    model.addElement(task);
                    input.setText("");
                }
            });

            JPanel composer = new JPanel(new BorderLayout(8, 0));
            composer.add(input, BorderLayout.CENTER);
            composer.add(add, BorderLayout.EAST);
            frame.add(new JScrollPane(taskList), BorderLayout.CENTER);
            frame.add(composer, BorderLayout.SOUTH);
            frame.setSize(360, 260);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`
  }
];

swingLessons.forEach((lesson) => {
  lesson.module = "Swing foundations";
  lesson.moduleIndex = 1;
});

if (typeof window.buildSwingcraftExtraLessons === "function") {
  swingLessons.push(...window.buildSwingcraftExtraLessons());
}

const customLessonsStorageKey = "desktopcraft-custom-lessons-v1";
const lessonCreationBanKey = "desktopcraft-lesson-creation-ban-v1";

const courses = [
  {
    id: "java-swing",
    title: "Java Swing: Zero to Builder",
    shortTitle: "Java Swing",
    language: "java",
    languageLabel: "Java",
    fileName: "Main.java",
    description: "Build robust Java desktop interfaces with Swing components, events, models, and layouts.",
    lessons: swingLessons
  },
  ...(typeof window.buildDesktopCourses === "function" ? window.buildDesktopCourses() : [])
];

if (typeof window.expandDesktopcraftCourses === "function") {
  window.expandDesktopcraftCourses(courses, 500);
}

const builtInLessonCounts = new Map(courses.map((course) => [course.id, course.lessons.length]));
loadCustomLessonsIntoCourses();

let currentCourse = courses[0];
let lessons = currentCourse.lessons;

let activeQuiz = [];
let quizIndex = 0;
let quizScore = 0;
let quizAnswered = false;
let quizFinished = false;
let databaseProgressReady = false;
let challengePassed = false;
let challengeAttempts = 0;
let challengeHintLevel = 0;

const legacyStorageKey = "swingcraft-progress-v1";
const legacyCodeStorageKey = "swingcraft-code-v1";
const activeCourseStorageKey = "desktopcraft-active-course-v1";
const statsStorageKey = "desktopcraft-stats-v1";
const progressStorageKey = (courseId) => `desktopcraft-progress-${courseId}-v1`;
const codeStorageKey = (courseId) => `desktopcraft-code-${courseId}-v1`;

const state = {
  courseId: currentCourse.id,
  activeLesson: 0,
  completed: new Set(),
  savedCode: {},
  previewState: {},
  searchQuery: ""
};

const elements = {
  sidebar: document.querySelector("#sidebar"),
  sidebarScrim: document.querySelector("#sidebarScrim"),
  courseTitle: document.querySelector("#courseTitle"),
  courseSelect: document.querySelector("#courseSelect"),
  courseLanguage: document.querySelector("#courseLanguage"),
  editorFileName: document.querySelector("#editorFileName"),
  lessonSearch: document.querySelector("#lessonSearch"),
  lessonNav: document.querySelector("#lessonNav"),
  progressLabel: document.querySelector("#progressLabel"),
  progressPercent: document.querySelector("#progressPercent"),
  progressBar: document.querySelector("#progressBar"),
  breadcrumbTitle: document.querySelector("#breadcrumbTitle"),
  lessonNumber: document.querySelector("#lessonNumber"),
  lessonTime: document.querySelector("#lessonTime"),
  lessonTitle: document.querySelector("#lessonTitle"),
  lessonDescription: document.querySelector("#lessonDescription"),
  lessonTags: document.querySelector("#lessonTags"),
  conceptTitle: document.querySelector("#conceptTitle"),
  conceptBody: document.querySelector("#conceptBody"),
  mentorNote: document.querySelector("#mentorNote"),
  keyPoints: document.querySelector("#keyPoints"),
  referenceLanguage: document.querySelector("#referenceLanguage"),
  referenceSummary: document.querySelector("#referenceSummary"),
  attributeReference: document.querySelector("#attributeReference"),
  functionReference: document.querySelector("#functionReference"),
  eventReference: document.querySelector("#eventReference"),
  flowReference: document.querySelector("#flowReference"),
  lessonGlossary: document.querySelector("#lessonGlossary"),
  lineWalkthrough: document.querySelector("#lineWalkthrough"),
  codeWalkthrough: document.querySelector(".code-walkthrough"),
  challengeTitle: document.querySelector("#challengeTitle"),
  challengeText: document.querySelector("#challengeText"),
  challengeStatus: document.querySelector("#challengeStatus"),
  challengeHint: document.querySelector("#challengeHint"),
  challengeHintText: document.querySelector("#challengeHintText"),
  quizPanel: document.querySelector("#quizPanel"),
  quizProgress: document.querySelector("#quizProgress"),
  quizQuestions: document.querySelector("#quizQuestions"),
  quizResult: document.querySelector("#quizResult"),
  checkQuiz: document.querySelector("#checkQuiz"),
  quizNext: document.querySelector("#quizNext"),
  codeEditor: document.querySelector("#codeEditor"),
  lineNumbers: document.querySelector("#lineNumbers"),
  previewCanvas: document.querySelector("#previewCanvas"),
  consoleBar: document.querySelector("#consoleBar"),
  consoleOutput: document.querySelector("#consoleOutput"),
  simulatorHelp: document.querySelector("#simulatorHelp"),
  simulatorHelpPanel: document.querySelector("#simulatorHelpPanel"),
  createLessonButton: document.querySelector("#createLessonButton"),
  lessonCreator: document.querySelector("#lessonCreator"),
  lessonCreatorForm: document.querySelector("#lessonCreatorForm"),
  creatorCourseName: document.querySelector("#creatorCourseName"),
  customLessonName: document.querySelector("#customLessonName"),
  customStarterCode: document.querySelector("#customStarterCode"),
  customAnswer: document.querySelector("#customAnswer"),
  customOutput: document.querySelector("#customOutput"),
  accountButton: document.querySelector("#accountButton"),
  accountMenu: document.querySelector("#accountMenu"),
  accountAvatar: document.querySelector("#accountAvatar"),
  accountName: document.querySelector("#accountName"),
  accountHandle: document.querySelector("#accountHandle"),
  signInLink: document.querySelector("#signInLink"),
  signOutButton: document.querySelector("#signOutButton"),
  previousLesson: document.querySelector("#previousLesson"),
  completeLesson: document.querySelector("#completeLesson"),
  practiceMasteryStep: document.querySelector("#practiceMasteryStep"),
  completeMasteryStep: document.querySelector("#completeMasteryStep"),
  toast: document.querySelector("#toast")
};

function loadState() {
  try {
    const preferredCourseId = localStorage.getItem(activeCourseStorageKey);
    currentCourse = courses.find((course) => course.id === preferredCourseId) || courses[0];
    lessons = currentCourse.lessons;
    state.courseId = currentCourse.id;

    const storedProgress = localStorage.getItem(progressStorageKey(state.courseId));
    const storedCode = localStorage.getItem(codeStorageKey(state.courseId));
    const progress = JSON.parse(storedProgress || (state.courseId === "java-swing" ? localStorage.getItem(legacyStorageKey) : "null"));
    const savedCode = JSON.parse(storedCode || (state.courseId === "java-swing" ? localStorage.getItem(legacyCodeStorageKey) : "null"));
    if (Array.isArray(progress?.completed)) {
      state.completed = new Set(progress.completed.filter((index) => index >= 0 && index < lessons.length));
    }
    if (Number.isInteger(progress?.activeLesson)) {
      state.activeLesson = Math.min(Math.max(progress.activeLesson, 0), lessons.length - 1);
    }
    if (savedCode && typeof savedCode === "object") state.savedCode = savedCode;
  } catch {
    try {
      localStorage.removeItem(progressStorageKey(state.courseId));
      localStorage.removeItem(codeStorageKey(state.courseId));
    } catch {
      // Restricted storage modes may make persistence unavailable.
    }
  }
}

function saveProgress() {
  try {
    localStorage.setItem(
      progressStorageKey(state.courseId),
      JSON.stringify({ activeLesson: state.activeLesson, completed: [...state.completed] })
    );
    localStorage.setItem(activeCourseStorageKey, state.courseId);
    const stats = JSON.parse(localStorage.getItem(statsStorageKey) || "{}");
    stats[state.courseId] = {
      title: currentCourse.title,
      completed: state.completed.size,
      total: lessons.length,
      xp: state.completed.size * 100,
      updatedAt: Date.now()
    };
    localStorage.setItem(statsStorageKey, JSON.stringify(stats));
    window.DesktopcraftAuth?.recordProgress(
      stats,
      databaseProgressReady
        ? { courseId: state.courseId, activeLesson: state.activeLesson, completed: [...state.completed] }
        : null
    );
  } catch {
    // The course still works when a browser blocks storage for local files.
  }
}

function saveCode() {
  state.savedCode[state.activeLesson] = elements.codeEditor.value;
  try {
    localStorage.setItem(codeStorageKey(state.courseId), JSON.stringify(state.savedCode));
  } catch {
    // Keep the edit in memory for the current session.
  }
}

function renderCourseChrome() {
  elements.courseSelect.innerHTML = courses
    .map((course) => `<option value="${course.id}">${course.languageLabel} · ${course.shortTitle}</option>`)
    .join("");
  elements.courseSelect.value = currentCourse.id;
  elements.courseTitle.textContent = currentCourse.title;
  elements.courseLanguage.textContent = `${currentCourse.languageLabel} · ${currentCourse.shortTitle}`;
  elements.editorFileName.textContent = currentCourse.fileName;
  elements.lessonSearch.placeholder = `Search ${currentCourse.shortTitle}…`;
  document.title = `${currentCourse.shortTitle} course — Desktopcraft`;
}

function renderAccount() {
  const user = window.DesktopcraftAuth?.currentUser();
  const initials = window.DesktopcraftAuth?.initials(user?.name || "Guest learner") || "GU";
  elements.accountButton.textContent = initials;
  elements.accountAvatar.textContent = initials;
  elements.accountName.textContent = user?.name || "Guest learner";
  elements.accountHandle.textContent = user?.username ? `@${user.username}` : "No account signed in";
  elements.signInLink.hidden = Boolean(user);
  elements.signOutButton.hidden = !user;
}

async function syncProgressFromDatabase() {
  await window.DesktopcraftAuth?.ready?.();
  renderAccount();
  if (!window.DesktopcraftAuth?.currentUser()) {
    databaseProgressReady = true;
    return;
  }

  const databaseCourses = await window.DesktopcraftAuth.loadDatabaseProgress?.();
  databaseProgressReady = true;
  if (!Array.isArray(databaseCourses)) return;

  let stats = {};
  try {
    stats = JSON.parse(localStorage.getItem(statsStorageKey) || "{}");
  } catch {
    stats = {};
  }
  const snapshots = [];

  courses.forEach((course) => {
    const remote = databaseCourses.find((entry) => entry.courseId === course.id);
    let local = null;
    try {
      local = JSON.parse(localStorage.getItem(progressStorageKey(course.id)) || "null");
    } catch {
      local = null;
    }
    if (!remote && !Array.isArray(local?.completed)) return;

    const completed = [...new Set([...(local?.completed || []), ...(remote?.completed || [])])]
      .filter((index) => Number.isInteger(index) && index >= 0 && index < course.lessons.length)
      .sort((left, right) => left - right);
    const localActive = Number.isInteger(local?.activeLesson) ? local.activeLesson : 0;
    const remoteActive = Number.isInteger(remote?.activeLesson) ? remote.activeLesson : 0;
    const useLocalPosition = Number(stats?.[course.id]?.updatedAt) > Number(remote?.updatedAt || 0);
    const activeLesson = Math.min(Math.max(useLocalPosition ? localActive : remoteActive, 0), course.lessons.length - 1);
    const snapshot = { courseId: course.id, activeLesson, completed };
    snapshots.push(snapshot);

    try {
      localStorage.setItem(progressStorageKey(course.id), JSON.stringify({ activeLesson, completed }));
    } catch {
      // Keep merged progress in memory if storage is restricted.
    }
    stats[course.id] = {
      title: course.title,
      completed: completed.length,
      total: course.lessons.length,
      xp: completed.length * 100,
      updatedAt: Math.max(Date.now(), Number(remote?.updatedAt) || 0)
    };
    if (course.id === state.courseId) {
      state.activeLesson = activeLesson;
      state.completed = new Set(completed);
    }
  });

  try {
    localStorage.setItem(statsStorageKey, JSON.stringify(stats));
  } catch {
    // The merged state still works for this page load.
  }
  snapshots.forEach((snapshot) => window.DesktopcraftAuth.recordProgress(stats, snapshot));
  renderLesson();
}

function switchCourse(courseId) {
  if (courseId === state.courseId) return;
  saveCode();
  saveProgress();

  currentCourse = courses.find((course) => course.id === courseId) || courses[0];
  lessons = currentCourse.lessons;
  state.courseId = currentCourse.id;
  state.activeLesson = 0;
  state.completed = new Set();
  state.savedCode = {};
  state.previewState = {};
  state.searchQuery = "";
  elements.lessonSearch.value = "";

  try {
    const storedProgress = localStorage.getItem(progressStorageKey(state.courseId));
    const storedCode = localStorage.getItem(codeStorageKey(state.courseId));
    const progress = JSON.parse(storedProgress || (state.courseId === "java-swing" ? localStorage.getItem(legacyStorageKey) : "null"));
    const savedCode = JSON.parse(storedCode || (state.courseId === "java-swing" ? localStorage.getItem(legacyCodeStorageKey) : "null"));
    if (Array.isArray(progress?.completed)) {
      state.completed = new Set(progress.completed.filter((index) => index >= 0 && index < lessons.length));
    }
    if (Number.isInteger(progress?.activeLesson)) {
      state.activeLesson = Math.min(Math.max(progress.activeLesson, 0), lessons.length - 1);
    }
    if (savedCode && typeof savedCode === "object") state.savedCode = savedCode;
  } catch {
    // Start the selected course fresh if its local state is unavailable.
  }

  renderCourseChrome();
  renderLesson();
  showToast(`${currentCourse.shortTitle} course loaded`);
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function renderNavigation() {
  const query = state.searchQuery.trim().toLowerCase();
  const visibleLessons = lessons
    .map((lesson, index) => ({ lesson, index }))
    .filter(({ lesson }) => {
      if (!query) return true;
      return [lesson.navTitle, lesson.navSubtitle, lesson.module, ...(lesson.tags || [])]
        .join(" ")
        .toLowerCase()
        .includes(query);
    });

  if (!visibleLessons.length) {
    elements.lessonNav.innerHTML = `<div class="lesson-search-empty"><strong>No lessons found</strong><span>Try a component name such as JTable or Timer.</span></div>`;
    return;
  }

  let currentModule = "";
  elements.lessonNav.innerHTML = visibleLessons
    .map(({ lesson, index }) => {
      const active = index === state.activeLesson;
      const completed = state.completed.has(index);
      const moduleHeading = lesson.module !== currentModule
        ? `<div class="nav-module"><span>MODULE ${String(lesson.moduleIndex).padStart(2, "0")}</span><strong>${escapeHtml(plainLanguage(lesson.module))}</strong></div>`
        : "";
      currentModule = lesson.module;
      return `
        ${moduleHeading}
        <button class="lesson-nav-button${active ? " active" : ""}${completed ? " completed" : ""}" data-lesson="${index}" ${active ? 'aria-current="step"' : ""}>
          <span class="nav-number">${completed ? "✓" : String(index + 1).padStart(3, "0")}</span>
          <span class="nav-copy">
            <strong>${escapeHtml(plainLanguage(lesson.navTitle))}</strong>
            <small>${escapeHtml(lesson.navSubtitle)}</small>
          </span>
          <span class="nav-state" aria-label="${completed ? "Completed" : "Not completed"}">
            ${completed ? '<svg viewBox="0 0 24 24"><path d="m6 12 4 4 8-9" /></svg>' : ""}
          </span>
        </button>`;
    })
    .join("");
}

function updateProgress() {
  const count = state.completed.size;
  const percent = Math.round((count / lessons.length) * 100);
  elements.progressLabel.textContent = `${count} of ${lessons.length} lessons`;
  elements.progressPercent.textContent = `${percent}%`;
  elements.progressBar.style.width = `${percent}%`;
}

function renderLesson() {
  const lesson = lessons[state.activeLesson];
  state.previewState = {};
  challengePassed = state.completed.has(state.activeLesson);
  challengeAttempts = 0;
  challengeHintLevel = 0;

  elements.breadcrumbTitle.textContent = plainLanguage(lesson.navTitle);
  elements.lessonNumber.textContent = `LESSON ${String(state.activeLesson + 1).padStart(2, "0")}`;
  elements.lessonTime.textContent = lesson.time;
  elements.lessonTitle.textContent = plainLanguage(lesson.title);
  elements.lessonDescription.textContent = plainLanguage(lesson.description);
  elements.lessonTags.innerHTML = lesson.tags.map((tag) => `<span class="lesson-tag">${escapeHtml(tag)}</span>`).join("");
  elements.conceptTitle.textContent = plainLanguage(lesson.conceptTitle);
  elements.conceptBody.innerHTML = lesson.conceptBody.map((paragraph) => `<p>${plainLanguageHtml(paragraph)}</p>`).join("");
  elements.mentorNote.innerHTML = plainLanguageHtml(lesson.mentorNote);
  elements.keyPoints.innerHTML = lesson.points
    .map(
      ([title, text], index) => `
      <div class="key-point">
        <span>${index + 1}</span>
        <div><strong>${escapeHtml(plainLanguage(title))}</strong><p>${escapeHtml(plainLanguage(text))}</p></div>
      </div>`
    )
    .join("");
  renderLessonReference(lesson);
  const explanationLevel = document.documentElement.dataset.explanationLevel || "balanced";
  if (explanationLevel === "detailed") elements.codeWalkthrough.open = true;
  else if (explanationLevel === "concise") elements.codeWalkthrough.open = false;
  elements.challengeTitle.textContent = plainLanguage(lesson.challengeTitle);
  elements.challengeText.innerHTML = plainLanguageHtml(lesson.challengeText);
  elements.challengeHintText.hidden = true;
  elements.challengeHint.textContent = "Need a hint?";
  updateChallenge(challengePassed);

  elements.codeEditor.value = state.savedCode[state.activeLesson] ?? lesson.code;
  updateLineNumbers();
  renderPreview();

  elements.quizPanel.hidden = false;
  renderQuiz();

  elements.previousLesson.disabled = state.activeLesson === 0;
  elements.completeLesson.innerHTML = state.completed.has(state.activeLesson)
    ? `Completed <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 12 4 4 8-9" /></svg>`
    : `Complete lesson <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>`;
  updateMasteryChecklist();

  elements.consoleBar.classList.remove("error");
  elements.consoleOutput.textContent = "Ready. Run your code to update the preview.";
  renderNavigation();
  updateProgress();
  saveProgress();
}

function updateLineNumbers() {
  const lines = elements.codeEditor.value.split("\n").length;
  elements.lineNumbers.innerHTML = Array.from({ length: lines }, (_, index) => index + 1).join("<br>");
  elements.lineNumbers.scrollTop = elements.codeEditor.scrollTop;
}

function getString(code, pattern, fallback) {
  const match = code.match(pattern);
  return match?.[1] ?? fallback;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

const plainLanguageRules = [
  [/Event Dispatch Thread/gi, "Swing UI thread"],
  [/synchronization context/gi, "UI thread"],
  [/\boperating-system\b/gi, "system"],
  [/\btop-level\b/gi, "main"],
  [/\bnon-editable\b/gi, "read-only"],
  [/\bconfiguration\b/gi, "settings"],
  [/\bconfigures\b/gi, "sets up"],
  [/\bconfigured\b/gi, "set up"],
  [/\bconfigure\b/gi, "set up"],
  [/\binitializes\b/gi, "starts"],
  [/\binitialized\b/gi, "started"],
  [/\binitialize\b/gi, "start"],
  [/\bconstructs\b/gi, "creates"],
  [/\bconstructed\b/gi, "created"],
  [/\bconstruct\b/gi, "create"],
  [/\binvokes\b/gi, "runs"],
  [/\binvoked\b/gi, "run"],
  [/\binvoke\b/gi, "run"],
  [/\bdispatches\b/gi, "sends"],
  [/\bdispatched\b/gi, "sent"],
  [/\bdispatch\b/gi, "send"],
  [/\bpublishes\b/gi, "sends"],
  [/\bemits\b/gi, "sends"],
  [/\bsubscribed\b/gi, "connected"],
  [/\bsupplied\b/gi, "given"],
  [/\breusable\b/gi, "shared"],
  [/\bconstrained\b/gi, "limited"],
  [/\bhierarchical\b/gi, "tree-like"],
  [/\bperiodic\b/gi, "repeated"],
  [/\bprivileged\b/gi, "system-level"],
  [/\basynchronous\b/gi, "background"],
  [/\bdynamically\b/gi, "as it runs"],
  [/\bindependently\b/gi, "on its own"],
  [/\bimmediately\b/gi, "right away"],
  [/\bsubsequent\b/gi, "next"],
  [/\bcorresponding\b/gi, "matching"],
  [/\bappropriate\b/gi, "right"],
  [/\bexplicit\b/gi, "clear"],
  [/\bconcise\b/gi, "short"],
  [/\bresponsibility\b/gi, "job"],
  [/\bcontributes\b/gi, "helps"],
  [/\bcomposition\b/gi, "putting parts together"],
  [/\barchitecture\b/gi, "design"],
  [/\bfunctionality\b/gi, "features"],
  [/\bcurriculum\b/gi, "course"],
  [/\bestimated\b/gi, "planned"],
  [/\bprimary\b/gi, "main"],
  [/\bcentral\b/gi, "main"],
  [/\bgeometry\b/gi, "size and position"],
  [/interface state/gi, "screen data"],
  [/application state/gi, "app data"],
  [/\binterfaces\b/gi, "screens"],
  [/\binterface\b/gi, "screen"],
  [/\bcomponents\b/gi, "controls"],
  [/\bcomponent\b/gi, "control"],
  [/\bcontainers\b/gi, "holders"],
  [/\bcontainer\b/gi, "holder"],
  [/reflects the result/gi, "shows the result"],
  [/reflect the result/gi, "show the result"],
  [/\brepresents\b/gi, "shows"],
  [/\bpersisted\b/gi, "saved"],
  [/\bpersists\b/gi, "saves"],
  [/\bpersist\b/gi, "save"],
  [/\blifecycle\b/gi, "start-to-finish steps"]
];

function preserveReplacementCase(match, replacement) {
  return /^[A-Z]/.test(match) ? replacement.charAt(0).toUpperCase() + replacement.slice(1) : replacement;
}

function plainLanguage(value) {
  return plainLanguageRules.reduce(
    (text, [pattern, replacement]) => text.replace(pattern, (match) => preserveReplacementCase(match, replacement)),
    String(value || "")
  );
}

function plainLanguageHtml(value) {
  return String(value || "")
    .split(/(<code\b[^>]*>[\s\S]*?<\/code>|<[^>]+>)/gi)
    .map((part) => part.startsWith("<") ? part : plainLanguage(part))
    .join("");
}

const referenceDescriptions = {
  JFrame: "Swing's top-level operating-system window. It owns the title bar, size, close behavior, and root content area.",
  JPanel: "A lightweight Swing container used to group controls and give a section its own layout.",
  JLabel: "Displays short, non-editable text or an icon.",
  JButton: "A clickable Swing control that publishes an ActionEvent.",
  JTextField: "A single-line editable text control. getText() reads its current value.",
  JTextArea: "A multi-line editable text component for longer content.",
  JList: "Displays a selectable collection backed by a list model.",
  JTable: "Displays rows and columns supplied by a table model.",
  GridLayout: "Arranges components in equal-sized rows and columns.",
  BorderLayout: "Arranges components in NORTH, SOUTH, EAST, WEST, and CENTER regions.",
  FlowLayout: "Places components in a row and wraps them when space runs out.",
  Tk: "Creates the Tkinter root window and owns the Python GUI event loop.",
  Frame: "Groups related controls inside a reusable interface region.",
  Label: "Displays non-editable interface text.",
  Button: "Presents an action the learner can trigger.",
  Entry: "Stores a single line of editable Tkinter text.",
  Form: "The top-level WinForms window and control container.",
  TextBox: "A WinForms text-input control whose Text property holds the current value.",
  FlowLayoutPanel: "A WinForms container that automatically flows and wraps child controls.",
  QApplication: "Initializes the Qt application and manages its event loop.",
  QWidget: "The base Qt visual object; it can be a window or a child region.",
  QLabel: "A Qt widget that displays text or an image.",
  QPushButton: "A Qt command button that emits the clicked signal.",
  QLineEdit: "A Qt single-line text editor.",
  QVBoxLayout: "Stacks Qt widgets vertically and manages their geometry.",
  BrowserWindow: "Electron's native application window, created by the main process.",
  HTMLElement: "A DOM object representing one element in the renderer interface.",
  querySelector: "Finds the first DOM element matching a CSS selector.",
  addEventListener: "Registers a callback that runs when the named browser event occurs.",
  addActionListener: "Registers the Swing callback that receives button and action events.",
  invokeLater: "Schedules Swing interface creation on the Event Dispatch Thread.",
  setText: "Updates the visible text held by a component.",
  getText: "Reads the component's current text at the moment the method is called.",
  setSize: "Sets the window's width and height in pixels.",
  setVisible: "Shows or hides the configured window.",
  setDefaultCloseOperation: "Chooses what Swing should do when the user closes the window.",
  setLocationRelativeTo: "Positions a Swing window relative to another component; null centers it on screen.",
  add: "Adds a component to a container, optionally in a named layout position.",
  pack: "Sizes a Swing window from the preferred sizes of its children.",
  config: "Updates one or more Tkinter widget options after construction.",
  pack: "Places a Tkinter widget using edge-based geometry rules, or sizes a Swing window from its children depending on context.",
  grid: "Places a Tkinter widget at a row and column.",
  mainloop: "Starts Tkinter's event loop so the window can paint and receive input.",
  Run: "Starts the WinForms message loop with the supplied Form.",
  connect: "Connects a Qt signal to the callback that should receive it.",
  show: "Makes a configured Qt widget visible.",
  exec: "Starts Qt's application event loop and returns when the application exits.",
  textContent: "Stores or replaces the plain text displayed inside a DOM element.",
  innerHTML: "Reads or replaces the HTML markup inside a DOM element.",
  title: "Controls the visible title of the application window or document.",
  text: "Controls the visible wording displayed by a widget.",
  command: "Names the Python callback a Tkinter control runs when activated.",
  padding: "Adds internal breathing room between a container edge and its content.",
  Dock: "Controls which edge or available area a WinForms control fills.",
  Size: "Stores a window or control's width and height.",
  Text: "Stores visible WinForms text, including titles, labels, and input values.",
  id: "Gives a DOM element a unique selector name.",
  class: "Assigns reusable CSS styling names to a DOM element.",
  value: "Stores the current value of an HTML input control."
};

function symbolBase(symbol) {
  return String(symbol).replace(/^.*(?:\.|->)/, "").replace(/[^A-Za-z0-9_$ /#-]/g, "");
}

function explainReferenceSymbol(symbol, kind = "function") {
  const base = symbolBase(symbol);
  if (referenceDescriptions[base]) return referenceDescriptions[base];
  if (/^set[A-Z]/.test(base)) {
    const property = base.slice(3).replace(/([a-z])([A-Z])/g, "$1 $2").toLowerCase();
    return `Sets the component's ${property} property to the supplied value.`;
  }
  if (/^get[A-Z]/.test(base)) {
    const property = base.slice(3).replace(/([a-z])([A-Z])/g, "$1 $2").toLowerCase();
    return `Reads the component's current ${property} value.`;
  }
  if (/^add[A-Z]/.test(base)) return `Registers or adds ${base.slice(3)} behavior to the object.`;
  if (kind === "attribute") return `Configures the ${base} value used by this interface.`;
  if (/^[A-Z]/.test(base)) return `Constructs or configures a ${base} object used by the desktop interface.`;
  return `Calls ${base} to perform one focused step in the lesson's interface workflow.`;
}

function uniqueReferenceItems(values, limit = 18) {
  const result = [];
  values.forEach((value) => {
    const clean = String(value || "").trim();
    if (clean && !result.includes(clean) && result.length < limit) result.push(clean);
  });
  return result;
}

function extractLessonAttributes(code, language) {
  const attributes = [];
  for (const match of code.matchAll(/\b(set[A-Z][A-Za-z0-9_]*)\s*\(/g)) attributes.push(match[1]);
  if (language === "python") {
    for (const match of code.matchAll(/\b(text|command|padding|fill|expand|pady|padx|width|height|state)\s*=/g)) attributes.push(match[1]);
  } else if (language === "csharp") {
    for (const match of code.matchAll(/\b(Text|Size|Width|Height|Dock|Padding|AutoSize|Enabled|Visible)\s*=/g)) attributes.push(match[1]);
  } else if (language === "javascript") {
    for (const match of code.matchAll(/\.([A-Za-z_$][\w$]*)\s*=/g)) attributes.push(match[1]);
    for (const match of code.matchAll(/\b(id|class|value|type|aria-[\w-]+)=["']/g)) attributes.push(match[1]);
  }
  for (const match of code.matchAll(/\b(BorderLayout\.[A-Z]+|SwingConstants\.[A-Z]+|Qt::[A-Za-z]+)\b/g)) attributes.push(match[1]);
  if (!attributes.length) attributes.push("component state", "visible text", "layout position");
  return uniqueReferenceItems(attributes, 14);
}

function extractLessonFunctions(code) {
  const ignored = new Set(["if", "for", "while", "switch", "catch", "return", "new", "class", "function", "sizeof"]);
  const functions = [];
  for (const match of code.matchAll(/\b([A-Za-z_$][\w$]*(?:(?:\.|->)[A-Za-z_$][\w$]*)?)\s*\(/g)) {
    const symbol = match[1];
    if (!ignored.has(symbol) && !ignored.has(symbolBase(symbol))) functions.push(symbol);
  }
  return uniqueReferenceItems(functions, 18);
}

function extractLessonEvents(code, language) {
  const definitions = [
    [/addActionListener|ActionListener/, "ActionEvent", "A Swing action such as a button click invokes the registered listener on the Event Dispatch Thread."],
    [/DocumentListener|addDocumentListener/, "Document change", "Editing the text model invokes a document listener so the interface can respond immediately."],
    [/KeyListener|KeyStroke|InputMap|KeyMap/, "Keyboard input", "A key event or key binding maps keyboard input to an application action."],
    [/\bcommand\s*=|\.config\s*\(/, "Tkinter command", "Tkinter calls the assigned Python function when the control is activated."],
    [/\.bind\s*\(/, "Tkinter binding", "The bound event pattern passes an event object into its Python callback."],
    [/\.Click\s*\+=/, "Click event", "WinForms raises Click and invokes the subscribed C# event handler."],
    [/QObject::connect|&QPushButton::clicked|SIGNAL\s*\(/, "Qt signal", "Qt emits a signal and delivers it to the connected lambda or slot."],
    [/addEventListener\s*\(/, "DOM event", "The Electron renderer listens for the named DOM event and runs its JavaScript callback."],
    [/\bTimer\b|\bafter\s*\(|QTimer/, "Scheduled event", "The toolkit schedules work through its event loop without blocking interface input."]
  ];
  const events = definitions.filter(([pattern]) => pattern.test(code)).map(([, name, description]) => [name, description]);
  if (!events.length) {
    events.push(["Application startup", `This ${language} example performs its visible work during setup; later user actions are handled by the toolkit's event loop.`]);
  }
  return events;
}

function lessonExecutionFlow(language) {
  const flows = {
    java: ["Import Swing and AWT types used by the example.", "Enter main() and schedule interface creation on Swing's UI thread.", "Construct the frame, controls, models, and layouts.", "Set component properties and add controls to their containers.", "Register listeners that translate user actions into state changes.", "Size, position, and reveal the frame; Swing's event loop keeps it responsive."],
    python: ["Import tkinter and the themed ttk widget set.", "Create the single Tk root window.", "Construct widgets and configure their starting values.", "Define callback functions, then connect them through command or bind.", "Place widgets with pack, grid, or place.", "Start mainloop() so Tk can paint and dispatch input events."],
    csharp: ["Import the .NET namespaces used by the form.", "Construct the Form and its child controls.", "Assign properties such as Text, Size, Dock, and Padding.", "Subscribe C# handlers to control events.", "Add controls to the form's Controls collection.", "Start Application.Run so Windows can dispatch messages to the form."],
    cpp: ["Include the Qt widget classes used by the program.", "Create QApplication before any visual widgets.", "Construct the window, controls, and layout objects.", "Configure widget properties and place controls in the layout.", "Connect signals to lambdas or slots that update state.", "Show the window and enter app.exec() to process Qt events."],
    javascript: ["Select or create the renderer's root DOM element.", "Describe the desktop interface with HTML elements.", "Store references to controls that the script will update.", "Register DOM listeners for clicks, input, and other actions.", "Read current values and update renderer state inside callbacks.", "Electron's renderer event loop keeps the interface responsive."]
  };
  return flows[language] || flows.javascript;
}

function explainCodeLine(line, language) {
  const text = line.trim();
  if (/^(\/\/|#(?!include)|\/\*|\*)/.test(text)) return "A comment for the reader; it documents intent and is not executed.";
  if (/^(import |from |using |#include|const \{.*require)/.test(text)) return "Loads a library, namespace, or toolkit type needed later in the example.";
  if (/\b(class|public class)\b/.test(text)) return "Declares the class that groups this program's interface code and behavior.";
  if (/\bmain\s*\(|static void Main|def main/.test(text)) return "Defines the program entry point where application startup begins.";
  if (/^def\s+/.test(text) || /function\s+\w+/.test(text)) return "Defines a reusable callback or helper function; its indented or braced lines form the function body.";
  if (/invokeLater/.test(text)) return referenceDescriptions.invokeLater;
  if (/addActionListener|\.Click\s*\+=|QObject::connect|addEventListener|\bcommand\s*=|\.bind\s*\(/.test(text)) return "Connects an interface event to the callback that should run when that event occurs.";
  if (/\bnew\s+[A-Z]|=\s*(?:ttk\.)?[A-Z]\w*\(|auto\s*\*.*new\s+Q|document\.createElement/.test(text)) return "Creates a window, control, layout, model, or other object and stores its reference for later use.";
  if (/\.add\s*\(|Controls\.Add|addWidget|\.pack\s*\(|\.grid\s*\(/.test(text)) return "Places a component inside its parent container so the layout manager can position it.";
  if (/\.(?:set|config|title|geometry|resize)\w*\s*\(|\b(?:Text|Size|Dock|Padding|Width|Height)\s*=|\.(?:textContent|innerHTML|title|value)\s*=/.test(text)) return "Assigns or updates an interface property such as text, dimensions, layout, or visible state.";
  if (/\bif\s*\(?/.test(text)) return "Checks a condition and runs the following block only when that condition is true.";
  if (/\b(return|System\.exit)\b|return app\.exec/.test(text)) return "Returns a result or finishes this part of the program's control flow.";
  if (/mainloop\s*\(|Application\.Run|app\.exec\s*\(|setVisible\s*\(\s*true|\.show\s*\(/.test(text)) return "Starts or reveals the interface and hands ongoing input processing to the toolkit event loop.";
  if (/^[}\]);]+[;,]?$/.test(text)) return "Closes the current block, callback, expression, or object definition.";
  if (/^(else|case |default:)/.test(text)) return "Begins an alternate branch of the program's decision logic.";
  if (/^[A-Za-z_$][\w$<>\[\] *]*\s+[A-Za-z_$][\w$]*\s*=|^(?:const|let|var|String|int|boolean|auto)\b/.test(text)) return "Declares a named value or component reference so later lines can read or update it.";
  if (/^<\/?[A-Za-z]/.test(text)) return "Defines part of the Electron renderer's visible HTML structure.";
  if (/^[{}()[\];,]+$/.test(text)) return "Provides the punctuation that groups and terminates the surrounding code block.";
  return `Executes this ${currentCourse.languageLabel} statement as part of the lesson's setup, state update, or interface behavior.`;
}

function renderReferenceItems(target, items, kind) {
  target.innerHTML = items.map((symbol) => `
    <div class="reference-item">
      <code>${escapeHtml(symbol)}</code>
      <p>${escapeHtml(plainLanguage(explainReferenceSymbol(symbol, kind)))}</p>
    </div>`).join("");
}

function renderLessonReference(lesson) {
  const code = String(lesson.code || "");
  const attributes = extractLessonAttributes(code, currentCourse.language);
  const functions = extractLessonFunctions(code);
  const events = extractLessonEvents(code, currentCourse.languageLabel);
  const flow = lessonExecutionFlow(currentCourse.language);
  const vocabulary = uniqueReferenceItems([lesson.tags?.[0], lesson.tags?.[1], lesson.tags?.[2], lesson.module, "Callback", "State"], 6);
  const lines = code.split("\n");

  elements.referenceLanguage.textContent = currentCourse.languageLabel.toUpperCase();
  elements.referenceSummary.textContent = `This guide shows ${attributes.length} setting${attributes.length === 1 ? "" : "s"}, ${functions.length} function or method call${functions.length === 1 ? "" : "s"}, ${events.length} event path${events.length === 1 ? "" : "s"}, and every code line in ${plainLanguage(lesson.title)}.`;
  renderReferenceItems(elements.attributeReference, attributes, "attribute");
  renderReferenceItems(elements.functionReference, functions.length ? functions : [lesson.tags?.[0] || "lesson operation"], "function");
  elements.eventReference.innerHTML = events.map(([name, description]) => `
    <div class="reference-item">
      <code>${escapeHtml(name)}</code>
      <p>${escapeHtml(plainLanguage(description))}</p>
    </div>`).join("");
  elements.flowReference.innerHTML = flow.map((step, index) => `<li><span>${index + 1}</span><p>${escapeHtml(plainLanguage(step))}</p></li>`).join("");
  elements.lessonGlossary.innerHTML = vocabulary.map((term) => `
    <div><dt>${escapeHtml(plainLanguage(term))}</dt><dd>${escapeHtml(plainLanguage(
      term === "Callback" ? "A function saved now and invoked later when an event occurs." :
      term === "State" ? "The data values that describe what the application currently knows or displays." :
      term === lesson.module ? `The curriculum section that groups this lesson with related ${currentCourse.shortTitle} skills.` :
      explainReferenceSymbol(term, "concept")
    ))}</dd></div>`).join("");
  elements.lineWalkthrough.innerHTML = lines.map((line, index) => {
    if (!line.trim()) return "";
    return `<li><span class="walkthrough-line">${index + 1}</span><div><code>${escapeHtml(line.trim())}</code><p>${escapeHtml(plainLanguage(explainCodeLine(line, currentCourse.language)))}</p></div></li>`;
  }).join("");
}

function readStoredCustomLessons() {
  try {
    const stored = JSON.parse(localStorage.getItem(customLessonsStorageKey) || "[]");
    return Array.isArray(stored) ? stored : [];
  } catch {
    return [];
  }
}

function normalizeLessonAnswer(value) {
  return String(value || "").replace(/\r\n/g, "\n").trim();
}

function hydrateCustomLesson(stored, course) {
  const title = String(stored.title || "Custom lesson").slice(0, 80);
  const starterCode = String(stored.starterCode || "").slice(0, 12000);
  const answer = String(stored.answer || "").slice(0, 12000);
  const expectedOutput = String(stored.expectedOutput || "").slice(0, 1000);
  return {
    custom: true,
    customId: stored.id,
    navTitle: title,
    navSubtitle: "Creator lesson",
    time: "CUSTOM",
    title,
    description: `A custom ${course.shortTitle} challenge created in the Desktopcraft lesson studio.`,
    tags: ["Custom lesson", course.languageLabel, "Creator studio"],
    module: "Custom lessons",
    moduleIndex: 99,
    conceptTitle: "Build toward the expected result",
    conceptBody: [
      `Start with the supplied code, then revise it until it behaves like the creator's answer.`,
      `Expected output: <code>${escapeHtml(expectedOutput)}</code>`
    ],
    mentorNote: "Read the expected output first, make one focused change at a time, and run the simulator after each change.",
    points: [
      ["Starter", "Begin with the code supplied by the lesson creator."],
      ["Answer", "Your final code is checked against the creator's answer."],
      ["Output", expectedOutput]
    ],
    challengeTitle: "Match the creator's answer",
    challengeText: `Edit the starter until it matches the saved answer and produces: <code>${escapeHtml(expectedOutput)}</code>`,
    challengeTest: (code) => normalizeLessonAnswer(code) === normalizeLessonAnswer(answer),
    expectedOutput,
    code: starterCode
  };
}

function loadCustomLessonsIntoCourses() {
  readStoredCustomLessons().forEach((stored) => {
    const course = courses.find((candidate) => candidate.id === stored.courseId);
    if (course && stored.title && stored.starterCode && stored.answer && stored.expectedOutput) {
      course.lessons.push(hydrateCustomLesson(stored, course));
    }
  });
}

let lessonCreationBannedInMemory = false;
const profanityPattern = /(?:^|[^a-z0-9])(?:f[\W_]*u[\W_]*c[\W_]*k(?:er|ing|ed|s)?|s[\W_]*h[\W_]*i[\W_]*t(?:ty|s)?|bitch(?:es|ing)?|ass(?:hole|holes|es)?|bastard(?:s)?|cunt(?:s)?|dick(?:s)?|pussy|whore(?:s)?|slut(?:s)?|damn|crap)(?=$|[^a-z0-9])/i;

function moderationText(value) {
  return String(value || "")
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[013457]/g, (character) => ({ "0": "o", "1": "i", "3": "e", "4": "a", "5": "s", "7": "t" })[character]);
}

function containsProfanity(values) {
  return values.some((value) => profanityPattern.test(moderationText(value)));
}

function isLessonCreationBanned() {
  if (lessonCreationBannedInMemory) return true;
  try {
    return Boolean(localStorage.getItem(lessonCreationBanKey));
  } catch {
    return false;
  }
}

function updateLessonCreatorState() {
  const banned = isLessonCreationBanned();
  elements.createLessonButton.disabled = banned;
  elements.createLessonButton.querySelector("span").textContent = banned ? "Lesson creation banned" : "Create lesson";
  elements.createLessonButton.setAttribute("aria-label", banned ? "Lesson creation banned" : "Create lesson");
  elements.createLessonButton.title = banned
    ? "Lesson creation was disabled after a profanity violation on this account."
    : "Create a lesson for the current course";
}

function eraseCustomLessonsAndBan() {
  lessonCreationBannedInMemory = true;
  try {
    localStorage.setItem(lessonCreationBanKey, JSON.stringify({ bannedAt: Date.now(), reason: "profanity" }));
    localStorage.removeItem(customLessonsStorageKey);
  } catch {
    // The in-memory ban still applies when persistent browser storage is unavailable.
  }

  courses.forEach((course) => {
    const builtInCount = builtInLessonCounts.get(course.id);
    course.lessons.splice(builtInCount);
    try {
      const progressKey = progressStorageKey(course.id);
      const progress = JSON.parse(localStorage.getItem(progressKey) || "null");
      if (Array.isArray(progress?.completed)) {
        progress.completed = progress.completed.filter((index) => index < builtInCount);
        progress.activeLesson = Math.min(progress.activeLesson || 0, builtInCount - 1);
        localStorage.setItem(progressKey, JSON.stringify(progress));
      }
      const editsKey = codeStorageKey(course.id);
      const edits = JSON.parse(localStorage.getItem(editsKey) || "null");
      if (edits && typeof edits === "object") {
        Object.keys(edits).forEach((index) => {
          if (Number(index) >= builtInCount) delete edits[index];
        });
        localStorage.setItem(editsKey, JSON.stringify(edits));
      }
    } catch {
      // In-memory lesson content is still erased.
    }
  });

  lessons = currentCourse.lessons;
  const builtInCount = builtInLessonCounts.get(currentCourse.id);
  state.activeLesson = Math.min(state.activeLesson, builtInCount - 1);
  state.completed = new Set([...state.completed].filter((index) => index < builtInCount));
  Object.keys(state.savedCode).forEach((index) => {
    if (Number(index) >= builtInCount) delete state.savedCode[index];
  });
  updateLessonCreatorState();
}

function openLessonCreator() {
  if (isLessonCreationBanned()) {
    showToast("Lesson creation is banned for this account");
    return;
  }
  elements.creatorCourseName.textContent = currentCourse.shortTitle;
  elements.lessonCreator.showModal();
  elements.customLessonName.focus();
}

function publishCustomLesson(event) {
  event.preventDefault();
  const storedLesson = {
    id: `custom-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    courseId: currentCourse.id,
    title: elements.customLessonName.value.trim(),
    starterCode: elements.customStarterCode.value.trim(),
    answer: elements.customAnswer.value.trim(),
    expectedOutput: elements.customOutput.value.trim(),
    createdAt: Date.now()
  };
  const content = [storedLesson.title, storedLesson.starterCode, storedLesson.answer, storedLesson.expectedOutput];
  if (content.some((value) => !value)) {
    showToast("Complete all four lesson fields");
    return;
  }
  if (containsProfanity(content)) {
    elements.lessonCreatorForm.reset();
    eraseCustomLessonsAndBan();
    elements.lessonCreator.close();
    renderLesson();
    showToast("Profanity detected: custom content erased and creation banned");
    return;
  }

  const storedLessons = readStoredCustomLessons();
  storedLessons.push(storedLesson);
  try {
    localStorage.setItem(customLessonsStorageKey, JSON.stringify(storedLessons));
  } catch {
    showToast("Lesson saved for this session; browser storage is unavailable");
  }
  currentCourse.lessons.push(hydrateCustomLesson(storedLesson, currentCourse));
  lessons = currentCourse.lessons;
  state.activeLesson = lessons.length - 1;
  elements.lessonCreatorForm.reset();
  elements.lessonCreator.close();
  renderLesson();
  showToast("Custom lesson published");
}

function windowMarkup(title, body) {
  return `
    <div class="swing-window">
      <div class="swing-titlebar">
        <span class="window-controls"><span></span><span></span><span></span></span>
        ${escapeHtml(title)}
      </div>
      <div class="swing-body">${body}</div>
    </div>`;
}

function renderLegacyPreview() {
  const lessonIndex = state.activeLesson;
  const code = elements.codeEditor.value;
  const title = getString(code, /new\s+JFrame\s*\(\s*"([^"]*)"\s*\)/, lessons[lessonIndex].navTitle);

  if (lessonIndex === 0) {
    const label = getString(code, /new\s+JLabel\s*\(\s*"([^"]*)"/, "My first desktop app");
    const hasListener = /addActionListener/.test(code);
    elements.previewCanvas.innerHTML = windowMarkup(
      title,
      `<div class="swing-stack"><strong class="swing-heading" id="demoLabel">${escapeHtml(label)}</strong><p class="swing-subtext">Your JFrame is visible.</p><button class="swing-button primary" id="demoButton">${hasListener ? "Try the event" : "It works"}</button></div>`
    );
    document.querySelector("#demoButton").addEventListener("click", () => {
      document.querySelector("#demoLabel").textContent = hasListener ? "Button event received!" : label;
      writeConsole(hasListener ? "ActionEvent received by demoButton." : "Button clicked. Add an ActionListener to make it react.");
    });
  }

  if (lessonIndex === 1) {
    const buttonLabels = [...code.matchAll(/new\s+JButton\s*\(\s*"([^"]*)"\s*\)/g)].map((match) => match[1]);
    const gridMatch = code.match(/new\s+GridLayout\s*\(\s*\d+\s*,\s*(\d+)/);
    const columns = gridMatch ? Math.min(Number(gridMatch[1]), 3) : 2;
    const labels = buttonLabels.length ? buttonLabels : ["One", "Two", "Three", "Four"];
    elements.previewCanvas.innerHTML = windowMarkup(
      title,
      `<div class="swing-grid ${columns === 3 ? "cols-3" : ""}">${labels
        .map((label) => `<button class="swing-button">${escapeHtml(label)}</button>`)
        .join("")}</div>`
    );
  }

  if (lessonIndex === 2) {
    const increment = /count\s*\[\s*0\s*\]\s*\+=\s*2/.test(code) ? 2 : 1;
    state.previewState.count = 0;
    elements.previewCanvas.innerHTML = windowMarkup(
      title,
      `<div class="swing-stack"><span class="swing-subtext">CURRENT COUNT</span><strong class="counter-number" id="counterValue">0</strong><button class="swing-button primary" id="counterButton">Add ${increment === 2 ? "two" : "one"}</button></div>`
    );
    document.querySelector("#counterButton").addEventListener("click", () => {
      state.previewState.count += increment;
      document.querySelector("#counterValue").textContent = state.previewState.count;
      writeConsole(`count[0] is now ${state.previewState.count}`);
    });
  }

  if (lessonIndex === 3) {
    const greeting = /setText\s*\(\s*"Welcome,\s*"/.test(code) ? "Welcome" : "Hello";
    elements.previewCanvas.innerHTML = windowMarkup(
      title,
      `<div class="swing-form"><input class="swing-input" id="nameInput" value="Ada" aria-label="Name"/><button class="swing-button primary" id="greetButton">Say hello</button><span class="swing-label" id="greetingLabel">Type your name above</span></div>`
    );
    const greet = () => {
      const name = document.querySelector("#nameInput").value.trim();
      document.querySelector("#greetingLabel").textContent = name ? `${greeting}, ${name}!` : "Please enter a name";
      writeConsole(name ? `Read \"${name}\" from nameField.` : "Input was blank; ask the user to try again.");
    };
    document.querySelector("#greetButton").addEventListener("click", greet);
    document.querySelector("#nameInput").addEventListener("keydown", (event) => {
      if (event.key === "Enter") greet();
    });
  }

  if (lessonIndex === 4) {
    const firstTask = getString(code, /model\.addElement\s*\(\s*"([^"]+)"\s*\)/, "Learn Swing");
    state.previewState.tasks = [firstTask];
    elements.previewCanvas.innerHTML = windowMarkup(
      title,
      `<div class="swing-form"><div class="swing-flow" style="width:100%"><input class="swing-input" id="taskInput" value="Practice layouts" aria-label="New task"/><button class="swing-button primary" id="addTask">Add task</button></div><div class="task-list" id="taskList"></div></div>`
    );
    const renderTasks = () => {
      document.querySelector("#taskList").innerHTML = state.previewState.tasks
        .map(
          (task, index) => `<div class="task-item"><span>${escapeHtml(task)}</span><button data-remove="${index}" aria-label="Remove ${escapeHtml(task)}">Remove</button></div>`
        )
        .join("");
    };
    renderTasks();
    document.querySelector("#addTask").addEventListener("click", () => {
      const input = document.querySelector("#taskInput");
      const task = input.value.trim();
      if (!task) return;
      state.previewState.tasks.push(task);
      input.value = "";
      renderTasks();
      writeConsole(`Added \"${task}\" to DefaultListModel.`);
    });
    document.querySelector("#taskList").addEventListener("click", (event) => {
      const removeIndex = event.target.dataset.remove;
      if (removeIndex === undefined) return;
      state.previewState.tasks.splice(Number(removeIndex), 1);
      renderTasks();
      writeConsole("Removed the selected task from the model.");
    });
  }
}

function renderPreview() {
  const simulator = window.DesktopSimulator || window.SwingSimulator;
  if (!simulator) {
    renderLegacyPreview();
    return { components: 0, listeners: 0 };
  }

  try {
    const metrics = simulator.render(elements.codeEditor.value, elements.previewCanvas, {
      onConsole: writeConsole,
      language: currentCourse.language,
      languageLabel: currentCourse.languageLabel
    });
    state.previewState.metrics = metrics;
    return metrics;
  } catch (error) {
    elements.consoleBar.classList.add("error");
    elements.consoleOutput.textContent = `Simulator error: ${error.message}`;
    return { components: 0, listeners: 0 };
  }
}

function javaStructureOnly(code) {
  return code
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/\/\/.*$/gm, "")
    .replace(/"(?:\\.|[^"\\])*"/g, '""')
    .replace(/'(?:\\.|[^'\\])*'/g, "''");
}

function checkCode() {
  const code = elements.codeEditor.value;
  const structure = javaStructureOnly(code);
  const pairs = [
    ["{", "}", "brace"],
    ["(", ")", "parenthesis"],
    ["[", "]", "bracket"]
  ];
  const mismatch = pairs.find(([open, close]) =>
    (structure.match(new RegExp(`\\${open}`, "g")) || []).length !==
    (structure.match(new RegExp(`\\${close}`, "g")) || []).length
  );

  if (mismatch) {
    elements.consoleBar.classList.add("error");
    elements.consoleOutput.textContent = `Structure check: an opening and closing ${mismatch[2]} do not match.`;
    showToast(`Check your ${mismatch[2]}s and try again`);
    return;
  }

  if (currentCourse.language === "java" && !/class\s+[A-Za-z_$][\w$]*/.test(code)) {
    elements.consoleBar.classList.add("error");
    elements.consoleOutput.textContent = "Structure check: could not find a Java class declaration.";
    showToast("Add a class before running the simulation");
    return;
  }

  elements.consoleBar.classList.remove("error");
  const metrics = renderPreview();
  const activeLesson = lessons[state.activeLesson];
  const passed = activeLesson.challengeTest(code);
  challengeAttempts += 1;
  challengePassed = passed;
  elements.consoleOutput.textContent = activeLesson.custom && passed
    ? `Answer matched. Expected output: ${activeLesson.expectedOutput}`
    : `Simulation ready: ${metrics.components} component${metrics.components === 1 ? "" : "s"}, ${metrics.listeners} listener${metrics.listeners === 1 ? "" : "s"}.`;
  updateChallenge(passed);
  saveCode();
  showToast(passed ? "Challenge complete — lesson unlocked!" : "Not quite yet — compare your edit with the challenge");
}

function updateChallenge(passed) {
  elements.challengeStatus.classList.toggle("success", passed);
  elements.challengeStatus.innerHTML = passed
    ? '<span class="status-dot"></span> Challenge complete'
    : `<span class="status-dot"></span> ${challengeAttempts ? "Keep experimenting — your preview still ran" : "Waiting for your code"}`;
  updateMasteryChecklist();
}

function updateMasteryChecklist() {
  elements.practiceMasteryStep.classList.toggle("complete", challengePassed);
  elements.completeMasteryStep.classList.toggle("complete", state.completed.has(state.activeLesson));
  elements.completeLesson.disabled = !challengePassed && !state.completed.has(state.activeLesson);
  elements.completeLesson.title = elements.completeLesson.disabled ? "Run the code and pass the quick challenge first" : "";
}

function showChallengeHint() {
  const lesson = lessons[state.activeLesson];
  const codeTargets = [...elements.challengeText.querySelectorAll("code")].map((item) => item.textContent.trim()).filter(Boolean);
  challengeHintLevel = Math.min(challengeHintLevel + 1, 2);
  elements.challengeHintText.hidden = false;
  elements.challengeHintText.textContent = challengeHintLevel === 1
    ? `Start small: find the line connected to ${lesson.tags.slice(0, 2).join(" or ")}, then change only what the challenge asks.`
    : codeTargets.length
      ? `Look for ${codeTargets.map((target) => `“${target}”`).join(" or ")} in the editor. Use Reset if you want a clean starting point, make the requested edit, then run again.`
      : "Use Reset for a clean starting point, reread the challenge one phrase at a time, and run after changing just one line.";
  elements.challengeHint.textContent = challengeHintLevel === 1 ? "Show another hint" : "Hint shown";
}

function writeConsole(message) {
  elements.consoleBar.classList.remove("error");
  elements.consoleOutput.textContent = message;
}

function nearbyLessonValues(getValue, correct, questionOffset) {
  const values = [correct];
  for (let step = 1; values.length < 3 && step < lessons.length + 2; step += 1) {
    const index = (state.activeLesson + questionOffset + step * 7) % lessons.length;
    const value = getValue(lessons[index]);
    if (value && !values.includes(value)) values.push(value);
  }
  const fallbacks = ["A different desktop concept", "An unrelated project step", "A separate toolkit feature"];
  fallbacks.forEach((value) => {
    if (values.length < 3 && !values.includes(value)) values.push(value);
  });
  return values;
}

function quizQuestion(question, correct, distractors, rotation, explanation) {
  const options = [correct, ...distractors.filter((value) => value !== correct)].slice(0, 3);
  while (options.length < 3) options.push(`Alternative ${options.length + 1}`);
  const shift = rotation % options.length;
  const rotated = [...options.slice(shift), ...options.slice(0, shift)];
  return { question, options: rotated, answer: rotated.indexOf(correct), explanation };
}

function buildQuizForLesson(lesson) {
  const api = lesson.tags?.[0] || lesson.navSubtitle || "desktop API";
  const point = (index, part) => lesson.points?.[index]?.[part] || `Lesson idea ${index + 1}`;
  const specs = [
    ["Which topic is the focus of this lesson?", lesson.title, (candidate) => candidate.title, `The lesson title is “${lesson.title},” and its examples reinforce that topic.`],
    [`Which API or concept is central to this ${currentCourse.shortTitle} lesson?`, api, (candidate) => candidate.tags?.[0] || candidate.navSubtitle, `${api} is the primary API named in this lesson's heading and code example.`],
    ["Which coding challenge belongs to this lesson?", lesson.challengeTitle, (candidate) => candidate.challengeTitle, `The practice task is “${lesson.challengeTitle}.” It checks the edit described above the simulator.`],
    ["Which curriculum module contains this lesson?", lesson.module, (candidate) => candidate.module, `This lesson appears in the “${lesson.module}” module.`],
    ["What is the first mental-model heading for this lesson?", point(0, 0), (candidate) => candidate.points?.[0]?.[0], `The first key idea is “${point(0, 0)}”: ${point(0, 1)}`],
    ["What is the second mental-model heading?", point(1, 0), (candidate) => candidate.points?.[1]?.[0], `The second key idea is “${point(1, 0)}”: ${point(1, 1)}`],
    ["What is the third mental-model heading?", point(2, 0), (candidate) => candidate.points?.[2]?.[0], `The third key idea is “${point(2, 0)}”: ${point(2, 1)}`],
    ["What is this lesson's estimated practice time?", lesson.time, (candidate) => candidate.time, `The lesson header estimates ${lesson.time} for this activity.`],
    ["Which short navigation title opens this lesson?", lesson.navTitle, (candidate) => candidate.navTitle, `“${lesson.navTitle}” is the title shown in the lesson navigator.`],
    ["Which subtitle identifies this lesson in the navigator?", lesson.navSubtitle, (candidate) => candidate.navSubtitle, `The navigator pairs this lesson with the subtitle “${lesson.navSubtitle}.”`],
    ["Which big-idea heading belongs to this lesson?", lesson.conceptTitle, (candidate) => candidate.conceptTitle, `The lesson introduces its explanation under “${lesson.conceptTitle}.”`],
    ["Which language or toolkit tag is attached to this lesson?", lesson.tags?.[1] || currentCourse.languageLabel, (candidate) => candidate.tags?.[1], `The lesson belongs to the ${currentCourse.languageLabel} track.`],
    ["Which project or practice tag appears on this lesson?", lesson.tags?.[2] || "Desktop", (candidate) => candidate.tags?.[2], `“${lesson.tags?.[2] || "Desktop"}” is the supporting practice tag shown beside the lesson.`],
    ["Which summary describes the active lesson?", lesson.description, (candidate) => candidate.description, `This summary matches the goal and example presented in the active lesson.`],
    ["Which statement explains the first key idea?", point(0, 1), (candidate) => candidate.points?.[0]?.[1], `This is the explanation paired with the “${point(0, 0)}” heading.`],
    ["Which statement explains the second key idea?", point(1, 1), (candidate) => candidate.points?.[1]?.[1], `This is the explanation paired with the “${point(1, 0)}” heading.`],
    ["Which statement explains the third key idea?", point(2, 1), (candidate) => candidate.points?.[2]?.[1], `This is the explanation paired with the “${point(2, 0)}” heading.`],
    ["Which action best follows an edit in the code playground?", "Run the code and inspect the simulated desktop window", () => "", "Running the edited code gives immediate visual feedback and lets you check the challenge."],
    ["Which course are you currently practicing?", currentCourse.title, () => "", `This lesson is part of ${currentCourse.title}.`],
    ["What should a focused desktop event callback do?", "Read current input, update state, and show useful feedback", () => "", "A focused callback reads what it needs, changes application state, and reflects the result without blocking the interface."]
  ];

  return specs.map(([question, correct, getter, explanation], index) => {
    let distractors;
    if (index === 17) {
      distractors = ["Close the simulator before checking the result", "Rewrite every lesson before running anything"];
    } else if (index === 18) {
      distractors = courses.filter((course) => course.id !== currentCourse.id).slice(0, 2).map((course) => course.title);
    } else if (index === 19) {
      distractors = ["Block the UI thread until every task finishes", "Ignore current input and change unrelated controls"];
    } else {
      distractors = nearbyLessonValues(getter, correct, index + 1).slice(1);
    }
    return quizQuestion(question, correct, distractors, state.activeLesson + index, explanation);
  });
}

function renderQuiz() {
  activeQuiz = buildQuizForLesson(lessons[state.activeLesson]);
  quizIndex = 0;
  quizScore = 0;
  quizAnswered = false;
  quizFinished = false;
  renderQuizQuestion();
}

function renderQuizQuestion() {
  const item = activeQuiz[quizIndex];
  elements.quizProgress.textContent = `QUESTION ${quizIndex + 1} OF ${activeQuiz.length} · ${quizScore} CORRECT`;
  elements.quizQuestions.innerHTML = `
    <div class="quiz-question">
      <h3>${quizIndex + 1}. ${escapeHtml(plainLanguage(item.question))}</h3>
      <div class="quiz-options">
        ${item.options
          .map(
            (option, optionIndex) => `
              <label class="quiz-option" data-quiz-option="${optionIndex}">
                <input type="radio" name="quiz-current" value="${optionIndex}" />
                <span>${escapeHtml(plainLanguage(option))}</span>
              </label>`
          )
          .join("")}
      </div>
    </div>`;
  elements.quizResult.textContent = "";
  elements.quizResult.className = "quiz-result";
  elements.checkQuiz.hidden = false;
  elements.checkQuiz.disabled = false;
  elements.quizNext.hidden = true;
  elements.quizNext.textContent = quizIndex === activeQuiz.length - 1 ? "Finish quiz" : "Next question";
}

function checkQuiz() {
  if (quizAnswered || quizFinished) return;
  const selected = document.querySelector('input[name="quiz-current"]:checked');
  if (!selected) {
    elements.quizResult.textContent = "Choose an answer before checking.";
    return;
  }
  const item = activeQuiz[quizIndex];
  const selectedIndex = Number(selected.value);
  const correct = selectedIndex === item.answer;
  if (correct) quizScore += 1;
  quizAnswered = true;
  document.querySelectorAll('[data-quiz-option]').forEach((option) => {
    const optionIndex = Number(option.dataset.quizOption);
    option.classList.toggle("correct", optionIndex === item.answer);
    option.classList.toggle("wrong", optionIndex === selectedIndex && !correct);
    option.querySelector("input").disabled = true;
  });
  elements.quizResult.className = `quiz-result ${correct ? "correct" : "incorrect"}`;
  elements.quizResult.innerHTML = correct
    ? `<strong>Correct.</strong> ${escapeHtml(plainLanguage(item.explanation))}`
    : `<strong>Not quite. The correct answer is “${escapeHtml(plainLanguage(item.options[item.answer]))}.”</strong><span>${escapeHtml(plainLanguage(item.explanation))}</span>`;
  elements.quizProgress.textContent = `QUESTION ${quizIndex + 1} OF ${activeQuiz.length} · ${quizScore} CORRECT`;
  elements.checkQuiz.hidden = true;
  elements.quizNext.hidden = false;
}

function nextQuizQuestion() {
  if (quizFinished) {
    renderQuiz();
    return;
  }
  if (!quizAnswered) return;
  if (quizIndex < activeQuiz.length - 1) {
    quizIndex += 1;
    quizAnswered = false;
    renderQuizQuestion();
    return;
  }

  quizFinished = true;
  const perfect = quizScore === activeQuiz.length;
  elements.quizProgress.textContent = `QUIZ COMPLETE · ${quizScore} OF ${activeQuiz.length} CORRECT`;
  const mastered = perfect && challengePassed;
  elements.quizQuestions.innerHTML = `<div class="quiz-summary"><strong>${quizScore} / ${activeQuiz.length}</strong><p>${mastered ? "Perfect score—this lesson is complete." : perfect ? "Perfect score. Now pass the quick challenge to finish the lesson." : "Review the explanations, then retake the quiz when you are ready."}</p></div>`;
  elements.quizResult.textContent = mastered ? "Excellent work. You answered every question correctly." : perfect ? "Your knowledge check is complete; finish the hands-on practice next." : `${activeQuiz.length - quizScore} answer${activeQuiz.length - quizScore === 1 ? "" : "s"} to revisit.`;
  elements.quizResult.className = `quiz-result ${perfect ? "correct" : "incorrect"}`;
  elements.checkQuiz.hidden = true;
  elements.quizNext.hidden = false;
  elements.quizNext.textContent = "Retake quiz";
  window.DesktopcraftAuth?.saveQuizAttempt({
    courseId: state.courseId,
    lessonIndex: state.activeLesson,
    score: quizScore
  });
  if (mastered) {
    state.completed.add(state.activeLesson);
    saveProgress();
    renderNavigation();
    updateProgress();
    elements.completeLesson.innerHTML = `Completed <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 12 4 4 8-9" /></svg>`;
    updateMasteryChecklist();
    createSparks(elements.quizNext);
  }
}

function setLesson(index) {
  saveCode();
  state.activeLesson = Math.min(Math.max(index, 0), lessons.length - 1);
  renderLesson();
  window.requestAnimationFrame(() => {
    elements.lessonNav.querySelector('[aria-current="step"]')?.scrollIntoView({ block: "nearest" });
  });
  window.scrollTo({ top: 0, behavior: "smooth" });
  closeSidebar();
}

function completeLesson() {
  const isAlreadyCompleted = state.completed.has(state.activeLesson);
  if (!isAlreadyCompleted && !challengePassed) {
    showToast("Pass the quick challenge to unlock completion");
    document.querySelector("#runCode")?.focus();
    return;
  }
  if (!isAlreadyCompleted) {
    state.completed.add(state.activeLesson);
    createSparks(elements.completeLesson);
    showToast(state.activeLesson === lessons.length - 1 ? "Course complete! You built the foundation." : "Lesson complete — onward!");
  }

  saveProgress();
  renderNavigation();
  updateProgress();

  if (state.activeLesson < lessons.length - 1) {
    window.setTimeout(() => setLesson(state.activeLesson + 1), isAlreadyCompleted ? 0 : 400);
  } else {
    elements.completeLesson.innerHTML = `Completed <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 12 4 4 8-9" /></svg>`;
  }
}

function createSparks(target) {
  const box = target.getBoundingClientRect();
  for (let index = 0; index < 12; index += 1) {
    const spark = document.createElement("span");
    spark.className = "completion-spark";
    spark.style.left = `${box.left + box.width / 2}px`;
    spark.style.top = `${box.top + box.height / 2}px`;
    spark.style.background = index % 2 ? "#ee945d" : "#d8ef72";
    spark.style.setProperty("--x", `${Math.cos((index / 12) * Math.PI * 2) * (45 + Math.random() * 25)}px`);
    spark.style.setProperty("--y", `${Math.sin((index / 12) * Math.PI * 2) * (45 + Math.random() * 25)}px`);
    document.body.appendChild(spark);
    window.setTimeout(() => spark.remove(), 900);
  }
}

let toastTimeout;
function showToast(message) {
  window.clearTimeout(toastTimeout);
  elements.toast.textContent = message;
  elements.toast.classList.add("show");
  toastTimeout = window.setTimeout(() => elements.toast.classList.remove("show"), 2200);
}

function openSidebar() {
  elements.sidebar.classList.add("open");
  elements.sidebarScrim.hidden = false;
}

function closeSidebar() {
  elements.sidebar.classList.remove("open");
  elements.sidebarScrim.hidden = true;
}

elements.lessonNav.addEventListener("click", (event) => {
  const button = event.target.closest("[data-lesson]");
  if (button) setLesson(Number(button.dataset.lesson));
});

elements.lessonSearch.addEventListener("input", (event) => {
  state.searchQuery = event.target.value;
  renderNavigation();
});

elements.lessonSearch.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    elements.lessonSearch.value = "";
    state.searchQuery = "";
    renderNavigation();
    elements.lessonSearch.blur();
  }
});

elements.codeEditor.addEventListener("input", () => {
  updateLineNumbers();
  state.savedCode[state.activeLesson] = elements.codeEditor.value;
  updateChallenge(false);
});

elements.codeEditor.addEventListener("scroll", () => {
  elements.lineNumbers.scrollTop = elements.codeEditor.scrollTop;
});

elements.codeEditor.addEventListener("keydown", (event) => {
  if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
    event.preventDefault();
    checkCode();
  }
  if (event.key === "Tab") {
    event.preventDefault();
    const start = elements.codeEditor.selectionStart;
    const end = elements.codeEditor.selectionEnd;
    elements.codeEditor.value = `${elements.codeEditor.value.slice(0, start)}    ${elements.codeEditor.value.slice(end)}`;
    elements.codeEditor.selectionStart = elements.codeEditor.selectionEnd = start + 4;
    updateLineNumbers();
  }
});

document.querySelector("#runCode").addEventListener("click", checkCode);
document.querySelector("#resetCode").addEventListener("click", () => {
  elements.codeEditor.value = lessons[state.activeLesson].code;
  delete state.savedCode[state.activeLesson];
  try {
    localStorage.setItem(codeStorageKey(state.courseId), JSON.stringify(state.savedCode));
  } catch {
    // The reset has still applied in memory.
  }
  updateLineNumbers();
  updateChallenge(false);
  renderPreview();
  writeConsole("Starter code restored.");
  showToast("Code reset to the lesson starter");
});

document.querySelector("#copyCode").addEventListener("click", async () => {
  try {
    await navigator.clipboard.writeText(elements.codeEditor.value);
    showToast(`${currentCourse.languageLabel} code copied`);
  } catch {
    elements.codeEditor.select();
    document.execCommand("copy");
    showToast(`${currentCourse.languageLabel} code copied`);
  }
});

document.querySelector("#menuButton").addEventListener("click", openSidebar);
document.querySelector("#sidebarClose").addEventListener("click", closeSidebar);
elements.createLessonButton.addEventListener("click", openLessonCreator);
document.querySelector("#closeLessonCreator").addEventListener("click", () => elements.lessonCreator.close());
document.querySelector("#cancelLessonCreator").addEventListener("click", () => elements.lessonCreator.close());
elements.lessonCreatorForm.addEventListener("submit", publishCustomLesson);
elements.lessonCreator.addEventListener("click", (event) => {
  if (event.target === elements.lessonCreator) elements.lessonCreator.close();
});
elements.courseSelect.addEventListener("change", (event) => switchCourse(event.target.value));
elements.sidebarScrim.addEventListener("click", closeSidebar);
elements.previousLesson.addEventListener("click", () => setLesson(state.activeLesson - 1));
elements.completeLesson.addEventListener("click", completeLesson);
elements.challengeHint.addEventListener("click", showChallengeHint);
elements.checkQuiz.addEventListener("click", checkQuiz);
elements.quizNext.addEventListener("click", nextQuizQuestion);
elements.accountButton.addEventListener("click", (event) => {
  event.stopPropagation();
  const willOpen = elements.accountMenu.hidden;
  elements.accountMenu.hidden = !willOpen;
  elements.accountButton.setAttribute("aria-expanded", String(willOpen));
});
elements.accountMenu.addEventListener("click", (event) => event.stopPropagation());
elements.signOutButton.addEventListener("click", () => {
  window.DesktopcraftAuth?.signOut();
  elements.accountMenu.hidden = true;
  elements.accountButton.setAttribute("aria-expanded", "false");
  renderAccount();
  showToast("Signed out. Your course progress is saved.");
});
document.addEventListener("click", () => {
  elements.accountMenu.hidden = true;
  elements.accountButton.setAttribute("aria-expanded", "false");
});
elements.simulatorHelp.addEventListener("click", () => {
  const willOpen = elements.simulatorHelpPanel.hidden;
  elements.simulatorHelpPanel.hidden = !willOpen;
  elements.simulatorHelp.setAttribute("aria-expanded", String(willOpen));
});

document.addEventListener("keydown", (event) => {
  if (event.key === "/" && !/INPUT|TEXTAREA|SELECT/.test(document.activeElement.tagName)) {
    event.preventDefault();
    openSidebar();
    elements.lessonSearch.focus();
  }
});

document.addEventListener("desktopcraft:customizationchange", (event) => {
  if (event.detail?.explanationLevel === "detailed") elements.codeWalkthrough.open = true;
  else if (event.detail?.explanationLevel === "concise") elements.codeWalkthrough.open = false;
});

document.querySelector("#resetProgress").addEventListener("click", () => {
  const shouldReset = window.confirm(`Reset your ${currentCourse.shortTitle} progress and edited code?`);
  if (!shouldReset) return;
  state.completed.clear();
  state.savedCode = {};
  state.activeLesson = 0;
  try {
    localStorage.removeItem(progressStorageKey(state.courseId));
    localStorage.removeItem(codeStorageKey(state.courseId));
  } catch {
    // Continue resetting the current session if storage is unavailable.
  }
  renderLesson();
  showToast("Course progress reset");
});

loadState();
renderCourseChrome();
renderAccount();
updateLessonCreatorState();
renderLesson();
void syncProgressFromDatabase();
