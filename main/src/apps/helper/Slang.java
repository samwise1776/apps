import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Recognizes and expands common conversational slang without dependencies. */
public final class Slang {
    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9']+");
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public Slang() {
        add("af", "very", "emphasis meaning extremely");
        add("aight", "all right", "agreement or acknowledgement");
        add("ain't", "is not", "informal negative contraction");
        add("bae", "partner", "a romantic partner or loved one");
        add("bet", "okay", "agreement or acceptance");
        add("brb", "be right back", "temporarily leaving");
        add("bro", "friend", "friendly form of address");
        add("bruh", "seriously", "surprise, disbelief, or frustration");
        add("bussin", "excellent", "especially good, often describing food");
        add("cap", "lie", "something false or exaggerated");
        add("cuz", "because", "short form of because");
        add("delulu", "delusional", "unrealistically optimistic or mistaken");
        add("fam", "close friends", "a trusted friend group");
        add("finna", "going to", "intending to do something soon");
        add("fire", "excellent", "very impressive or enjoyable");
        add("fr", "for real", "sincerely or genuinely");
        add("goat", "greatest of all time", "the best in a category");
        add("gonna", "going to", "informal future intention");
        add("gotta", "have to", "informal statement of necessity");
        add("hbu", "how about you", "returns a question to someone");
        add("hmu", "contact me", "request to send a message");
        add("idc", "I do not care", "lack of preference or concern");
        add("idk", "I do not know", "uncertainty or missing knowledge");
        add("ikr", "I know, right", "strong agreement");
        add("imo", "in my opinion", "marks a personal view");
        add("imho", "in my humble opinion", "marks a personal view");
        add("irl", "in real life", "outside the internet");
        add("iykyk", "if you know, you know", "shared insider knowledge");
        add("lit", "exciting", "excellent, energetic, or enjoyable");
        add("lmao", "laughing a lot", "strong amusement");
        add("lol", "laughing out loud", "amusement or a light tone");
        add("lowkey", "somewhat", "quietly or to a moderate degree");
        add("mid", "mediocre", "average or disappointing");
        add("nah", "no", "informal disagreement");
        add("ngl", "not going to lie", "signals an honest opinion");
        add("noob", "beginner", "an inexperienced person");
        add("omg", "oh my goodness", "surprise or excitement");
        add("omw", "on my way", "currently traveling there");
        add("rn", "right now", "at the present moment");
        add("slaps", "is excellent", "describes especially good music or food");
        add("slay", "do extremely well", "succeed impressively");
        add("smh", "shaking my head", "disappointment or disbelief");
        add("sus", "suspicious", "untrustworthy or questionable");
        add("tbh", "to be honest", "introduces a candid opinion");
        add("tho", "though", "informal contrast");
        add("thx", "thanks", "short form of thanks");
        add("wanna", "want to", "informal expression of desire");
        add("wyd", "what are you doing", "asks what someone is doing");
        add("wym", "what do you mean", "asks for clarification");
        add("y'all", "you all", "plural form of you");
        add("yall", "you all", "plural form of you");
        add("yeet", "throw forcefully", "throw something with energy");
        add("yolo", "you only live once", "encourages taking an opportunity");
    }

    private void add(String slang, String expansion, String meaning) {
        entries.put(slang.toLowerCase(Locale.ROOT), new Entry(slang, expansion, meaning));
    }

    public boolean isSlang(String word) { return entries.containsKey(normalize(word)); }
    public String expand(String word) { Entry e = entries.get(normalize(word)); return e == null ? word : e.expansion(); }
    public String explain(String word) { Entry e = entries.get(normalize(word)); return e == null ? "No slang definition found for “" + word + "”." : e.slang() + " means “" + e.expansion() + "”: " + e.meaning() + "."; }

    public List<String> findSlang(String text) {
        List<String> found = new ArrayList<>();
        Matcher matcher = WORD.matcher(text == null ? "" : text);
        while (matcher.find()) {
            String key = normalize(matcher.group());
            if (entries.containsKey(key) && !found.contains(key)) found.add(key);
        }
        return found;
    }

    public String expandText(String text) {
        if (text == null || text.isEmpty()) return text == null ? "" : text;
        Matcher matcher = WORD.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String original = matcher.group();
            String replacement = expand(original);
            if (Character.isUpperCase(original.charAt(0)) && !replacement.isEmpty())
                replacement = Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public Map<String, String> getExpansions() {
        Map<String, String> copy = new LinkedHashMap<>();
        entries.forEach((key, value) -> copy.put(key, value.expansion()));
        return Map.copyOf(copy);
    }

    private static String normalize(String word) { return word == null ? "" : word.toLowerCase(Locale.ROOT).trim(); }
    private record Entry(String slang, String expansion, String meaning) { }

    public static void main(String[] args) {
        Slang slang = new Slang();
        if (args.length == 0) { System.out.println("Usage: java Slang <word or sentence>"); return; }
        String input = String.join(" ", args);
        System.out.println("Expanded: " + slang.expandText(input));
        for (String word : slang.findSlang(input)) System.out.println(slang.explain(word));
    }
}
