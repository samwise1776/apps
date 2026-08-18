package neural;

/**
 * Softmax applied per-vector. The element-wise derivative is only meaningful for
 * classification output layers where it is paired with cross-entropy loss.
 */
public final class Softmax implements ActivationFunction {
    @Override
    public double apply(double input) {
        return Math.exp(input);
    }

    @Override
    public double derivative(double activated) {
        return activated * (1.0 - activated);
    }

    /** Normalizes a vector into a probability distribution. */
    public static Vector softmax(Vector input) {
        double max = input.max();
        double sum = 0.0;
        Vector result = new Vector(input.size());
        for (int i = 0; i < input.size(); i++) {
            double value = Math.exp(input.get(i) - max);
            result.set(i, value);
            sum += value;
        }
        if (sum == 0.0) {
            throw new ArithmeticException("Softmax sum is zero");
        }
        for (int i = 0; i < input.size(); i++) {
            result.set(i, result.get(i) / sum);
        }
        return result;
    }

    @Override
    public String name() {
        return "Softmax";
    }
}
