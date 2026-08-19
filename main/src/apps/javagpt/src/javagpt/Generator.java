package javagpt;

import java.util.*;

public class Generator {
    private final GPT model;
    private final Tokenizer tokenizer;
    private final Random rng;

    public Generator(GPT model, Tokenizer tokenizer) {
        this.model = model;
        this.tokenizer = tokenizer;
        this.rng = new Random();
    }

    public String generate(String prompt, int maxTokens, float temperature, int topK) {
        int[] promptTokens = tokenizer.encodeRaw(prompt);
        int contextLen = model.config.contextLength;

        List<Integer> generated = new ArrayList<>();
        int[] context = Arrays.copyOf(promptTokens, Math.min(promptTokens.length, contextLen));

        for (int t = 0; t < maxTokens; t++) {
            // Pad or truncate to context length
            int[] input = new int[contextLen];
            int start = Math.max(0, context.length - contextLen);
            System.arraycopy(context, start, input, contextLen - (context.length - start), context.length - start);

            // forward pass
            int[][] batch = new int[][]{input};
            Tensor logits = model.forward(batch);

            // get logits for last position
            int lastPos = contextLen - 1;
            int V = model.config.vocabSize;
            float[] lastLogits = new float[V];
            System.arraycopy(logits.data, lastPos * V, lastLogits, 0, V);

            // apply temperature
            lastLogits = MathUtil.applyTemperature(lastLogits, temperature);

            // softmax
            float[] probs = MathUtil.softmax(lastLogits);

            // top-k sampling
            int nextToken = MathUtil.topKSample(probs, topK, rng);

            // stop on EOS
            if (nextToken == Tokenizer.EOS) break;

            generated.add(nextToken);

            // extend context
            int[] newContext = new int[context.length + 1];
            System.arraycopy(context, 0, newContext, 0, context.length);
            newContext[context.length] = nextToken;
            context = newContext;
        }

        int[] result = new int[generated.size()];
        for (int i = 0; i < result.length; i++) result[i] = generated.get(i);
        return tokenizer.decode(result);
    }

    public String generateWithStats(String prompt, int maxTokens, float temperature, int topK) {
        long startTime = System.currentTimeMillis();
        String result = generate(prompt, maxTokens, temperature, topK);
        long elapsed = System.currentTimeMillis() - startTime;
        int tokensGenerated = result.length();
        float tokPerSec = (elapsed > 0) ? (tokensGenerated * 1000f / elapsed) : 0;
        return String.format("%s\n[Generated %d tokens in %.2fs (%.1f tok/s)]",
                result, tokensGenerated, elapsed / 1000.0, tokPerSec);
    }
}
