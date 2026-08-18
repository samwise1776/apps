package videoforge.effects;

import org.json.JSONObject;

/**
 * Project background configuration. Lives on the {@link videoforge.project.VideoProject}
 * and is applied below all video tracks during rendering and preview.
 *
 * <p>When the active source aspect ratio does not match the project canvas,
 * the {@code fill mode} determines what happens:</p>
 * <ul>
 *   <li>BLANK     - just the background, video letterboxed to fit.</li>
 *   <li>BLUR      - a blurred, scaled copy of the source fills the frame.</li>
 * </ul>
 */
public final class BackgroundEffect {

    public enum Type { SOLID, GRADIENT, IMAGE, VIDEO, TRANSPARENT }

    public enum FillMode { NONE, FIT, FILL, STRETCH, BLUR_BEHIND }

    private Type type = Type.SOLID;
    private int solidColor = 0x000000;
    private int gradientColorA = 0x222222;
    private int gradientColorB = 0x000000;
    private boolean gradientVertical = true;
    private String imagePath;
    private String videoPath;
    private FillMode fillMode = FillMode.BLUR_BEHIND;
    private double blurStrength = 40;

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public int getSolidColor() { return solidColor; }
    public void setSolidColor(int c) { this.solidColor = c; }
    public int getGradientColorA() { return gradientColorA; }
    public void setGradientColorA(int c) { this.gradientColorA = c; }
    public int getGradientColorB() { return gradientColorB; }
    public void setGradientColorB(int c) { this.gradientColorB = c; }
    public boolean isGradientVertical() { return gradientVertical; }
    public void setGradientVertical(boolean v) { this.gradientVertical = v; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String p) { this.imagePath = p; }
    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String p) { this.videoPath = p; }
    public FillMode getFillMode() { return fillMode; }
    public void setFillMode(FillMode m) { this.fillMode = m; }
    public double getBlurStrength() { return blurStrength; }
    public void setBlurStrength(double v) { this.blurStrength = v; }

    public boolean isDefault() {
        return type == Type.SOLID && solidColor == 0x000000 && fillMode == FillMode.BLUR_BEHIND;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("type", type.name());
        o.put("solidColor", solidColor);
        o.put("gradientColorA", gradientColorA);
        o.put("gradientColorB", gradientColorB);
        o.put("gradientVertical", gradientVertical);
        o.put("imagePath", imagePath == null ? "" : imagePath);
        o.put("videoPath", videoPath == null ? "" : videoPath);
        o.put("fillMode", fillMode.name());
        o.put("blurStrength", blurStrength);
        return o;
    }

    public void load(JSONObject o) {
        try {
            type = Type.valueOf(o.optString("type", Type.SOLID.name()));
        } catch (IllegalArgumentException e) {
            type = Type.SOLID;
        }
        solidColor = o.optInt("solidColor", solidColor);
        gradientColorA = o.optInt("gradientColorA", gradientColorA);
        gradientColorB = o.optInt("gradientColorB", gradientColorB);
        gradientVertical = o.optBoolean("gradientVertical", gradientVertical);
        imagePath = o.optString("imagePath", "");
        videoPath = o.optString("videoPath", "");
        try {
            fillMode = FillMode.valueOf(o.optString("fillMode", FillMode.BLUR_BEHIND.name()));
        } catch (IllegalArgumentException e) {
            fillMode = FillMode.BLUR_BEHIND;
        }
        blurStrength = o.optDouble("blurStrength", blurStrength);
    }

    public BackgroundEffect copy() {
        BackgroundEffect b = new BackgroundEffect();
        b.load(toJson());
        return b;
    }
}
