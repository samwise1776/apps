package javagpt;

import java.io.*;
import java.util.Random;

public class Embedding {
    public final int vocabSize, embedDim;
    public final float[] weight; // (vocabSize, embedDim)
    public final float[] gradWeight;

    public Embedding(int vocabSize, int embedDim, Random rng) {
        this.vocabSize = vocabSize;
        this.embedDim = embedDim;
        this.weight = new float[vocabSize * embedDim];
        this.gradWeight = new float[vocabSize * embedDim];
        float std = (float) (1.0 / Math.sqrt(embedDim));
        for (int i = 0; i < weight.length; i++) {
            weight[i] = (float) (std * rng.nextGaussian());
        }
    }

    // cached for backward
    private int[] lastIndices;

    public Tensor forward(int[] indices) {
        this.lastIndices = indices.clone();
        int seqLen = indices.length;
        Tensor out = new Tensor(1, seqLen, embedDim);
        for (int s = 0; s < seqLen; s++) {
            int id = indices[s];
            if (id >= 0 && id < vocabSize) {
                System.arraycopy(weight, id * embedDim, out.data, s * embedDim, embedDim);
            }
        }
        return out;
    }

    public Tensor forwardBatch(int[][] batchIndices) {
        int B = batchIndices.length;
        int S = batchIndices[0].length;
        this.lastIndices = new int[B * S];
        Tensor out = new Tensor(B, S, embedDim);
        for (int b = 0; b < B; b++) {
            for (int s = 0; s < S; s++) {
                int id = batchIndices[b][s];
                lastIndices[b * S + s] = id;
                if (id >= 0 && id < vocabSize) {
                    System.arraycopy(weight, id * embedDim, out.data, (b * S + s) * embedDim, embedDim);
                }
            }
        }
        return out;
    }

    public Tensor backward(Tensor gradOut) {
        // gradOut: (B, S, embedDim)
        // Accumulate gradients into weight
        int B = gradOut.shape[0], S = gradOut.shape[1];
        for (int b = 0; b < B; b++) {
            for (int s = 0; s < S; s++) {
                int id = lastIndices[b * S + s];
                if (id >= 0 && id < vocabSize) {
                    int gradOff = (b * S + s) * embedDim;
                    int wOff = id * embedDim;
                    for (int d = 0; d < embedDim; d++) {
                        gradWeight[wOff + d] += gradOut.data[gradOff + d];
                    }
                }
            }
        }
        return null; // no gradient flows to indices
    }

    public void zeroGrad() {
        java.util.Arrays.fill(gradWeight, 0f);
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(vocabSize);
        out.writeInt(embedDim);
        for (float w : weight) out.writeFloat(w);
    }

    public void load(DataInputStream in) throws IOException {
        int vs = in.readInt(), ed = in.readInt();
        for (int i = 0; i < weight.length; i++) weight[i] = in.readFloat();
    }
}
