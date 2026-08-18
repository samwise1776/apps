package neural;

/** Momentum SGD. Keeps per-parameter velocity to smooth gradient updates. */
public final class Momentum implements Optimizer {
    private final double learningRate;
    private final double momentum;
    private final java.util.Map<Object, double[]> velocity = new java.util.HashMap<>();

    public Momentum(double learningRate, double momentum) {
        if (momentum < 0.0 || momentum >= 1.0) {
            throw new IllegalArgumentException("Momentum must be in [0, 1)");
        }
        this.learningRate = learningRate;
        this.momentum = momentum;
    }

    @Override
    public void update(Matrix weights, Matrix weightGradients) {
        double[] velocityMatrix = velocity.computeIfAbsent(weights, key -> new double[weights.rows() * weights.cols()]);
        for (int i = 0; i < weights.rows(); i++) {
            for (int j = 0; j < weights.cols(); j++) {
                int index = i * weights.cols() + j;
                velocityMatrix[index] = momentum * velocityMatrix[index] + learningRate * weightGradients.get(i, j);
                weights.set(i, j, weights.get(i, j) - velocityMatrix[index]);
            }
        }
    }

    @Override
    public void update(Vector biases, Vector biasGradients) {
        double[] velocityVector = velocity.computeIfAbsent(biases, key -> new double[biases.size()]);
        for (int i = 0; i < biases.size(); i++) {
            velocityVector[i] = momentum * velocityVector[i] + learningRate * biasGradients.get(i);
            biases.set(i, biases.get(i) - velocityVector[i]);
        }
    }

    @Override
    public String name() {
        return "Momentum(lr=" + learningRate + ", mu=" + momentum + ")";
    }
}
