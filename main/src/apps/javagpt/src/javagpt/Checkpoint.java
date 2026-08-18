package javagpt;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Checkpoint {
    public static void saveFull(GPT model, Tokenizer tokenizer, String modelPath, String tokenizerPath) throws IOException {
        Files.createDirectories(Path.of(modelPath).getParent());
        model.save(modelPath);
        tokenizer.save(tokenizerPath);
        System.out.println("Model saved to: " + modelPath);
        System.out.println("Tokenizer saved to: " + tokenizerPath);
    }

    public static GPT loadModel(String path) throws IOException {
        return GPT.load(path);
    }

    public static Tokenizer loadTokenizer(String path) throws IOException {
        Tokenizer tokenizer = new Tokenizer();
        tokenizer.load(path);
        return tokenizer;
    }

    public static void printModelInfo(String modelPath, String tokenizerPath) throws IOException {
        GPT model = loadModel(modelPath);
        GPTConfig c = model.config;
        File f = new File(modelPath);
        Tokenizer tok = loadTokenizer(tokenizerPath);

        System.out.println("=== Model Info ===");
        System.out.println("  File:          " + modelPath);
        System.out.printf("  File size:     %.2f MB%n", f.length() / 1e6);
        System.out.printf("  Parameters:    %,d (~%.2fM)%n", c.parameterCount(), c.parameterCount() / 1e6);
        System.out.printf("  Config name:   %s%n", c.name);
        System.out.printf("  Vocabulary:    %d%n", tok.getVocabSize());
        System.out.printf("  Embedding:     %d%n", c.embedDim);
        System.out.printf("  Layers:        %d%n", c.numLayers);
        System.out.printf("  Heads:         %d%n", c.numHeads);
        System.out.printf("  Head dim:      %d%n", c.headDim);
        System.out.printf("  FF dim:        %d%n", c.ffDim);
        System.out.printf("  Context:       %d%n", c.contextLength);
        System.out.printf("  Est RAM:       ~%.1f MB%n", c.estimatedRamBytes() / 1e6);
    }
}
