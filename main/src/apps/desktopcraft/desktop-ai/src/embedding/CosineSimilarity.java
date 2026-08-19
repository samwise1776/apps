package embedding;

import java.util.List;
import java.util.Map;

/** Cosine similarity and ranking helpers for embedding search. */
public final class CosineSimilarity {
    private CosineSimilarity() {}

    public static double cosine(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector dimension mismatch: " + a.length + " vs " + b.length);
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /** Ranks embeddings against a query vector, most similar first. */
    public static List<ScoredEmbedding> rank(double[] query, List<Embedding> corpus, int limit) {
        java.util.List<ScoredEmbedding> scored = new java.util.ArrayList<>();
        for (Embedding embedding : corpus) {
            scored.add(new ScoredEmbedding(embedding, cosine(query, embedding.vector())));
        }
        scored.sort((left, right) -> Double.compare(right.score, left.score));
        if (limit > 0 && scored.size() > limit) {
            return scored.subList(0, limit);
        }
        return scored;
    }

    /** Result of a similarity search. */
    public static final class ScoredEmbedding implements Comparable<ScoredEmbedding> {
        private final Embedding embedding;
        private final double score;

        ScoredEmbedding(Embedding embedding, double score) {
            this.embedding = embedding;
            this.score = score;
        }

        public Embedding embedding() {
            return embedding;
        }

        public double score() {
            return score;
        }

        @Override
        public int compareTo(ScoredEmbedding other) {
            return Double.compare(other.score, score);
        }
    }
}
