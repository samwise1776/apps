package javagpt;

import java.io.*;
import java.util.Random;

public class GPT {
    public final GPTConfig config;
    public final Embedding tokenEmb;
    public final Embedding posEmb;
    public final TransformerBlock[] blocks;
    public final LayerNorm finalLn;
    public final Linear lmHead;

    private Tensor xNorm;
    private Tensor posEmbOut;
    private int[] lastTokens;
    private int lastSeqLen;

    public GPT(GPTConfig config, Random rng) {
        this.config = config;
        this.tokenEmb = new Embedding(config.vocabSize, config.embedDim, rng);
        this.posEmb = new Embedding(config.contextLength, config.embedDim, rng);
        this.blocks = new TransformerBlock[config.numLayers];
        for (int i = 0; i < config.numLayers; i++) {
            blocks[i] = new TransformerBlock(config.embedDim, config.numHeads, rng);
        }
        this.finalLn = new LayerNorm(config.embedDim, 1e-5f);
        this.lmHead = new Linear(config.embedDim, config.vocabSize, rng);
    }

    /**
     * Forward pass. Returns logits of shape (B, S, vocabSize).
     */
    public Tensor forward(int[][] tokens) {
        int B = tokens.length;
        int S = tokens[0].length;
        this.lastTokens = tokens[0];
        this.lastSeqLen = S;

        // token embeddings: (B, S, embedDim)
        Tensor x = tokenEmb.forwardBatch(tokens);

        // positional embeddings: (S, embedDim) broadcast to (B, S, embedDim)
        int[] posIndices = new int[S];
        for (int i = 0; i < S; i++) posIndices[i] = i;
        posEmbOut = posEmb.forward(posIndices); // (1, S, embedDim)
        // broadcast add
        for (int b = 0; b < B; b++) {
            for (int s = 0; s < S * config.embedDim; s++) {
                x.data[b * S * config.embedDim + s] += posEmbOut.data[s];
            }
        }

        // transformer blocks
        for (TransformerBlock block : blocks) {
            x = block.forward(x);
        }

        // final layer norm
        xNorm = x;
        x = finalLn.forward(x);

        // language model head
        Tensor logits = lmHead.forward(x); // (B, S, vocabSize)
        return logits;
    }

    /**
     * Backward pass. Given grad of shape (B, S, vocabSize), compute all gradients.
     */
    public void backward(Tensor gradLogits) {
        // backward through lmHead
        Tensor gradLnOut = lmHead.backward(gradLogits);

        // backward through final LN
        Tensor gradX = finalLn.backward(gradLnOut);

        // backward through transformer blocks in reverse
        for (int i = config.numLayers - 1; i >= 0; i--) {
            gradX = blocks[i].backward(gradX);
        }

        // gradX flows to posEmb and tokenEmb
        // For positional embeddings
        int S = lastSeqLen;
        int E = config.embedDim;
        int[] posIndices = new int[S];
        for (int i = 0; i < S; i++) posIndices[i] = i;

        // accumulate positional embedding gradients
        for (int s = 0; s < S; s++) {
            for (int d = 0; d < E; d++) {
                posEmb.gradWeight[posIndices[s] * E + d] += gradX.data[s * E + d];
            }
        }

        // accumulate token embedding gradients
        for (int s = 0; s < S; s++) {
            int id = lastTokens[s];
            if (id >= 0 && id < config.vocabSize) {
                for (int d = 0; d < E; d++) {
                    tokenEmb.gradWeight[id * E + d] += gradX.data[s * E + d];
                }
            }
        }
    }

    /**
     * Compute cross-entropy loss given logits and targets.
     * logits: (B, S, V), targets: (B, S)
     * Returns average loss and fills gradLogits.
     */
    public float computeLoss(Tensor logits, int[][] targets, Tensor gradLogits) {
        int B = logits.shape[0], S = logits.shape[1], V = logits.shape[2];
        float totalLoss = 0;
        int count = 0;

        java.util.Arrays.fill(gradLogits.data, 0f);

        for (int b = 0; b < B; b++) {
            for (int s = 0; s < S; s++) {
                int target = targets[b][s];
                int base = (b * S + s) * V;

                // softmax
                float maxVal = Float.NEGATIVE_INFINITY;
                for (int v = 0; v < V; v++) {
                    if (logits.data[base + v] > maxVal) maxVal = logits.data[base + v];
                }
                float sumExp = 0;
                float[] probs = new float[V];
                for (int v = 0; v < V; v++) {
                    probs[v] = (float) Math.exp(logits.data[base + v] - maxVal);
                    sumExp += probs[v];
                }
                for (int v = 0; v < V; v++) probs[v] /= (sumExp + 1e-20f);

                // loss = -log(p(target))
                float pTarget = (target >= 0 && target < V) ? probs[target] : 1e-20f;
                totalLoss += -Math.log(pTarget + 1e-20f);
                count++;

                // grad = probs - one_hot(target)
                for (int v = 0; v < V; v++) {
                    gradLogits.data[base + v] = (probs[v] - (v == target ? 1f : 0f)) / count;
                }
            }
        }
        return totalLoss / count;
    }

    public void zeroAllGrads() {
        tokenEmb.zeroGrad();
        posEmb.zeroGrad();
        for (TransformerBlock block : blocks) block.zeroGrad();
        finalLn.zeroGrad();
        lmHead.zeroGrad();
    }

    public void save(String path) throws IOException {
        DataOutputStream out = new DataOutputStream(new FileOutputStream(path));
        config.save(out);
        tokenEmb.save(out);
        posEmb.save(out);
        for (TransformerBlock block : blocks) block.save(out);
        finalLn.save(out);
        lmHead.save(out);
        out.close();
    }

    public static GPT load(String path) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(path));
        GPTConfig config = GPTConfig.load(in);
        GPT model = new GPT(config, new Random(0));
        model.tokenEmb.load(in);
        model.posEmb.load(in);
        for (TransformerBlock block : model.blocks) block.load(in);
        model.finalLn.load(in);
        model.lmHead.load(in);
        in.close();
        return model;
    }
}
