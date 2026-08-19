package storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import profile.AppSettings;
import profile.Profile;

public final class SaveManager {
  private final Path directory;

  public SaveManager() {
    this(Path.of(System.getProperty("user.home"), ".learner"));
  }

  public SaveManager(Path directory) {
    this.directory = directory;
  }

  public List<String> profileNames() {
    Path index = directory.resolve("profiles.properties");
    Properties p = loadProperties(index);
    String value = p.getProperty("names", "");
    List<String> result = new ArrayList<>();
    for (String item : value.split("\\|")) if (!item.isBlank()) result.add(item);
    return result;
  }

  public Profile loadProfile(String name) {
    Profile profile = new Profile(name);
    Properties p = loadProperties(profilePath(name));
    profile.restorePoints(integer(p, "points", 0));
    int savedCurrentLesson = integer(p, "currentLesson", 1);
    readInts(p.getProperty("completed", ""), profile.completedLessonIds()::add);
    readInts(p.getProperty("favorites", ""), profile.favoriteLessonIds()::add);
    readInts(p.getProperty("history", ""), id -> profile.viewLesson(id, subjectFor(id)));
    readStrings(p.getProperty("achievements", ""), profile.achievements()::add);
    readStrings(p.getProperty("openedSubjects", ""), profile.openedSubjects()::add);
    readMap(p.getProperty("best", ""), profile.bestQuizScores());
    readMap(p.getProperty("attempts", ""), profile.quizAttempts());
    readMap(p.getProperty("totals", ""), profile.quizScoreTotals());
    profile.restoreCurrentLesson(savedCurrentLesson);
    return profile;
  }

  public void saveProfile(Profile profile) throws IOException {
    Files.createDirectories(directory);
    Properties p = new Properties();
    p.setProperty("name", profile.name());
    p.setProperty("points", Integer.toString(profile.points()));
    p.setProperty("currentLesson", Integer.toString(profile.currentLessonId()));
    p.setProperty("completed", join(profile.completedLessonIds()));
    p.setProperty("favorites", join(profile.favoriteLessonIds()));
    p.setProperty("history", join(profile.history()));
    p.setProperty("achievements", String.join("|", profile.achievements()));
    p.setProperty("openedSubjects", String.join("|", profile.openedSubjects()));
    p.setProperty("best", joinMap(profile.bestQuizScores()));
    p.setProperty("attempts", joinMap(profile.quizAttempts()));
    p.setProperty("totals", joinMap(profile.quizScoreTotals()));
    atomicStore(profilePath(profile.name()), p, "Learner profile");
    List<String> names = profileNames();
    if (!names.contains(profile.name())) names.add(profile.name());
    Properties index = new Properties();
    index.setProperty("names", String.join("|", names));
    atomicStore(directory.resolve("profiles.properties"), index, "Learner profiles");
  }

  public AppSettings loadSettings() {
    Properties p = loadProperties(directory.resolve("settings.properties"));
    AppSettings settings = new AppSettings();
    settings.setFontSize(integer(p, "fontSize", 18));
    settings.setAnimation(Boolean.parseBoolean(p.getProperty("animation", "true")));
    settings.setSound(Boolean.parseBoolean(p.getProperty("sound", "false")));
    return settings;
  }

  public void saveSettings(AppSettings settings) throws IOException {
    Properties p = new Properties();
    p.setProperty("fontSize", Integer.toString(settings.fontSize()));
    p.setProperty("animation", Boolean.toString(settings.animation()));
    p.setProperty("sound", Boolean.toString(settings.sound()));
    atomicStore(directory.resolve("settings.properties"), p, "Learner settings");
  }

  public void deleteProfile(Profile profile) throws IOException {
    Files.deleteIfExists(profilePath(profile.name()));
    List<String> names = profileNames();
    names.remove(profile.name());
    Properties index = new Properties();
    index.setProperty("names", String.join("|", names));
    atomicStore(directory.resolve("profiles.properties"), index, "Learner profiles");
  }

  private Path profilePath(String name) {
    return directory.resolve("profile-" + name.replaceAll("[^A-Za-z0-9_-]", "_") + ".properties");
  }

  private Properties loadProperties(Path file) {
    Properties p = new Properties();
    if (!Files.exists(file)) return p;
    try (InputStream in = Files.newInputStream(file)) {
      p.load(in);
    } catch (IOException | IllegalArgumentException ex) {
      System.err.println("Could not load " + file + ": " + ex.getMessage());
    }
    return p;
  }

  private void atomicStore(Path file, Properties p, String comment) throws IOException {
    Files.createDirectories(directory);
    Path temp = Files.createTempFile(directory, "save-", ".tmp");
    try (OutputStream out = Files.newOutputStream(temp)) {
      p.store(out, comment);
    }
    try {
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private int integer(Properties p, String key, int fallback) {
    try {
      return Math.max(0, Integer.parseInt(p.getProperty(key, Integer.toString(fallback))));
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private void readInts(String text, Consumer<Integer> target) {
    for (String value : text.split(","))
      try {
        if (!value.isBlank()) {
          int id = Integer.parseInt(value);
          if (id >= 1 && id <= 10_000) target.accept(id);
        }
      } catch (NumberFormatException ignored) {
        System.err.println("Ignored invalid saved lesson ID: " + value);
      }
  }

  private void readStrings(String text, Consumer<String> target) {
    for (String value : text.split("\\|")) if (!value.isBlank()) target.accept(value);
  }

  private void readMap(String text, java.util.Map<String, Integer> target) {
    for (String item : text.split("\\|")) {
      int cut = item.lastIndexOf('=');
      if (cut > 0)
        try {
          target.put(item.substring(0, cut), Integer.parseInt(item.substring(cut + 1)));
        } catch (NumberFormatException ignored) {
          System.err.println("Ignored invalid saved score: " + item);
        }
    }
  }

  private String join(Iterable<?> values) {
    StringBuilder b = new StringBuilder();
    for (Object v : values) {
      if (b.length() > 0) b.append(',');
      b.append(v);
    }
    return b.toString();
  }

  private String joinMap(java.util.Map<String, Integer> map) {
    StringBuilder b = new StringBuilder();
    for (var e : map.entrySet()) {
      if (b.length() > 0) b.append('|');
      b.append(e.getKey()).append('=').append(e.getValue());
    }
    return b.toString();
  }

  private String subjectFor(int id) {
    return lessons.Curriculum.SUBJECTS.get((id - 1) / 1000);
  }
}
