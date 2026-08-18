package javagpt;

import java.io.*;
import java.util.Arrays;

public class AdamW {
    private final float lr, beta1, beta2, epsilon, weightDecay;
    private int step = 0;

    private float[][] params;
    private float[][] grads;
    private float[][] m;
    private float[][] v;
    private int paramCount = 0;

    public AdamW(float lr, float beta1, float beta2, float epsilon, float weightDecay) {
        this.lr = lr;
        this.beta1 = beta1;
        this.beta2 = beta2;
        this.epsilon = epsilon;
        this.weightDecay = weightDecay;
    }

    public void registerAllWithGrads(GPT model) {
        params = new float[100][];
        grads = new float[100][];
        m = new float[100][];
        v = new float[100][];
        paramCount = 0;

        addWithGrad(model.tokenEmb.weight, model.tokenEmb.gradWeight);
        addWithGrad(model.posEmb.weight, model.posEmb.gradWeight);
        for (TransformerBlock block : model.blocks) {
            addWithGrad(block.ln1.gamma, block.ln1.gradGamma);
            addWithGrad(block.ln1.beta, block.ln1.gradBeta);
            addWithGrad(block.attn.qProj.weight, block.attn.qProj.gradWeight);
            addWithGrad(block.attn.qProj.bias, block.attn.qProj.gradBias);
            addWithGrad(block.attn.kProj.weight, block.attn.kProj.gradWeight);
            addWithGrad(block.attn.kProj.bias, block.attn.kProj.gradBias);
            addWithGrad(block.attn.vProj.weight, block.attn.vProj.gradWeight);
            addWithGrad(block.attn.vProj.bias, block.attn.vProj.gradBias);
            addWithGrad(block.attn.oProj.weight, block.attn.oProj.gradWeight);
            addWithGrad(block.attn.oProj.bias, block.attn.oProj.gradBias);
            addWithGrad(block.ff.w1.weight, block.ff.w1.gradWeight);
            addWithGrad(block.ff.w1.bias, block.ff.w1.gradBias);
            addWithGrad(block.ff.w2.weight, block.ff.w2.gradWeight);
            addWithGrad(block.ff.w2.bias, block.ff.w2.gradBias);
            addWithGrad(block.ln2.gamma, block.ln2.gradGamma);
            addWithGrad(block.ln2.beta, block.ln2.gradBeta);
        }
        addWithGrad(model.finalLn.gamma, model.finalLn.gradGamma);
        addWithGrad(model.finalLn.beta, model.finalLn.gradBeta);
        addWithGrad(model.lmHead.weight, model.lmHead.gradWeight);
        addWithGrad(model.lmHead.bias, model.lmHead.gradBias);
    }

    private void addWithGrad(float[] param, float[] grad) {
        if (paramCount >= params.length) {
            int newLen = params.length * 2;
            params = Arrays.copyOf(params, newLen);
            grads = Arrays.copyOf(grads, newLen);
            m = Arrays.copyOf(m, newLen);
            v = Arrays.copyOf(v, newLen);
        }
        params[paramCount] = param;
        grads[paramCount] = grad;
        m[paramCount] = new float[param.length];
        v[paramCount] = new float[param.length];
        paramCount++;
    }

    public void stepWithGrads() {
        step++;
        float bc1 = 1.0f - (float) Math.pow(beta1, step);
        float bc2 = 1.0f - (float) Math.pow(beta2, step);

        for (int p = 0; p < paramCount; p++) {
            float[] param = params[p];
            float[] grad = grads[p];
            for (int i = 0; i < param.length; i++) {
                float g = grad[i];
                if (weightDecay > 0) g += weightDecay * param[i];
                m[p][i] = beta1 * m[p][i] + (1 - beta1) * g;
                v[p][i] = beta2 * v[p][i] + (1 - beta2) * g * g;
                float mHat = m[p][i] / bc1;
                float vHat = v[p][i] / bc2;
                param[i] -= lr * mHat / ((float) Math.sqrt(vHat) + epsilon);
            }
        }
    }

    public int getStep() { return step; }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(step);
    }

    public void load(DataInputStream in) throws IOException {
        step = in.readInt();
    }
}
