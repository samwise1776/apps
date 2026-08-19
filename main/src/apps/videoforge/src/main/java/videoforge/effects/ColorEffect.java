package videoforge.effects;

import org.json.JSONObject;

/**
 * Color correction effect. Exposes the standard grading controls plus
 * one-click presets (black &amp; white, sepia).
 *
 * <p>Ranges follow editor conventions: brightness/contrast/saturation and the
 * highlight/shadow lift are -100..100; gamma is 0.1..5 (1 = neutral);
 * exposure is in stops (-3..3); temperature/tint are -100..100.</p>
 */
public final class ColorEffect extends Effect {

    public enum Preset { NONE, BLACK_WHITE, SEPIA }

    private double brightness = 0;    // -100..100
    private double contrast = 0;      // -100..100
    private double saturation = 0;    // -100..100
    private double temperature = 0;   // -100..100
    private double tint = 0;          // -100..100
    private double gamma = 1.0;       // 0.1..5
    private double exposure = 0;      // stops -3..3
    private double highlights = 0;    // -100..100
    private double shadows = 0;       // -100..100
    private Preset preset = Preset.NONE;

    public ColorEffect() {}

    @Override
    public String type() { return "color"; }

    public double getBrightness() { return brightness; }
    public void setBrightness(double v) { this.brightness = v; }
    public double getContrast() { return contrast; }
    public void setContrast(double v) { this.contrast = v; }
    public double getSaturation() { return saturation; }
    public void setSaturation(double v) { this.saturation = v; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double v) { this.temperature = v; }
    public double getTint() { return tint; }
    public void setTint(double v) { this.tint = v; }
    public double getGamma() { return gamma; }
    public void setGamma(double v) { this.gamma = v; }
    public double getExposure() { return exposure; }
    public void setExposure(double v) { this.exposure = v; }
    public double getHighlights() { return highlights; }
    public void setHighlights(double v) { this.highlights = v; }
    public double getShadows() { return shadows; }
    public void setShadows(double v) { this.shadows = v; }
    public Preset getPreset() { return preset; }
    public void setPreset(Preset preset) { this.preset = preset; }

    public boolean isNeutral() {
        return preset == Preset.NONE
                && brightness == 0 && contrast == 0 && saturation == 0
                && temperature == 0 && tint == 0 && gamma == 1.0
                && exposure == 0 && highlights == 0 && shadows == 0;
    }

    @Override
    public JSONObject toJson() {
        JSONObject o = baseJson();
        o.put("brightness", brightness);
        o.put("contrast", contrast);
        o.put("saturation", saturation);
        o.put("temperature", temperature);
        o.put("tint", tint);
        o.put("gamma", gamma);
        o.put("exposure", exposure);
        o.put("highlights", highlights);
        o.put("shadows", shadows);
        o.put("preset", preset.name());
        return o;
    }

    @Override
    public void loadJson(JSONObject o) {
        brightness = o.optDouble("brightness", brightness);
        contrast = o.optDouble("contrast", contrast);
        saturation = o.optDouble("saturation", saturation);
        temperature = o.optDouble("temperature", temperature);
        tint = o.optDouble("tint", tint);
        gamma = o.optDouble("gamma", gamma);
        exposure = o.optDouble("exposure", exposure);
        highlights = o.optDouble("highlights", highlights);
        shadows = o.optDouble("shadows", shadows);
        try {
            preset = Preset.valueOf(o.optString("preset", Preset.NONE.name()));
        } catch (IllegalArgumentException e) {
            preset = Preset.NONE;
        }
    }
}
