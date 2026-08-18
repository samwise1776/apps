package javagpt;

import java.io.*;
import java.util.Random;

public class FeedForward {
    public final Linear w1, w2;
    private Tensor geluInput;

    public FeedForward(int embedDim, int ffDim, Random rng) {
        this.w1 = new Linear(embedDim, ffDim, rng);
        this.w2 = new Linear(ffDim, embedDim, rng);
    }

    public Tensor forward(Tensor x) {
        Tensor h = w1.forward(x);       // (..., ffDim)
        geluInput = h;
        h = Tensor.gelu(h);             // (..., ffDim)
        return w2.forward(h);           // (..., embedDim)
    }

    public Tensor backward(Tensor gradOut) {
        Tensor gradH2 = w2.backward(gradOut);       // (..., ffDim)
        Tensor gradGelu = Tensor.geluDerv(geluInput, gradH2);
        return w1.backward(gradGelu);                // (..., embedDim)
    }

    public void zeroGrad() {
        w1.zeroGrad();
        w2.zeroGrad();
    }

    public void save(DataOutputStream out) throws IOException {
        w1.save(out);
        w2.save(out);
    }

    public void load(DataInputStream in) throws IOException {
        w1.load(in);
        w2.load(in);
    }
}
