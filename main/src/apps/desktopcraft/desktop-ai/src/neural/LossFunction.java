package neural;

/** Loss function contract. */
public interface LossFunction {
    /** Average loss over predicted vs target vectors. */
    double loss(Vector predicted, Vector target);

    /** Gradient of the loss with respect to pre-activation outputs for backpropagation. */
    Vector gradient(Vector predicted, Vector target);

    String name();
}
