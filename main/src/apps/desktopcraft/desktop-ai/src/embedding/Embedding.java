package embedding;

import java.util.List;

/** A single vector representation of a text chunk. */
public final class Embedding {
    private final String text;
    private final double[] vector;

    public Embedding(String text, double[] vector) {
        this.text = text;
        this.vector = vector.clone();
    }

    public String text() {
        return text;
    }

    public double[] vector() {
        return vector.clone();
    }

    public int dimension() {
        return vector.length;
    }

    @Override
    public String toString() {
        return "Embedding(dim=" + vector.length + ", text=" + text.length() + " chars)";
    }
}
