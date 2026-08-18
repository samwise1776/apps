package neural;

/** Optimizer contract: applies gradient updates to a parameter matrix or vector. */
public interface Optimizer {
    void update(Matrix weights, Matrix weightGradients);

    void update(Vector biases, Vector biasGradients);

    String name();
}
