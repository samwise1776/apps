package neural;

/** A single neuron: weights, bias, weighted sum, and activation value. */
public final class Neuron {
    private final Vector weights;
    private double bias;
    private double weightedSum;
    private double activation;

    public Neuron(int inputs, double initBias) {
        this.weights = new Vector(inputs);
        this.bias = initBias;
    }

    public Neuron(Vector weights, double bias) {
        this.weights = weights.copy();
        this.bias = bias;
    }

    public int inputs() {
        return weights.size();
    }

    public Vector weights() {
        return weights;
    }

    public double weight(int index) {
        return weights.get(index);
    }

    public void setWeight(int index, double value) {
        weights.set(index, value);
    }

    public double bias() {
        return bias;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public double weightedSum() {
        return weightedSum;
    }

    public double activation() {
        return activation;
    }

    /** Computes the pre-activation weighted sum from an input vector. */
    public double feed(Vector input) {
        this.weightedSum = weights.dot(input) + bias;
        return weightedSum;
    }

    /** Applies an activation function to the stored weighted sum. */
    public double activate(ActivationFunction function) {
        this.activation = function.apply(weightedSum);
        return activation;
    }
}
