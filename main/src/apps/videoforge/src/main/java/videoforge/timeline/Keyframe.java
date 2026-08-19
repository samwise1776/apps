package videoforge.timeline;

/**
 * A single keyframe on a clip property.
 *
 * <p>Time is measured relative to the start of the owning clip, in microseconds.</p>
 */
public final class Keyframe {

    private long timeMicros;
    private double value;
    private Interpolation interpolation = Interpolation.LINEAR;

    public Keyframe() {}

    public Keyframe(long timeMicros, double value) {
        this.timeMicros = timeMicros;
        this.value = value;
    }

    public Keyframe(long timeMicros, double value, Interpolation interpolation) {
        this(timeMicros, value);
        this.interpolation = interpolation;
    }

    public long getTimeMicros() { return timeMicros; }
    public void setTimeMicros(long timeMicros) { this.timeMicros = timeMicros; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public Interpolation getInterpolation() { return interpolation; }
    public void setInterpolation(Interpolation interpolation) { this.interpolation = interpolation; }

    public Keyframe copy() {
        return new Keyframe(timeMicros, value, interpolation);
    }
}
