package videoforge.effects;

import org.json.JSONObject;

/**
 * Text style container used by TEXT clips and title templates.
 *
 * <p>All text is rendered by FFmpeg's drawtext filter (or a JavaFX fallback for
 * the on-screen preview). Position and scale animation are handled through the
 * clip's keyframes; static style lives here.</p>
 */
public final class TextEffect extends Effect {

    private String text = "Sample Text";
    private String font = "DejaVu Sans";
    private double fontSize = 48;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private int color = 0xFFFFFF;
    private boolean backgroundEnabled;
    private int backgroundColor = 0x000000;
    private double backgroundOpacity = 0.7;
    private double opacity = 1.0;
    private boolean strokeEnabled;
    private int strokeColor = 0x000000;
    private double strokeWidth = 2;
    private boolean shadowEnabled;
    private int shadowColor = 0x000000;
    private double shadowDistance = 3;
    private double shadowBlur = 5;
    private String align = "center";        // left|center|right
    private double letterSpacing;           // px
    private double lineSpacing;             // px
    private boolean fadeIn;
    private boolean fadeOut;
    private boolean slideIn;
    private boolean slideOut;
    private boolean typewriter;
    private boolean popIn;
    private boolean zoomIn;
    private double slideDistance = 100;
    private double animationDuration = 0.5; // seconds

    public TextEffect() {}

    @Override
    public String type() { return "text"; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text == null ? "" : text; }

    public String getFont() { return font; }
    public void setFont(String font) { this.font = font; }

    public double getFontSize() { return fontSize; }
    public void setFontSize(double fontSize) { this.fontSize = fontSize; }

    public boolean isBold() { return bold; }
    public void setBold(boolean bold) { this.bold = bold; }
    public boolean isItalic() { return italic; }
    public void setItalic(boolean italic) { this.italic = italic; }
    public boolean isUnderline() { return underline; }
    public void setUnderline(boolean underline) { this.underline = underline; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public boolean isBackgroundEnabled() { return backgroundEnabled; }
    public void setBackgroundEnabled(boolean enabled) { this.backgroundEnabled = enabled; }
    public int getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int c) { this.backgroundColor = c; }
    public double getBackgroundOpacity() { return backgroundOpacity; }
    public void setBackgroundOpacity(double v) { this.backgroundOpacity = v; }

    public double getOpacity() { return opacity; }
    public void setOpacity(double opacity) { this.opacity = opacity; }

    public boolean isStrokeEnabled() { return strokeEnabled; }
    public void setStrokeEnabled(boolean enabled) { this.strokeEnabled = enabled; }
    public int getStrokeColor() { return strokeColor; }
    public void setStrokeColor(int c) { this.strokeColor = c; }
    public double getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(double v) { this.strokeWidth = v; }

    public boolean isShadowEnabled() { return shadowEnabled; }
    public void setShadowEnabled(boolean enabled) { this.shadowEnabled = enabled; }
    public int getShadowColor() { return shadowColor; }
    public void setShadowColor(int c) { this.shadowColor = c; }
    public double getShadowDistance() { return shadowDistance; }
    public void setShadowDistance(double v) { this.shadowDistance = v; }
    public double getShadowBlur() { return shadowBlur; }
    public void setShadowBlur(double v) { this.shadowBlur = v; }

    public String getAlign() { return align; }
    public void setAlign(String align) { this.align = align; }

    public double getLetterSpacing() { return letterSpacing; }
    public void setLetterSpacing(double v) { this.letterSpacing = v; }
    public double getLineSpacing() { return lineSpacing; }
    public void setLineSpacing(double v) { this.lineSpacing = v; }

    public boolean isFadeIn() { return fadeIn; }
    public void setFadeIn(boolean fadeIn) { this.fadeIn = fadeIn; }
    public boolean isFadeOut() { return fadeOut; }
    public void setFadeOut(boolean fadeOut) { this.fadeOut = fadeOut; }
    public boolean isSlideIn() { return slideIn; }
    public void setSlideIn(boolean slideIn) { this.slideIn = slideIn; }
    public boolean isSlideOut() { return slideOut; }
    public void setSlideOut(boolean slideOut) { this.slideOut = slideOut; }
    public boolean isTypewriter() { return typewriter; }
    public void setTypewriter(boolean typewriter) { this.typewriter = typewriter; }
    public boolean isPopIn() { return popIn; }
    public void setPopIn(boolean popIn) { this.popIn = popIn; }
    public boolean isZoomIn() { return zoomIn; }
    public void setZoomIn(boolean zoomIn) { this.zoomIn = zoomIn; }

    public double getSlideDistance() { return slideDistance; }
    public void setSlideDistance(double v) { this.slideDistance = v; }
    public double getAnimationDuration() { return animationDuration; }
    public void setAnimationDuration(double v) { this.animationDuration = v; }

    @Override
    public JSONObject toJson() {
        JSONObject o = baseJson();
        o.put("text", text);
        o.put("font", font);
        o.put("fontSize", fontSize);
        o.put("bold", bold);
        o.put("italic", italic);
        o.put("underline", underline);
        o.put("color", color);
        o.put("backgroundEnabled", backgroundEnabled);
        o.put("backgroundColor", backgroundColor);
        o.put("backgroundOpacity", backgroundOpacity);
        o.put("opacity", opacity);
        o.put("strokeEnabled", strokeEnabled);
        o.put("strokeColor", strokeColor);
        o.put("strokeWidth", strokeWidth);
        o.put("shadowEnabled", shadowEnabled);
        o.put("shadowColor", shadowColor);
        o.put("shadowDistance", shadowDistance);
        o.put("shadowBlur", shadowBlur);
        o.put("align", align);
        o.put("letterSpacing", letterSpacing);
        o.put("lineSpacing", lineSpacing);
        o.put("fadeIn", fadeIn);
        o.put("fadeOut", fadeOut);
        o.put("slideIn", slideIn);
        o.put("slideOut", slideOut);
        o.put("typewriter", typewriter);
        o.put("popIn", popIn);
        o.put("zoomIn", zoomIn);
        o.put("slideDistance", slideDistance);
        o.put("animationDuration", animationDuration);
        return o;
    }

    @Override
    public void loadJson(JSONObject o) {
        text = o.optString("text", text);
        font = o.optString("font", font);
        fontSize = o.optDouble("fontSize", fontSize);
        bold = o.optBoolean("bold", bold);
        italic = o.optBoolean("italic", italic);
        underline = o.optBoolean("underline", underline);
        color = o.optInt("color", color);
        backgroundEnabled = o.optBoolean("backgroundEnabled", backgroundEnabled);
        backgroundColor = o.optInt("backgroundColor", backgroundColor);
        backgroundOpacity = o.optDouble("backgroundOpacity", backgroundOpacity);
        opacity = o.optDouble("opacity", opacity);
        strokeEnabled = o.optBoolean("strokeEnabled", strokeEnabled);
        strokeColor = o.optInt("strokeColor", strokeColor);
        strokeWidth = o.optDouble("strokeWidth", strokeWidth);
        shadowEnabled = o.optBoolean("shadowEnabled", shadowEnabled);
        shadowColor = o.optInt("shadowColor", shadowColor);
        shadowDistance = o.optDouble("shadowDistance", shadowDistance);
        shadowBlur = o.optDouble("shadowBlur", shadowBlur);
        align = o.optString("align", align);
        letterSpacing = o.optDouble("letterSpacing", letterSpacing);
        lineSpacing = o.optDouble("lineSpacing", lineSpacing);
        fadeIn = o.optBoolean("fadeIn", fadeIn);
        fadeOut = o.optBoolean("fadeOut", fadeOut);
        slideIn = o.optBoolean("slideIn", slideIn);
        slideOut = o.optBoolean("slideOut", slideOut);
        typewriter = o.optBoolean("typewriter", typewriter);
        popIn = o.optBoolean("popIn", popIn);
        zoomIn = o.optBoolean("zoomIn", zoomIn);
        slideDistance = o.optDouble("slideDistance", slideDistance);
        animationDuration = o.optDouble("animationDuration", animationDuration);
    }
}
