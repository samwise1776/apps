package neural;

/** Identity activation. */
public final class Linear implements ActivationFunction {
    @Override
    public double apply(double input) {
        return input;
    }

    @Override
    public double derivative(double activated) {
        return 1.0;
    }

    @Override
    public String name() {
        return "Linear";
    }
}
