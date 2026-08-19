import java.util.Arrays;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

/**
 * Matrix
 *
 * A small matrix math library for the neural network.
 *
 * Supports:
 * - Matrix creation
 * - Random weights
 * - Addition
 * - Subtraction
 * - Scalar multiplication
 * - Element-wise multiplication
 * - Matrix multiplication
 * - Transpose
 * - ReLU
 * - ReLU derivative
 * - Sigmoid
 * - Softmax
 * - Conversion from/to arrays
 *
 * This lets the AI move from:
 *
 * x1 * weight1 + x2 * weight2
 *
 * to:
 *
 * output = weights * inputs + bias
 */
public class Matrix {

    private final int rows;
    private final int cols;

    private final double[][] data;

    private static final Random RANDOM =
            new Random();

    /*
     * =========================================
     * CONSTRUCTOR
     * =========================================
     */

    public Matrix(int rows, int cols) {

        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException(
                    "Matrix size must be greater than zero."
            );
        }

        this.rows = rows;
        this.cols = cols;

        this.data =
                new double[rows][cols];
    }


    /*
     * Create from existing array.
     */

    public Matrix(double[][] values) {

        if (
                values == null
                ||
                values.length == 0
                ||
                values[0].length == 0
        ) {

            throw new IllegalArgumentException(
                    "Matrix cannot be empty."
            );
        }

        this.rows =
                values.length;

        this.cols =
                values[0].length;

        this.data =
                new double[rows][cols];

        for (int row = 0; row < rows; row++) {

            if (values[row].length != cols) {

                throw new IllegalArgumentException(
                        "All matrix rows must have the same length."
                );
            }

            System.arraycopy(
                    values[row],
                    0,
                    data[row],
                    0,
                    cols
            );
        }
    }


    /*
     * =========================================
     * BASIC INFORMATION
     * =========================================
     */

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }


    /*
     * =========================================
     * GET / SET
     * =========================================
     */

    public double get(
            int row,
            int col
    ) {

        checkPosition(
                row,
                col
        );

        return data[row][col];
    }


    public void set(
            int row,
            int col,
            double value
    ) {

        checkPosition(
                row,
                col
        );

        data[row][col] =
                value;
    }


    /*
     * =========================================
     * RANDOM MATRIX
     * =========================================
     */

    public static Matrix random(
            int rows,
            int cols
    ) {

        Matrix result =
                new Matrix(
                        rows,
                        cols
                );

        /*
         * Small random neural-network
         * starting weights.
         */

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result.data[row][col] =
                        (RANDOM.nextDouble() - 0.5)
                                * 2.0;
            }
        }

        return result;
    }


    /*
     * Smaller random values.
     *
     * Often better for neural-network weights.
     */

    public static Matrix randomSmall(
            int rows,
            int cols
    ) {

        Matrix result =
                new Matrix(
                        rows,
                        cols
                );

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result.data[row][col] =
                        (RANDOM.nextDouble() - 0.5)
                                * 0.2;
            }
        }

        return result;
    }


    /*
     * =========================================
     * FROM ARRAY
     * =========================================
     *
     * Example:
     *
     * [1, 2, 3]
     *
     * becomes:
     *
     * [1]
     * [2]
     * [3]
     */

    public static Matrix fromArray(
            double[] array
    ) {

        Matrix result =
                new Matrix(
                        array.length,
                        1
                );

        for (
                int i = 0;
                i < array.length;
                i++
        ) {

            result.data[i][0] =
                    array[i];
        }

        return result;
    }


    /*
     * =========================================
     * TO ARRAY
     * =========================================
     */

    public double[] toArray() {

        double[] result =
                new double[
                        rows * cols
                        ];

        int index =
                0;

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result[index++] =
                        data[row][col];
            }
        }

        return result;
    }


    /*
     * =========================================
     * COPY
     * =========================================
     */

    public Matrix copy() {

        return new Matrix(
                data
        );
    }


    /*
     * =========================================
     * MATRIX ADDITION
     * =========================================
     *
     * A + B
     */

    public Matrix add(
            Matrix other
    ) {

        checkSameSize(
                other
        );

        Matrix result =
                new Matrix(
                        rows,
                        cols
                );

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result.data[row][col] =
                        data[row][col]
                                +
                        other.data[row][col];
            }
        }

        return result;
    }


    /*
     * Add number to every element.
     */

    public Matrix add(
            double value
    ) {

        Matrix result =
                copy();

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result.data[row][col] +=
                        value;
            }
        }

        return result;
    }


    /*
     * =========================================
     * SUBTRACTION
     * =========================================
     */

    public Matrix subtract(
            Matrix other
    ) {

        checkSameSize(
                other
        );

        Matrix result =
                new Matrix(
                        rows,
                        cols
                );

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result.data[row][col] =
                        data[row][col]
                                -
                        other.data[row][col];
            }
        }

        return result;
    }


    /*
     * Static subtraction:
     *
     * Matrix.subtract(a, b)
     */

    public static Matrix subtract(
            Matrix a,
            Matrix b
    ) {

        return a.subtract(
                b
        );
    }


    /*
     * =========================================
     * SCALAR MULTIPLICATION
     * =========================================
     *
     * Matrix * number
     */

    public Matrix multiply(
            double number
    ) {

        Matrix result =
                new Matrix(
                        rows,
                        cols
                );

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result.data[row][col] =
                        data[row][col]
                                *
                        number;
            }
        }

        return result;
    }


    /*
     * =========================================
     * ELEMENT-WISE MULTIPLICATION
     * =========================================
     *
     * Also called the Hadamard product.
     *
     * [1,2]    [3,4]
     *
     * becomes
     *
     * [3,8]
     */

    public Matrix hadamard(
            Matrix other
    ) {

        checkSameSize(
                other
        );

        Matrix result =
                new Matrix(
                        rows,
                        cols
                );

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result.data[row][col] =
                        data[row][col]
                                *
                        other.data[row][col];
            }
        }

        return result;
    }


    /*
     * =========================================
     * MATRIX MULTIPLICATION
     * =========================================
     *
     * This is one of the most important
     * operations in a neural network.
     *
     * C = A * B
     */

    public static Matrix multiply(
            Matrix a,
            Matrix b
    ) {

        if (
                a.cols
                        !=
                b.rows
        ) {

            throw new IllegalArgumentException(
                    "Cannot multiply "
                            + a.rows
                            + "x"
                            + a.cols
                            + " by "
                            + b.rows
                            + "x"
                            + b.cols
            );
        }

        Matrix result =
                new Matrix(
                        a.rows,
                        b.cols
                );

        for (
                int row = 0;
                row < a.rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < b.cols;
                    col++
            ) {

                double total =
                        0.0;

                for (
                        int k = 0;
                        k < a.cols;
                        k++
                ) {

                    total +=
                            a.data[row][k]
                                    *
                            b.data[k][col];
                }

                result.data[row][col] =
                        total;
            }
        }

        return result;
    }


    /*
     * This is the operation that lets us write:
     *
     * hidden =
     * weightsInputHidden * input
     */

    public Matrix multiply(
            Matrix other
    ) {

        return multiply(
                this,
                other
        );
    }


    /*
     * =========================================
     * TRANSPOSE
     * =========================================
     *
     * Converts:
     *
     * [1 2 3]
     * [4 5 6]
     *
     * into:
     *
     * [1 4]
     * [2 5]
     * [3 6]
     */

    public Matrix transpose() {

        Matrix result =
                new Matrix(
                        cols,
                        rows
                );

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result.data[col][row] =
                        data[row][col];
            }
        }

        return result;
    }


    public static Matrix transpose(
            Matrix matrix
    ) {

        return matrix.transpose();
    }


    /*
     * =========================================
     * APPLY FUNCTION
     * =========================================
     *
     * Runs a math function on
     * every number.
     */

    public Matrix map(
            DoubleUnaryOperator function
    ) {

        Matrix result =
                new Matrix(
                        rows,
                        cols
                );

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result.data[row][col] =
                        function.applyAsDouble(
                                data[row][col]
                        );
            }
        }

        return result;
    }


    /*
     * =========================================
     * RELU
     * =========================================
     */

    public Matrix relu() {

        return map(
                value ->
                        Math.max(
                                0.0,
                                value
                        )
        );
    }


    /*
     * =========================================
     * RELU DERIVATIVE
     * =========================================
     */

    public Matrix reluDerivative() {

        return map(
                value ->
                        value > 0.0
                                ? 1.0
                                : 0.0
        );
    }


    /*
     * =========================================
     * SIGMOID
     * =========================================
     */

    public Matrix sigmoid() {

        return map(
                value ->
                        1.0
                                /
                        (
                                1.0
                                        +
                                Math.exp(
                                        -value
                                )
                        )
        );
    }


    /*
     * =========================================
     * SIGMOID DERIVATIVE
     * =========================================
     *
     * Assumes the matrix already contains
     * sigmoid output values.
     *
     * derivative = x * (1 - x)
     */

    public Matrix sigmoidDerivative() {

        return map(
                value ->
                        value
                                *
                        (
                                1.0
                                        -
                                value
                        )
        );
    }


    /*
     * =========================================
     * SOFTMAX
     * =========================================
     *
     * Converts values into probabilities.
     */

    public Matrix softmax() {

        double maximum =
                Double.NEGATIVE_INFINITY;

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                maximum =
                        Math.max(
                                maximum,
                                data[row][col]
                        );
            }
        }


        Matrix result =
                new Matrix(
                        rows,
                        cols
                );


        double sum =
                0.0;


        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                double exponent =
                        Math.exp(
                                data[row][col]
                                        -
                                maximum
                        );

                result.data[row][col] =
                        exponent;

                sum +=
                        exponent;
            }
        }


        if (sum == 0.0) {
            return result;
        }


        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result.data[row][col] /=
                        sum;
            }
        }


        return result;
    }


    /*
     * =========================================
     * SUM
     * =========================================
     */

    public double sum() {

        double result =
                0.0;

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result +=
                        data[row][col];
            }
        }

        return result;
    }


    /*
     * =========================================
     * MAX
     * =========================================
     */

    public double max() {

        double result =
                Double.NEGATIVE_INFINITY;

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                result =
                        Math.max(
                                result,
                                data[row][col]
                        );
            }
        }

        return result;
    }


    /*
     * =========================================
     * ARGMAX
     * =========================================
     *
     * Returns the flattened index
     * containing the largest number.
     */

    public int argMax() {

        int bestIndex =
                0;

        double bestValue =
                Double.NEGATIVE_INFINITY;

        int index =
                0;

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            for (
                    int col = 0;
                    col < cols;
                    col++
            ) {

                if (
                        data[row][col]
                                >
                        bestValue
                ) {

                    bestValue =
                            data[row][col];

                    bestIndex =
                            index;
                }

                index++;
            }
        }

        return bestIndex;
    }


    /*
     * =========================================
     * FILL
     * =========================================
     */

    public Matrix fill(
            double value
    ) {

        Matrix result =
                new Matrix(
                        rows,
                        cols
                );

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            Arrays.fill(
                    result.data[row],
                    value
            );
        }

        return result;
    }


    /*
     * =========================================
     * ZERO MATRIX
     * =========================================
     */

    public static Matrix zeros(
            int rows,
            int cols
    ) {

        return new Matrix(
                rows,
                cols
        );
    }


    /*
     * =========================================
     * ONES MATRIX
     * =========================================
     */

    public static Matrix ones(
            int rows,
            int cols
    ) {

        Matrix result =
                new Matrix(
                        rows,
                        cols
                );

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            Arrays.fill(
                    result.data[row],
                    1.0
            );
        }

        return result;
    }


    /*
     * =========================================
     * DIMENSION CHECKING
     * =========================================
     */

    private void checkSameSize(
            Matrix other
    ) {

        if (
                rows != other.rows
                ||
                cols != other.cols
        ) {

            throw new IllegalArgumentException(
                    "Matrix sizes do not match: "
                            + rows
                            + "x"
                            + cols
                            + " vs "
                            + other.rows
                            + "x"
                            + other.cols
            );
        }
    }


    private void checkPosition(
            int row,
            int col
    ) {

        if (
                row < 0
                ||
                row >= rows
                ||
                col < 0
                ||
                col >= cols
        ) {

            throw new IndexOutOfBoundsException(
                    "Invalid matrix position: "
                            + row
                            + ", "
                            + col
            );
        }
    }


    /*
     * =========================================
     * PRINT
     * =========================================
     */

    public void print() {

        for (
                int row = 0;
                row < rows;
                row++
        ) {

            System.out.println(
                    Arrays.toString(
                            data[row]
                    )
            );
        }
    }


    /*
     * =========================================
     * TO STRING
     * =========================================
     */

    @Override
    public String toString() {

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                "Matrix("
        );

        builder.append(
                rows
        );

        builder.append(
                "x"
        );

        builder.append(
                cols
        );

        builder.append(
                ")\n"
        );


        for (
                int row = 0;
                row < rows;
                row++
        ) {

            builder.append(
                    Arrays.toString(
                            data[row]
                    )
            );

            builder.append(
                    "\n"
            );
        }


        return builder.toString();
    }
}