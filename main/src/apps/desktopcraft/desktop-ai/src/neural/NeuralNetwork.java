package neural;

import util.Json;
import util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A real multi-layer perceptron trainer with forward propagation, backpropagation,
 * gradient descent (SGD/Momentum/Adam), batching, validation, and JSON serialization.
 */
public final class NeuralNetwork {
    public interface Listener {
        void onEpoch(TrainingMetrics metrics, NeuralNetwork network);
    }

    private final List<DenseLayer> layers;
    private final Random random;
    private volatile boolean stopRequested = false;

    public NeuralNetwork(int inputSize, int[] hiddenSizes, int outputSize, ActivationFunction activation, long seed) {
        this.random = new Random(seed);
        this.layers = new ArrayList<>();
        int previous = inputSize;
        for (int size : hiddenSizes) {
            layers.add(new DenseLayer(previous, size, activation, random));
            previous = size;
        }
        layers.add(new DenseLayer(previous, outputSize, new Linear(), random));
    }

    public NeuralNetwork(int inputSize, int[] hiddenSizes, int outputSize, ActivationFunction activation) {
        this(inputSize, hiddenSizes, outputSize, activation, System.nanoTime());
    }

    public NeuralNetwork(List<DenseLayer> layers) {
        this.layers = layers;
        this.random = new Random();
    }

    public List<DenseLayer> layers() {
        return layers;
    }

    public int inputSize() {
        return layers.get(0).inputSize();
    }

    public int outputSize() {
        return layers.get(layers.size() - 1).outputSize();
    }

    public void requestStop() {
        stopRequested = true;
    }

    public void resetStop() {
        stopRequested = false;
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    /** Forward pass returning the network output. */
    public Vector forward(Vector input) {
        Vector current = input;
        for (DenseLayer layer : layers) {
            current = layer.forward(current);
        }
        return current;
    }

    /** Forward pass returning the activation of every layer (input + hidden + output). */
    public List<Vector> forwardTrace(Vector input) {
        List<Vector> trace = new ArrayList<>();
        Vector current = input;
        trace.add(current);
        for (DenseLayer layer : layers) {
            current = layer.forward(current);
            trace.add(current);
        }
        return trace;
    }

    public double loss(Dataset dataset, LossFunction lossFunction) {
        double total = 0.0;
        for (Sample sample : dataset.all()) {
            total += lossFunction.loss(forward(sample.input()), sample.target());
        }
        return dataset.size() == 0 ? 0.0 : total / dataset.size();
    }

    public double accuracy(Dataset dataset) {
        if (dataset.outputSize() == 1) {
            int correct = 0;
            for (Sample sample : dataset.all()) {
                double predicted = forward(sample.input()).get(0);
                double expected = sample.target().get(0);
                if (Math.abs(predicted - expected) < 0.5) correct++;
            }
            return dataset.size() == 0 ? 0.0 : (double) correct / dataset.size();
        }
        int correct = 0;
        for (Sample sample : dataset.all()) {
            if (forward(sample.input()).argmax() == sample.target().argmax()) correct++;
        }
        return dataset.size() == 0 ? 0.0 : (double) correct / dataset.size();
    }

    /**
     * Backpropagation over one sample. Returns the output-layer error gradient used
     * by {@link #applyGradients}.
     */
    public Vector backpropagate(Sample sample, LossFunction lossFunction) {
        List<Vector> trace = forwardTrace(sample.input());
        Vector output = trace.get(trace.size() - 1);
        Vector gradient = lossFunction.gradient(output, sample.target());
        for (int i = layers.size() - 1; i >= 0; i--) {
            gradient = layers.get(i).backward(trace.get(i), trace.get(i + 1), gradient);
        }
        return gradient;
    }

    /** Applies stored per-layer gradients using an optimizer. */
    public void applyGradients(Optimizer optimizer) {
        for (DenseLayer layer : layers) {
            optimizer.update(layer.weights(), layer.weightGradients());
            optimizer.update(layer.biases(), layer.biasGradients());
        }
    }

    /**
     * Trains the network with the given loss function, optimizer, epochs, and batch
     * size. Reports metrics each epoch through the listener.
     */
    public void train(Dataset dataset, LossFunction lossFunction, Optimizer optimizer,
                      int epochs, int batchSize, Listener listener) {
        resetStop();
        List<Sample> shuffled = dataset.shuffled(random);
        for (int epoch = 0; epoch < epochs; epoch++) {
            long started = System.currentTimeMillis();
            for (int start = 0; start < shuffled.size(); start += batchSize) {
                int end = Math.min(start + batchSize, shuffled.size());
                for (int index = start; index < end; index++) {
                    Sample sample = shuffled.get(index);
                    backpropagate(sample, lossFunction);
                    applyGradients(optimizer);
                }
            }
            double loss = lossFunction.loss(forward(dataset.get(0).input()), dataset.get(0).target());
            double fullLoss = loss(dataset, lossFunction);
            double accuracy = accuracy(dataset);
            if (listener != null) {
                listener.onEpoch(new TrainingMetrics(epoch + 1, fullLoss, accuracy, System.currentTimeMillis() - started), this);
            }
            if (stopRequested) {
                Log.info("Training stopped by user at epoch %d", epoch + 1);
                break;
            }
        }
        resetStop();
    }

    /** Simple full-batch training without listener. */
    public void train(Dataset dataset, LossFunction lossFunction, Optimizer optimizer, int epochs) {
        train(dataset, lossFunction, optimizer, epochs, dataset.size(), null);
    }

    public String describe() {
        StringBuilder out = new StringBuilder("NeuralNetwork(");
        out.append(inputSize());
        for (DenseLayer layer : layers) {
            out.append(" -> ").append(layer.outputSize());
        }
        out.append(")");
        return out.toString();
    }

    /** Serializes weights, biases, activations, and topology to a JSON map. */
    public Map<String, Object> toJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Object> layersJson = new ArrayList<>();
        for (DenseLayer layer : layers) {
            Map<String, Object> layerJson = new LinkedHashMap<>();
            layerJson.put("input", layer.inputSize());
            layerJson.put("output", layer.outputSize());
            layerJson.put("activation", layer.activation().name());
            layerJson.put("weights", layer.weights().toJson());
            layerJson.put("biases", layer.biases().toJson());
            layersJson.add(layerJson);
        }
        root.put("layers", layersJson);
        root.put("format", "desktopcraft-neural-v1");
        return root;
    }

    public String toJsonString() {
        return Json.writePretty(toJson());
    }

    public static NeuralNetwork fromJson(Object node) {
        List<Object> layersJson = Json.array(Json.object(node).get("layers"));
        List<DenseLayer> layers = new ArrayList<>();
        for (Object item : layersJson) {
            Map<String, Object> layerJson = Json.object(item);
            Matrix weights = Matrix.fromJson(layerJson.get("weights"));
            Vector biases = Vector.fromJson(layerJson.get("biases"));
            String activationName = Json.string(layerJson, "activation", "ReLU");
            ActivationFunction activation = activationByName(activationName);
            layers.add(new DenseLayer(weights, biases, activation));
        }
        return new NeuralNetwork(layers);
    }

    public static NeuralNetwork load(String json) {
        return fromJson(Json.parse(json));
    }

    public static ActivationFunction activationByName(String name) {
        if (name == null) return new ReLU();
        switch (name.toLowerCase()) {
            case "relu": return new ReLU();
            case "sigmoid": return new Sigmoid();
            case "tanh": return new Tanh();
            case "softmax": return new Softmax();
            case "linear": return new Linear();
            default: return new ReLU();
        }
    }
}
