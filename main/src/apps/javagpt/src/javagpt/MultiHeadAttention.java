package javagpt;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

public class MultiHeadAttention {
    public final int embedDim, numHeads, headDim;
    public final Linear qProj, kProj, vProj, oProj;
    private int B, S;
    private Tensor qReshaped, kReshaped, vReshaped;
    private Tensor scores;
    private Tensor attnWeights;
    private Tensor attnOut;
    private Tensor qForBack, kForBack, vForBack;
    private boolean[] causalMask;

    public MultiHeadAttention(int embedDim, int numHeads, Random rng) {
        this.embedDim = embedDim;
        this.numHeads = numHeads;
        this.headDim = embedDim / numHeads;
        this.qProj = new Linear(embedDim, embedDim, rng);
        this.kProj = new Linear(embedDim, embedDim, rng);
        this.vProj = new Linear(embedDim, embedDim, rng);
        this.oProj = new Linear(embedDim, embedDim, rng);
    }

    public Tensor forward(Tensor x) {
        // x: (B, S, embedDim)
        B = x.shape[0];
        S = x.shape[1];

        Tensor Q = qProj.forward(x); // (B, S, embedDim)
        Tensor K = kProj.forward(x);
        Tensor V = vProj.forward(x);

        qForBack = Q;
        kForBack = K;
        vForBack = V;

        // reshape to (B, S, H, D)
        Q = Q.reshapeForAttention(B, S, numHeads, headDim);
        K = K.reshapeForAttention(B, S, numHeads, headDim);
        V = V.reshapeForAttention(B, S, numHeads, headDim);

        // transpose to (B, H, S, D)
        Q = Q.transpose(1, 2);
        K = K.transpose(1, 2);
        V = V.transpose(1, 2);

        qReshaped = Q;
        kReshaped = K;
        vReshaped = V;

        // scores = Q @ K^T / sqrt(headDim)  -> (B, H, S, S)
        Tensor Kt = K.transpose(2, 3); // (B, H, D, S)
        scores = Tensor.matmul(Q, Kt); // (B, H, S, S)

        float scale = (float) Math.sqrt(headDim);
        for (int i = 0; i < scores.size; i++) scores.data[i] /= scale;

        // causal mask
        causalMask = new boolean[S * S];
        for (int i = 0; i < S; i++) {
            for (int j = i + 1; j < S; j++) {
                scores.data[i * S + j] = Float.NEGATIVE_INFINITY;
                causalMask[i * S + j] = true;
            }
        }
        // Apply mask across all batches and heads
        for (int b = 0; b < B; b++) {
            for (int h = 0; h < numHeads; h++) {
                int off = (b * numHeads + h) * S * S;
                for (int i = 0; i < S; i++) {
                    for (int j = i + 1; j < S; j++) {
                        scores.data[off + i * S + j] = Float.NEGATIVE_INFINITY;
                    }
                }
            }
        }

        // softmax
        attnWeights = Tensor.softmax(scores, 3); // (B, H, S, S)

        // output = attnWeights @ V -> (B, H, S, D)
        attnOut = Tensor.matmul(attnWeights, V);

        // transpose to (B, S, H, D) then reshape to (B, S, embedDim)
        Tensor out = attnOut.transpose(1, 2); // (B, S, H, D)
        out = out.reshape(B, S, embedDim);

        // output projection
        return oProj.forward(out);
    }

    public Tensor backward(Tensor gradOut) {
        // gradOut: (B, S, embedDim)
        // backward through oProj
        Tensor gradAttnOut = oProj.backward(gradOut); // (B, S, embedDim)

        // reshape to (B, S, H, D)
        Tensor gradReshaped = gradAttnOut.reshapeForAttention(B, S, numHeads, headDim);
        // transpose to (B, H, S, D)
        Tensor gradHSD = gradReshaped.transpose(1, 2);

        // grad_weights = gradHSD @ V^T
        Tensor Vt = vReshaped.transpose(2, 3);
        Tensor gradAttnWeights = Tensor.matmul(gradHSD, Vt); // (B, H, S, S)

        // grad_V = attnWeights^T @ gradHSD
        Tensor aT = attnWeights.transpose(2, 3); // (B, H, S, S)
        Tensor gradV = Tensor.matmul(aT, gradHSD); // (B, H, S, D)

        // backward through softmax
        Tensor gradScores = new Tensor(scores.shape.clone());
        for (int b = 0; b < B; b++) {
            for (int h = 0; h < numHeads; h++) {
                int off = (b * numHeads + h) * S * S;
                for (int i = 0; i < S; i++) {
                    float dot = 0;
                    for (int j = 0; j < S; j++) {
                        dot += attnWeights.data[off + i * S + j] * gradAttnWeights.data[off + i * S + j];
                    }
                    for (int j = 0; j < S; j++) {
                        gradScores.data[off + i * S + j] = attnWeights.data[off + i * S + j] * (gradAttnWeights.data[off + i * S + j] - dot);
                    }
                }
            }
        }

        // scale by 1/sqrt(headDim)
        float scale = (float) Math.sqrt(headDim);
        for (int i = 0; i < gradScores.size; i++) gradScores.data[i] /= scale;

        // grad_Q = gradScores @ K
        Tensor gradQ = Tensor.matmul(gradScores, kReshaped); // (B, H, S, D)

        // grad_K = gradScores^T @ Q
        Tensor gST = gradScores.transpose(2, 3); // (B, H, S, S)
        Tensor gradK = Tensor.matmul(gST, qReshaped); // (B, H, S, D)

        // reshape gradQ, gradK, gradV: (B, H, S, D) -> (B, S, H, D) -> (B, S, embedDim)
        Tensor gQflat = gradQ.transpose(1, 2).reshape(B, S, embedDim);
        Tensor gKflat = gradK.transpose(1, 2).reshape(B, S, embedDim);
        Tensor gVflat = gradV.transpose(1, 2).reshape(B, S, embedDim);

        // backward through projections
        Tensor gradQin = qProj.backward(gQflat);
        Tensor gradKin = kProj.backward(gKflat);
        Tensor gradVin = vProj.backward(gVflat);

        // sum gradients from Q, K, V paths
        Tensor gradInput = new Tensor(B, S, embedDim);
        for (int i = 0; i < gradInput.size; i++) {
            gradInput.data[i] = gradQin.data[i] + gradKin.data[i] + gradVin.data[i];
        }
        return gradInput;
    }

    public void zeroGrad() {
        qProj.zeroGrad();
        kProj.zeroGrad();
        vProj.zeroGrad();
        oProj.zeroGrad();
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(embedDim);
        out.writeInt(numHeads);
        qProj.save(out);
        kProj.save(out);
        vProj.save(out);
        oProj.save(out);
    }

    public void load(DataInputStream in) throws IOException {
        in.readInt(); in.readInt(); // skip embedDim, numHeads (already set by constructor)
        qProj.load(in);
        kProj.load(in);
        vProj.load(in);
        oProj.load(in);
    }
}
