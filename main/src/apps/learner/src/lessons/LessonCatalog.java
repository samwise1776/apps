package lessons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class LessonCatalog {
  public static final int TOTAL = 10_000;
  private final List<Lesson> lessons;

  public LessonCatalog() {
    LessonGenerator generator = new LessonGenerator();
    List<Lesson> generated = new ArrayList<>(TOTAL);
    int id = 1;
    for (String subject : Curriculum.SUBJECTS) {
      for (int level = 1; level <= 1000; level++)
        generated.add(generator.generate(id++, subject, level));
    }
    lessons = Collections.unmodifiableList(generated);
  }

  public int size() {
    return lessons.size();
  }

  public Lesson byId(int id) {
    if (id < 1 || id > lessons.size())
      throw new IllegalArgumentException("Lesson ID must be 1-" + lessons.size());
    return lessons.get(id - 1);
  }

  public List<Lesson> search(
      String query,
      String subject,
      String topic,
      String difficulty,
      String completion,
      Set<Integer> completed) {
    String q = query == null ? "" : query.trim().toLowerCase();
    List<Lesson> result = new ArrayList<>();
    for (Lesson lesson : lessons) {
      if (!q.isEmpty() && !lesson.contains(q)) continue;
      if (!"All".equals(subject) && !lesson.subject().equals(subject)) continue;
      if (!"All".equals(topic) && !lesson.topic().equals(topic)) continue;
      if (!"All".equals(difficulty) && !lesson.difficulty().name().equals(difficulty.toUpperCase()))
        continue;
      boolean done = completed.contains(lesson.id());
      if ("Completed".equals(completion) && !done) continue;
      if ("Not Completed".equals(completion) && done) continue;
      result.add(lesson);
    }
    return result;
  }

  public int countForSubject(String subject) {
    Curriculum.subjectIndex(subject);
    return 1000;
  }
}
