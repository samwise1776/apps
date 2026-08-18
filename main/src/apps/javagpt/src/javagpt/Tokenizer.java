package javagpt;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Tokenizer {
    public static final int PAD = 0;
    public static final int UNK = 1;
    public static final int BOS = 2;
    public static final int EOS = 3;
    public static final int SPECIAL_COUNT = 4;

    private final Map<Character, Integer> charToId = new HashMap<>();
    private final Map<Integer, Character> idToChar = new HashMap<>();
    private int vocabSize = SPECIAL_COUNT;

    public Tokenizer() {
        idToChar.put(PAD, '\0');
        idToChar.put(UNK, '?');
        idToChar.put(BOS, '\u0002');
        idToChar.put(EOS, '\u0003');
    }

    public void buildFromText(String text) {
        Set<Character> chars = new TreeSet<>();
        for (char c : text.toCharArray()) chars.add(c);
        for (char c : chars) {
            if (!charToId.containsKey(c)) {
                int id = vocabSize++;
                charToId.put(c, id);
                idToChar.put(id, c);
            }
        }
    }

    public int[] encode(String text) {
        int[] tokens = new int[text.length() + 2];
        tokens[0] = BOS;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            tokens[i + 1] = charToId.getOrDefault(c, UNK);
        }
        tokens[text.length() + 1] = EOS;
        return tokens;
    }

    public int[] encodeRaw(String text) {
        int[] tokens = new int[text.length()];
        for (int i = 0; i < text.length(); i++) {
            tokens[i] = charToId.getOrDefault(text.charAt(i), UNK);
        }
        return tokens;
    }

    public String decode(int[] tokens) {
        StringBuilder sb = new StringBuilder();
        for (int t : tokens) {
            if (t == BOS || t == EOS || t == PAD) continue;
            Character c = idToChar.get(t);
            sb.append(c != null ? c : '?');
        }
        return sb.toString();
    }

    public String decodeSingle(int token) {
        if (token == BOS || token == EOS || token == PAD) return "";
        Character c = idToChar.get(token);
        return c != null ? c.toString() : "?";
    }

    public int getVocabSize() { return vocabSize; }

    public void save(String path) throws IOException {
        DataOutputStream out = new DataOutputStream(new FileOutputStream(path));
        out.writeInt(vocabSize);
        out.writeInt(charToId.size());
        for (Map.Entry<Character, Integer> e : charToId.entrySet()) {
            out.writeChar(e.getKey());
            out.writeInt(e.getValue());
        }
        out.close();
    }

    public void load(String path) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(path));
        vocabSize = in.readInt();
        int count = in.readInt();
        charToId.clear();
        idToChar.clear();
        idToChar.put(PAD, '\0');
        idToChar.put(UNK, '?');
        idToChar.put(BOS, '\u0002');
        idToChar.put(EOS, '\u0003');
        for (int i = 0; i < count; i++) {
            char c = in.readChar();
            int id = in.readInt();
            charToId.put(c, id);
            idToChar.put(id, c);
        }
        in.close();
    }

    public String vocabInfo() {
        return "VocabSize=" + vocabSize + " (special=" + SPECIAL_COUNT + ", chars=" + (vocabSize - SPECIAL_COUNT) + ")";
    }
}
