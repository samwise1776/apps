package lessons;

public final class Lesson {
  public enum Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
  }

  private final int id;
  private final int level;
  private final String subject;
  private final String topic;
  private final Difficulty difficulty;
  private final String title;
  private final String goal;
  private final String explanation;
  private final String example;
  private final String practice;
  private final String challenge;
  private final String tip;

  public Lesson(
      int id,
      int level,
      String subject,
      String topic,
      Difficulty difficulty,
      String title,
      String goal,
      String explanation,
      String example,
      String practice,
      String challenge,
      String tip) {
    this.id = id;
    this.level = level;
    this.subject = subject;
    this.topic = topic;
    this.difficulty = difficulty;
    this.title = title;
    this.goal = goal;
    this.explanation = explanation;
    this.example = example;
    this.practice = practice;
    this.challenge = challenge;
    this.tip = tip;
  }

  public int id() {
    return id;
  }

  public int level() {
    return level;
  }

  public String subject() {
    return subject;
  }

  public String topic() {
    return topic;
  }

  public Difficulty difficulty() {
    return difficulty;
  }

  public String title() {
    return title;
  }

  public String goal() {
    return goal;
  }

  public String displayText() {
    return "Lesson #"
        + id
        + "\n\nSubject: "
        + subject
        + "\nTopic: "
        + topic
        + "\nDifficulty: "
        + pretty(difficulty.name())
        + "\n\nGoal\n"
        + goal
        + "\n\nExplanation\n"
        + explanation
        + "\n\nExample\n"
        + example
        + "\n\nPractice\n"
        + practice
        + "\n\nChallenge\n"
        + challenge
        + "\n\nNernal's Tip\n"
        + tip;
  }

  public boolean contains(String query) {
    String q = query.toLowerCase();
    return Integer.toString(id).equals(q)
        || title.toLowerCase().contains(q)
        || subject.toLowerCase().contains(q)
        || topic.toLowerCase().contains(q)
        || goal.toLowerCase().contains(q)
        || explanation.toLowerCase().contains(q);
  }

  private static String pretty(String value) {
    return value.substring(0, 1) + value.substring(1).toLowerCase();
  }

  @Override
  public String toString() {
    return String.format("#%,d  %-12s  %-24s  %s", id, subject, topic, title);
  }
}
