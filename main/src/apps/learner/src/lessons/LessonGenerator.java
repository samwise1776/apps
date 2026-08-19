package lessons;

public final class LessonGenerator {
  private static final String[] VERBS = {
    "recognize", "explain", "apply", "compare", "analyze", "create"
  };
  private static final String[] TIPS = {
    "Say your reasoning out loud; clear explanations reveal missing steps.",
    "Use a small example first, then test whether the same idea works more broadly.",
    "Mistakes are evidence. Find the first step where your result changed.",
    "Connect this idea to something you already understand.",
    "Pause before answering and identify exactly what the question asks."
  };

  public Lesson generate(int id, String subject, int level) {
    String topic = Curriculum.topic(subject, level);
    Lesson.Difficulty difficulty =
        level <= 333
            ? Lesson.Difficulty.BEGINNER
            : level <= 666 ? Lesson.Difficulty.INTERMEDIATE : Lesson.Difficulty.ADVANCED;
    int withinTopic = (level - 1) % 100 + 1;
    String verb = VERBS[Math.min(VERBS.length - 1, (level - 1) / 167)];
    String title = topic + ": " + focus(withinTopic);
    String goal =
        "Learn to "
            + verb
            + " "
            + topic.toLowerCase()
            + " using "
            + focus(withinTopic).toLowerCase()
            + ".";
    String explanation = explanation(subject, topic, withinTopic, difficulty);
    String example = example(subject, topic, withinTopic);
    String practice =
        "Complete "
            + (2 + Math.min(6, withinTopic / 15))
            + " focused tasks. For each one, write or say why your approach works.";
    String challenge = challenge(subject, topic, withinTopic, difficulty);
    return new Lesson(
        id,
        level,
        subject,
        topic,
        difficulty,
        title,
        goal,
        explanation,
        example,
        practice,
        challenge,
        TIPS[(id - 1) % TIPS.length]);
  }

  private String focus(int n) {
    String[] focus = {
      "Core Idea",
      "Guided Practice",
      "Patterns",
      "Accuracy",
      "Real-World Use",
      "Multiple Methods",
      "Reasoning",
      "Connections",
      "Problem Solving",
      "Mastery Check"
    };
    return focus[Math.min(9, (n - 1) / 10)] + " " + ((n - 1) % 10 + 1);
  }

  private String explanation(String subject, String topic, int n, Lesson.Difficulty difficulty) {
    return switch (subject) {
      case "Coding" ->
          "In Java, "
              + topic.toLowerCase()
              + " lets programs represent instructions clearly. Read code from top to bottom, track"
              + " each value, and predict output before running it.";
      case "Math" ->
          topic
              + " is built from relationships between quantities. At "
              + difficulty.name().toLowerCase()
              + " level, focus on both an accurate result and a method you can justify.";
      case "Spelling" ->
          "Words in "
              + topic.toLowerCase()
              + " share useful sound, meaning, or word-part patterns. "
              + "Notice the pattern, pronounce the word, then check it in context.";
      case "Science" ->
          "Scientists study "
              + topic.toLowerCase()
              + " by asking testable questions and using evidence. "
              + "Separate observations from interpretations.";
      default ->
          topic
              + " becomes meaningful when facts are connected to context and evidence. "
              + "This lesson develops idea "
              + n
              + " through explanation, application, and reflection.";
    };
  }

  private String example(String subject, String topic, int n) {
    if (subject.equals("Coding")) {
      return switch ((n - 1) / 20) {
        case 0 -> "int score = " + (10 + n) + ";\nSystem.out.println(score);";
        case 1 -> "if (score >= " + n + ") {\n    System.out.println(\"Goal reached\");\n}";
        case 2 -> "for (int i = 0; i < " + (2 + n % 8) + "; i++) {\n    System.out.println(i);\n}";
        case 3 -> "static int doubleIt(int value) {\n    return value * 2;\n}";
        default -> "int[] values = {2, 4, 6};\nSystem.out.println(values[" + (n % 3) + "]);";
      };
    }
    if (subject.equals("Math"))
      return "Work through a "
          + topic.toLowerCase()
          + " example with values "
          + (n + 2)
          + " and "
          + (n % 9 + 2)
          + ", then verify using an inverse operation or estimate.";
    return "Find one authentic example of "
        + topic.toLowerCase()
        + ". Label the important evidence and explain what it demonstrates.";
  }

  private String challenge(String subject, String topic, int n, Lesson.Difficulty difficulty) {
    String depth =
        difficulty == Lesson.Difficulty.BEGINNER
            ? "change one detail"
            : difficulty == Lesson.Difficulty.INTERMEDIATE
                ? "compare two approaches"
                : "design a new example and defend your reasoning";
    return "Apply "
        + topic.toLowerCase()
        + " to a new situation: "
        + depth
        + ". Record what changed, what stayed true, and what you learned.";
  }
}
