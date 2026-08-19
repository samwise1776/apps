package videoforge.timeline;

/**
 * Transition types between adjacent clips on the same track.
 * Rendered by the render engine via FFmpeg.
 */
public enum TransitionType {
    NONE("Cut", false),
    CROSSFADE("Crossfade", true),
    FADE_TO_BLACK("Fade to Black", true),
    FADE_TO_WHITE("Fade to White", true),
    SLIDE_LEFT("Slide Left", true),
    SLIDE_RIGHT("Slide Right", true),
    SLIDE_UP("Slide Up", true),
    SLIDE_DOWN("Slide Down", true),
    ZOOM("Zoom", true),
    BLUR("Blur", true),
    WIPE_LEFT("Wipe", true),
    CIRCLE_WIPE("Circle Wipe", true);

    private final String label;
    private final boolean durationEditable;

    TransitionType(String label, boolean durationEditable) {
        this.label = label;
        this.durationEditable = durationEditable;
    }

    public String label() {
        return label;
    }

    public boolean supportsDuration() {
        return durationEditable;
    }

    @Override
    public String toString() {
        return label;
    }

    public static TransitionType from(String s) {
        for (TransitionType t : values()) {
            if (t.name().equalsIgnoreCase(s) || t.label.equalsIgnoreCase(s)) {
                return t;
            }
        }
        return NONE;
    }
}
