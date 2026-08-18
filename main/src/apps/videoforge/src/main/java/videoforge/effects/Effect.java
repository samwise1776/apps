package videoforge.effects;

import org.json.JSONObject;

/**
 * Base class for clip effects.
 *
 * <p>Effects are data objects: the render engine translates them into FFmpeg
 * filter primitives, and the inspector UI edits them. Each concrete effect
 * serializes to/from JSON so effects survive project saves.</p>
 */
public abstract class Effect {

    private String id = java.util.UUID.randomUUID().toString();
    private String name = getClass().getSimpleName();
    private boolean enabled = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** Human-readable type used in serialization. */
    public abstract String type();

    public abstract JSONObject toJson();

    protected JSONObject baseJson() {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("type", type());
        o.put("name", name);
        o.put("enabled", enabled);
        return o;
    }

    public void loadBase(JSONObject o) {
        id = o.optString("id", id);
        name = o.optString("name", name);
        enabled = o.optBoolean("enabled", true);
    }

    public Effect copy() {
        return fromJson(toJson());
    }

    public static Effect fromJson(JSONObject o) {
        String type = o.optString("type", "");
        Effect e = switch (type) {
            case "blur" -> new BlurEffect();
            case "color" -> new ColorEffect();
            case "crop" -> new CropEffect();
            case "chroma" -> new ChromaKeyEffect();
            default -> null;
        };
        if (e != null) {
            e.loadBase(o);
            e.loadJson(o);
        }
        return e;
    }

    public abstract void loadJson(JSONObject o);
}
