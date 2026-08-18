package neural;

/** Activation function contract: forward application and derivative of the forward output. */
public interface ActivationFunction {
    double apply(double input);

    double derivative(double activated);

    String name();
}
