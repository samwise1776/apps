package neural;

import java.util.Arrays;

/**
 * A simple N-dimensional tensor. The training engine is built on Matrix and Vector
 * for clarity, while Tensor provides a general ND container used for feature data,
 * reshaping, and neural-network visualization.
 */
public final class Tensor {
    private final int[] shape;
    private final double[] data;

    public Tensor(int... shape) {
        this.shape = shape.clone();
        int size = 1;
        for (int dimension : shape) {
            size *= dimension;
        }
        this.data = new double[size];
    }

    public Tensor(double[] data, int... shape) {
        this(shape);
        if (data.length != this.data.length) {
            throw new IllegalArgumentException("Data length " + data.length + " does not match shape " + Arrays.toString(shape));
        }
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    public int[] shape() {
        return shape.clone();
    }

    public int size() {
        return data.length;
    }

    public int rank() {
        return shape.length;
    }

    public double get(int... index) {
        return data[offset(index)];
    }

    public void set(double value, int... index) {
        data[offset(index)] = value;
    }

    public double flat(int index) {
        return data[index];
    }

    public void setFlat(int index, double value) {
        data[index] = value;
    }

    public double[] toArray() {
        return data.clone();
    }

    /** 2D access, convenient for matrix-shaped tensors. */
    public double get2(int row, int col) {
        return get(row, col);
    }

    public void set2(int row, int col, double value) {
        set(value, row, col);
    }

    public Tensor apply(java.util.function.DoubleUnaryOperator function) {
        Tensor result = new Tensor(shape);
        for (int i = 0; i < data.length; i++) {
            result.data[i] = function.applyAsDouble(data[i]);
        }
        return result;
    }

    public Tensor reshape(int... newShape) {
        Tensor result = new Tensor(newShape);
        if (result.data.length != data.length) {
            throw new IllegalArgumentException("Cannot reshape " + data.length + " elements to " + Arrays.toString(newShape));
        }
        System.arraycopy(data, 0, result.data, 0, data.length);
        return result;
    }

    public static Tensor fromMatrix(Matrix matrix) {
        Tensor tensor = new Tensor(matrix.rows(), matrix.cols());
        for (int i = 0; i < matrix.rows(); i++) {
            for (int j = 0; j < matrix.cols(); j++) {
                tensor.set2(i, j, matrix.get(i, j));
            }
        }
        return tensor;
    }

    public static Tensor fromVector(Vector vector) {
        Tensor tensor = new Tensor(vector.size());
        for (int i = 0; i < vector.size(); i++) {
            tensor.setFlat(i, vector.get(i));
        }
        return tensor;
    }

    @Override
    public String toString() {
        if (rank() == 1) {
            return Arrays.toString(data);
        }
        return "Tensor(" + Arrays.toString(shape) + ")";
    }

    private int offset(int... index) {
        if (index.length != shape.length) {
            throw new IllegalArgumentException("Index rank " + index.length + " does not match shape rank " + shape.length);
        }
        int offset = 0;
        for (int i = 0; i < shape.length; i++) {
            if (index[i] < 0 || index[i] >= shape[i]) {
                throw new ArrayIndexOutOfBoundsException("Index " + index[i] + " out of range for dim " + i + " (size " + shape[i] + ")");
            }
            offset = offset * shape[i] + index[i];
        }
        return offset;
    }
}
