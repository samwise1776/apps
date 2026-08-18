package javagpt;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Dataset {
    private final int[] allTokens;
    private final int contextLength;
    private int pos = 0;

    public Dataset(int[] tokens, int contextLength) {
        this.allTokens = tokens;
        this.contextLength = contextLength;
    }

    public static Dataset fromFile(String path, Tokenizer tokenizer, int contextLength) throws IOException {
        String text = Files.readString(Path.of(path));
        int[] tokens = tokenizer.encodeRaw(text);
        return new Dataset(tokens, contextLength);
    }

    /**
     * Returns [0]=inputs (batchSize x contextLength), [1]=targets (batchSize x contextLength)
     */
    public int[][][] getNextBatch(int batchSize) {
        int[][] inputs = new int[batchSize][];
        int[][] targets = new int[batchSize][];

        for (int b = 0; b < batchSize; b++) {
            if (pos + contextLength + 1 > allTokens.length) {
                pos = 0;
            }
            inputs[b] = new int[contextLength];
            targets[b] = new int[contextLength];
            System.arraycopy(allTokens, pos, inputs[b], 0, contextLength);
            System.arraycopy(allTokens, pos + 1, targets[b], 0, contextLength);
            pos += contextLength;
        }
        return new int[][][]{inputs, targets};
    }

    public boolean hasMore() {
        return pos + contextLength + 1 < allTokens.length;
    }

    public void reset() { pos = 0; }

    public int size() { return allTokens.length; }
}
