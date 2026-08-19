package neural;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

/** A dense real vector used for neuron activations, biases, targets and gradients. */
public final class Vector {
    private final double[] data;

    public Vector(int size) {
        this.data = new double[size];
    }

    public Vector(double[] data) {
        this.data = data.clone();
    }

    public Vector(List<Double> data) {
        this.data = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            this.data[i] = data.get(i);
        }
    }

    public int size() {
        return data.length;
    }

    public double get(int index) {
        return data[index];
    }

    public void set(int index, double value) {
        data[index] = value;
    }

    public double[] toArray() {
        return data.clone();
    }

    public Vector copy() {
        return new Vector(data);
    }

    public static Vector zeros(int size) {
        return new Vector(size);
    }

    public static Vector ones(int size) {
        Vector v = new Vector(size);
        for (int i = 0; i < size; i++) {
            v.set(i, 1.0);
        }
        return v;
    }

    public static Vector random(int size, Random random, double scale) {
        Vector v = new Vector(size);
        for (int i = 0; i < size; i++) {
            v.set(i, (random.nextDouble() * 2.0 - 1.0) * scale);
        }
        return v;
    }

    public static Vector fromMatrixColumn(Matrix matrix, int column) {
        Vector v = new Vector(matrix.rows());
        for (int i = 0; i < matrix.rows(); i++) {
            v.set(i, matrix.get(i, column));
        }
        return v;
    }

    public static Vector add(Vector a, Vector b) {
        requireSameSize(a, b, "add");
        Vector result = new Vector(a.size());
        for (int i = 0; i < a.size(); i++) {
            result.set(i, a.get(i) + b.get(i));
        }
        return result;
    }

    public static Vector subtract(Vector a, Vector b) {
        requireSameSize(a, b, "subtract");
        Vector result = new Vector(a.size());
        for (int i = 0; i < a.size(); i++) {
            result.set(i, a.get(i) - b.get(i));
        }
        return result;
    }

    public static Vector scale(Vector a, double factor) {
        Vector result = new Vector(a.size());
        for (int i = 0; i < a.size(); i++) {
            result.set(i, a.get(i) * factor);
        }
        return result;
    }

    public static Vector hadamard(Vector a, Vector b) {
        requireSameSize(a, b, "hadamard");
        Vector result = new Vector(a.size());
        for (int i = 0; i < a.size(); i++) {
            result.set(i, a.get(i) * b.get(i));
        }
        return result;
    }

    public Vector apply(DoubleUnaryOperator function) {
        Vector result = new Vector(size());
        for (int i = 0; i < size(); i++) {
            result.set(i, function.applyAsDouble(data[i]));
        }
        return result;
    }

    public double dot(Vector other) {
        requireSameSize(this, other, "dot");
        double sum = 0.0;
        for (int i = 0; i < size(); i++) {
            sum += data[i] * other.data[i];
        }
        return sum;
    }

    public double magnitude() {
        return Math.sqrt(dot(this));
    }

    public double sum() {
        double total = 0.0;
        for (double value : data) {
            total += value;
        }
        return total;
    }

    public double max() {
        double max = Double.NEGATIVE_INFINITY;
        for (double value : data) {
            max = Math.max(max, value);
        }
        return max;
    }

    public int argmax() {
        int index = 0;
        for (int i = 1; i < size(); i++) {
            if (data[i] > data[index]) index = i;
        }
        return index;
    }

    public List<Double> toJson() {
        List<Double> out = new ArrayList<>();
        for (double value : data) {
            out.add(value);
        }
        return out;
    }

    public static Vector fromJson(Object node) {
        List<Object> values = util.Json.array(node);
        Vector v = new Vector(values.size());
        for (int i = 0; i < values.size(); i++) {
            v.set(i, ((Number) values.get(i)).doubleValue());
        }
        return v;
    }

    @Override
    public String toString() {
        return java.util.Arrays.toString(data);
    }

    private static void requireSameSize(Vector a, Vector b, String operation) {
        if (a.size() != b.size()) {
            throw new IllegalArgumentException("Vector size mismatch for " + operation + ": "
                    + a.size() + " vs " + b.size());
        }
    }
}
