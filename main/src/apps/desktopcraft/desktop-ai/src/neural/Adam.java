package neural;

/** Adam optimizer with bias-corrected first and second moment estimates. */
public final class Adam implements Optimizer {
    private final double learningRate;
    private final double beta1;
    private final double beta2;
    private final double epsilon;
    private long step = 0;

    private static final class State {
        final double[] m;
        final double[] v;
        State(int size) {
            this.m = new double[size];
            this.v = new double[size];
        }
    }

    private final java.util.Map<Object, State> states = new java.util.HashMap<>();

    public Adam(double learningRate) {
        this(learningRate, 0.9, 0.999, 1e-8);
    }

    public Adam(double learningRate, double beta1, double beta2, double epsilon) {
        this.learningRate = learningRate;
        this.beta1 = beta1;
        this.beta2 = beta2;
        this.epsilon = epsilon;
    }

    @Override
    public void update(Matrix weights, Matrix weightGradients) {
        step++;
        double mHat = 1.0 - Math.pow(beta1, step);
        double vHat = 1.0 - Math.pow(beta2, step);
        State state = states.computeIfAbsent(weights, key -> new State(weights.rows() * weights.cols()));
        for (int i = 0; i < weights.rows(); i++) {
            for (int j = 0; j < weights.cols(); j++) {
                int index = i * weights.cols() + j;
                double gradient = weightGradients.get(i, j);
                state.m[index] = beta1 * state.m[index] + (1.0 - beta1) * gradient;
                state.v[index] = beta2 * state.v[index] + (1.0 - beta2) * gradient * gradient;
                double correctedM = state.m[index] / mHat;
                double correctedV = state.v[index] / vHat;
                weights.set(i, j, weights.get(i, j) - learningRate * correctedM / (Math.sqrt(correctedV) + epsilon));
            }
        }
    }

    @Override
    public void update(Vector biases, Vector biasGradients) {
        step++;
        double mHat = 1.0 - Math.pow(beta1, step);
        double vHat = 1.0 - Math.pow(beta2, step);
        State state = states.computeIfAbsent(biases, key -> new State(biases.size()));
        for (int i = 0; i < biases.size(); i++) {
            double gradient = biasGradients.get(i);
            state.m[i] = beta1 * state.m[i] + (1.0 - beta1) * gradient;
            state.v[i] = beta2 * state.v[i] + (1.0 - beta2) * gradient * gradient;
            double correctedM = state.m[i] / mHat;
            double correctedV = state.v[i] / vHat;
            biases.set(i, biases.get(i) - learningRate * correctedM / (Math.sqrt(correctedV) + epsilon));
        }
    }

    @Override
    public String name() {
        return "Adam(lr=" + learningRate + ")";
    }
}
