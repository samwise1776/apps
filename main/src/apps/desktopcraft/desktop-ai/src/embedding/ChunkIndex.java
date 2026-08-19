package embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Indexes code chunks with embeddings so the AI can retrieve the most relevant
 * files and lines for a question before generating an answer.
 */
public final class ChunkIndex {
    public static final class Entry {
        private final String file;
        private final int startLine;
        private final int endLine;
        private final String content;
        private final Embedding embedding;

        Entry(String file, int startLine, int endLine, String content, Embedding embedding) {
            this.file = file;
            this.startLine = startLine;
            this.endLine = endLine;
            this.content = content;
            this.embedding = embedding;
        }

        public String file() {
            return file;
        }

        public int startLine() {
            return startLine;
        }

        public int endLine() {
            return endLine;
        }

        public String content() {
            return content;
        }

        public Embedding embedding() {
            return embedding;
        }

        @Override
        public String toString() {
            return file + ":" + startLine + "-" + endLine;
        }
    }

    private final EmbeddingProvider provider;
    private final Chunker chunker;
    private final List<Entry> entries = new ArrayList<>();
    private final ConcurrentHashMap<String, Integer> fileVersions = new ConcurrentHashMap<>();
    private volatile boolean dirty = true;

    public ChunkIndex(EmbeddingProvider provider) {
        this(provider, new Chunker());
    }

    public ChunkIndex(EmbeddingProvider provider, Chunker chunker) {
        this.provider = provider;
        this.chunker = chunker;
    }

    /** Indexes a file, replacing any previous chunks for that file. */
    public void index(String file, String content) {
        synchronized (entries) {
            entries.removeIf(entry -> entry.file.equals(file));
            List<String> chunks = chunker.chunk(content);
            List<Embedding> embeddings = provider.embedAll(chunks);
            int line = 1;
            for (int i = 0; i < chunks.size(); i++) {
                int chunkLines = countLines(chunks.get(i));
                entries.add(new Entry(file, line, line + chunkLines - 1, chunks.get(i), embeddings.get(i)));
                line += Math.max(1, chunkLines - chunker.overlap());
            }
            fileVersions.put(file, (fileVersions.getOrDefault(file, 0) + 1));
            dirty = true;
        }
    }

    public void remove(String file) {
        synchronized (entries) {
            entries.removeIf(entry -> entry.file.equals(file));
            dirty = true;
        }
    }

    public void clear() {
        synchronized (entries) {
            entries.clear();
            dirty = true;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        dirty = false;
    }

    public int size() {
        synchronized (entries) {
            return entries.size();
        }
    }

    public int fileCount() {
        return fileVersions.size();
    }

    /** Returns the most relevant chunks for a query. */
    public List<Entry> search(String query, int limit) {
        Embedding queryEmbedding = provider.embed(query);
        List<Entry> snapshot;
        synchronized (entries) {
            snapshot = new ArrayList<>(entries);
        }
        List<Pair> pairs = new ArrayList<>();
        for (Entry entry : snapshot) {
            pairs.add(new Pair(entry, entry.embedding().vector()));
        }
        double[] queryVector = queryEmbedding.vector();
        pairs.sort((left, right) -> Double.compare(
                CosineSimilarity.cosine(queryVector, right.vector),
                CosineSimilarity.cosine(queryVector, left.vector)));
        List<Entry> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, pairs.size()); i++) {
            result.add(pairs.get(i).entry);
        }
        return result;
    }

    public List<Entry> all() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    private static final class Pair {
        final Entry entry;
        final double[] vector;

        Pair(Entry entry, double[] vector) {
            this.entry = entry;
            this.vector = vector;
        }
    }

    private static int countLines(String text) {
        int count = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') count++;
        }
        return count;
    }
}
