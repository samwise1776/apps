package videoforge.effects;

import org.json.JSONObject;

/**
 * Chroma key (green screen) effect. Key color is stored as an 0xRRGGBB int;
 * tolerance/softness/spill are 0..1.
 */
public final class ChromaKeyEffect extends Effect {

    private int keyColor = 0x00FF00;   // default green
    private double tolerance = 0.2;
    private double softness = 0.15;
    private double spill = 0.2;

    public ChromaKeyEffect() {}

    @Override
    public String type() { return "chroma"; }

    public int getKeyColor() { return keyColor; }
    public void setKeyColor(int keyColor) { this.keyColor = keyColor; }

    public double getTolerance() { return tolerance; }
    public void setTolerance(double tolerance) { this.tolerance = tolerance; }

    public double getSoftness() { return softness; }
    public void setSoftness(double softness) { this.softness = softness; }

    public double getSpill() { return spill; }
    public void setSpill(double spill) { this.spill = spill; }

    @Override
    public JSONObject toJson() {
        JSONObject o = baseJson();
        o.put("keyColor", keyColor);
        o.put("tolerance", tolerance);
        o.put("softness", softness);
        o.put("spill", spill);
        return o;
    }

    @Override
    public void loadJson(JSONObject o) {
        keyColor = o.optInt("keyColor", keyColor);
        tolerance = o.optDouble("tolerance", tolerance);
        softness = o.optDouble("softness", softness);
        spill = o.optDouble("spill", spill);
    }
}
