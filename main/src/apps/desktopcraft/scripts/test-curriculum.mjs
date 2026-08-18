import vm from "node:vm";
import { readFile } from "node:fs/promises";

const window = {};
const context = vm.createContext({ window });
for (const file of ["lessons-extra.js", "desktop-courses.js", "language-courses.js", "curriculum-expansion.js"]) {
  vm.runInContext(await readFile(file, "utf8"), context, { filename: file });
}

const swing = window.buildSwingcraftExtraLessons();
const desktopCourses = window.expandDesktopcraftCourses(window.buildDesktopCourses());
const failures = [];
const assert = (condition, message) => { if (!condition) failures.push(message); };
const expectedLanguages = new Set([
  "python", "csharp", "cpp", "javascript", "kotlin", "rust", "go", "dart",
  "swift", "ruby", "php", "typescript", "lua", "racket"
]);

assert(swing.length >= 100, `Swing curriculum has only ${swing.length} expanded lessons`);
assert(desktopCourses.length === 14, `expected 14 non-Swing courses, found ${desktopCourses.length}`);
assert(new Set(desktopCourses.map((course) => course.id)).size === desktopCourses.length, "course IDs are not unique");
assert(desktopCourses.every((course) => expectedLanguages.has(course.language)), "course language identifier is invalid");
assert(desktopCourses.every((course) => course.lessons.length === 500), "every non-Swing course must contain exactly 500 lessons");

for (const course of desktopCourses) {
  const titles = new Set();
  course.lessons.forEach((lesson, index) => {
    const where = `${course.id} lesson ${index + 1}`;
    assert(typeof lesson.title === "string" && lesson.title.length >= 12, `${where} has a weak title`);
    assert(typeof lesson.description === "string" && lesson.description.length >= 60, `${where} has a weak description`);
    assert(Array.isArray(lesson.conceptBody) && lesson.conceptBody.length >= 2, `${where} lacks concept teaching`);
    assert(Array.isArray(lesson.points) && lesson.points.length >= 3, `${where} lacks learning points`);
    assert(typeof lesson.code === "string" && lesson.code.length >= 120, `${where} lacks a useful code example`);
    assert(typeof lesson.challengeTest === "function" && typeof lesson.challengeSolution === "function", `${where} lacks an executable challenge`);
    const solved = lesson.challengeSolution(lesson.code);
    assert(lesson.challengeTest(solved), `${where} solution does not pass its challenge`);
    assert(!titles.has(lesson.title), `${where} duplicates a lesson title`);
    titles.add(lesson.title);
  });
}

for (const [index, lesson] of swing.entries()) {
  assert(lesson.challengeTest(lesson.challengeSolution(lesson.code)), `Swing expanded lesson ${index + 1} has an invalid solution`);
}

if (failures.length) throw new Error(`Curriculum audit failed:\n${failures.slice(0, 50).join("\n")}`);
console.log(`Verified ${desktopCourses.reduce((sum, course) => sum + course.lessons.length, 0) + swing.length} expanded lessons: unique course structure, teaching depth, code examples, and solvable challenges.`);
