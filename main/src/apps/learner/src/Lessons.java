import java.util.List;
import java.util.Set;
import lessons.Curriculum;
import lessons.Lesson;
import lessons.LessonCatalog;

/**
 * Compatibility entry point retained for older code that referenced Lessons. New code should use
 * {@link LessonCatalog} directly.
 */
@Deprecated
public final class Lessons {
  public static final int TOTAL_LESSONS = LessonCatalog.TOTAL;
  private final LessonCatalog catalog = new LessonCatalog();

  public int size() {
    return catalog.size();
  }

  public Lesson get(int index) {
    return catalog.byId(index + 1);
  }

  public Lesson byId(int id) {
    return catalog.byId(id);
  }

  public int countForSubject(String subject) {
    return catalog.countForSubject(subject);
  }

  public List<String> subjects() {
    return Curriculum.SUBJECTS;
  }

  public List<Lesson> search(String query, String subject) {
    return catalog.search(query, subject, "All", "All", "All", Set.of());
  }
}
