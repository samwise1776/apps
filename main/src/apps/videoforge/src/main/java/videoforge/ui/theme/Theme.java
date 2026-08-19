package videoforge.ui.theme;

import videoforge.config.AppConfig;

/**
 * Theme management: dark/light themes with an accent color. Returns CSS that is
 * applied to the root scene. The accent is injected so users can pick one in
 * Settings and the whole app re-themes.
 */
public final class Theme {

    private Theme() {}

    public static String css() {
        AppConfig cfg = AppConfig.get();
        String accent = cfg.getString("accent");
        if (accent == null || accent.isBlank()) {
            accent = "#0af";
        }
        String theme = cfg.getString("theme");
        boolean dark = !"light".equals(theme);

        String bg = dark ? "#1b1f27" : "#f0f1f4";
        String panel = dark ? "#232834" : "#ffffff";
        String border = dark ? "#333a49" : "#d0d4dd";
        String text = dark ? "#d8dee9" : "#1f2733";
        String muted = dark ? "#8b93a3" : "#5a6474";
        String trackHeader = dark ? "#1f2430" : "#e8eaef";
        String clipVideo = dark ? "#2d4a6b" : "#7fa8d4";
        String clipAudio = dark ? "#3a5d3a" : "#8fc48f";
        String clipText = dark ? "#6b4a2d" : "#d9b37f";
        String clipImage = dark ? "#4a3a6b" : "#b39fd4";
        String clipEffect = dark ? "#6b2d3a" : "#d47f93";

        String faint = "rgba(" + rgb(accent).r + "," + rgb(accent).g + "," + rgb(accent).b + ",0.15)";

        return """
                .root {
                    -fx-base: %s;
                    -fx-background: %s;
                    -fx-control-inner-background: %s;
                    -fx-accent: %s;
                    -fx-focus-color: %s;
                    -fx-faint-focus-color: %s;
                    -fx-font-size: 12px;
                }
                .app-panel { -fx-background-color: %s; }
                .app-panel-border { -fx-border-color: %s; -fx-border-width: 0 1 0 0; }
                .app-border-bottom { -fx-border-color: %s; -fx-border-width: 0 0 1 0; }
                .label { -fx-text-fill: %s; }
                .muted { -fx-text-fill: %s; -fx-font-size: 11px; }
                .section-title { -fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 12px; }
                .timeline-canvas { -fx-background-color: %s; }
                .toolbar { -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 0 0 1 0; }
                .statusbar { -fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1 0 0 0; -fx-padding: 2 8 2 8; }
                .clip-video { -fx-fill: %s; }
                .clip-audio { -fx-fill: %s; }
                .clip-text { -fx-fill: %s; }
                .clip-image { -fx-fill: %s; }
                .clip-effect { -fx-fill: %s; }
                .button, .combo-box, .text-field, .spinner, .slider, .check-box, .radio-button, .menu-button {
                    -fx-font-size: 12px;
                }
                .menu-bar { -fx-background-color: %s; }
                .menu-bar .label { -fx-text-fill: %s; }
                .table-view, .list-view { -fx-background-color: %s; }
                """.formatted(
                bg, bg, panel, accent, accent, faint,
                panel, border, border, text, muted, text,
                panel, panel, border, trackHeader, border,
                clipVideo, clipAudio, clipText, clipImage, clipEffect,
                trackHeader, text, panel);
    }

    private record Rgb(int r, int g, int b) {}

    private static Rgb rgb(String hex) {
        String h = hex.replace("#", "");
        if (h.length() == 3) {
            h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
        }
        try {
            int v = (int) Long.parseLong(h, 16);
            return new Rgb((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF);
        } catch (Exception e) {
            return new Rgb(0, 170, 255);
        }
    }
}
