package javagpt;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    private static final String MODEL_DIR = "models";
    private static final String MODEL_PATH = MODEL_DIR + "/javagpt.bin";
    private static final String TOK_PATH = MODEL_DIR + "/tokenizer.bin";
    private static final String DATA_PATH = "data/training.txt";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }
        switch (args[0].toLowerCase()) {
            case "train" -> train(args);
            case "generate", "gen" -> generate(args);
            case "chat" -> chat(args);
            case "gui" -> gui();
            case "info" -> info();
            case "test" -> runTests();
            default -> printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("JavaGPT - Transformer Language Model in Pure Java");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -cp build/javagpt javagpt.Main train [config]");
        System.out.println("  java -cp build/javagpt javagpt.Main generate \"prompt\"");
        System.out.println("  java -cp build/javagpt javagpt.Main chat");
        System.out.println("  java -cp build/javagpt javagpt.Main gui");
        System.out.println("  java -cp build/javagpt javagpt.Main info");
        System.out.println("  java -cp build/javagpt javagpt.Main test");
        System.out.println();
        System.out.println("Configs: tiny (default), small, medium");
    }

    private static void train(String[] args) throws Exception {
        GPTConfig config = GPTConfig.tiny();
        if (args.length > 1) {
            switch (args[1].toLowerCase()) {
                case "small" -> config = GPTConfig.small();
                case "medium" -> config = GPTConfig.medium();
                case "tiny" -> config = GPTConfig.tiny();
            }
        }

        // Build tokenizer from training data
        String trainingText = Files.readString(Path.of(DATA_PATH));
        Tokenizer tokenizer = new Tokenizer();
        tokenizer.buildFromText(trainingText);
        System.out.println("Tokenizer: " + tokenizer.vocabInfo());
        System.out.println("Training data: " + trainingText.length() + " chars, " +
                tokenizer.encodeRaw(trainingText).length + " tokens");

        config.vocabSize = tokenizer.getVocabSize();
        config.recalc();

        // Create model
        GPT model = new GPT(config, new Random(42));

        // Train
        Dataset dataset = Dataset.fromFile(DATA_PATH, tokenizer, config.contextLength);
        Trainer trainer = new Trainer(model, config);
        trainer.train(dataset);

        // Save
        Files.createDirectories(Paths.get(MODEL_DIR));
        Checkpoint.saveFull(model, tokenizer, MODEL_PATH, TOK_PATH);
        System.out.println("\nDone! Model trained and saved.");
    }

    private static void generate(String[] args) throws Exception {
        GPT model = Checkpoint.loadModel(MODEL_PATH);
        Tokenizer tokenizer = Checkpoint.loadTokenizer(TOK_PATH);
        Generator gen = new Generator(model, tokenizer);

        String prompt = args.length > 1 ? args[1] : "Hello";
        System.out.println("Prompt: " + prompt);
        System.out.println();
        System.out.println(gen.generateWithStats(prompt, 200, 0.8f, 40));
    }

    private static void chat(String[] args) throws Exception {
        GPT model = Checkpoint.loadModel(MODEL_PATH);
        Tokenizer tokenizer = Checkpoint.loadTokenizer(TOK_PATH);
        Generator gen = new Generator(model, tokenizer);

        System.out.println("=== JavaGPT Chat ===");
        System.out.println("Type 'exit' to quit, 'temp <value>' to set temperature");
        System.out.println();

        float temperature = 0.8f;
        int topK = 40;
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("You: ");
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;
            if (line.startsWith("temp ")) {
                try {
                    temperature = Float.parseFloat(line.substring(5).trim());
                    System.out.println("Temperature set to " + temperature);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid temperature");
                }
                continue;
            }
            if (line.isEmpty()) continue;

            System.out.print("AI: ");
            System.out.println(gen.generate(line, 200, temperature, topK));
            System.out.println();
        }
    }

    private static void gui() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            GUI app = new GUI();
            app.setVisible(true);
        });
    }

    private static void info() throws Exception {
        File modelFile = new File(MODEL_PATH);
        if (!modelFile.exists()) {
            System.out.println("No model found at " + MODEL_PATH);
            System.out.println("Train first with: java -cp build/javagpt javagpt.Main train");
            return;
        }
        Checkpoint.printModelInfo(MODEL_PATH, TOK_PATH);
    }

    private static void runTests() {
        System.out.println("=== JavaGPT Mathematical Tests ===\n");
        int passed = 0, failed = 0;

        // Test 1: Softmax sums to 1
        {
            float[] logits = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
            float[] probs = MathUtil.softmax(logits);
            float sum = 0;
            for (float p : probs) sum += p;
            boolean ok = Math.abs(sum - 1.0f) < 1e-4f;
            System.out.println("  [Softmax] sums to 1: " + (ok ? "PASS" : "FAIL (" + sum + ")"));
            if (ok) passed++; else failed++;
        }

        // Test 2: Softmax is monotonic
        {
            float[] a = {1, 2, 3};
            float[] b = {1, 2, 4};
            float[] pa = MathUtil.softmax(a);
            float[] pb = MathUtil.softmax(b);
            boolean ok = pb[2] > pa[2] && pb[0] < pa[0];
            System.out.println("  [Softmax] monotonic: " + (ok ? "PASS" : "FAIL"));
            if (ok) passed++; else failed++;
        }

        // Test 3: Matmul dimensions
        {
            Tensor A = Tensor.ones(2, 3);
            Tensor B = Tensor.ones(3, 4);
            Tensor C = Tensor.matmul(A, B);
            boolean ok = C.shape[0] == 2 && C.shape[1] == 4 && Math.abs(C.data[0] - 3.0f) < 1e-5f;
            System.out.println("  [Matmul] 2x3 @ 3x4 = 2x4: " + (ok ? "PASS" : "FAIL"));
            if (ok) passed++; else failed++;
        }

        // Test 4: Batched matmul
        {
            Tensor A = Tensor.ones(2, 3, 4);
            Tensor B = Tensor.ones(2, 4, 5);
            Tensor C = Tensor.matmul(A, B);
            boolean ok = C.shape[0] == 2 && C.shape[1] == 3 && C.shape[2] == 5;
            System.out.println("  [BatchMatmul] (2,3,4)@(2,4,5)=(2,3,5): " + (ok ? "PASS" : "FAIL"));
            if (ok) passed++; else failed++;
        }

        // Test 5: Transpose
        {
            Tensor A = new Tensor(2, 3);
            A.data = new float[]{1, 2, 3, 4, 5, 6};
            Tensor At = A.transpose(0, 1);
            boolean ok = At.shape[0] == 3 && At.shape[1] == 2 &&
                    At.data[0] == 1 && At.data[1] == 4 && At.data[2] == 2 && At.data[3] == 5;
            System.out.println("  [Transpose] (2,3)->(3,2): " + (ok ? "PASS" : "FAIL"));
            if (ok) passed++; else failed++;
        }

        // Test 6: LayerNorm
        {
            Random rng = new Random(0);
            LayerNorm ln = new LayerNorm(4, 1e-5f);
            Tensor x = Tensor.randn(rng, 1, 4);
            Tensor y = ln.forward(x);
            float mean = 0;
            for (int i = 0; i < 4; i++) mean += y.data[i];
            mean /= 4;
            boolean ok = Math.abs(mean) < 0.01f; // mean should be ~0
            System.out.println("  [LayerNorm] mean ~0: " + (ok ? "PASS" : "FAIL (" + mean + ")"));
            if (ok) passed++; else failed++;
        }

        // Test 7: GELU
        {
            Tensor x = new Tensor(4);
            x.data = new float[]{-1f, 0f, 1f, 2f};
            Tensor y = Tensor.gelu(x);
            boolean ok = Math.abs(y.data[1]) < 1e-5f && y.data[2] > 0 && y.data[3] > y.data[2];
            System.out.println("  [GELU] properties: " + (ok ? "PASS" : "FAIL"));
            if (ok) passed++; else failed++;
        }

        // Test 8: Tokenizer encode/decode
        {
            Tokenizer tok = new Tokenizer();
            tok.buildFromText("Hello world");
            int[] encoded = tok.encode("Hello");
            String decoded = tok.decode(encoded);
            boolean ok = decoded.contains("Hello");
            System.out.println("  [Tokenizer] encode/decode: " + (ok ? "PASS" : "FAIL (" + decoded + ")"));
            if (ok) passed++; else failed++;
        }

        // Test 9: Gradients are finite
        {
            Random rng = new Random(1);
            GPTConfig c = GPTConfig.tiny();
            c.vocabSize = 10;
            c.recalc();
            GPT model = new GPT(c, rng);
            int[][] tokens = new int[][]{{1, 2, 3}};
            Tensor logits = model.forward(tokens);
            int[][] targets = new int[][]{{2, 3, 4}};
            Tensor gradLogits = new Tensor(logits.shape.clone());
            model.computeLoss(logits, targets, gradLogits);
            model.backward(gradLogits);
            // Check no NaN and non-zero
            boolean hasNaN = false;
            boolean hasNonZero = false;
            for (float g : model.tokenEmb.gradWeight) {
                if (Float.isNaN(g)) hasNaN = true;
                if (g != 0) hasNonZero = true;
            }
            for (float g : model.lmHead.gradWeight) {
                if (Float.isNaN(g)) hasNaN = true;
                if (g != 0) hasNonZero = true;
            }
            boolean ok = !hasNaN && hasNonZero;
            System.out.println("  [Backprop] gradients finite and non-zero: " + (ok ? "PASS" : "FAIL"));
            if (!hasNaN) passed++; else failed++;
        }

        // Test 10: Loss decreases on repetitive data
        {
            Random rng = new Random(0);
            GPTConfig c = GPTConfig.tiny();
            c.vocabSize = 10;
            c.embedDim = 32;
            c.numLayers = 1;
            c.numHeads = 2;
            c.contextLength = 8;
            c.batchSize = 4;
            c.recalc();
            GPT model = new GPT(c, rng);
            AdamW opt = new AdamW(1e-3f, 0.9f, 0.999f, 1e-8f, 0.01f);
            opt.registerAllWithGrads(model);

            // Create repetitive pattern data
            int[] pattern = {1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3};
            Dataset dataset = new Dataset(pattern, c.contextLength);

            float firstLoss = 0;
            for (int step = 0; step < 50; step++) {
                dataset.reset();
                int[][][] batch = dataset.getNextBatch(c.batchSize);
                Tensor logits = model.forward(batch[0]);
                Tensor gradLogits = new Tensor(logits.shape.clone());
                float loss = model.computeLoss(logits, batch[1], gradLogits);
                if (step == 0) firstLoss = loss;
                model.backward(gradLogits);
                opt.stepWithGrads();
                model.zeroAllGrads();
            }

            dataset.reset();
            int[][][] batch = dataset.getNextBatch(c.batchSize);
            Tensor logits = model.forward(batch[0]);
            Tensor gradLogits = new Tensor(logits.shape.clone());
            float finalLoss = model.computeLoss(logits, batch[1], gradLogits);
            boolean ok = finalLoss < firstLoss;
            System.out.printf("  [Training] loss decreased: %.4f -> %.4f  %s%n",
                    firstLoss, finalLoss, ok ? "PASS" : "FAIL");
            if (ok) passed++; else failed++;
        }

        // Test 11: Model save/load
        {
            try {
                Random rng = new Random(0);
                GPTConfig c = GPTConfig.tiny();
                c.vocabSize = 20;
                c.recalc();
                GPT model1 = new GPT(c, rng);
                int[][] tokens = new int[][]{{1, 2, 3, 4}};
                Tensor out1 = model1.forward(tokens);

                Files.createDirectories(Paths.get("models"));
                model1.save("models/test_model.bin");
                GPT model2 = GPT.load("models/test_model.bin");
                Tensor out2 = model2.forward(tokens);

                boolean ok = true;
                float maxDiff = 0;
                for (int i = 0; i < out1.size; i++) {
                    float diff = Math.abs(out1.data[i] - out2.data[i]);
                    if (diff > maxDiff) maxDiff = diff;
                    if (diff > 1e-4f) { ok = false; break; }
                }
                System.out.println("  [SaveLoad] identical output (maxDiff=" + maxDiff + "): " + (ok ? "PASS" : "FAIL"));
                if (ok) passed++; else failed++;
                new File("models/test_model.bin").delete();
            } catch (Exception e) {
                System.out.println("  [SaveLoad] FAIL: " + e.getMessage());
                failed++;
            }
        }

        // Test 12: Causal mask
        {
            Random rng = new Random(0);
            GPTConfig c = GPTConfig.tiny();
            c.vocabSize = 20;
            c.recalc();
            GPT model = new GPT(c, rng);
            int[] prompt = {1, 2, 3, 4, 5};
            int[] full = {1, 2, 3, 4, 5};
            int[] partial = {1, 2, 3, 4, 0};

            // Full context
            Tensor logitsFull = model.forward(new int[][]{prompt});
            // Partial (last token different)
            Tensor logitsPartial = model.forward(new int[][]{partial});

            // Due to causal mask, first 4 positions should produce same logits
            boolean ok = true;
            int V = c.vocabSize;
            for (int s = 0; s < 4; s++) {
                for (int v = 0; v < V; v++) {
                    if (Math.abs(logitsFull.data[s * V + v] - logitsPartial.data[s * V + v]) > 1e-4f) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) break;
            }
            System.out.println("  [CausalMask] future tokens hidden: " + (ok ? "PASS" : "FAIL"));
            if (ok) passed++; else failed++;
        }

        System.out.printf("%nResults: %d passed, %d failed%n", passed, failed);
    }
}
