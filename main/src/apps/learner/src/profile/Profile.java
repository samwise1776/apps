package profile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lessons.Curriculum;

public final class Profile {
  private String name;
  private int points;
  private int currentLessonId = 1;
  private final Set<Integer> completed = new HashSet<>();
  private final Set<Integer> favorites = new HashSet<>();
  private final Set<String> achievements = new HashSet<>();
  private final Set<String> openedSubjects = new HashSet<>();
  private final Map<String, Integer> bestQuizScores = new HashMap<>();
  private final Map<String, Integer> quizAttempts = new HashMap<>();
  private final Map<String, Integer> quizScoreTotals = new HashMap<>();
  private final Deque<Integer> history = new ArrayDeque<>();

  public Profile(String name) {
    rename(name);
  }

  public String name() {
    return name;
  }

  public int points() {
    return points;
  }

  public int currentLessonId() {
    return currentLessonId;
  }

  public Set<Integer> completedLessonIds() {
    return completed;
  }

  public Set<Integer> favoriteLessonIds() {
    return favorites;
  }

  public Set<String> achievements() {
    return achievements;
  }

  public Map<String, Integer> bestQuizScores() {
    return bestQuizScores;
  }

  public Map<String, Integer> quizAttempts() {
    return quizAttempts;
  }

  public Map<String, Integer> quizScoreTotals() {
    return quizScoreTotals;
  }

  public Set<String> openedSubjects() {
    return openedSubjects;
  }

  public List<Integer> history() {
    return new ArrayList<>(history);
  }

  public void rename(String value) {
    String clean = value == null ? "" : value.trim();
    if (!clean.matches("[A-Za-z0-9 _-]{1,24}"))
      throw new IllegalArgumentException("Name must be 1-24 letters, numbers, spaces, _ or -.");
    name = clean;
  }

  public boolean completeLesson(int id, String subject) {
    validateLesson(id);
    currentLessonId = id;
    if (!completed.add(id)) return false;
    points += 10;
    openedSubjects.add(subject);
    updateAchievements();
    return true;
  }

  public int recordQuiz(String subject, int percent) {
    Curriculum.subjectIndex(subject);
    if (percent < 0 || percent > 100) throw new IllegalArgumentException("Score must be 0-100");
    int oldBest = bestQuizScores.getOrDefault(subject, 0);
    int improvement = Math.max(0, percent - oldBest);
    points += improvement * 2;
    bestQuizScores.put(subject, Math.max(oldBest, percent));
    quizAttempts.merge(subject, 1, Integer::sum);
    quizScoreTotals.merge(subject, percent, Integer::sum);
    if (percent == 100) achievements.add("Perfect!");
    updateAchievements();
    return improvement * 2;
  }

  public void viewLesson(int id, String subject) {
    validateLesson(id);
    currentLessonId = id;
    openedSubjects.add(subject);
    history.remove(id);
    history.addFirst(id);
    while (history.size() > 30) history.removeLast();
    updateAchievements();
  }

  public void toggleFavorite(int id) {
    validateLesson(id);
    if (!favorites.add(id)) favorites.remove(id);
  }

  public int completedForSubject(String subject) {
    int start = Curriculum.subjectIndex(subject) * 1000 + 1;
    int end = start + 999;
    return (int) completed.stream().filter(id -> id >= start && id <= end).count();
  }

  public int averageQuizScore() {
    int attempts = quizAttempts.values().stream().mapToInt(Integer::intValue).sum();
    int total = quizScoreTotals.values().stream().mapToInt(Integer::intValue).sum();
    return attempts == 0 ? 0 : Math.round((float) total / attempts);
  }

  public int level() {
    return 1 + (int) Math.sqrt(points / 250.0);
  }

  public int nextLevelPoints() {
    int n = level();
    return n * n * 250;
  }

  public void restorePoints(int value) {
    points = Math.max(0, value);
  }

  public void restoreCurrentLesson(int value) {
    if (value >= 1 && value <= 10_000) currentLessonId = value;
  }

  private void updateAchievements() {
    if (!completed.isEmpty()) achievements.add("First Step");
    if (completedForSubject("Math") >= 10) achievements.add("Math Starter");
    if (completedForSubject("Coding") >= 25) achievements.add("Coder");
    if (openedSubjects.size() == 10) achievements.add("Explorer");
    if (completed.size() >= 100) achievements.add("Century");
    if (completed.size() >= 500) achievements.add("Dedicated Learner");
  }

  private void validateLesson(int id) {
    if (id < 1 || id > 10_000) throw new IllegalArgumentException("Invalid lesson ID: " + id);
  }
}
