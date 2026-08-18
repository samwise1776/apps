package javagpt;

import java.util.Arrays;
import java.util.Random;

public final class Tensor {
    public float[] data;
    public int[] shape;
    public int ndim;
    public int size;

    public Tensor(int... shape) {
        this.shape = shape.clone();
        this.ndim = shape.length;
        this.size = 1;
        for (int s : shape) this.size *= s;
        this.data = new float[this.size];
    }

    public Tensor(float[] data, int... shape) {
        this.data = data;
        this.shape = shape.clone();
        this.ndim = shape.length;
        this.size = 1;
        for (int s : shape) this.size *= s;
    }

    public float get(int... idx) {
        int off = 0;
        for (int i = 0; i < idx.length; i++) off = off * shape[i] + idx[i];
        return data[off];
    }

    public void set(float val, int... idx) {
        int off = 0;
        for (int i = 0; i < idx.length; i++) off = off * shape[i] + idx[i];
        data[off] = val;
    }

    public int stride(int axis) {
        int s = 1;
        for (int i = axis + 1; i < ndim; i++) s *= shape[i];
        return s;
    }

    public int offset(int... idx) {
        int off = 0;
        for (int i = 0; i < idx.length; i++) off = off * shape[i] + idx[i];
        return off;
    }

    public Tensor reshape(int... newShape) {
        int newSize = 1;
        for (int s : newShape) newSize *= s;
        if (newSize != this.size) {
            throw new IllegalArgumentException("Cannot reshape " + this.size + " to " + Arrays.toString(newShape));
        }
        return new Tensor(Arrays.copyOf(data, data.length), newShape);
    }

    public Tensor view(int... newShape) {
        return reshape(newShape);
    }

    public Tensor contiguous() {
        return new Tensor(Arrays.copyOf(data, data.length), shape.clone());
    }

    public Tensor clone() {
        return new Tensor(Arrays.copyOf(data, data.length), shape.clone());
    }

    public void fill(float val) {
        Arrays.fill(data, val);
    }

    public void zeroGrad() {
        Arrays.fill(data, 0f);
    }

    // ---- Creation ----

    public static Tensor zeros(int... shape) {
        return new Tensor(shape);
    }

    public static Tensor ones(int... shape) {
        Tensor t = new Tensor(shape);
        Arrays.fill(t.data, 1f);
        return t;
    }

    public static Tensor randn(Random rng, int... shape) {
        Tensor t = new Tensor(shape);
        for (int i = 0; i < t.size; i++) {
            double u1 = rng.nextDouble();
            double u2 = rng.nextDouble();
            t.data[i] = (float) Math.sqrt(-2 * Math.log(u1 + 1e-20)) * (float) Math.cos(2 * Math.PI * u2);
        }
        return t;
    }

    public static Tensor randnScaled(Random rng, int fanIn, int... shape) {
        Tensor t = new Tensor(shape);
        float std = (float) (1.0 / Math.sqrt(fanIn));
        for (int i = 0; i < t.size; i++) {
            double u1 = rng.nextDouble();
            double u2 = rng.nextDouble();
            t.data[i] = (float) (std * Math.sqrt(-2 * Math.log(u1 + 1e-20)) * Math.cos(2 * Math.PI * u2));
        }
        return t;
    }

    // ---- Element-wise ----

    public static Tensor add(Tensor a, Tensor b) {
        Tensor out = new Tensor(a.shape.clone());
        if (a.size == b.size) {
            for (int i = 0; i < a.size; i++) out.data[i] = a.data[i] + b.data[i];
        } else {
            // broadcast b (assume b is 1D bias)
            for (int i = 0; i < a.size; i++) {
                int featIdx = i % b.size;
                out.data[i] = a.data[i] + b.data[featIdx];
            }
        }
        return out;
    }

    public static Tensor addInPlace(Tensor a, Tensor b) {
        if (a.size == b.size) {
            for (int i = 0; i < a.size; i++) a.data[i] += b.data[i];
        } else {
            for (int i = 0; i < a.size; i++) a.data[i] += b.data[i % b.size];
        }
        return a;
    }

    public static Tensor sub(Tensor a, Tensor b) {
        Tensor out = new Tensor(a.shape.clone());
        for (int i = 0; i < a.size; i++) out.data[i] = a.data[i] - b.data[i];
        return out;
    }

    public static Tensor mul(Tensor a, Tensor b) {
        Tensor out = new Tensor(a.shape.clone());
        if (a.size == b.size) {
            for (int i = 0; i < a.size; i++) out.data[i] = a.data[i] * b.data[i];
        } else {
            for (int i = 0; i < a.size; i++) out.data[i] = a.data[i] * b.data[i % b.size];
        }
        return out;
    }

    public static Tensor scale(Tensor a, float s) {
        Tensor out = new Tensor(a.shape.clone());
        for (int i = 0; i < a.size; i++) out.data[i] = a.data[i] * s;
        return out;
    }

    public static Tensor sqrt(Tensor a) {
        Tensor out = new Tensor(a.shape.clone());
        for (int i = 0; i < a.size; i++) out.data[i] = (float) Math.sqrt(a.data[i] + 1e-12f);
        return out;
    }

    public static Tensor exp(Tensor a) {
        Tensor out = new Tensor(a.shape.clone());
        for (int i = 0; i < a.size; i++) out.data[i] = (float) Math.exp(a.data[i]);
        return out;
    }

    public static Tensor tanh(Tensor a) {
        Tensor out = new Tensor(a.shape.clone());
        for (int i = 0; i < a.size; i++) out.data[i] = (float) Math.tanh(a.data[i]);
        return out;
    }

    // ---- Matrix multiply ----

    public static Tensor matmul(Tensor a, Tensor b) {
        // 2D: (M,K) @ (K,N) -> (M,N)
        if (a.ndim == 2 && b.ndim == 2) {
            int M = a.shape[0], K = a.shape[1], N = b.shape[1];
            Tensor out = new Tensor(M, N);
            for (int i = 0; i < M; i++) {
                for (int k = 0; k < K; k++) {
                    float aik = a.data[i * K + k];
                    for (int j = 0; j < N; j++) {
                        out.data[i * N + j] += aik * b.data[k * N + j];
                    }
                }
            }
            return out;
        }
        // 3D batched: (B,M,K) @ (B,K,N) -> (B,M,N)
        if (a.ndim == 3 && b.ndim == 3) {
            int B = a.shape[0], M = a.shape[1], K = a.shape[2], N = b.shape[2];
            Tensor out = new Tensor(B, M, N);
            for (int bIdx = 0; bIdx < B; bIdx++) {
                int aOff = bIdx * M * K;
                int bOff = bIdx * K * N;
                int oOff = bIdx * M * N;
                for (int i = 0; i < M; i++) {
                    for (int k = 0; k < K; k++) {
                        float aik = a.data[aOff + i * K + k];
                        for (int j = 0; j < N; j++) {
                            out.data[oOff + i * N + j] += aik * b.data[bOff + k * N + j];
                        }
                    }
                }
            }
            return out;
        }
        // 4D batched: (..., M, K) @ (..., K, N) -> (..., M, N)
        if (a.ndim == 4 && b.ndim == 4) {
            int B = a.shape[0], H = a.shape[1], M = a.shape[2], K = a.shape[3], N = b.shape[3];
            Tensor out = new Tensor(B, H, M, N);
            for (int bIdx = 0; bIdx < B; bIdx++) {
                for (int h = 0; h < H; h++) {
                    int aOff = (bIdx * H + h) * M * K;
                    int bOff = (bIdx * H + h) * K * N;
                    int oOff = (bIdx * H + h) * M * N;
                    for (int i = 0; i < M; i++) {
                        for (int kk = 0; kk < K; kk++) {
                            float aik = a.data[aOff + i * K + kk];
                            for (int j = 0; j < N; j++) {
                                out.data[oOff + i * N + j] += aik * b.data[bOff + kk * N + j];
                            }
                        }
                    }
                }
            }
            return out;
        }
        throw new IllegalArgumentException("matmul expects 2D, 3D or 4D tensors, got " + a.ndim + "D and " + b.ndim + "D");
    }

    // ---- Transpose ----

    public Tensor transpose(int dim0, int dim1) {
        if (ndim == 2 && dim0 == 0 && dim1 == 1) {
            int rows = shape[0], cols = shape[1];
            Tensor out = new Tensor(cols, rows);
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < cols; j++)
                    out.data[j * rows + i] = data[i * cols + j];
            return out;
        }
        if (ndim == 4 && dim0 == 2 && dim1 == 3) {
            // (B, H, S, D) -> (B, H, D, S)
            int B = shape[0], H = shape[1], S = shape[2], D = shape[3];
            Tensor out = new Tensor(B, H, D, S);
            for (int b = 0; b < B; b++)
                for (int h = 0; h < H; h++)
                    for (int s = 0; s < S; s++)
                        for (int d = 0; d < D; d++)
                            out.data[b * H * D * S + h * D * S + d * S + s] =
                                    data[b * H * S * D + h * S * D + s * D + d];
            return out;
        }
        if (ndim == 4 && dim0 == 1 && dim1 == 2) {
            // (B, H, S, D) -> (B, S, H, D)
            int B = shape[0], H = shape[1], S = shape[2], D = shape[3];
            Tensor out = new Tensor(B, S, H, D);
            for (int b = 0; b < B; b++)
                for (int h = 0; h < H; h++)
                    for (int s = 0; s < S; s++)
                        for (int d = 0; d < D; d++)
                            out.data[b * S * H * D + s * H * D + h * D + d] =
                                    data[b * H * S * D + h * S * D + s * D + d];
            return out;
        }
        throw new UnsupportedOperationException("transpose(" + dim0 + "," + dim1 + ") for " + ndim + "D tensor");
    }

    // ---- Softmax along last axis ----

    public static Tensor softmax(Tensor x, int axis) {
        int[] s = x.shape.clone();
        int outer = 1, inner = s[axis];
        for (int i = 0; i < axis; i++) outer *= s[i];
        int total = 1;
        for (int i = axis + 1; i < s.length; i++) total *= s[i];

        Tensor out = new Tensor(s.clone());
        for (int o = 0; o < outer; o++) {
            for (int t = 0; t < total; t++) {
                int base = o * inner * total + t;
                float maxVal = Float.NEGATIVE_INFINITY;
                for (int i = 0; i < inner; i++) {
                    float v = x.data[base + i * total];
                    if (v > maxVal) maxVal = v;
                }
                float sumExp = 0;
                for (int i = 0; i < inner; i++) {
                    float e = (float) Math.exp(x.data[base + i * total] - maxVal);
                    out.data[base + i * total] = e;
                    sumExp += e;
                }
                for (int i = 0; i < inner; i++) {
                    out.data[base + i * total] /= (sumExp + 1e-20f);
                }
            }
        }
        return out;
    }

    // ---- Reductions ----

    public float sum() {
        float s = 0;
        for (int i = 0; i < size; i++) s += data[i];
        return s;
    }

    public float mean() {
        return sum() / size;
    }

    public float max() {
        float m = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < size; i++) if (data[i] > m) m = data[i];
        return m;
    }

    public Tensor sumAxis(int axis) {
        int[] outShape = new int[shape.length - 1];
        for (int i = 0, j = 0; i < shape.length; i++) {
            if (i != axis) outShape[j++] = shape[i];
        }
        if (outShape.length == 0) {
            return new Tensor(new float[]{sum()}, 1);
        }
        Tensor out = new Tensor(outShape);
        int outer = 1;
        for (int i = 0; i < axis; i++) outer *= shape[i];
        int reduce = shape[axis];
        int inner = 1;
        for (int i = axis + 1; i < shape.length; i++) inner *= shape[i];

        for (int o = 0; o < outer; o++) {
            for (int r = 0; r < reduce; r++) {
                for (int in = 0; in < inner; in++) {
                    out.data[o * inner + in] += data[o * reduce * inner + r * inner + in];
                }
            }
        }
        return out;
    }

    public Tensor meanAxis(int axis) {
        Tensor s = sumAxis(axis);
        float divisor = shape[axis];
        Tensor out = new Tensor(s.shape.clone());
        for (int i = 0; i < s.size; i++) out.data[i] = s.data[i] / divisor;
        return out;
    }

    // ---- Keepdims variants ----

    public Tensor sumAxisKeepdims(int axis) {
        Tensor reduced = sumAxis(axis);
        int[] newShape = new int[shape.length];
        System.arraycopy(shape, 0, newShape, 0, shape.length);
        newShape[axis] = 1;
        return reduced.reshape(newShape);
    }

    public Tensor meanAxisKeepdims(int axis) {
        Tensor reduced = meanAxis(axis);
        int[] newShape = new int[shape.length];
        System.arraycopy(shape, 0, newShape, 0, shape.length);
        newShape[axis] = 1;
        return reduced.reshape(newShape);
    }

    // ---- Indexing (select along first axis) ----

    public Tensor indexSelect(int axis, int[] indices) {
        int[] newShape = shape.clone();
        newShape[axis] = indices.length;
        Tensor out = new Tensor(newShape);

        int outer = 1;
        for (int i = 0; i < axis; i++) outer *= shape[i];
        int inner = 1;
        for (int i = axis + 1; i < shape.length; i++) inner *= shape[i];
        int oldStride = shape[axis];

        for (int o = 0; o < outer; o++) {
            for (int idx = 0; idx < indices.length; idx++) {
                int srcOff = (o * oldStride + indices[idx]) * inner;
                int dstOff = (o * indices.length + idx) * inner;
                System.arraycopy(data, srcOff, out.data, dstOff, inner);
            }
        }
        return out;
    }

    // ---- Add rows (scatter) ----

    public void indexAdd(int axis, int[] indices, Tensor source) {
        int outer = 1;
        for (int i = 0; i < axis; i++) outer *= shape[i];
        int inner = 1;
        for (int i = axis + 1; i < shape.length; i++) inner *= shape[i];
        int stride = shape[axis];

        for (int o = 0; o < outer; o++) {
            for (int idx = 0; idx < indices.length; idx++) {
                int dstOff = (o * stride + indices[idx]) * inner;
                int srcOff = (o * indices.length + idx) * inner;
                for (int in = 0; in < inner; in++) {
                    data[dstOff + in] += source.data[srcOff + in];
                }
            }
        }
    }

    // ---- Reshape for attention ----

    public Tensor reshapeForAttention(int B, int S, int H, int D) {
        // (B, S, H*D) -> (B, S, H, D)
        Tensor out = new Tensor(B, S, H, D);
        for (int b = 0; b < B; b++)
            for (int s = 0; s < S; s++)
                System.arraycopy(data, b * S * H * D + s * H * D,
                        out.data, b * S * H * D + s * H * D, H * D);
        return out;
    }

    public Tensor reshapeFromAttention(int B, int S, int H, int D) {
        // (B, H, S, D) -> (B, S, H, D) then effectively (B, S, H*D)
        Tensor transposed = transpose(1, 2); // (B, S, H, D)
        return transposed.reshape(B, S, H * D);
    }

    // ---- GELU derivative (element-wise) ----

    public static Tensor gelu(Tensor x) {
        Tensor out = new Tensor(x.shape.clone());
        final float SQRT2_OVER_PI = 0.7978845608f;
        final float GELU_CONST = 0.044715f;
        for (int i = 0; i < x.size; i++) {
            float v = x.data[i];
            float inner = SQRT2_OVER_PI * (v + GELU_CONST * v * v * v);
            float tanhVal = (float) Math.tanh(inner);
            out.data[i] = 0.5f * v * (1.0f + tanhVal);
        }
        return out;
    }

    public static Tensor geluDerv(Tensor x, Tensor gradOut) {
        Tensor out = new Tensor(x.shape.clone());
        final float SQRT2_OVER_PI = 0.7978845608f;
        final float GELU_CONST = 0.044715f;
        for (int i = 0; i < x.size; i++) {
            float v = x.data[i];
            float inner = SQRT2_OVER_PI * (v + GELU_CONST * v * v * v);
            float tanhVal = (float) Math.tanh(inner);
            float sech2 = 1.0f - tanhVal * tanhVal;
            float dInner = SQRT2_OVER_PI * (1.0f + 3.0f * GELU_CONST * v * v);
            float d = 0.5f * (1.0f + tanhVal) + 0.5f * v * sech2 * dInner;
            out.data[i] = gradOut.data[i] * d;
        }
        return out;
    }

    // ---- Print for debugging ----

    public void print(String label) {
        System.out.println(label + " shape=" + Arrays.toString(shape) + " sum=" + String.format("%.4f", sum()));
    }

    public static void printStats(String label, Tensor t) {
        float min = Float.MAX_VALUE, max = Float.MIN_VALUE, sum = 0;
        for (int i = 0; i < t.size; i++) {
            float v = t.data[i];
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
        }
        System.out.printf("%s [%s] min=%.4f max=%.4f mean=%.6f%n",
                label, Arrays.toString(t.shape), min, max, sum / t.size);
    }

    public boolean hasNaN() {
        for (int i = 0; i < size; i++) if (Float.isNaN(data[i]) || Float.isInfinite(data[i])) return true;
        return false;
    }
}
