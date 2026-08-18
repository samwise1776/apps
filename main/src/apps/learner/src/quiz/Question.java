package quiz;

public final class Question {
  private final String prompt;
  private final String[] options;
  private final int correctIndex;
  private final String explanation;

  public Question(String prompt, String[] options, int correctIndex, String explanation) {
    if (options.length < 2 || correctIndex < 0 || correctIndex >= options.length)
      throw new IllegalArgumentException("Invalid question options");
    this.prompt = prompt;
    this.options = options.clone();
    this.correctIndex = correctIndex;
    this.explanation = explanation;
  }

  public String prompt() {
    return prompt;
  }

  public String[] options() {
    return options.clone();
  }

  public int correctIndex() {
    return correctIndex;
  }

  public String explanation() {
    return explanation;
  }
}
