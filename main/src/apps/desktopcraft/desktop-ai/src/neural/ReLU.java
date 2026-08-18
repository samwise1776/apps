package neural;

/** Rectified linear unit. */
public final class ReLU implements ActivationFunction {
    @Override
    public double apply(double input) {
        return Math.max(0.0, input);
    }

    @Override
    public double derivative(double activated) {
        return activated > 0.0 ? 1.0 : 0.0;
    }

    @Override
    public String name() {
        return "ReLU";
    }
}
