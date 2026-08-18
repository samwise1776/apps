package neural;

/** Mean squared error loss. */
public final class MSE implements LossFunction {
    @Override
    public double loss(Vector predicted, Vector target) {
        double sum = 0.0;
        for (int i = 0; i < predicted.size(); i++) {
            double diff = predicted.get(i) - target.get(i);
            sum += diff * diff;
        }
        return sum / predicted.size();
    }

    @Override
    public Vector gradient(Vector predicted, Vector target) {
        Vector result = new Vector(predicted.size());
        for (int i = 0; i < predicted.size(); i++) {
            result.set(i, 2.0 * (predicted.get(i) - target.get(i)) / predicted.size());
        }
        return result;
    }

    @Override
    public String name() {
        return "MSE";
    }
}
