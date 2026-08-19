package quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class QuizManager {
  public List<Question> create(String subject, int level) {
    Random random = new Random(subject.hashCode() * 31L + level + System.nanoTime());
    List<Question> pool = new ArrayList<>();
    if (subject.equals("Math")) {
      int ceiling = level < 334 ? 20 : level < 667 ? 100 : 500;
      for (int i = 0; i < 15; i++) {
        int a = 2 + random.nextInt(ceiling), b = 2 + random.nextInt(ceiling);
        int answer = a + b;
        pool.add(
            shuffledQuestion(
                "What is " + a + " + " + b + "?",
                "" + answer,
                new String[] {"" + (answer + 1), "" + (answer - 2), "" + (answer + 10)},
                a + " + " + b + " = " + answer + ". Add the ones, then the larger place values.",
                random));
      }
    } else {
      String[] topic = concepts(subject);
      for (int i = 0; i < topic.length; i += 3)
        pool.add(
            shuffledQuestion(
                topic[i],
                topic[i + 1],
                new String[] {topic[i + 2], "None of these", "Both are always true"},
                topic[i + 1] + " is correct because it matches the key idea for this course level.",
                random));
    }
    Collections.shuffle(pool, random);
    return new ArrayList<>(pool.subList(0, Math.min(5, pool.size())));
  }

  private Question shuffledQuestion(
      String prompt, String correct, String[] wrong, String explanation, Random random) {
    List<String> options = new ArrayList<>();
    options.add(correct);
    Collections.addAll(options, wrong);
    Collections.shuffle(options, random);
    return new Question(
        prompt, options.toArray(String[]::new), options.indexOf(correct), explanation);
  }

  private String[] concepts(String subject) {
    return switch (subject) {
      case "Coding" ->
          new String[] {
            "What stores a value in Java?",
            "A variable",
            "A comment",
            "What repeats instructions?",
            "A loop",
            "A String only",
            "What makes a decision?",
            "An if statement",
            "An import",
            "What groups reusable code?",
            "A method",
            "A color",
            "What describes an object blueprint?",
            "A class",
            "A file path"
          };
      case "Spelling" ->
          new String[] {
            "Which is correct?",
            "because",
            "becaus",
            "Which is correct?",
            "separate",
            "seperate",
            "Which means they are?",
            "they're",
            "their",
            "Which is correct?",
            "necessary",
            "neccessary",
            "Which is correct?",
            "definitely",
            "definately"
          };
      case "Science" ->
          new String[] {
            "Evidence comes from...",
            "observations",
            "wishes",
            "A testable proposed answer is...",
            "a hypothesis",
            "a legend",
            "Plants make food using...",
            "sunlight",
            "moonlight",
            "Matter has...",
            "mass and volume",
            "only color",
            "A conclusion uses...",
            "evidence",
            "luck"
          };
      case "History" ->
          new String[] {
            "A timeline orders...",
            "events by date",
            "places by size",
            "A primary source comes from...",
            "the time studied",
            "a fictional future",
            "A cause explains...",
            "why something happened",
            "its color",
            "An effect is...",
            "a result",
            "a title",
            "Historians compare sources to...",
            "check evidence",
            "erase history"
          };
      case "Geography" ->
          new String[] {
            "A map legend explains...",
            "symbols",
            "weather only",
            "Opposite of east is...",
            "west",
            "north",
            "Climate means...",
            "long-term weather patterns",
            "one storm",
            "Latitude measures from...",
            "the equator",
            "a city",
            "A continent is...",
            "a large landmass",
            "a river"
          };
      case "Reading" ->
          new String[] {
            "The main idea is...",
            "what a text is mostly about",
            "the font",
            "An inference combines...",
            "clues and prior knowledge",
            "page numbers",
            "Context clues help with...",
            "word meaning",
            "book prices",
            "A summary contains...",
            "key ideas",
            "every word",
            "Evidence should...",
            "support an answer",
            "change the title"
          };
      case "Writing" ->
          new String[] {
            "A topic sentence gives...",
            "the main idea",
            "a page number",
            "Revision improves...",
            "ideas and organization",
            "ink color",
            "Editing checks...",
            "grammar and spelling",
            "weather",
            "Details should be...",
            "relevant",
            "random",
            "A conclusion should...",
            "close the writing",
            "start an unrelated topic"
          };
      case "Art" ->
          new String[] {
            "Value means...",
            "lightness or darkness",
            "price only",
            "Texture describes...",
            "surface quality",
            "sound",
            "Contrast emphasizes...",
            "differences",
            "dates",
            "Lines can create...",
            "shape and movement",
            "temperature",
            "Composition arranges...",
            "visual elements",
            "passwords"
          };
      default ->
          new String[] {
            "A useful goal is...",
            "specific",
            "unclear",
            "A budget tracks...",
            "money",
            "weather",
            "Reflection helps us...",
            "learn from results",
            "avoid learning",
            "Large tasks become manageable when...",
            "divided into steps",
            "ignored",
            "Good communication includes...",
            "active listening",
            "interrupting"
          };
    };
  }
}
