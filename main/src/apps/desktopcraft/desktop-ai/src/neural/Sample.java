package neural;

import java.util.ArrayList;
import java.util.List;

/** A single supervised training example. */
public final class Sample {
    private final Vector input;
    private final Vector target;

    public Sample(Vector input, Vector target) {
        this.input = input;
        this.target = target;
    }

    public Vector input() {
        return input;
    }

    public Vector target() {
        return target;
    }
}
