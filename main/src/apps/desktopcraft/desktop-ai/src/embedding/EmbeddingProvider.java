package embedding;

import java.util.List;

/**
 * Abstraction for producing vector embeddings. The local implementation works
 * without any external service; a real model (OpenAI embeddings, a local sentence
 * model) can be plugged in behind this same interface.
 */
public interface EmbeddingProvider {
    Embedding embed(String text);

    List<Embedding> embedAll(List<String> texts);

    int dimension();

    String name();
}
