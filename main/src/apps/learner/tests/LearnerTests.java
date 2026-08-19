import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import lessons.Curriculum;
import lessons.Lesson;
import lessons.LessonCatalog;
import profile.Profile;
import quiz.QuizManager;
import storage.SaveManager;

public final class LearnerTests {
  private static int passed;

  public static void main(String[] args) throws Exception {
    LessonCatalog catalog = new LessonCatalog();
    check("Lesson count", catalog.size() == 10_000);
    for (String subject : Curriculum.SUBJECTS)
      check(subject + " lesson count", catalog.countForSubject(subject) == 1_000);
    check("Lesson IDs", catalog.byId(1).id() == 1 && catalog.byId(10_000).id() == 10_000);
    check("Topic progression", !catalog.byId(1).topic().equals(catalog.byId(101).topic()));
    check(
        "Difficulty progression",
        catalog.byId(1).difficulty() == Lesson.Difficulty.BEGINNER
            && catalog.byId(900).difficulty() == Lesson.Difficulty.ADVANCED);
    check(
        "Search by topic",
        catalog.search("", "Math", "Fractions", "All", "All", Set.of()).size() == 100);
    check("Search by ID", catalog.search("245", "All", "All", "All", "All", Set.of()).size() == 1);

    Profile profile = new Profile("Test Learner");
    check("First completion", profile.completeLesson(1, "Math") && profile.points() == 10);
    check(
        "Duplicate completion blocked",
        !profile.completeLesson(1, "Math") && profile.points() == 10);
    check(
        "Quiz score",
        profile.recordQuiz("Math", 80) == 160 && profile.bestQuizScores().get("Math") == 80);
    check("Quiz farming blocked", profile.recordQuiz("Math", 80) == 0);
    check("Quiz improvement only", profile.recordQuiz("Math", 90) == 20);
    check("Dynamic quiz", new QuizManager().create("Math", 900).size() == 5);

    Path temp = Files.createTempDirectory("learner-test-");
    SaveManager saves = new SaveManager(temp);
    profile.toggleFavorite(1);
    profile.viewLesson(245, "Math");
    saves.saveProfile(profile);
    Profile restored = saves.loadProfile("Test Learner");
    check("Save/load points", restored.points() == profile.points());
    check("Save/load completion", restored.completedLessonIds().contains(1));
    check("Save/load favorite", restored.favoriteLessonIds().contains(1));
    check("Save/load current lesson", restored.currentLessonId() == 245);
    Files.walk(temp)
        .sorted(java.util.Comparator.reverseOrder())
        .forEach(
            path -> {
              try {
                Files.delete(path);
              } catch (java.io.IOException ex) {
                throw new RuntimeException(ex);
              }
            });

    boolean invalidHandled = false;
    try {
      catalog.byId(0);
    } catch (IllegalArgumentException ex) {
      invalidHandled = true;
    }
    check("Invalid ID validation", invalidHandled);
    System.out.println("\nAll " + passed + " tests passed.");
  }

  private static void check(String name, boolean condition) {
    if (!condition) throw new AssertionError("[FAIL] " + name);
    passed++;
    System.out.println("[PASS] " + name);
  }
}
