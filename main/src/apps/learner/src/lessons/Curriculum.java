package lessons;

import java.util.List;

public final class Curriculum {
  public static final List<String> SUBJECTS =
      List.of(
          "Math",
          "Coding",
          "Spelling",
          "Science",
          "History",
          "Geography",
          "Reading",
          "Writing",
          "Art",
          "Life Skills");

  private static final String[][] TOPICS = {
    {
      "Addition & Subtraction",
      "Multiplication & Division",
      "Fractions",
      "Decimals",
      "Percentages",
      "Ratios",
      "Pre-Algebra",
      "Geometry",
      "Statistics",
      "Mixed Challenges"
    },
    {
      "Variables & Output",
      "Conditions",
      "Loops",
      "Methods",
      "Arrays & Collections",
      "Classes & Objects",
      "Files",
      "Swing GUIs",
      "Algorithms & Debugging",
      "Java Projects"
    },
    {
      "Sounds & Patterns",
      "Vowel Teams",
      "Prefixes",
      "Suffixes",
      "Compound Words",
      "Homophones",
      "Word Origins",
      "Academic Words",
      "Editing",
      "Spelling Mastery"
    },
    {
      "Scientific Inquiry",
      "Matter",
      "Forces & Motion",
      "Energy",
      "Cells",
      "Ecosystems",
      "Earth Science",
      "Space",
      "Chemistry",
      "Integrated Science"
    },
    {
      "Historical Evidence",
      "Ancient Civilizations",
      "Middle Ages",
      "Exploration",
      "Revolutions",
      "Industrial Era",
      "World Conflicts",
      "Civil Rights",
      "Modern History",
      "Historical Analysis"
    },
    {
      "Maps & Direction",
      "Landforms",
      "Weather & Climate",
      "Continents",
      "Population",
      "Culture",
      "Economics",
      "Human Environment",
      "Global Systems",
      "Geographic Analysis"
    },
    {
      "Main Ideas",
      "Text Evidence",
      "Vocabulary",
      "Inference",
      "Story Structure",
      "Author's Purpose",
      "Nonfiction",
      "Poetry",
      "Comparison",
      "Critical Reading"
    },
    {
      "Sentences",
      "Paragraphs",
      "Description",
      "Narrative",
      "Opinion",
      "Informative Writing",
      "Research",
      "Revision",
      "Style",
      "Publishing"
    },
    {
      "Line & Shape",
      "Color",
      "Value",
      "Texture",
      "Composition",
      "Perspective",
      "Art History",
      "Design",
      "Mixed Media",
      "Portfolio"
    },
    {
      "Goals",
      "Organization",
      "Communication",
      "Money",
      "Health",
      "Digital Citizenship",
      "Problem Solving",
      "Teamwork",
      "Career Skills",
      "Independent Living"
    }
  };

  private Curriculum() {}

  public static int subjectIndex(String subject) {
    int index = SUBJECTS.indexOf(subject);
    if (index < 0) throw new IllegalArgumentException("Unknown subject: " + subject);
    return index;
  }

  public static String topic(String subject, int level) {
    if (level < 1 || level > 1000) throw new IllegalArgumentException("Level must be 1-1000");
    return TOPICS[subjectIndex(subject)][(level - 1) / 100];
  }

  public static List<String> topics(String subject) {
    return List.of(TOPICS[subjectIndex(subject)]);
  }
}
