import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Completes an unfinished word from Dictionary vocabulary.
 *
 * Examples:
 *   java Unfinished prog       -> program
 *   java Unfinished "I like prog" -> I like program
 */
public final class Unfinished {

    private final Dictionary dictionary;


    public Unfinished(Dictionary dictionary) {

        if (dictionary == null) {

            throw new IllegalArgumentException(
                    "dictionary is required"
            );
        }


        this.dictionary =
                dictionary;
    }


    public String completeWord(String unfinished) {

        List<String> matches =
                suggest(unfinished, 1);


        return matches.isEmpty()
                ? unfinished
                : matches.get(0);
    }


    public List<String> suggest(
            String unfinished,
            int maximum
    ) {

        if (
                unfinished == null
                        ||
                unfinished.isBlank()
                        ||
                maximum <= 0
        ) {

            return List.of();
        }


        String prefix =
                unfinished
                        .toLowerCase(Locale.ROOT)
                        .trim();


        List<String> matches =
                new ArrayList<>();


        for (String word : dictionary.getWords()) {

            if (
                    word != null
                            &&
                    !word.startsWith("<")
                            &&
                    word.startsWith(prefix)
                            &&
                    word.length() > prefix.length()
            ) {

                matches.add(word);
            }
        }


        matches.sort(
                Comparator
                        .comparingInt(
                                (String word) ->
                                        dictionary.getFrequency(word)
                        )
                        .reversed()
                        .thenComparingInt(String::length)
                        .thenComparing(Comparator.naturalOrder())
        );


        if (matches.size() > maximum) {

            return new ArrayList<>(
                    matches.subList(0, maximum)
            );
        }


        return matches;
    }


    /** Completes the final word while preserving the rest of the sentence. */
    public String completeText(String text) {

        if (text == null || text.isBlank()) {

            return text == null ? "" : text;
        }


        int end =
                text.length();


        int start =
                end;


        while (
                start > 0
                        &&
                Character.isLetterOrDigit(text.charAt(start - 1))
        ) {

            start--;
        }


        if (start == end) {

            return text;
        }


        String partial =
                text.substring(start, end);


        String completed =
                completeWord(partial);


        if (
                !partial.isEmpty()
                        &&
                Character.isUpperCase(partial.charAt(0))
                        &&
                !completed.isEmpty()
        ) {

            completed =
                    Character.toUpperCase(completed.charAt(0))
                            +
                    completed.substring(1);
        }


        return text.substring(0, start) + completed;
    }


    public static void main(String[] args) {

        if (args.length == 0) {

            System.out.println(
                    "Usage: java Unfinished <unfinished word or text>"
            );

            return;
        }


        Dictionary dictionary =
                new Dictionary();


        Unfinished unfinished =
                new Unfinished(dictionary);


        String text =
                String.join(" ", args);


        System.out.println(
                unfinished.completeText(text)
        );


        System.out.println(
                "Suggestions: "
                        +
                unfinished.suggest(
                        dictionary.tokenize(text)
                                .stream()
                                .reduce((first, last) -> last)
                                .orElse(""),
                        10
                )
        );
    }
}
