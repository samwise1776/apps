package videoforge.effects;

import org.json.JSONObject;

/**
 * Crop effect. Values are normalized 0..1 fractions of the frame edges.
 * A value of 0 crops nothing, 1 crops the whole side.
 */
public final class CropEffect extends Effect {

    private double top;
    private double bottom;
    private double left;
    private double right;

    public CropEffect() {}

    @Override
    public String type() { return "crop"; }

    public double getTop() { return top; }
    public void setTop(double top) { this.top = top; }
    public double getBottom() { return bottom; }
    public void setBottom(double bottom) { this.bottom = bottom; }
    public double getLeft() { return left; }
    public void setLeft(double left) { this.left = left; }
    public double getRight() { return right; }
    public void setRight(double right) { this.right = right; }

    public boolean isNeutral() {
        return top <= 0 && bottom <= 0 && left <= 0 && right <= 0;
    }

    @Override
    public JSONObject toJson() {
        JSONObject o = baseJson();
        o.put("top", top);
        o.put("bottom", bottom);
        o.put("left", left);
        o.put("right", right);
        return o;
    }

    @Override
    public void loadJson(JSONObject o) {
        top = o.optDouble("top", 0);
        bottom = o.optDouble("bottom", 0);
        left = o.optDouble("left", 0);
        right = o.optDouble("right", 0);
    }
}
