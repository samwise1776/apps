package neural;

import util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

/** A real dense matrix with the operations a neural network needs. */
public final class Matrix {
    private final int rows;
    private final int cols;
    private final double[][] data;

    public Matrix(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Matrix dimensions must be positive: " + rows + "x" + cols);
        }
        this.rows = rows;
        this.cols = cols;
        this.data = new double[rows][cols];
    }

    public Matrix(double[][] data) {
        this.rows = data.length;
        this.cols = data.length == 0 ? 0 : data[0].length;
        this.data = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(data[i], 0, this.data[i], 0, cols);
        }
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public double get(int row, int col) {
        return data[row][col];
    }

    public void set(int row, int col, double value) {
        data[row][col] = value;
    }

    public double[] row(int row) {
        return data[row];
    }

    public double[][] toArray() {
        double[][] copy = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(data[i], 0, copy[i], 0, cols);
        }
        return copy;
    }

    public Matrix copy() {
        return new Matrix(toArray());
    }

    public static Matrix zeros(int rows, int cols) {
        return new Matrix(rows, cols);
    }

    public static Matrix ones(int rows, int cols) {
        Matrix m = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m.set(i, j, 1.0);
            }
        }
        return m;
    }

    /** Xavier/Glorot initialization scaled to fan-in. */
    public static Matrix random(int rows, int cols, Random random, double scale) {
        Matrix m = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m.set(i, j, (random.nextDouble() * 2.0 - 1.0) * scale);
            }
        }
        return m;
    }

    public static Matrix identity(int size) {
        Matrix m = new Matrix(size, size);
        for (int i = 0; i < size; i++) {
            m.set(i, i, 1.0);
        }
        return m;
    }

    public static Matrix fromVector(Vector vector) {
        Matrix m = new Matrix(vector.size(), 1);
        for (int i = 0; i < vector.size(); i++) {
            m.set(i, 0, vector.get(i));
        }
        return m;
    }

    public static Matrix add(Matrix a, Matrix b) {
        requireSameShape(a, b, "add");
        Matrix result = new Matrix(a.rows, a.cols);
        for (int i = 0; i < a.rows; i++) {
            for (int j = 0; j < a.cols; j++) {
                result.set(i, j, a.get(i, j) + b.get(i, j));
            }
        }
        return result;
    }

    public static Matrix subtract(Matrix a, Matrix b) {
        requireSameShape(a, b, "subtract");
        Matrix result = new Matrix(a.rows, a.cols);
        for (int i = 0; i < a.rows; i++) {
            for (int j = 0; j < a.cols; j++) {
                result.set(i, j, a.get(i, j) - b.get(i, j));
            }
        }
        return result;
    }

    /** Element-wise product. */
    public static Matrix hadamard(Matrix a, Matrix b) {
        requireSameShape(a, b, "hadamard");
        Matrix result = new Matrix(a.rows, a.cols);
        for (int i = 0; i < a.rows; i++) {
            for (int j = 0; j < a.cols; j++) {
                result.set(i, j, a.get(i, j) * b.get(i, j));
            }
        }
        return result;
    }

    public static Matrix scale(Matrix a, double factor) {
        Matrix result = new Matrix(a.rows, a.cols);
        for (int i = 0; i < a.rows; i++) {
            for (int j = 0; j < a.cols; j++) {
                result.set(i, j, a.get(i, j) * factor);
            }
        }
        return result;
    }

    /** Standard matrix product: rows(a) x cols(b). */
    public static Matrix multiply(Matrix a, Matrix b) {
        if (a.cols != b.rows) {
            throw new IllegalArgumentException("Matrix product mismatch: " + a.rows + "x" + a.cols
                    + " * " + b.rows + "x" + b.cols);
        }
        Matrix result = new Matrix(a.rows, b.cols);
        for (int i = 0; i < a.rows; i++) {
            for (int j = 0; j < b.cols; j++) {
                double sum = 0.0;
                for (int k = 0; k < a.cols; k++) {
                    sum += a.get(i, k) * b.get(k, j);
                }
                result.set(i, j, sum);
            }
        }
        return result;
    }

    /** Multiplies a matrix (input x hidden) by a column vector; returns a column vector. */
    public static Vector multiplyVector(Matrix matrix, Vector vector) {
        if (matrix.cols != vector.size()) {
            throw new IllegalArgumentException("Matrix/vector product mismatch: " + matrix.rows + "x" + matrix.cols
                    + " * " + vector.size());
        }
        double[] result = new double[matrix.rows];
        for (int i = 0; i < matrix.rows; i++) {
            double sum = 0.0;
            for (int j = 0; j < matrix.cols; j++) {
                sum += matrix.get(i, j) * vector.get(j);
            }
            result[i] = sum;
        }
        return new Vector(result);
    }

    public static Matrix transpose(Matrix m) {
        Matrix result = new Matrix(m.cols, m.rows);
        for (int i = 0; i < m.rows; i++) {
            for (int j = 0; j < m.cols; j++) {
                result.set(j, i, m.get(i, j));
            }
        }
        return result;
    }

    public Matrix apply(DoubleUnaryOperator function) {
        Matrix result = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result.set(i, j, function.applyAsDouble(data[i][j]));
            }
        }
        return result;
    }

    public double sum() {
        double total = 0.0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                total += data[i][j];
            }
        }
        return total;
    }

    /** Converts the matrix to a JSON-serializable structure (list of rows). */
    public List<List<Double>> toJson() {
        List<List<Double>> rowsOut = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            List<Double> rowOut = new ArrayList<>();
            for (int j = 0; j < cols; j++) {
                rowOut.add(data[i][j]);
            }
            rowsOut.add(rowOut);
        }
        return rowsOut;
    }

    public static Matrix fromJson(Object node) {
        List<Object> rowsIn = Json.array(node);
        int rowCount = rowsIn.size();
        int colCount = Json.array(rowsIn.get(0)).size();
        Matrix result = new Matrix(rowCount, colCount);
        for (int i = 0; i < rowCount; i++) {
            List<Object> rowIn = Json.array(rowsIn.get(i));
            for (int j = 0; j < colCount; j++) {
                result.set(i, j, ((Number) rowIn.get(j)).doubleValue());
            }
        }
        return result;
    }

    public String shape() {
        return rows + "x" + cols;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < rows; i++) {
            if (i > 0) out.append(", ");
            out.append(java.util.Arrays.toString(data[i]));
        }
        return out.append("]").toString();
    }

    private static void requireSameShape(Matrix a, Matrix b, String operation) {
        if (a.rows != b.rows || a.cols != b.cols) {
            throw new IllegalArgumentException("Shape mismatch for " + operation + ": "
                    + a.shape() + " vs " + b.shape());
        }
    }
}
