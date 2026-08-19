package javagpt;

import java.io.*;
import java.util.Random;

public class TransformerBlock {
    public final LayerNorm ln1, ln2;
    public final MultiHeadAttention attn;
    public final FeedForward ff;

    public TransformerBlock(int embedDim, int numHeads, Random rng) {
        this.ln1 = new LayerNorm(embedDim, 1e-5f);
        this.ln2 = new LayerNorm(embedDim, 1e-5f);
        this.attn = new MultiHeadAttention(embedDim, numHeads, rng);
        this.ff = new FeedForward(embedDim, embedDim * 4, rng);
    }

    public Tensor forward(Tensor x) {
        // Pre-norm transformer block:
        // x = x + MHA(LayerNorm(x))
        // x = x + FF(LayerNorm(x))
        Tensor ln1Out = ln1.forward(x);
        Tensor attnOut = attn.forward(ln1Out);
        // residual
        Tensor x1 = Tensor.add(x, attnOut);

        Tensor ln2Out = ln2.forward(x1);
        Tensor ffOut = ff.forward(ln2Out);
        // residual
        return Tensor.add(x1, ffOut);
    }

    public Tensor backward(Tensor gradOut) {
        // gradOut flows through residual addition for FF
        Tensor gradX1 = gradOut; // residual: grad passes through

        // backward through FF
        Tensor gradLn2 = ff.backward(gradOut);

        // backward through LN2 (on x1)
        Tensor gradAfterLn2 = ln2.backward(gradLn2);

        // add gradient from attn residual
        Tensor gradX = Tensor.add(gradX1, gradAfterLn2);

        // backward through attention (on x)
        Tensor gradLn1 = attn.backward(gradX);

        // backward through LN1
        Tensor gradInput = ln1.backward(gradLn1);

        // add original residual
        Tensor result = Tensor.add(gradOut, gradInput);
        return result;
    }

    public void zeroGrad() {
        ln1.zeroGrad();
        ln2.zeroGrad();
        attn.zeroGrad();
        ff.zeroGrad();
    }

    public void save(DataOutputStream out) throws IOException {
        ln1.save(out);
        ln2.save(out);
        attn.save(out);
        ff.save(out);
    }

    public void load(DataInputStream in) throws IOException {
        ln1.load(in);
        ln2.load(in);
        attn.load(in);
        ff.load(in);
    }
}
