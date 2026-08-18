package neural;

import java.util.Random;

/**
 * A dense (fully connected) layer. Stores weights, biases, activation, and the most
 * recent weight/bias gradients produced during backpropagation.
 */
public final class DenseLayer implements Layer {
    private final int inputSize;
    private final int outputSize;
    private final Matrix weights;
    private final Vector biases;
    private final ActivationFunction activation;
    private final Matrix weightGradients;
    private final Vector biasGradients;
    private final double initScale;

    public DenseLayer(int inputSize, int outputSize, ActivationFunction activation, Random random) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.activation = activation;
        this.initScale = Math.sqrt(2.0 / (inputSize + outputSize));
        this.weights = Matrix.random(outputSize, inputSize, random, initScale);
        this.biases = Vector.zeros(outputSize);
        this.weightGradients = Matrix.zeros(outputSize, inputSize);
        this.biasGradients = Vector.zeros(outputSize);
    }

    public DenseLayer(Matrix weights, Vector biases, ActivationFunction activation) {
        this.inputSize = weights.cols();
        this.outputSize = weights.rows();
        this.weights = weights.copy();
        this.biases = biases.copy();
        this.activation = activation;
        this.initScale = Math.sqrt(2.0 / (inputSize + outputSize));
        this.weightGradients = Matrix.zeros(outputSize, inputSize);
        this.biasGradients = Vector.zeros(outputSize);
    }

    public double initScale() {
        return initScale;
    }

    @Override
    public int inputSize() {
        return inputSize;
    }

    @Override
    public int outputSize() {
        return outputSize;
    }

    @Override
    public ActivationFunction activation() {
        return activation;
    }

    @Override
    public Matrix weights() {
        return weights;
    }

    @Override
    public Vector biases() {
        return biases;
    }

    public Matrix weightGradients() {
        return weightGradients;
    }

    public Vector biasGradients() {
        return biasGradients;
    }

    @Override
    public Vector forward(Vector input) {
        Vector weighted = Matrix.multiplyVector(weights, input);
        for (int i = 0; i < outputSize; i++) {
            weighted.set(i, weighted.get(i) + biases.get(i));
        }
        return weighted.apply(activation::apply);
    }

    @Override
    public Vector backward(Vector input, Vector output, Vector gradient) {
        for (int i = 0; i < outputSize; i++) {
            double derivative = activation.derivative(output.get(i));
            biasGradients.set(i, gradient.get(i) * derivative);
        }
        for (int i = 0; i < outputSize; i++) {
            double factor = biasGradients.get(i);
            for (int j = 0; j < inputSize; j++) {
                weightGradients.set(i, j, factor * input.get(j));
            }
        }
        Vector inputGradient = new Vector(inputSize);
        for (int j = 0; j < inputSize; j++) {
            double sum = 0.0;
            for (int i = 0; i < outputSize; i++) {
                sum += weights.get(i, j) * biasGradients.get(i);
            }
            inputGradient.set(j, sum);
        }
        return inputGradient;
    }
}
