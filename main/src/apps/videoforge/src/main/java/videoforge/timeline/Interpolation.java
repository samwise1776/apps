package videoforge.timeline;

/**
 * Interpolation mode for keyframes.
 */
public enum Interpolation {
    LINEAR("Linear"),
    EASE_IN("Ease In"),
    EASE_OUT("Ease Out"),
    EASE_IN_OUT("Ease In/Out"),
    HOLD("Hold");

    private final String label;

    Interpolation(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Interpolation from(String s) {
        for (Interpolation i : values()) {
            if (i.name().equalsIgnoreCase(s) || i.label.equalsIgnoreCase(s)) {
                return i;
            }
        }
        return LINEAR;
    }

    /**
     * Ease factor between 0 and 1 for a progress value (0..1).
     * For ease-in/out we apply a smoothstep-style curve.
     */
    public double ease(double t) {
        switch (this) {
            case LINEAR:
                return t;
            case EASE_IN:
                return t * t;
            case EASE_OUT:
                return 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT:
                return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
            case HOLD:
                return 0;
            default:
                return t;
        }
    }
}
