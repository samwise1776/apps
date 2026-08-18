package videoforge.effects;

import org.json.JSONObject;

/**
 * Gaussian blur effect. Two modes:
 * <ul>
 *   <li>WHOLE  - blurs the entire frame.</li>
 *   <li>REGION - blurs a rectangular area (region expressed in normalized 0..1
 *       coordinates relative to the output canvas).</li>
 * </ul>
 *
 * <p>The region structure is shared with face-blur/moving-blur support: a
 * {@code RegionBlur} can carry a keyframe list in the future, and the render
 * engine already evaluates region coordinates per-frame so a tracked box can
 * later be driven by keyframes without a renderer change.</p>
 */
public final class BlurEffect extends Effect {

    public enum Mode { WHOLE, REGION }

    private Mode mode = Mode.WHOLE;
    private double strength = 10.0;          // 0..100 sigma scale
    private double regionX = 0.3;            // 0..1
    private double regionY = 0.2;            // 0..1
    private double regionW = 0.4;            // 0..1
    private double regionH = 0.3;            // 0..1
    private double feather = 0.1;            // 0..1
    private boolean pixelate;                // mosaic-style pixelation fallback

    public BlurEffect() {}

    @Override
    public String type() { return "blur"; }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }

    public double getStrength() { return strength; }
    public void setStrength(double strength) { this.strength = strength; }

    public double getRegionX() { return regionX; }
    public void setRegionX(double x) { this.regionX = x; }

    public double getRegionY() { return regionY; }
    public void setRegionY(double y) { this.regionY = y; }

    public double getRegionW() { return regionW; }
    public void setRegionW(double w) { this.regionW = w; }

    public double getRegionH() { return regionH; }
    public void setRegionH(double h) { this.regionH = h; }

    public double getFeather() { return feather; }
    public void setFeather(double feather) { this.feather = feather; }

    public boolean isPixelate() { return pixelate; }
    public void setPixelate(boolean pixelate) { this.pixelate = pixelate; }

    @Override
    public JSONObject toJson() {
        JSONObject o = baseJson();
        o.put("mode", mode.name());
        o.put("strength", strength);
        o.put("regionX", regionX);
        o.put("regionY", regionY);
        o.put("regionW", regionW);
        o.put("regionH", regionH);
        o.put("feather", feather);
        o.put("pixelate", pixelate);
        return o;
    }

    @Override
    public void loadJson(JSONObject o) {
        try {
            mode = Mode.valueOf(o.optString("mode", Mode.WHOLE.name()));
        } catch (IllegalArgumentException e) {
            mode = Mode.WHOLE;
        }
        strength = o.optDouble("strength", strength);
        regionX = o.optDouble("regionX", regionX);
        regionY = o.optDouble("regionY", regionY);
        regionW = o.optDouble("regionW", regionW);
        regionH = o.optDouble("regionH", regionH);
        feather = o.optDouble("feather", feather);
        pixelate = o.optBoolean("pixelate", pixelate);
    }
}
