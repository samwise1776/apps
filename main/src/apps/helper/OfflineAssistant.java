import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A dependency-free general assistant using local rules, knowledge, and math. */
public final class OfflineAssistant {
    private static final Pattern NAME = Pattern.compile("(?i)(?:my name is|call me)\\s+([a-z][a-z'-]{1,30})");
    private final Brain brain;
    private final Map<String, String> knowledge = new LinkedHashMap<>();
    private final Map<String, String> memories = new LinkedHashMap<>();
    private final List<String> questionHistory = new ArrayList<>();
    private final Path memoryFile;
    private String userName = "";

    private static final String MEMORY_START = "<!-- NOVA_MEMORY_START -->";
    private static final String MEMORY_END = "<!-- NOVA_MEMORY_END -->";

    public OfflineAssistant(Brain brain) {
        this.brain = brain;
        this.memoryFile = findMemoryFile();
        addKnowledge();
        loadMemories();
    }

    public Brain.Answer answer(String input) {
        loadMemories();
        String question = input == null ? "" : input.trim();
        String lower = question.toLowerCase(Locale.ROOT);
        if (question.isEmpty()) return result("Type a question and I’ll help.", "EMPTY", 1);
        recordQuestion(question);

        if (lower.matches("(?:what|show|list|tell me)(?: do you)? remember(?: about me)?[?.!]*"))
            return result(memorySummary(), "MEMORY", 1);

        if (lower.startsWith("remember that ") || lower.startsWith("remember ")) {
            String fact = question.replaceFirst("(?i)^remember(?: that)?\\s+", "").replaceAll("[.!]+$", "").trim();
            if (fact.isEmpty()) return result("Tell me what to remember after the word “remember.”", "MEMORY", 1);
            rememberFact(fact);
            return result("I’ll remember that " + fact + ". I saved it in MEMORY.md.", "MEMORY", 1);
        }

        if (lower.matches("forget (?:everything|all memories|all)[.!]*")) {
            memories.clear();
            userName = "";
            saveMemories();
            return result("I forgot all saved personal memories. The project documentation remains intact.", "MEMORY", 1);
        }

        Matcher name = NAME.matcher(question);
        if (name.find()) {
            userName = capitalize(name.group(1));
            memories.put("name", userName);
            saveMemories();
            return result("Nice to meet you, " + userName + ". I saved your name in MEMORY.md and will remember it across restarts.", "MEMORY", 1);
        }

        Brain.Answer rememberedAnswer = answerFromMemory(lower);
        if (rememberedAnswer != null) return rememberedAnswer;
        if (lower.matches(".*\\b(what is my name|who am i)\\b.*"))
            return result(userName.isEmpty() ? "You haven’t told me your name yet. Say: my name is Ray." : "Your name is " + userName + ".", "MEMORY", 1);
        if (lower.matches(".*\\b(time|current time)\\b.*"))
            return result("The local time is " + LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")) + ".", "TIME", 1);
        if (lower.matches(".*\\b(date|today|day is it)\\b.*"))
            return result("Today is " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")) + ".", "DATE", 1);

        String expression = extractExpression(lower);
        if (expression != null) {
            try {
                double value = new MathParser(expression).parse();
                String shown = value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.US, "%.10f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
                return result(expression + " = " + shown, "MATH", 1);
            } catch (IllegalArgumentException ignored) { }
        }

        if (lower.matches("^how (?:do i|can i|should i|to)\\b.*")) {
            String goal = lower.replaceFirst("how (?:do i |can i |should i |to )", "").replaceAll("[?.!]$", "");
            return result(memoryPrefix(lower) + "A reliable way to " + goal + " is: define the exact result, split it into small steps, try the smallest step first, check the result, and adjust. Tell me what tools or constraints you have and I can make those steps more specific.", "GUIDANCE", .75);
        }
        if (lower.startsWith("why "))
            return result(memoryPrefix(lower) + "That can have more than one cause. Separate what you observed from what you assume, identify what changed, then test the most likely cause first. Give me the specific subject and evidence and I’ll reason through it with you.", "REASONING", .65);

        for (Map.Entry<String, String> entry : knowledge.entrySet()) {
            if (containsTopic(lower, entry.getKey()))
                return result(memoryPrefix(lower) + entry.getValue(), "KNOWLEDGE", .95);
        }

        if (lower.matches("^(what|who|when|where|why|how|define|compare|is|are|can|could|should|would|will|do|does|did|has|have)\\b.*"))
            return universalAnswer(question, lower);

        Brain.Answer learned = brain.think(question);
        if (!"UNKNOWN".equals(learned.category) && learned.confidence >= .40) return learned;
        return universalAnswer(question, lower);
    }

    /** Always produces a useful direct response, even for an unknown subject. */
    private Brain.Answer universalAnswer(String original, String lower) {
        String subject = subjectOf(lower);
        String memory = memoryPrefix(lower);

        if (lower.startsWith("what is ") || lower.startsWith("what are ") || lower.startsWith("define "))
            return result(memory + capitalize(subject) + " is the subject you asked about. In practical terms, understand it by identifying what category it belongs to, its main purpose, its important parts, and one concrete example. My built-in offline knowledge has no verified entry for this specific term, so I won’t invent a definition.", "DIRECT_DEFINITION", .50);
        if (lower.startsWith("who "))
            return result("The person or group connected to " + subject + " depends on the specific role and time period. My offline knowledge does not contain a verified identity for it, so the responsible answer is that I cannot name someone accurately from the information stored in this program.", "DIRECT_WHO", .45);
        if (lower.startsWith("when "))
            return result("The timing of " + subject + " depends on the event, location, and time zone. There is no verified date for it in my local knowledge, so I should not make one up.", "DIRECT_WHEN", .45);
        if (lower.startsWith("where "))
            return result("The location of " + subject + " is not recorded in my built-in knowledge. A precise answer would require a city, organization, address, or geographic reference; I won’t invent one.", "DIRECT_WHERE", .45);
        if (lower.startsWith("why "))
            return result("The most likely explanation for " + subject + " is a combination of conditions, causes, and incentives. Check what changed immediately before it happened, what mechanism links cause to result, and whether another explanation fits the evidence better.", "DIRECT_WHY", .60);
        if (lower.startsWith("how "))
            return result(memory + "To approach " + subject + ": define the desired result, list requirements, break the work into testable steps, complete the smallest step, verify it, then repeat. Watch for safety, cost, and reversible choices.", "DIRECT_HOW", .60);
        if (lower.contains(" or ") || lower.startsWith("compare "))
            return result(memory + "Choose between the alternatives by comparing the same criteria: goal fit, correctness, effort, cost, risk, maintainability, and reversibility. Prefer the option that satisfies the goal with the fewest unacceptable tradeoffs.", "DIRECT_COMPARE", .60);
        if (lower.matches("^(is|are|can|could|should|would|will|do|does|did|has|have)\\b.*"))
            return result(memory + "My best offline answer is: it depends on the exact conditions. Test the claim against a concrete example and look for a counterexample; if one exists, the answer is not universally yes. For a decision, choose the safer reversible option until you have stronger evidence.", "DIRECT_YES_NO", .55);
        return result(memory + "Here is a practical answer about “" + summarize(original) + "”: separate the goal, known facts, assumptions, and constraints. Act first on the part supported by evidence, test the result, and revise anything that fails. I have answered from local reasoning without pretending to know facts that are not stored in this program.", "DIRECT_GENERAL", .50);
    }

    private void addKnowledge() {
        knowledge.put("java", "Java is a strongly typed, object-oriented language compiled to JVM bytecode. Start with variables, methods, conditions, loops, classes, collections, exceptions, and tests. This application itself is pure Java Swing.");
        knowledge.put("programming", "Programming is expressing a solution as precise instructions. Good programs separate responsibilities, use clear names, validate inputs, handle failures, and are tested with representative cases.");
        knowledge.put("artificial intelligence| ai |machine learning", "Artificial intelligence is the broad field of making computers perform tasks associated with intelligence. Machine learning learns patterns from examples; this app’s Brain is a small trained softmax text classifier, not a large language model.");
        knowledge.put("neural network", "A neural network combines inputs with learned weights and biases. During training, error gradients adjust those values. Brain.java uses bag-of-words inputs, softmax probabilities, cross-entropy loss, and gradient descent.");
        knowledge.put("computer", "A computer executes instructions, stores data in memory, and communicates through input/output devices. An operating system manages hardware and provides services to applications.");
        knowledge.put("internet", "The internet is a network of networks that moves packets using protocols such as IP and TCP. The web is one service on it, usually using HTTP and URLs.");
        knowledge.put("http", "HTTP is a request-response protocol. A client sends a method, URL, headers, and optional body; a server returns a status, headers, and body.");
        knowledge.put("json", "JSON is a text data format built from objects, arrays, strings, numbers, booleans, and null. It is commonly used for configuration and HTTP APIs.");
        knowledge.put("class", "A class defines data and behavior for objects. Fields store state, constructors initialize it, and methods implement behavior.");
        knowledge.put("variable", "A variable is a named storage location. In Java it has a declared type, such as int, double, String, or a custom class.");
        knowledge.put("method|function", "A method is a named block of reusable behavior. It may accept parameters, return a value, modify object state, or perform an action.");
        knowledge.put("loop", "A loop repeats work. Java provides for, enhanced for, while, and do-while loops. Always ensure the loop has a valid stopping condition.");
        knowledge.put("array", "An array stores a fixed-size sequence of same-typed values. Java indexes arrays from zero; accessing outside 0 through length-1 throws an exception.");
        knowledge.put("database", "A database stores structured information and supports reliable queries and updates. Relational databases use tables and SQL; other databases use documents, key-value pairs, graphs, or other models.");
        knowledge.put("algorithm", "An algorithm is a finite procedure for solving a problem. Evaluate correctness first, then time and memory costs, clarity, and behavior on edge cases.");
        knowledge.put("debug", "To debug: reproduce the problem, reduce it to the smallest case, inspect actual values, form one testable hypothesis, test it, fix the cause, and add a regression test.");
        knowledge.put("swing", "Swing is Java’s desktop UI toolkit. Create and update components on the Event Dispatch Thread, and put slow work on background threads so the interface remains responsive.");
        knowledge.put("thread", "A thread is one sequence of execution inside a process. Shared mutable state needs coordination; Swing additionally requires UI updates on its Event Dispatch Thread.");
        knowledge.put("gravity", "Gravity is the attraction between masses. Near Earth’s surface it accelerates falling objects at about 9.81 m/s² when air resistance is ignored.");
        knowledge.put("photosynthesis", "Photosynthesis lets plants, algae, and some bacteria use light energy to convert carbon dioxide and water into sugars, releasing oxygen as a byproduct.");
        knowledge.put("water", "Water is H₂O. At standard atmospheric pressure it freezes near 0°C and boils near 100°C, though pressure and dissolved substances change those points.");
        knowledge.put("earth", "Earth is the third planet from the Sun and the only world currently known to support life. It has one natural satellite, the Moon.");
        knowledge.put("sun", "The Sun is the star at the center of our solar system. Nuclear fusion in its core converts hydrogen into helium and releases energy.");
        knowledge.put("moon", "The Moon is Earth’s natural satellite. Its gravity is the main cause of ocean tides, and its phases result from the changing portion illuminated by the Sun as seen from Earth.");
        knowledge.put("solar system", "The solar system consists of the Sun and objects bound to it by gravity, including eight planets, dwarf planets, moons, asteroids, and comets.");
        knowledge.put("atom", "An atom is the basic unit of a chemical element. It contains a nucleus of protons and neutrons surrounded by electrons; chemical behavior mainly depends on its electrons.");
        knowledge.put("dna", "DNA is the molecule that stores hereditary information in living organisms. Its sequence uses four bases—A, C, G, and T—and cells read portions called genes.");
        knowledge.put("evolution", "Evolution is change in inherited traits of populations across generations. Mutation creates variation, while natural selection, genetic drift, and gene flow alter how common variants become.");
        knowledge.put("democracy", "Democracy is government in which political authority ultimately comes from the people, commonly through elections, representation, civil rights, accountable institutions, and rule of law.");
        knowledge.put("capital of france|france capital", "The capital of France is Paris.");
        knowledge.put("capital of japan|japan capital", "The capital of Japan is Tokyo.");
        knowledge.put("capital of canada|canada capital", "The capital of Canada is Ottawa.");
        knowledge.put("capital of australia|australia capital", "The capital of Australia is Canberra.");
        knowledge.put("world war ii|second world war", "World War II was a global war fought from 1939 to 1945. The Allies defeated the Axis powers; the war caused immense destruction, the Holocaust, and major political changes.");
        knowledge.put("shakespeare", "William Shakespeare was an English playwright and poet, traditionally dated 1564–1616, known for works including Hamlet, Macbeth, Romeo and Juliet, and many sonnets.");
        knowledge.put("pi", "Pi (π) is the ratio of a circle’s circumference to its diameter. It is irrational and begins 3.141592653589793.");
    }

    private void rememberFact(String fact) {
        Matcher preference = Pattern.compile("(?i)^my\\s+(.+?)\\s+is\\s+(.+)$").matcher(fact);
        if (preference.matches()) {
            String key = preference.group(1).trim().toLowerCase(Locale.ROOT);
            String value = preference.group(2).trim();
            memories.put(key, value);
            if ("name".equals(key)) userName = capitalize(value);
        } else {
            memories.put("fact-" + (memories.size() + 1), fact);
        }
        saveMemories();
    }

    private String memorySummary() {
        if (memories.isEmpty()) return "I don’t have any saved personal memories yet. Say “remember that ...” to add one.";
        StringBuilder answer = new StringBuilder("I remember:\n");
        memories.forEach((key, value) -> answer.append("• ").append(displayKey(key)).append(": ").append(value).append('\n'));
        return answer.toString().stripTrailing();
    }

    private void loadMemories() {
        if (!Files.isRegularFile(memoryFile)) return;
        try {
            String text = Files.readString(memoryFile);
            int start = text.indexOf(MEMORY_START);
            int end = text.indexOf(MEMORY_END);
            if (start < 0 || end <= start) return;
            String block = text.substring(start + MEMORY_START.length(), end);
            boolean readingQuestions = false;
            memories.clear();
            questionHistory.clear();
            for (String line : block.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.equals("## Question history")) { readingQuestions = true; continue; }
                if (trimmed.startsWith("## ")) { readingQuestions = false; continue; }
                if (readingQuestions && trimmed.startsWith("- Question: ")) {
                    questionHistory.add(unescapeMemory(trimmed.substring("- Question: ".length())));
                    continue;
                }
                Matcher oldItem = Pattern.compile("^- \\*\\*(.+?)\\*\\*: (.*)$").matcher(trimmed);
                if (oldItem.matches()) {
                    memories.put(unescapeMemory(oldItem.group(1)).toLowerCase(Locale.ROOT), unescapeMemory(oldItem.group(2)));
                    continue;
                }
                Matcher field = Pattern.compile("^([A-Za-z][A-Za-z0-9 _-]*): (.*)$").matcher(trimmed);
                if (!readingQuestions && field.matches())
                    memories.put(field.group(1).toLowerCase(Locale.ROOT), unescapeMemory(field.group(2)));
            }
            String savedName = memories.get("name");
            if (savedName != null) userName = capitalize(savedName);
        } catch (IOException ignored) {
            // The assistant remains usable even if memory cannot be read.
        }
    }

    private void recordQuestion(String question) {
        questionHistory.add(question);
        saveMemories();
    }

    private Brain.Answer answerFromMemory(String question) {
        Matcher personal = Pattern.compile("(?i).*(?:what is|what's|tell me) my ([a-z0-9 _-]+?)[?.!]*$").matcher(question);
        if (personal.matches()) {
            String key = personal.group(1).trim().toLowerCase(Locale.ROOT);
            String value = memories.get(key);
            if (value != null) return result("You told me your " + displayKey(key) + " is " + value + ".", "MEMORY_GROUNDED", 1);
        }
        for (Map.Entry<String, String> memory : memories.entrySet()) {
            String key = displayKey(memory.getKey());
            if (!memory.getKey().startsWith("fact-") && containsMeaningfulPhrase(question, key))
                return result("You told me your " + key + " is " + memory.getValue() + ".", "MEMORY_GROUNDED", 1);
        }
        return null;
    }

    private String memoryPrefix(String question) {
        Map.Entry<String, String> best = null;
        int bestScore = 0;
        for (Map.Entry<String, String> memory : memories.entrySet()) {
            int score = overlapScore(question, displayKey(memory.getKey()) + " " + memory.getValue());
            if (score > bestScore) { best = memory; bestScore = score; }
        }
        if (best == null || bestScore == 0) return "";
        return "Using your saved memory (“" + displayKey(best.getKey()) + ": " + best.getValue() + "”), ";
    }

    private static int overlapScore(String left, String right) {
        int score = 0;
        for (String word : left.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
            if ((word.length() >= 3 || "ai".equals(word)) && right.toLowerCase(Locale.ROOT).matches(".*\\b" + Pattern.quote(word) + "\\b.*")) score++;
        return score;
    }

    private static boolean containsMeaningfulPhrase(String text, String phrase) {
        return phrase.length() >= 3 && text.matches(".*\\b" + Pattern.quote(phrase) + "\\b.*");
    }

    private void saveMemories() {
        try {
            String text = Files.exists(memoryFile) ? Files.readString(memoryFile) : "# Nova Memory\n";
            StringBuilder block = new StringBuilder(MEMORY_START).append("\n\n## Saved assistant memories\n\n");
            if (memories.isEmpty()) block.append("_No personal memories saved._\n");
            else memories.forEach((key, value) -> block.append(fieldName(key)).append(": ").append(escapeMemory(value)).append('\n'));
            block.append("\n## Question history\n\n");
            if (questionHistory.isEmpty()) block.append("_No questions recorded._\n");
            else for (String question : questionHistory)
                block.append("- Question: ").append(escapeMemory(question)).append('\n');
            block.append('\n').append(MEMORY_END);
            int start = text.indexOf(MEMORY_START);
            int end = text.indexOf(MEMORY_END);
            if (start >= 0 && end >= start) text = text.substring(0, start) + block + text.substring(end + MEMORY_END.length());
            else text = text.stripTrailing() + "\n\n" + block + "\n";
            Path parent = memoryFile.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(memoryFile, text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
            // A read-only file must not crash the chat UI.
        }
    }

    private static Path findMemoryFile() {
        Path helper = Path.of("helper", "MEMORY.md");
        if (Files.exists(helper) || Files.isDirectory(Path.of("helper"))) return helper;
        return Path.of("MEMORY.md");
    }

    private static String displayKey(String key) { return key.startsWith("fact-") ? "fact" : key.replace('-', ' '); }
    private static String fieldName(String key) {
        if (key.startsWith("fact-")) return "Fact " + key.substring("fact-".length());
        String words = key.replace('-', ' ').trim();
        if (words.startsWith("ai ")) return "AI " + words.substring(3);
        if (words.equals("ai")) return "AI";
        return words.isEmpty() ? "Memory" : Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
    private static String escapeMemory(String text) { return text.replace("\\", "\\\\").replace("\n", "\\n").replace("*", "\\*"); }
    private static String unescapeMemory(String text) { return text.replace("\\n", "\n").replace("\\*", "*").replace("\\\\", "\\"); }

    private static boolean containsTopic(String text, String alternatives) {
        for (String topic : alternatives.split("\\|")) {
            topic = topic.trim();
            if (topic.length() <= 2 ? text.contains(" " + topic + " ") : text.contains(topic)) return true;
        }
        return false;
    }

    private static String extractExpression(String text) {
        String candidate = text.replace("what is", "").replace("calculate", "").replace("solve", "")
                .replace("plus", "+").replace("minus", "-").replace("times", "*").replace("multiplied by", "*")
                .replace("divided by", "/").replace("modulo", "%").replace("?", "").trim();
        return candidate.matches("[0-9+*/%.()^\\-\\s]+") && candidate.matches(".*\\d.*") ? candidate : null;
    }

    private static Brain.Answer result(String text, String type, double confidence) { return new Brain.Answer(text, type, confidence); }
    private static String capitalize(String s) { return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT); }
    private static String summarize(String s) { return s.length() <= 70 ? s : s.substring(0, 67) + "..."; }
    private static String subjectOf(String text) {
        String subject = text.replaceFirst("^(what (?:is|are)|who(?: is| are| did)?|when(?: is| are| did| does)?|where(?: is| are| did| does)?|why(?: is| are| did| does)?|how(?: do i| can i| should i| to)?|define|compare|is|are|can|could|should|would|will|do|does|did|has|have)\\s+", "")
                .replaceAll("[?.!]+$", "").trim();
        return subject.isEmpty() ? "that question" : subject;
    }

    private static final class MathParser {
        private final String input; private int position;
        MathParser(String input) { this.input = input; }
        double parse() { double value = expression(); spaces(); if (position != input.length() || !Double.isFinite(value)) throw new IllegalArgumentException(); return value; }
        double expression() { double v = term(); while (true) { spaces(); if (take('+')) v += term(); else if (take('-')) v -= term(); else return v; } }
        double term() { double v = power(); while (true) { spaces(); if (take('*')) v *= power(); else if (take('/')) { double d=power(); if(d==0) throw new IllegalArgumentException(); v/=d; } else if(take('%')) v%=power(); else return v; } }
        double power() { double v = unary(); spaces(); if (take('^')) v = Math.pow(v, power()); return v; }
        double unary() { spaces(); if(take('+')) return unary(); if(take('-')) return -unary(); if(take('(')){double v=expression();if(!take(')'))throw new IllegalArgumentException();return v;} return number(); }
        double number() { spaces(); int start=position; while(position<input.length()&&(Character.isDigit(input.charAt(position))||input.charAt(position)=='.'))position++; if(start==position)throw new IllegalArgumentException(); return Double.parseDouble(input.substring(start,position)); }
        boolean take(char c){spaces();if(position<input.length()&&input.charAt(position)==c){position++;return true;}return false;} void spaces(){while(position<input.length()&&Character.isWhitespace(input.charAt(position)))position++;}
    }
}
