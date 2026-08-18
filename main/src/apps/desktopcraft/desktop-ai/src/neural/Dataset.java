package neural;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** An ordered collection of samples, optionally split into train/validation folds. */
public final class Dataset {
    private final List<Sample> samples = new ArrayList<>();
    private final int inputSize;
    private final int outputSize;

    public Dataset(int inputSize, int outputSize) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
    }

    public Dataset(List<Sample> samples) {
        this.samples.addAll(samples);
        this.inputSize = samples.isEmpty() ? 0 : samples.get(0).input().size();
        this.outputSize = samples.isEmpty() ? 0 : samples.get(0).target().size();
    }

    public int inputSize() {
        return inputSize;
    }

    public int outputSize() {
        return outputSize;
    }

    public int size() {
        return samples.size();
    }

    public Sample get(int index) {
        return samples.get(index);
    }

    public void add(Sample sample) {
        if (sample.input().size() != inputSize || sample.target().size() != outputSize) {
            throw new IllegalArgumentException("Sample dimensions do not match dataset");
        }
        samples.add(sample);
    }

    public void shuffle(Random random) {
        Collections.shuffle(samples, random);
    }

    /** Returns a shuffled copy, leaving this dataset unchanged. */
    public List<Sample> shuffled(Random random) {
        List<Sample> copy = new ArrayList<>(samples);
        Collections.shuffle(copy, random);
        return copy;
    }

    public List<Sample> all() {
        return new ArrayList<>(samples);
    }

    public static Dataset xor() {
        Dataset dataset = new Dataset(2, 1);
        dataset.add(new Sample(new Vector(new double[]{0, 0}), new Vector(new double[]{0})));
        dataset.add(new Sample(new Vector(new double[]{0, 1}), new Vector(new double[]{1})));
        dataset.add(new Sample(new Vector(new double[]{1, 0}), new Vector(new double[]{1})));
        dataset.add(new Sample(new Vector(new double[]{1, 1}), new Vector(new double[]{0})));
        return dataset;
    }

    /** A 3x3 grid -> one-hot class dataset for demonstrating training/visualization. */
    public static Dataset digitsDemo(Random random) {
        Dataset dataset = new Dataset(9, 3);
        double[][] patterns = {
            {1, 1, 1, 1, 0, 1, 1, 1, 1}, // 0-like
            {1, 0, 1, 0, 0, 1, 1, 1, 1},
            {1, 1, 1, 0, 0, 1, 0, 1, 0}
        };
        double[][] labels = {
            {1, 0, 0}, {0, 1, 0}, {0, 0, 1}
        };
        for (int example = 0; example < patterns.length; example++) {
            for (int repeat = 0; repeat < 40; repeat++) {
                Vector input = new Vector(9);
                for (int i = 0; i < 9; i++) {
                    input.set(i, patterns[example][i] + (random.nextDouble() * 0.25 - 0.125));
                }
                dataset.add(new Sample(input, new Vector(labels[example])));
            }
        }
        return dataset;
    }
}
