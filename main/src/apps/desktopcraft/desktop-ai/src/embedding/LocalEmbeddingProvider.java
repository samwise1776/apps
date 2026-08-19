package embedding;

import util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dependency-free local embedding provider. Uses a fixed vocabulary of feature
 * tokens (word stems plus character n-grams) so that code chunks sharing names,
 * keywords, and structure land near each other in the vector space.
 *
 * This is deliberately modular: swap in a real model by implementing
 * {@link EmbeddingProvider} and registering it in the factory.
 */
public final class LocalEmbeddingProvider implements EmbeddingProvider {
    public static final int DIMENSION = 384;
    private final int nGramSize;
    private final Map<String, Integer> featureIndex = new ConcurrentHashMap<>();
    private final int[] featureCounter = {0};
    private final boolean useCharGrams;

    public LocalEmbeddingProvider() {
        this(3, true);
    }

    public LocalEmbeddingProvider(int nGramSize, boolean useCharGrams) {
        this.nGramSize = nGramSize;
        this.useCharGrams = useCharGrams;
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }

    @Override
    public String name() {
        return "LocalEmbedding(" + DIMENSION + "d)";
    }

    @Override
    public Embedding embed(String text) {
        double[] vector = new double[DIMENSION];
        List<String> features = features(text);
        for (String feature : features) {
            int index = featureIndex.computeIfAbsent(feature, key -> {
                if (featureCounter[0] >= DIMENSION) {
                    return featureCounter[0] % DIMENSION;
                }
                return featureCounter[0]++;
            });
            vector[index] += 1.0;
        }
        if (vector.length == 0) {
            return new Embedding(text, vector);
        }
        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm > 0.0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return new Embedding(text, vector);
    }

    @Override
    public List<Embedding> embedAll(List<String> texts) {
        List<Embedding> result = new ArrayList<>();
        for (String text : texts) {
            result.add(embed(text));
        }
        return result;
    }

    private List<String> features(String text) {
        List<String> result = new ArrayList<>();
        String lower = text.toLowerCase();
        List<String> tokens = Text.tokens(lower);
        for (String token : tokens) {
            if (token.length() >= 2 && !token.matches("\\d+")) {
                result.add("w:" + token);
                if (useCharGrams) {
                    for (int i = 0; i + nGramSize <= token.length(); i++) {
                        result.add("n:" + token.substring(i, i + nGramSize));
                    }
                }
            }
        }
        return result;
    }
}
