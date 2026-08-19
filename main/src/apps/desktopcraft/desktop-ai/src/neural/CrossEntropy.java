package neural;

/** Categorical cross-entropy loss (one-hot or label targets). */
public final class CrossEntropy implements LossFunction {
    private static final double EPSILON = 1e-12;

    @Override
    public double loss(Vector predicted, Vector target) {
        double sum = 0.0;
        for (int i = 0; i < predicted.size(); i++) {
            double clamped = Math.max(EPSILON, Math.min(1.0 - EPSILON, predicted.get(i)));
            if (target.get(i) > 0.0) {
                sum -= target.get(i) * Math.log(clamped);
            }
        }
        return sum;
    }

    @Override
    public Vector gradient(Vector predicted, Vector target) {
        Vector result = new Vector(predicted.size());
        for (int i = 0; i < predicted.size(); i++) {
            double clamped = Math.max(EPSILON, Math.min(1.0 - EPSILON, predicted.get(i)));
            result.set(i, clamped - target.get(i));
        }
        return result;
    }

    @Override
    public String name() {
        return "CrossEntropy";
    }
}
