package javagpt;

import java.io.*;
import java.util.Random;

public class LayerNorm {
    public final int features;
    public final float[] gamma;  // (features,)
    public final float[] beta;   // (features,)
    public final float[] gradGamma;
    public final float[] gradBeta;
    private final float eps;

    // cached for backward
    private Tensor input;
    private Tensor xHat;
    private Tensor mean;
    private Tensor var;

    public LayerNorm(int features, float eps) {
        this.features = features;
        this.eps = eps;
        this.gamma = new float[features];
        this.beta = new float[features];
        this.gradGamma = new float[features];
        this.gradBeta = new float[features];
        java.util.Arrays.fill(gamma, 1f);
    }

    public Tensor forward(Tensor x) {
        // x: (..., features)
        this.input = x;
        int[] shape = x.shape.clone();
        int outer = 1;
        for (int i = 0; i < shape.length - 1; i++) outer *= shape[i];
        int feat = shape[shape.length - 1];

        Tensor m = new Tensor(outer, 1);
        Tensor v = new Tensor(outer, 1);
        Tensor h = new Tensor(x.data.clone(), shape);

        for (int o = 0; o < outer; o++) {
            float sum = 0;
            for (int f = 0; f < feat; f++) sum += x.data[o * feat + f];
            float meanVal = sum / feat;
            m.data[o] = meanVal;
            float varSum = 0;
            for (int f = 0; f < feat; f++) {
                float diff = x.data[o * feat + f] - meanVal;
                varSum += diff * diff;
            }
            float varVal = varSum / feat;
            v.data[o] = varVal;
            float invStd = 1.0f / (float) Math.sqrt(varVal + eps);
            for (int f = 0; f < feat; f++) {
                float normalized = (x.data[o * feat + f] - meanVal) * invStd;
                h.data[o * feat + f] = gamma[f] * normalized + beta[f];
            }
        }

        this.mean = m;
        this.var = v;
        // Recompute xHat for backward
        Tensor xHatT = new Tensor(shape);
        for (int o = 0; o < outer; o++) {
            float invStd = 1.0f / (float) Math.sqrt(v.data[o] + eps);
            for (int f = 0; f < feat; f++) {
                xHatT.data[o * feat + f] = (x.data[o * feat + f] - mean.data[o]) * invStd;
            }
        }
        this.xHat = xHatT;
        return h;
    }

    public Tensor backward(Tensor gradOut) {
        int[] shape = input.shape.clone();
        int outer = 1;
        for (int i = 0; i < shape.length - 1; i++) outer *= shape[i];
        int feat = shape[shape.length - 1];

        // gradGamma and gradBeta
        for (int f = 0; f < feat; f++) {
            float gG = 0, gB = 0;
            for (int o = 0; o < outer; o++) {
                gG += gradOut.data[o * feat + f] * xHat.data[o * feat + f];
                gB += gradOut.data[o * feat + f];
            }
            gradGamma[f] += gG;
            gradBeta[f] += gB;
        }

        // gradInput
        Tensor gradInput = new Tensor(shape);
        for (int o = 0; o < outer; o++) {
            float invStd = 1.0f / (float) Math.sqrt(var.data[o] + eps);
            float gxhatSum = 0, gxhatxhatSum = 0;
            for (int f = 0; f < feat; f++) {
                float gxhat = gradOut.data[o * feat + f] * gamma[f];
                gxhatSum += gxhat;
                gxhatxhatSum += gxhat * xHat.data[o * feat + f];
            }
            for (int f = 0; f < feat; f++) {
                float gxhat = gradOut.data[o * feat + f] * gamma[f];
                gradInput.data[o * feat + f] = invStd * (gxhat - gxhatSum / feat - xHat.data[o * feat + f] * gxhatxhatSum / feat);
            }
        }
        return gradInput;
    }

    public void zeroGrad() {
        java.util.Arrays.fill(gradGamma, 0f);
        java.util.Arrays.fill(gradBeta, 0f);
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(features);
        out.writeFloat(eps);
        for (float g : gamma) out.writeFloat(g);
        for (float b : beta) out.writeFloat(b);
    }

    public void load(DataInputStream in) throws IOException {
        int f = in.readInt();
        float e = in.readFloat();
        for (int i = 0; i < gamma.length; i++) gamma[i] = in.readFloat();
        for (int i = 0; i < beta.length; i++) beta[i] = in.readFloat();
    }
}
