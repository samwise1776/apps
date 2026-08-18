package neural;

/** Logistic sigmoid. */
public final class Sigmoid implements ActivationFunction {
    @Override
    public double apply(double input) {
        if (input >= 0.0) {
            return 1.0 / (1.0 + Math.exp(-input));
        }
        double exp = Math.exp(input);
        return exp / (1.0 + exp);
    }

    @Override
    public double derivative(double activated) {
        return activated * (1.0 - activated);
    }

    @Override
    public String name() {
        return "Sigmoid";
    }
}
