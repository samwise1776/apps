package neural;

/** Hyperbolic tangent. */
public final class Tanh implements ActivationFunction {
    @Override
    public double apply(double input) {
        return Math.tanh(input);
    }

    @Override
    public double derivative(double activated) {
        return 1.0 - activated * activated;
    }

    @Override
    public String name() {
        return "Tanh";
    }
}
