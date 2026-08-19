package javagpt;

import java.io.*;
import java.nio.file.*;

public class GPTConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public int vocabSize = 256;
    public int embedDim = 128;
    public int numLayers = 2;
    public int numHeads = 4;
    public int headDim;          // embedDim / numHeads
    public int ffDim;            // embedDim * 4
    public int contextLength = 64;
    public int batchSize = 8;
    public float learningRate = 1e-3f;
    public int trainingSteps = 1000;
    public float weightDecay = 0.01f;
    public float beta1 = 0.9f;
    public float beta2 = 0.999f;
    public float epsilon = 1e-8f;
    public float gradClip = 1.0f;
    public String name = "tiny";

    public GPTConfig() { recalc(); }

    public void recalc() {
        headDim = embedDim / numHeads;
        ffDim = embedDim * 4;
    }

    public long parameterCount() {
        recalc();
        long p = 0;
        p += (long) vocabSize * embedDim;           // token embeddings
        p += (long) contextLength * embedDim;        // positional embeddings
        for (int i = 0; i < numLayers; i++) {
            p += (long) 4 * embedDim * embedDim;    // Wq,Wk,Wv,Wo
            p += (long) 4 * embedDim;                // bq,bk,bv,bo
            p += (long) 2 * embedDim;                // LN1 gamma,beta
            p += (long) embedDim * ffDim + ffDim;    // FFN w1,b1
            p += (long) ffDim * embedDim + embedDim;  // FFN w2,b2
            p += (long) 2 * embedDim;                // LN2 gamma,beta
        }
        p += (long) 2 * embedDim;                    // final LN gamma,beta
        p += (long) embedDim * vocabSize;            // output projection
        return p;
    }

    public long estimatedRamBytes() {
        long paramBytes = parameterCount() * 4L;
        return paramBytes * 3; // params + grads + optimizer moments
    }

    public void printInfo() {
        recalc();
        long params = parameterCount();
        long ram = estimatedRamBytes();
        System.out.println("=== Model Configuration: " + name + " ===");
        System.out.printf("  Vocabulary:    %,d%n", vocabSize);
        System.out.printf("  Embedding:     %d%n", embedDim);
        System.out.printf("  Layers:        %d%n", numLayers);
        System.out.printf("  Heads:         %d%n", numHeads);
        System.out.printf("  Head dim:      %d%n", headDim);
        System.out.printf("  FF dim:        %d%n", ffDim);
        System.out.printf("  Context:       %d%n", contextLength);
        System.out.printf("  Batch:         %d%n", batchSize);
        System.out.printf("  Learning rate: %.1e%n", learningRate);
        System.out.printf("  Parameters:    %,d (~%.2fM)%n", params, params / 1e6);
        System.out.printf("  Est RAM:       ~%.1f MB%n", ram / 1e6);
        System.out.printf("  Weight size:   ~%.1f MB%n", (params * 4L) / 1e6);
    }

    public static GPTConfig tiny() {
        GPTConfig c = new GPTConfig();
        c.name = "tiny";
        c.vocabSize = 256;
        c.embedDim = 128;
        c.numLayers = 2;
        c.numHeads = 4;
        c.contextLength = 64;
        c.batchSize = 8;
        c.learningRate = 3e-3f;
        c.trainingSteps = 2000;
        c.recalc();
        return c;
    }

    public static GPTConfig small() {
        GPTConfig c = new GPTConfig();
        c.name = "small";
        c.vocabSize = 256;
        c.embedDim = 256;
        c.numLayers = 6;
        c.numHeads = 8;
        c.contextLength = 128;
        c.batchSize = 4;
        c.learningRate = 1e-3f;
        c.trainingSteps = 5000;
        c.recalc();
        return c;
    }

    public static GPTConfig medium() {
        GPTConfig c = new GPTConfig();
        c.name = "medium";
        c.vocabSize = 256;
        c.embedDim = 384;
        c.numLayers = 8;
        c.numHeads = 12;
        c.contextLength = 256;
        c.batchSize = 2;
        c.learningRate = 5e-4f;
        c.trainingSteps = 10000;
        c.recalc();
        return c;
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(1); // version
        out.writeUTF(name);
        out.writeInt(vocabSize);
        out.writeInt(embedDim);
        out.writeInt(numLayers);
        out.writeInt(numHeads);
        out.writeInt(contextLength);
        out.writeInt(batchSize);
        out.writeFloat(learningRate);
        out.writeInt(trainingSteps);
        out.writeFloat(weightDecay);
        out.writeFloat(beta1);
        out.writeFloat(beta2);
        out.writeFloat(epsilon);
        out.writeFloat(gradClip);
        recalc();
    }

    public static GPTConfig load(DataInputStream in) throws IOException {
        int version = in.readInt();
        GPTConfig c = new GPTConfig();
        c.name = in.readUTF();
        c.vocabSize = in.readInt();
        c.embedDim = in.readInt();
        c.numLayers = in.readInt();
        c.numHeads = in.readInt();
        c.contextLength = in.readInt();
        c.batchSize = in.readInt();
        c.learningRate = in.readFloat();
        c.trainingSteps = in.readInt();
        c.weightDecay = in.readFloat();
        c.beta1 = in.readFloat();
        c.beta2 = in.readFloat();
        c.epsilon = in.readFloat();
        c.gradClip = in.readFloat();
        c.recalc();
        return c;
    }
}
