package neural;

/** A neural-network layer: transforms one vector into the next. */
public interface Layer {
    /** Number of inputs this layer accepts. */
    int inputSize();

    /** Number of outputs this layer produces. */
    int outputSize();

    /** Forward pass. */
    Vector forward(Vector input);

    /**
     * Backward pass. Receives the gradient of the loss with respect to this layer's
     * output and returns the gradient with respect to the layer's input.
     */
    Vector backward(Vector input, Vector output, Vector gradient);

    ActivationFunction activation();

    Matrix weights();

    Vector biases();
}
