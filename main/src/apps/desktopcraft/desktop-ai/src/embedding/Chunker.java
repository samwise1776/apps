package embedding;

import util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Splits large code/text into overlapping chunks for embedding and retrieval. */
public final class Chunker {
    public static final int DEFAULT_CHUNK_SIZE = 240;
    public static final int DEFAULT_OVERLAP = 40;

    private final int chunkSize;
    private final int overlap;

    public Chunker() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public Chunker(int chunkSize, int overlap) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public int chunkSize() {
        return chunkSize;
    }

    public int overlap() {
        return overlap;
    }

    /** Splits text into line-based chunks, preferring to break at blank lines. */
    public List<String> chunk(String text) {
        List<String> result = new ArrayList<>();
        List<String> lines = Text.lines(text);
        if (lines.isEmpty()) {
            return result;
        }
        int start = 0;
        while (start < lines.size()) {
            int end = Math.min(start + chunkSize, lines.size());
            StringBuilder current = new StringBuilder();
            for (int i = start; i < end; i++) {
                current.append(lines.get(i));
                if (i < end - 1) current.append('\n');
            }
            result.add(current.toString());
            int next = start + chunkSize - overlap;
            if (next <= start) next = start + 1;
            start = next;
        }
        return result;
    }

    /** Chunks by sentences for natural-language text (markdown/docs). */
    public List<String> chunkSentences(String text) {
        List<String> result = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (current.length() > 0 && current.length() + sentence.length() > chunkSize) {
                result.add(current.toString().trim());
                current.setLength(0);
            }
            if (current.length() > 0) current.append(' ');
            current.append(sentence);
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }
}
