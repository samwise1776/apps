package javagpt;

import java.util.Random;

public final class MathUtil {
    public static final float EPSILON = 1e-12f;
    public static final float SQRT2_OVER_PI = 0.7978845608f;
    public static final float GELU_CONST = 0.044715f;

    private MathUtil() {}

    public static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) if (v > max) max = v;
        float sum = 0;
        float[] out = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            out[i] = (float) Math.exp(logits[i] - max);
            sum += out[i];
        }
        for (int i = 0; i < out.length; i++) out[i] /= (sum + EPSILON);
        return out;
    }

    public static int sampleFromDistribution(float[] probs, Random rng) {
        float r = rng.nextFloat();
        float cumulative = 0;
        for (int i = 0; i < probs.length; i++) {
            cumulative += probs[i];
            if (r <= cumulative) return i;
        }
        return probs.length - 1;
    }

    public static int topKSample(float[] probs, int k, Random rng) {
        int n = probs.length;
        k = Math.min(k, n);
        int[] topIdx = new int[k];
        float[] topVals = new float[k];
        for (int i = 0; i < k; i++) {
            topVals[i] = -1;
            topIdx[i] = -1;
        }
        for (int i = 0; i < n; i++) {
            if (probs[i] > topVals[0]) {
                topVals[0] = probs[i];
                topIdx[0] = i;
                // bubble up
                for (int j = 0; j < k - 1 && topVals[j] > topVals[j + 1]; j++) {
                    float tv = topVals[j]; topVals[j] = topVals[j + 1]; topVals[j + 1] = tv;
                    int ti = topIdx[j]; topIdx[j] = topIdx[j + 1]; topIdx[j + 1] = ti;
                }
            }
        }
        float sum = 0;
        for (int i = 0; i < k; i++) sum += topVals[i];
        float r = rng.nextFloat() * sum;
        float cum = 0;
        for (int i = 0; i < k; i++) {
            cum += topVals[i];
            if (r <= cum) return topIdx[i];
        }
        return topIdx[k - 1];
    }

    public static float[] applyTemperature(float[] logits, float temperature) {
        float[] out = new float[logits.length];
        for (int i = 0; i < logits.length; i++) out[i] = logits[i] / Math.max(temperature, 1e-8f);
        return out;
    }
}
