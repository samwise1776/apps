import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dictionary
 *
 * A vocabulary/token system for a small Java AI.
 *
 * Features:
 * - Learns words
 * - Assigns every word a unique token ID
 * - Converts sentences into tokens
 * - Converts tokens back into words
 * - Creates bag-of-words vectors
 * - Creates word-count vectors
 * - Tracks word frequency
 * - Supports unknown words
 * - Saves vocabulary to disk
 * - Loads vocabulary from disk
 *
 * Example:
 *
 * Dictionary dictionary = new Dictionary();
 *
 * dictionary.learnSentence("hello how are you");
 *
 * int[] tokens = dictionary.encode("hello you");
 * double[] vector = dictionary.toBinaryVector("hello you");
 */
public final class Dictionary {

    private static final List<Path> SYSTEM_WORD_LISTS =
            List.of(
                    Path.of("/usr/share/dict/words"),
                    Path.of("/usr/share/dict/american-english"),
                    Path.of("/usr/share/hunspell/en_US.dic")
            );

    private static final String[] COMMON_SLANG = {
            "af", "aight", "aint", "bae", "bet", "boujee", "brb",
            "bro", "bruh", "bussin", "cap", "chill", "cringe", "cuz",
            "delulu", "dope", "dude", "extra", "fam", "finna", "fire",
            "flex", "fr", "fye", "goat", "goated", "gonna", "gotta",
            "gucci", "hangry", "hbu", "hella", "hmu", "idc", "idk",
            "ikr", "imo", "imho", "irl", "iykyk", "kinda", "lit",
            "lmao", "lmfao", "lol", "lowkey", "mid", "nah", "ngl",
            "noob", "nope", "npc", "omg", "omw", "oop", "periodt",
            "pog", "rn", "rofl", "salty", "sheesh", "ship", "simp",
            "slaps", "slay", "smh", "sus", "tbh", "tho", "thx",
            "totes", "troll", "vibe", "vibing", "wanna", "whatevs",
            "wyd", "wym", "yall", "yeet", "yolo", "yup"
    };

    private static final String[] COMMON_COMPLETIONS = {
            "about", "after", "again", "because", "before", "computer",
            "could", "dictionary", "different", "example", "first",
            "friend", "good", "great", "hello", "help", "important",
            "information", "java", "language", "little", "message",
            "people", "please", "program", "programming", "question",
            "really", "right", "should", "something", "thanks", "their",
            "there", "these", "thing", "think", "through", "together",
            "understand", "where", "which", "without", "word", "would"
    };

    /*
     * Special tokens.
     */
    public static final String PAD = "<PAD>";
    public static final String UNKNOWN = "<UNKNOWN>";
    public static final String START = "<START>";
    public static final String END = "<END>";

    /*
     * word -> token ID
     */
    private final Map<String, Integer> wordToId =
            new LinkedHashMap<>();

    /*
     * token ID -> word
     */
    private final List<String> idToWord =
            new ArrayList<>();

    /*
     * Tracks how many times each word has been seen.
     */
    private final Map<String, Integer> frequencies =
            new HashMap<>();

    /*
     * Allows words like:
     *
     * don't
     * hello
     * ai123
     */
    private static final Pattern WORD_PATTERN =
            Pattern.compile("[a-zA-Z0-9]+(?:'[a-zA-Z0-9]+)?");

    public Dictionary() {

        /*
         * Always create special tokens first.
         *
         * Their IDs will remain stable:
         *
         * 0 = PAD
         * 1 = UNKNOWN
         * 2 = START
         * 3 = END
         */

        addSpecialToken(PAD);
        addSpecialToken(UNKNOWN);
        addSpecialToken(START);
        addSpecialToken(END);

        loadDefaultEnglishWords();
        addCommonCompletions();
        addSlang();
    }

    /**
     * Loads the first English word list installed on the computer. On Linux,
     * this normally provides about 100,000 words. The dictionary still works
     * when no operating-system word list is installed.
     */
    public int loadDefaultEnglishWords() {

        for (Path candidate : SYSTEM_WORD_LISTS) {

            if (Files.isRegularFile(candidate)) {

                try {

                    return loadWordList(candidate);

                } catch (IOException ignored) {

                    // Try the next installed list.
                }
            }
        }

        return 0;
    }

    /** Loads one word per line. Hunspell suffix flags after '/' are ignored. */
    public int loadWordList(Path file)
            throws IOException {

        int before =
                size();


        try (BufferedReader reader = Files.newBufferedReader(file)) {

            String line;


            while ((line = reader.readLine()) != null) {

                String word =
                        line.strip();


                int slash =
                        word.indexOf('/');


                if (slash >= 0) {

                    word =
                            word.substring(0, slash);
                }


                if (
                        !word.isEmpty()
                                &&
                        !Character.isDigit(word.charAt(0))
                ) {

                    addWord(word);
                }
            }
        }


        return size() - before;
    }

    /** Adds common conversational abbreviations and modern slang. */
    public void addSlang() {

        for (String word : COMMON_SLANG) {

            addWord(word);
            addWord(word);
        }
    }

    private void addCommonCompletions() {

        for (String word : COMMON_COMPLETIONS) {

            // A small frequency boost makes everyday words rank above obscure
            // words when several completions share the same prefix.
            addWord(word);
            addWord(word);
            addWord(word);
        }
    }

    /*
     * ========================================
     * TOKENIZATION
     * ========================================
     */

    public List<String> tokenize(String text) {

        List<String> words =
                new ArrayList<>();

        if (text == null || text.isBlank()) {
            return words;
        }

        String normalized =
                text.toLowerCase(Locale.ROOT);

        Matcher matcher =
                WORD_PATTERN.matcher(normalized);

        while (matcher.find()) {

            words.add(
                    matcher.group()
            );
        }

        return words;
    }

    /*
     * ========================================
     * LEARNING WORDS
     * ========================================
     */

    public int addWord(String word) {

        word = normalizeWord(word);

        if (word.isEmpty()) {
            return getUnknownId();
        }

        Integer existing =
                wordToId.get(word);

        if (existing != null) {

            frequencies.put(
                    word,
                    frequencies.getOrDefault(word, 0) + 1
            );

            return existing;
        }

        int newId =
                idToWord.size();

        wordToId.put(
                word,
                newId
        );

        idToWord.add(
                word
        );

        frequencies.put(
                word,
                1
        );

        return newId;
    }

    private void addSpecialToken(String token) {

        int id =
                idToWord.size();

        wordToId.put(
                token,
                id
        );

        idToWord.add(
                token
        );

        frequencies.put(
                token,
                0
        );
    }

    /*
     * Learn all words in a sentence.
     */
    public void learnSentence(String sentence) {

        List<String> words =
                tokenize(sentence);

        for (String word : words) {

            addWord(word);
        }
    }

    /*
     * Learn multiple sentences.
     */
    public void learnSentences(
            Collection<String> sentences
    ) {

        if (sentences == null) {
            return;
        }

        for (String sentence : sentences) {

            learnSentence(sentence);
        }
    }

    /*
     * ========================================
     * ENCODING
     * ========================================
     */

    /**
     * Turns:
     *
     * "hello world"
     *
     * into something like:
     *
     * [4, 5]
     */
    public int[] encode(String text) {

        List<String> words =
                tokenize(text);

        int[] tokens =
                new int[words.size()];

        for (int i = 0; i < words.size(); i++) {

            tokens[i] =
                    getId(words.get(i));
        }

        return tokens;
    }

    /*
     * Encode while automatically learning words.
     */
    public int[] learnAndEncode(String text) {

        List<String> words =
                tokenize(text);

        int[] tokens =
                new int[words.size()];

        for (int i = 0; i < words.size(); i++) {

            tokens[i] =
                    addWord(words.get(i));
        }

        return tokens;
    }

    /*
     * Adds START and END tokens.
     *
     * hello world
     *
     * becomes:
     *
     * <START> hello world <END>
     */
    public int[] encodeWithBoundaries(
            String text
    ) {

        int[] original =
                encode(text);

        int[] result =
                new int[original.length + 2];

        result[0] =
                getStartId();

        System.arraycopy(
                original,
                0,
                result,
                1,
                original.length
        );

        result[result.length - 1] =
                getEndId();

        return result;
    }

    /*
     * ========================================
     * DECODING
     * ========================================
     */

    public String decode(int[] tokens) {

        if (tokens == null) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        for (int token : tokens) {

            String word =
                    getWord(token);

            /*
             * Usually we don't want these
             * printed as normal text.
             */
            if (
                    word.equals(PAD)
                    ||
                    word.equals(START)
                    ||
                    word.equals(END)
            ) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(word);
        }

        return result.toString();
    }

    /*
     * ========================================
     * AI VECTORS
     * ========================================
     */

    /**
     * Binary bag-of-words vector.
     *
     * Each position represents one vocabulary word.
     *
     * Example:
     *
     * vocabulary:
     *
     * hello = 4
     * world = 5
     *
     * "hello world"
     *
     * becomes roughly:
     *
     * [0,0,0,0,1,1]
     */
    public double[] toBinaryVector(
            String text
    ) {

        double[] vector =
                new double[size()];

        List<String> words =
                tokenize(text);

        for (String word : words) {

            int id =
                    getId(word);

            if (
                    id >= 0
                    &&
                    id < vector.length
            ) {

                vector[id] =
                        1.0;
            }
        }

        return vector;
    }

    /**
     * Word-count vector.
     *
     * If a word occurs three times,
     * its vector position becomes 3.
     */
    public double[] toCountVector(
            String text
    ) {

        double[] vector =
                new double[size()];

        List<String> words =
                tokenize(text);

        for (String word : words) {

            int id =
                    getId(word);

            if (
                    id >= 0
                    &&
                    id < vector.length
            ) {

                vector[id] +=
                        1.0;
            }
        }

        return vector;
    }

    /*
     * Normalized frequency vector.
     *
     * This divides counts by the number
     * of words in the sentence.
     */
    public double[] toFrequencyVector(
            String text
    ) {

        double[] vector =
                toCountVector(text);

        List<String> words =
                tokenize(text);

        if (words.isEmpty()) {
            return vector;
        }

        for (int i = 0; i < vector.length; i++) {

            vector[i] /=
                    words.size();
        }

        return vector;
    }

    /*
     * ========================================
     * LOOKUPS
     * ========================================
     */

    public int getId(String word) {

        word =
                normalizeWord(word);

        Integer id =
                wordToId.get(word);

        if (id == null) {

            return getUnknownId();
        }

        return id;
    }

    public String getWord(int id) {

        if (
                id < 0
                ||
                id >= idToWord.size()
        ) {

            return UNKNOWN;
        }

        return idToWord.get(id);
    }

    public boolean contains(String word) {

        return wordToId.containsKey(
                normalizeWord(word)
        );
    }

    public int size() {

        return idToWord.size();
    }

    /*
     * ========================================
     * FREQUENCIES
     * ========================================
     */

    public int getFrequency(String word) {

        word =
                normalizeWord(word);

        return frequencies.getOrDefault(
                word,
                0
        );
    }

    public List<String> getMostCommonWords(
            int amount
    ) {

        List<String> words =
                new ArrayList<>(wordToId.keySet());

        words.remove(PAD);
        words.remove(UNKNOWN);
        words.remove(START);
        words.remove(END);

        words.sort(
                (a, b) ->
                        Integer.compare(
                                getFrequency(b),
                                getFrequency(a)
                        )
        );

        if (amount < words.size()) {

            return new ArrayList<>(
                    words.subList(
                            0,
                            Math.max(0, amount)
                    )
            );
        }

        return words;
    }

    /*
     * ========================================
     * SPECIAL TOKEN IDs
     * ========================================
     */

    public int getPadId() {

        return wordToId.get(PAD);
    }

    public int getUnknownId() {

        return wordToId.get(UNKNOWN);
    }

    public int getStartId() {

        return wordToId.get(START);
    }

    public int getEndId() {

        return wordToId.get(END);
    }

    /*
     * ========================================
     * VOCABULARY ACCESS
     * ========================================
     */

    public List<String> getWords() {

        return Collections.unmodifiableList(
                idToWord
        );
    }

    public Map<String, Integer> getVocabulary() {

        return Collections.unmodifiableMap(
                wordToId
        );
    }

    /*
     * ========================================
     * SAVING
     * ========================================
     */

    public void save(Path file)
            throws IOException {

        Path parent =
                file.getParent();

        if (parent != null) {

            Files.createDirectories(
                    parent
            );
        }

        try (
                BufferedWriter writer =
                        Files.newBufferedWriter(file)
        ) {

            for (
                    int id = 0;
                    id < idToWord.size();
                    id++
            ) {

                String word =
                        idToWord.get(id);

                int frequency =
                        frequencies.getOrDefault(
                                word,
                                0
                        );

                /*
                 * Format:
                 *
                 * ID<TAB>WORD<TAB>FREQUENCY
                 */

                writer.write(
                        id
                        + "\t"
                        + escape(word)
                        + "\t"
                        + frequency
                );

                writer.newLine();
            }
        }
    }

    /*
     * ========================================
     * LOADING
     * ========================================
     */

    public void load(Path file)
            throws IOException {

        if (!Files.exists(file)) {

            throw new FileNotFoundException(
                    "Dictionary file not found: "
                    + file
            );
        }

        wordToId.clear();
        idToWord.clear();
        frequencies.clear();

        List<String> lines =
                Files.readAllLines(file);

        for (String line : lines) {

            if (line.isBlank()) {
                continue;
            }

            String[] parts =
                    line.split(
                            "\t",
                            3
                    );

            if (parts.length < 3) {
                continue;
            }

            int id;

            try {

                id =
                        Integer.parseInt(
                                parts[0]
                        );

            } catch (NumberFormatException e) {

                continue;
            }

            String word =
                    unescape(parts[1]);

            int frequency;

            try {

                frequency =
                        Integer.parseInt(
                                parts[2]
                        );

            } catch (NumberFormatException e) {

                frequency = 0;
            }

            /*
             * Ensure list is large enough.
             */

            while (
                    idToWord.size()
                    <= id
            ) {

                idToWord.add(null);
            }

            idToWord.set(
                    id,
                    word
            );

            wordToId.put(
                    word,
                    id
            );

            frequencies.put(
                    word,
                    frequency
            );
        }

        /*
         * Verify special tokens.
         */
        ensureSpecialTokens();
    }

    /*
     * ========================================
     * REMOVE RARE WORDS
     * ========================================
     */

    public void removeRareWords(
            int minimumFrequency
    ) {

        List<String> keep =
                new ArrayList<>();

        for (String word : idToWord) {

            if (word == null) {
                continue;
            }

            if (isSpecialToken(word)) {

                keep.add(word);

            } else if (
                    getFrequency(word)
                    >= minimumFrequency
            ) {

                keep.add(word);
            }
        }

        rebuildVocabulary(keep);
    }

    /*
     * ========================================
     * INTERNAL HELPERS
     * ========================================
     */

    private void rebuildVocabulary(
            List<String> words
    ) {

        Map<String, Integer> oldFrequencies =
                new HashMap<>(
                        frequencies
                );

        wordToId.clear();
        idToWord.clear();
        frequencies.clear();

        /*
         * Special tokens first.
         */

        addSpecialToken(PAD);
        addSpecialToken(UNKNOWN);
        addSpecialToken(START);
        addSpecialToken(END);

        for (String word : words) {

            if (isSpecialToken(word)) {
                continue;
            }

            int id =
                    idToWord.size();

            wordToId.put(
                    word,
                    id
            );

            idToWord.add(
                    word
            );

            frequencies.put(
                    word,
                    oldFrequencies.getOrDefault(
                            word,
                            0
                    )
            );
        }
    }

    private void ensureSpecialTokens() {

        if (
                !wordToId.containsKey(PAD)
                ||
                !wordToId.containsKey(UNKNOWN)
                ||
                !wordToId.containsKey(START)
                ||
                !wordToId.containsKey(END)
        ) {

            List<String> oldWords =
                    new ArrayList<>(
                            idToWord
                    );

            rebuildVocabulary(
                    oldWords
            );
        }
    }

    private boolean isSpecialToken(
            String word
    ) {

        return word.equals(PAD)
                ||
                word.equals(UNKNOWN)
                ||
                word.equals(START)
                ||
                word.equals(END);
    }

    private String normalizeWord(
            String word
    ) {

        if (word == null) {
            return "";
        }

        return word
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String escape(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\n", "\\n");
    }

    private String unescape(String text) {

        return text
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    /*
     * ========================================
     * DEBUG INFORMATION
     * ========================================
     */

    public void printDictionary() {

        System.out.println(
                "========== DICTIONARY =========="
        );

        for (
                int id = 0;
                id < idToWord.size();
                id++
        ) {

            String word =
                    idToWord.get(id);

            System.out.println(
                    id
                    + " -> "
                    + word
                    + " | frequency="
                    + getFrequency(word)
            );
        }

        System.out.println(
                "Total tokens: "
                + size()
        );

        System.out.println(
                "================================"
        );
    }
}
