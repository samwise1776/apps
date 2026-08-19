package neural;

/** Stochastic gradient descent with a fixed learning rate. */
public final class SGD implements Optimizer {
    private final double learningRate;

    public SGD(double learningRate) {
        this.learningRate = learningRate;
    }

    public double learningRate() {
        return learningRate;
    }

    @Override
    public void update(Matrix weights, Matrix weightGradients) {
        for (int i = 0; i < weights.rows(); i++) {
            for (int j = 0; j < weights.cols(); j++) {
                weights.set(i, j, weights.get(i, j) - learningRate * weightGradients.get(i, j));
            }
        }
    }

    @Override
    public void update(Vector biases, Vector biasGradients) {
        for (int i = 0; i < biases.size(); i++) {
            biases.set(i, biases.get(i) - learningRate * biasGradients.get(i));
        }
    }

    @Override
    public String name() {
        return "SGD(lr=" + learningRate + ")";
    }
}
