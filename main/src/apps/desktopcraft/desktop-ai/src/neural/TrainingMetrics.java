package neural;

/** Snapshot of training progress for one epoch (or evaluation pass). */
public final class TrainingMetrics {
    private final int epoch;
    private final double loss;
    private final double accuracy;
    private final long elapsedMillis;

    public TrainingMetrics(int epoch, double loss, double accuracy, long elapsedMillis) {
        this.epoch = epoch;
        this.loss = loss;
        this.accuracy = accuracy;
        this.elapsedMillis = elapsedMillis;
    }

    public int epoch() {
        return epoch;
    }

    public double loss() {
        return loss;
    }

    public double accuracy() {
        return accuracy;
    }

    public long elapsedMillis() {
        return elapsedMillis;
    }

    @Override
    public String toString() {
        return String.format("epoch=%d loss=%.6f acc=%.1f%% (%dms)", epoch, loss, accuracy * 100.0, elapsedMillis);
    }
}
