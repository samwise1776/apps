package javagpt;

import java.io.*;
import java.util.Random;

public class Linear {
    public final int inFeatures, outFeatures;
    public final float[] weight;  // (inFeatures, outFeatures)
    public final float[] bias;    // (outFeatures,)
    public final float[] gradWeight;
    public final float[] gradBias;

    // cached for backward
    private Tensor input;
    private Tensor output;

    public Linear(int inFeatures, int outFeatures, Random rng) {
        this.inFeatures = inFeatures;
        this.outFeatures = outFeatures;
        this.weight = new float[inFeatures * outFeatures];
        this.bias = new float[outFeatures];
        this.gradWeight = new float[inFeatures * outFeatures];
        this.gradBias = new float[outFeatures];
        float std = (float) (1.0 / Math.sqrt(inFeatures));
        for (int i = 0; i < weight.length; i++) {
            weight[i] = (float) (std * rng.nextGaussian());
        }
    }

    public Tensor forward(Tensor x) {
        // x: (..., inFeatures) -> (..., outFeatures)
        this.input = x;
        int[] shape = x.shape.clone();
        shape[shape.length - 1] = outFeatures;
        Tensor out = new Tensor(shape);

        int outer = 1;
        for (int i = 0; i < shape.length - 1; i++) outer *= shape[i];

        for (int o = 0; o < outer; o++) {
            int inOff = o * inFeatures;
            int outOff = o * outFeatures;
            for (int j = 0; j < outFeatures; j++) {
                float sum = bias[j];
                for (int k = 0; k < inFeatures; k++) {
                    sum += x.data[inOff + k] * weight[k * outFeatures + j];
                }
                out.data[outOff + j] = sum;
            }
        }
        this.output = out;
        return out;
    }

    public Tensor backward(Tensor gradOut) {
        // gradOut: (..., outFeatures)
        int outer = 1;
        for (int i = 0; i < gradOut.ndim - 1; i++) outer *= gradOut.shape[i];

        // gradWeight
        for (int i = 0; i < inFeatures; i++) {
            for (int j = 0; j < outFeatures; j++) {
                float g = 0;
                for (int o = 0; o < outer; o++) {
                    g += input.data[o * inFeatures + i] * gradOut.data[o * outFeatures + j];
                }
                gradWeight[i * outFeatures + j] += g;
            }
        }

        // gradBias
        for (int j = 0; j < outFeatures; j++) {
            float g = 0;
            for (int o = 0; o < outer; o++) {
                g += gradOut.data[o * outFeatures + j];
            }
            gradBias[j] += g;
        }

        // gradInput
        int[] inShape = input.shape.clone();
        Tensor gradInput = new Tensor(inShape);
        for (int o = 0; o < outer; o++) {
            int inOff = o * inFeatures;
            int outOff = o * outFeatures;
            for (int i = 0; i < inFeatures; i++) {
                float g = 0;
                for (int j = 0; j < outFeatures; j++) {
                    g += gradOut.data[outOff + j] * weight[i * outFeatures + j];
                }
                gradInput.data[inOff + i] = g;
            }
        }
        return gradInput;
    }

    public void zeroGrad() {
        java.util.Arrays.fill(gradWeight, 0f);
        java.util.Arrays.fill(gradBias, 0f);
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(inFeatures);
        out.writeInt(outFeatures);
        for (float w : weight) out.writeFloat(w);
        for (float b : bias) out.writeFloat(b);
    }

    public void load(DataInputStream in) throws IOException {
        int inF = in.readInt(), outF = in.readInt();
        for (int i = 0; i < weight.length; i++) weight[i] = in.readFloat();
        for (int i = 0; i < bias.length; i++) bias[i] = in.readFloat();
    }
}
