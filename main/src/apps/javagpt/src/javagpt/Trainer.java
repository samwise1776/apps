package javagpt;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Trainer {
    private final GPT model;
    private final GPTConfig config;
    private final AdamW optimizer;
    private final Random rng;
    private final List<Float> lossHistory = new ArrayList<>();

    public Trainer(GPT model, GPTConfig config) {
        this.model = model;
        this.config = config;
        this.optimizer = new AdamW(config.learningRate, config.beta1, config.beta2, config.epsilon, config.weightDecay);
        this.optimizer.registerAllWithGrads(model);
        this.rng = new Random(42);
    }

    public void train(Dataset dataset) {
        System.out.println("\n=== JavaGPT Training ===");
        config.printInfo();
        System.out.println();

        int[][] inputs = new int[config.batchSize][config.contextLength];
        int[][] targets = new int[config.batchSize][config.contextLength];
        Tensor gradLogits = new Tensor(config.batchSize, config.contextLength, config.vocabSize);

        long startTime = System.currentTimeMillis();
        int totalTokens = 0;

        for (int step = 1; step <= config.trainingSteps; step++) {
            dataset.reset();
            float stepLoss = 0;
            int batchesThisStep = 0;

            while (dataset.hasMore() && batchesThisStep < 10) {
                int[][][] batch = dataset.getNextBatch(config.batchSize);
                inputs = batch[0];
                targets = batch[1];

                // forward
                Tensor logits = model.forward(inputs);

                // compute loss
                float loss = model.computeLoss(logits, targets, gradLogits);
                stepLoss += loss;
                batchesThisStep++;

                // clip gradients
                clipGradients(gradLogits, config.gradClip);

                // backward
                model.backward(gradLogits);

                // update weights
                optimizer.stepWithGrads();

                // zero grads for next step
                model.zeroAllGrads();

                totalTokens += config.batchSize * config.contextLength;
            }

            float avgLoss = stepLoss / Math.max(batchesThisStep, 1);
            lossHistory.add(avgLoss);

            if (step % 10 == 0 || step == 1 || step == config.trainingSteps) {
                long elapsed = System.currentTimeMillis() - startTime;
                float tokPerSec = (elapsed > 0) ? (totalTokens * 1000f / elapsed) : 0;
                System.out.printf("Step %,6d / %,d  loss=%.4f  [%.1f tok/s]%n",
                        step, config.trainingSteps, avgLoss, tokPerSec);
            }

            // periodic checkpoint
            if (step % 500 == 0 && step < config.trainingSteps) {
                try {
                    saveCheckpoint("models/checkpoint_step" + step + ".bin");
                } catch (IOException e) {
                    System.err.println("Checkpoint save failed: " + e.getMessage());
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("%nTraining complete. Time: %.1fs  Tokens: %,d  Avg final loss: %.4f%n",
                elapsed / 1000.0, totalTokens,
                lossHistory.isEmpty() ? 0 : lossHistory.get(lossHistory.size() - 1));
    }

    private void clipGradients(Tensor grad, float maxNorm) {
        float norm = 0;
        for (int i = 0; i < grad.size; i++) norm += grad.data[i] * grad.data[i];
        norm = (float) Math.sqrt(norm);
        if (norm > maxNorm) {
            float scale = maxNorm / norm;
            for (int i = 0; i < grad.size; i++) grad.data[i] *= scale;
        }
        // Also clip per-parameter gradients
        clipParamGrads(model.tokenEmb.gradWeight, maxNorm);
        clipParamGrads(model.posEmb.gradWeight, maxNorm);
        for (TransformerBlock block : model.blocks) {
            clipParamGrads(block.attn.qProj.gradWeight, maxNorm);
            clipParamGrads(block.attn.kProj.gradWeight, maxNorm);
            clipParamGrads(block.attn.vProj.gradWeight, maxNorm);
            clipParamGrads(block.attn.oProj.gradWeight, maxNorm);
            clipParamGrads(block.ff.w1.gradWeight, maxNorm);
            clipParamGrads(block.ff.w2.gradWeight, maxNorm);
        }
    }

    private void clipParamGrads(float[] grads, float maxNorm) {
        float norm = 0;
        for (float g : grads) norm += g * g;
        norm = (float) Math.sqrt(norm);
        if (norm > maxNorm) {
            float scale = maxNorm / norm;
            for (int i = 0; i < grads.length; i++) grads[i] *= scale;
        }
    }

    public void saveCheckpoint(String path) throws IOException {
        Files.createDirectories(Path.of(path).getParent());
        model.save(path);
        System.out.println("  Saved checkpoint: " + path);
    }

    public List<Float> getLossHistory() { return lossHistory; }
}
