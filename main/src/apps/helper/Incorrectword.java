import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects likely misspellings and suggests nearby Dictionary words. */
public final class Incorrectword {
    private static final Pattern WORD = Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)?");
    private final Dictionary dictionary;
    private final Slang slang;
    private static final Map<String, String> COMMON_CORRECTIONS = Map.ofEntries(
            Map.entry("helo", "hello"), Map.entry("jvaa", "java"),
            Map.entry("teh", "the"), Map.entry("realy", "really"),
            Map.entry("programing", "programming"), Map.entry("recieve", "receive"),
            Map.entry("seperate", "separate"), Map.entry("definately", "definitely"),
            Map.entry("wierd", "weird"), Map.entry("occured", "occurred"),
            Map.entry("untill", "until"), Map.entry("becuase", "because")
    );

    public Incorrectword(Dictionary dictionary) {
        if (dictionary == null) throw new IllegalArgumentException("dictionary is required");
        this.dictionary = dictionary;
        this.slang = new Slang();
    }

    public boolean isIncorrect(String word) {
        String normalized = normalize(word);
        return !normalized.isEmpty() && (COMMON_CORRECTIONS.containsKey(normalized)
                || (!dictionary.contains(normalized) && !slang.isSlang(normalized)));
    }

    public List<String> suggest(String incorrect, int maximum) {
        String source = normalize(incorrect);
        if (source.isEmpty() || maximum <= 0 || !isIncorrect(source)) return List.of();
        String preferred = COMMON_CORRECTIONS.get(source);
        int allowed = source.length() <= 4 ? 1 : source.length() <= 8 ? 2 : 3;
        List<Candidate> candidates = new ArrayList<>();
        for (String word : dictionary.getWords()) {
            if (word == null || word.startsWith("<") || !WORD.matcher(word).matches()) continue;
            if (Math.abs(word.length() - source.length()) > allowed) continue;
            int distance = distance(source, word, allowed);
            if (distance <= allowed) candidates.add(new Candidate(word, distance, dictionary.getFrequency(word)));
        }
        candidates.sort(Comparator.comparingInt(Candidate::distance)
                .thenComparing(Comparator.comparingInt(Candidate::frequency).reversed())
                .thenComparingInt(candidate -> candidate.word().length())
                .thenComparing(Candidate::word));
        List<String> result = new ArrayList<>();
        if (preferred != null) result.add(preferred);
        for (Candidate candidate : candidates) {
            if (result.size() >= maximum) break;
            if (!result.contains(candidate.word())) result.add(candidate.word());
        }
        return result;
    }

    public String correctWord(String word) {
        List<String> choices = suggest(word, 1);
        if (choices.isEmpty()) return word;
        String corrected = choices.get(0);
        return Character.isUpperCase(word.charAt(0))
                ? Character.toUpperCase(corrected.charAt(0)) + corrected.substring(1) : corrected;
    }

    public List<String> findIncorrectWords(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = WORD.matcher(text == null ? "" : text);
        while (matcher.find()) if (isIncorrect(matcher.group()) && !result.contains(matcher.group())) result.add(matcher.group());
        return result;
    }

    public String correctText(String text) {
        if (text == null || text.isEmpty()) return text == null ? "" : text;
        Matcher matcher = WORD.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(result, Matcher.quoteReplacement(correctWord(matcher.group())));
        matcher.appendTail(result);
        return result.toString();
    }

    /** Bounded Levenshtein distance; stops rows that cannot beat the limit. */
    private static int distance(String left, String right, int limit) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) previous[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i; int rowMinimum = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
                rowMinimum = Math.min(rowMinimum, current[j]);
            }
            if (rowMinimum > limit) return limit + 1;
            int[] swap = previous; previous = current; current = swap;
        }
        return previous[right.length()];
    }

    private static String normalize(String word) { return word == null ? "" : word.toLowerCase(Locale.ROOT).trim(); }
    private record Candidate(String word, int distance, int frequency) { }

    public static void main(String[] args) {
        if (args.length == 0) { System.out.println("Usage: java Incorrectword <word or sentence>"); return; }
        Incorrectword checker = new Incorrectword(new Dictionary());
        String input = String.join(" ", args);
        System.out.println("Corrected: " + checker.correctText(input));
        for (String word : checker.findIncorrectWords(input)) System.out.println(word + " -> " + checker.suggest(word, 5));
    }
}
