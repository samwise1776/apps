package videoforge.ui;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import videoforge.editing.SnapEngine;
import videoforge.editing.TimelineOperations;
import videoforge.logging.AppLog;
import videoforge.media.MediaFile;
import videoforge.timeline.Marker;
import videoforge.timeline.Timeline;
import videoforge.timeline.TimelineClip;
import videoforge.timeline.Track;
import videoforge.undo.UndoManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Professional multi-track timeline: time ruler, playhead, markers, snapping,
 * drag-to-move, edge trimming, box selection, zoom and scrolling. All drawing
 * is custom canvas rendering driven directly by the timeline model.
 */
public final class TimelineView extends Pane {

    private static final AppLog LOG = AppLog.get("editor");
    private static final double RULER_H = 26;
    private static final double HEADER_W = 160;
    private static final double TRACK_H = 60;
    private static final double EDGE_SNAP_PX = 6;

    private final AppContext ctx;
    private final Canvas canvas = new Canvas();
    private final ScrollBar hScroll = new ScrollBar();
    private final ScrollBar vScroll = new ScrollBar();
    private final SnapEngine snapEngine;

    private double pxPerSecond = 40;
    private double scrollH;
    private double scrollV;

    // interaction state
    private enum Mode { NONE, MOVE, TRIM_START, TRIM_END, BOX, PLAYHEAD_DRAG }
    private Mode mode = Mode.NONE;
    private TimelineClip dragClip;
    private final Set<String> dragClips = new LinkedHashSet<>();
    private TimelineOperations.Edge trimEdge;
    private double pressX, pressY;
    private long pressTime;
    private double startScrollH, startScrollV;
    private long moveStartTime;
    private long trimStartTime;
    private long trimOriginalStart;
    private long trimOriginalEnd;
    private UndoManager.TimelineSnapshotCommand pendingCommand;
    private long snapGuideTime = -1;
    private double boxX0, boxY0;

    public TimelineView(AppContext ctx) {
        this.ctx = ctx;
        this.snapEngine = new SnapEngine(this::timeToPx, ctx.config().getInt("snapThresholdPixels"));

        getChildren().addAll(canvas, hScroll, vScroll);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        hScroll.setOrientation(Orientation.HORIZONTAL);
        vScroll.setOrientation(Orientation.VERTICAL);
        hScroll.setMin(0);
        vScroll.setMin(0);
        hScroll.valueProperty().addListener((o, a, b) -> {
            scrollH = b.doubleValue();
            redraw();
        });
        vScroll.valueProperty().addListener((o, a, b) -> {
            scrollV = b.doubleValue();
            redraw();
        });

        canvas.setOnMousePressed(this::onPress);
        canvas.setOnMouseDragged(this::onDrag);
        canvas.setOnMouseReleased(this::onRelease);
        canvas.setOnMouseMoved(this::onMove);
        canvas.setOnScroll(e -> {
            if (e.isControlDown()) {
                double old = pxPerSecond;
                pxPerSecond = Math.max(4, Math.min(400, pxPerSecond * (e.getDeltaY() > 0 ? 1.2 : 1 / 1.2)));
                // keep the time under the cursor stable
                double cursorTime = pxToTime(e.getX());
                scrollH = cursorTime / 1e6 * pxPerSecond - (e.getX() - HEADER_W);
                updateScrollRange();
                redraw();
                e.consume();
            }
        });
        canvas.setOnContextMenuRequested(this::showContextMenu);
        installDragDrop();

        ctx.project().timeline().addListener(this::onModelChangeDirect);
        widthProperty().addListener(o -> updateScrollRange());
        updateScrollRange();
    }

    private void onModelChange() {
        updateScrollRange();
        redraw();
    }

    private void onModelChangeDirect(Timeline.ChangeType type) {
        updateScrollRange();
        redraw();
    }

    // ---------- geometry ----------

    private double timeToPx(long micros) {
        return HEADER_W + micros / 1_000_000.0 * pxPerSecond;
    }

    private long pxToTime(double x) {
        double secs = (x - HEADER_W + scrollH) / pxPerSecond;
        return Math.round(secs * 1_000_000L);
    }

    private double trackY(Track t) {
        return RULER_H + ctx.project().timeline().tracks().indexOf(t) * TRACK_H - scrollV;
    }

    private double contentWidth() {
        Timeline tl = ctx.project().timeline();
        return HEADER_W + tl.duration() / 1_000_000.0 * pxPerSecond + 200;
    }

    private double contentHeight() {
        return RULER_H + ctx.project().timeline().tracks().size() * TRACK_H + 40;
    }

    private void updateScrollRange() {
        double cw = Math.max(1, contentWidth());
        double ch = Math.max(1, contentHeight());
        double vw = Math.max(0, getWidth());
        double vh = Math.max(0, getHeight());
        hScroll.setMax(Math.max(0, cw - vw));
        vScroll.setMax(Math.max(0, ch - vh));
        hScroll.setVisibleAmount(vw);
        vScroll.setVisibleAmount(vh);
        hScroll.setPrefWidth(Math.max(0, getWidth() - 18));
        vScroll.setPrefHeight(Math.max(0, getHeight() - 18));
        layoutScrollBars();
    }

    private void layoutScrollBars() {
        double w = getWidth();
        double h = getHeight();
        hScroll.setLayoutX(0);
        hScroll.setLayoutY(h - 18);
        vScroll.setLayoutX(w - 18);
        vScroll.setLayoutY(0);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        updateScrollRange();
        redraw();
    }

    // ---------- drawing ----------

    public void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = Math.max(1, canvas.getWidth());
        double h = Math.max(1, canvas.getHeight());
        Timeline tl = ctx.project().timeline();

        g.setFill(Color.rgb(24, 28, 36));
        g.fillRect(0, 0, w, h);

        drawRuler(g, tl, w);
        drawTracks(g, tl, w, h);
        drawRange(g, tl);
        drawPlayhead(g, tl);
        drawSnapGuide(g, tl);
        drawBoxSelection(g);
    }

    private void drawRuler(GraphicsContext g, Timeline tl, double w) {
        // ruler background + header
        g.setFill(Color.rgb(30, 35, 44));
        g.fillRect(0, 0, w, RULER_H);
        g.setFill(Color.rgb(20, 24, 31));
        g.fillRect(0, 0, HEADER_W, RULER_H);
        g.setStroke(Color.rgb(52, 60, 74));
        g.strokeLine(0, RULER_H, w, RULER_H);
        g.strokeLine(HEADER_W, 0, HEADER_W, RULER_H);

        // choose a tick step giving ~80px spacing
        double stepSec = 0.5;
        while (stepSec * pxPerSecond < 40) {
            stepSec *= 2;
        }
        while (stepSec * pxPerSecond > 160) {
            stepSec /= 2;
        }
        double startSec = Math.max(0, (scrollH) / pxPerSecond);
        double endSec = (w + scrollH) / pxPerSecond;
        g.setFont(Font.font("Monospaced", 10));
        for (double s = Math.floor(startSec / stepSec) * stepSec; s <= endSec; s += stepSec) {
            double x = HEADER_W + s * pxPerSecond - scrollH;
            if (x < HEADER_W || x > w) {
                continue;
            }
            g.setStroke(Color.rgb(70, 80, 95));
            g.strokeLine(x, RULER_H - 6, x, RULER_H);
            g.setFill(Color.rgb(180, 190, 205));
            String label = formatRuler(s);
            g.fillText(label, x + 3, RULER_H - 8);
        }

        // markers
        for (Marker m : tl.markers()) {
            double x = timeToPx(m.getTimeMicros()) - scrollH;
            if (x < HEADER_W || x > w) {
                continue;
            }
            Color c = parseColor(m.getColor(), Color.YELLOW);
            g.setFill(c);
            g.fillPolygon(new double[]{x, x + 9, x + 9}, new double[]{RULER_H, RULER_H - 4, RULER_H - 10}, 3);
            g.setFill(c.deriveColor(0, 1, 1, 0.5));
            g.fillRect(x + 1, RULER_H - 1, 1, 4);
        }
    }

    private static String formatRuler(double seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        long total = Math.round(seconds);
        long m = total / 60;
        long s = total % 60;
        return String.format("%d:%02d", m, s);
    }

    private void drawTracks(GraphicsContext g, Timeline tl, double w, double h) {
        List<Track> tracks = tl.tracks();
        for (int i = 0; i < tracks.size(); i++) {
            Track t = tracks.get(i);
            double y = trackY(t);
            if (y + TRACK_H < RULER_H || y > h) {
                continue;
            }
            // row background
            g.setFill(i % 2 == 0 ? Color.rgb(27, 32, 41) : Color.rgb(25, 29, 37));
            g.fillRect(0, y, w, TRACK_H);

            // header
            g.setFill(Color.rgb(32, 37, 46));
            g.fillRect(0, y, HEADER_W, TRACK_H);
            g.setStroke(Color.rgb(52, 60, 74));
            g.strokeLine(HEADER_W, y, HEADER_W, y + TRACK_H);
            g.setFill(trackColor(t.getKind()));
            g.fillRect(0, y, 4, TRACK_H);

            g.setFill(Color.rgb(215, 222, 232));
            g.setFont(Font.font("SansSerif", 11));
            g.fillText(t.getName(), 10, y + 20);
            g.setFill(Color.rgb(150, 158, 172));
            g.setFont(Font.font("SansSerif", 9));
            StringBuilder flags = new StringBuilder();
            if (t.isMuted()) flags.append("M ");
            if (t.isSoloed()) flags.append("S ");
            if (t.isLocked()) flags.append("\uD83D\uDD12 ");
            if (t.isHidden()) flags.append("\uD83D\uDE48 ");
            g.fillText(flags.toString(), 10, y + 38);

            if (t.isHidden()) {
                continue;
            }
            drawClips(g, t, tl);
        }

        // horizontal row separators
        g.setStroke(Color.rgb(45, 52, 63));
        for (Track t : tracks) {
            double y = trackY(t);
            g.strokeLine(0, y + TRACK_H, w, y + TRACK_H);
        }
    }

    private void drawClips(GraphicsContext g, Track track, Timeline tl) {
        double y = trackY(track);
        for (TimelineClip clip : track.clips()) {
            double x = timeToPx(clip.getTimelineStart()) - scrollH;
            double cw = Math.max(2, clip.duration() / 1_000_000.0 * pxPerSecond);
            if (x + cw < HEADER_W || x > getWidth()) {
                continue;
            }
            Color base = clipColor(clip);
            if (!clip.isEnabled()) {
                base = base.deriveColor(0, 0.4, 1.1, 0.35);
            } else if (clip.isHidden()) {
                base = base.deriveColor(0, 0.5, 1.1, 0.4);
            }
            boolean selected = tl.selectedIds().contains(clip.getId());
            g.setFill(base);
            g.fillRoundRect(x, y + 4, cw, TRACK_H - 8, 5, 5);
            if (selected) {
                g.setStroke(parseColor(ctx.config().getString("accent"), Color.CYAN).deriveColor(0, 1, 1, 0.95));
                g.setLineWidth(2);
            } else {
                g.setStroke(Color.rgb(20, 24, 31));
                g.setLineWidth(1);
            }
            g.strokeRoundRect(x, y + 4, cw, TRACK_H - 8, 5, 5);

            // label
            if (cw > 26) {
                g.setFill(Color.rgb(235, 240, 248));
                g.setFont(Font.font("SansSerif", 10));
                String label = clip.getLabel().isEmpty() ? clip.getName() : clip.getLabel();
                String clipped = cw < 90 ? truncate(label, Math.max(3, (int) (cw / 8))) : truncate(label, Math.max(3, (int) (cw / 7)));
                g.fillText(clipped, x + 5, y + 20);
                g.setFill(Color.rgb(200, 210, 225));
                g.setFont(Font.font("SansSerif", 9));
                g.fillText(videoforge.timeline.Timecode.of(clip.getTimelineStart()), x + 5, y + 34);
            }
            if (clip.isMuted() || clip.getVolume() <= 0.01) {
                g.setFill(Color.rgb(255, 120, 120));
                g.fillText("MUTED", x + 5, y + 48);
            }
            if (clip.isLocked()) {
                g.setFill(Color.rgb(235, 235, 235));
                g.fillText("\uD83D\uDD12", x + cw - 18, y + 20);
            }
            if (!clip.allKeyframes().isEmpty() && cw > 50) {
                g.setFill(Color.WHITE);
                for (int kf = 0; kf < 3 && kf < clip.allKeyframes().size(); kf++) {
                    g.fillRect(x + 6 + kf * 7, y + TRACK_H - 12, 4, 4);
                }
            }
            if (clip.getTransitionIn() != videoforge.timeline.TransitionType.NONE) {
                g.setFill(Color.rgb(255, 210, 80));
                g.fillRect(x, y + 4, 6, TRACK_H - 8);
            }
            if (clip.getTransitionOut() != videoforge.timeline.TransitionType.NONE) {
                g.setFill(Color.rgb(80, 210, 255));
                g.fillRect(x + cw - 6, y + 4, 6, TRACK_H - 8);
            }
        }
    }

    private void drawRange(GraphicsContext g, Timeline tl) {
        if (tl.inPoint() >= 0 && tl.outPoint() > tl.inPoint()) {
            double x0 = timeToPx(tl.inPoint()) - scrollH;
            double x1 = timeToPx(tl.outPoint()) - scrollH;
            g.setFill(Color.rgb(0, 170, 255, 0.12));
            g.fillRect(x0, RULER_H, x1 - x0, getHeight() - RULER_H);
            g.setStroke(Color.rgb(0, 170, 255, 0.7));
            g.setLineWidth(1);
            g.strokeLine(x0, RULER_H, x0, getHeight());
            g.strokeLine(x1, RULER_H, x1, getHeight());
        }
    }

    private void drawPlayhead(GraphicsContext g, Timeline tl) {
        double x = timeToPx(tl.playhead()) - scrollH;
        g.setStroke(Color.rgb(255, 70, 70));
        g.setLineWidth(2);
        g.strokeLine(x, RULER_H, x, getHeight());
        g.setFill(Color.rgb(255, 70, 70));
        g.fillPolygon(new double[]{x - 5, x + 5, x}, new double[]{RULER_H, RULER_H, RULER_H - 8}, 3);
    }

    private void drawSnapGuide(GraphicsContext g, Timeline tl) {
        if (snapGuideTime >= 0) {
            double x = timeToPx(snapGuideTime) - scrollH;
            g.setStroke(Color.rgb(255, 200, 60, 0.85));
            g.setLineDashes(4, 4);
            g.strokeLine(x, RULER_H, x, getHeight());
            g.setLineDashes(null);
        }
    }

    private void drawBoxSelection(GraphicsContext g) {
        if (mode == Mode.BOX) {
            double x = Math.min(pressX, boxX0);
            double y = Math.min(pressY, boxY0);
            double w = Math.abs(boxX0 - pressX);
            double h = Math.abs(boxY0 - pressY);
            g.setFill(Color.rgb(0, 170, 255, 0.12));
            g.fillRect(x, y, w, h);
            g.setStroke(Color.rgb(0, 170, 255, 0.8));
            g.strokeRect(x, y, w, h);
        }
    }

    private Color trackColor(Track.Kind kind) {
        return switch (kind) {
            case VIDEO -> Color.rgb(0, 170, 255);
            case AUDIO -> Color.rgb(0, 200, 130);
            case TEXT -> Color.rgb(255, 170, 60);
            case IMAGE -> Color.rgb(170, 110, 255);
            case EFFECT -> Color.rgb(255, 80, 120);
        };
    }

    private Color clipColor(TimelineClip clip) {
        if (clip.getColor() != null && clip.getColor().startsWith("#")) {
            return parseColor(clip.getColor(), Color.rgb(60, 90, 120));
        }
        return switch (clip.getKind()) {
            case VIDEO -> Color.rgb(52, 86, 120);
            case AUDIO -> Color.rgb(60, 115, 80);
            case TEXT -> Color.rgb(130, 92, 52);
            case IMAGE -> Color.rgb(100, 76, 140);
            case SHAPE -> Color.rgb(130, 66, 88);
        };
    }

    private static Color parseColor(String hex, Color fallback) {
        try {
            String s = hex.startsWith("#") ? hex.substring(1) : hex;
            if (s.length() == 6) {
                return Color.rgb(Integer.parseInt(s.substring(0, 2), 16),
                        Integer.parseInt(s.substring(2, 4), 16),
                        Integer.parseInt(s.substring(4, 6), 16));
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, Math.max(1, max - 1)) + "\u2026";
    }

    // ---------- mouse ----------

    private TimelineClip clipAt(double x, double y) {
        Timeline tl = ctx.project().timeline();
        for (Track t : tl.tracks()) {
            if (t.isHidden()) {
                continue;
            }
            double ty = trackY(t);
            if (y < ty || y > ty + TRACK_H) {
                continue;
            }
            for (TimelineClip c : t.clips()) {
                double cx = timeToPx(c.getTimelineStart()) - scrollH;
                double cw = Math.max(2, c.duration() / 1_000_000.0 * pxPerSecond);
                if (x >= cx && x <= cx + cw) {
                    return c;
                }
            }
        }
        return null;
    }

    private Track trackAt(double y) {
        Timeline tl = ctx.project().timeline();
        for (Track t : tl.tracks()) {
            double ty = trackY(t);
            if (y >= ty && y < ty + TRACK_H) {
                return t;
            }
        }
        return null;
    }

    private void onPress(javafx.scene.input.MouseEvent e) {
        Timeline tl = ctx.project().timeline();
        canvas.requestFocus();
        pressX = e.getX();
        pressY = e.getY();
        pressTime = pxToTime(e.getX());
        if (e.getButton() == MouseButton.SECONDARY) {
            return;
        }

        // playhead drag on ruler
        if (e.getY() < RULER_H && e.getX() > HEADER_W) {
            mode = Mode.PLAYHEAD_DRAG;
            setPlayheadAt(e);
            return;
        }

        TimelineClip clip = clipAt(e.getX(), e.getY());
        if (clip != null) {
            double cx = timeToPx(clip.getTimelineStart()) - scrollH;
            double cw = Math.max(2, clip.duration() / 1_000_000.0 * pxPerSecond);
            boolean nearStart = Math.abs(e.getX() - cx) < EDGE_SNAP_PX;
            boolean nearEnd = Math.abs(e.getX() - (cx + cw)) < EDGE_SNAP_PX;
            if (!e.isShiftDown()) {
                if (!tl.selectedIds().contains(clip.getId())) {
                    tl.select(clip.getId());
                }
            } else {
                if (tl.selectedIds().contains(clip.getId())) {
                    tl.selectedIds().remove(clip.getId());
                    tl.fire(Timeline.ChangeType.SELECTION);
                } else {
                    tl.addToSelection(clip.getId());
                }
            }
            dragClips.clear();
            dragClips.addAll(tl.selectedIds());
            // ignore locked clips for drags
            dragClips.removeIf(id -> {
                TimelineClip c = tl.clipById(id);
                return c != null && c.isLocked();
            });
            if (dragClips.isEmpty()) {
                return;
            }
            if (nearStart && dragClips.size() == 1) {
                mode = Mode.TRIM_START;
                trimEdge = TimelineOperations.Edge.START;
            } else if (nearEnd && dragClips.size() == 1) {
                mode = Mode.TRIM_END;
                trimEdge = TimelineOperations.Edge.END;
            } else {
                mode = Mode.MOVE;
            }
            dragClip = clip;
            moveStartTime = clip.getTimelineStart();
            trimOriginalStart = clip.getTimelineStart();
            trimOriginalEnd = clip.timelineEnd();
            pendingCommand = new UndoManager.TimelineSnapshotCommand(tl, dragLabel());
        } else {
            mode = Mode.BOX;
            boxX0 = e.getX();
            boxY0 = e.getY();
            if (!e.isShiftDown()) {
                tl.clearSelection();
            }
        }
    }

    private String dragLabel() {
        return switch (mode) {
            case MOVE -> "Move Clip";
            case TRIM_START, TRIM_END -> "Trim Clip";
            default -> "Edit";
        };
    }

    private void onDrag(javafx.scene.input.MouseEvent e) {
        Timeline tl = ctx.project().timeline();
        switch (mode) {
            case PLAYHEAD_DRAG -> {
                setPlayheadAt(e);
            }
            case MOVE -> {
                long delta = pxToTime(e.getX()) - pressTime;
                long target = moveStartTime + delta;
                boolean snap = ctx.config().getBool("snapEnabled");
                if (snap) {
                    List<TimelineClip> excluded = new ArrayList<>();
                    for (String id : dragClips) {
                        TimelineClip c = tl.clipById(id);
                        if (c != null) {
                            excluded.add(c);
                        }
                    }
                    long snapped = snapEngine.snap(tl, target, excluded);
                    snapGuideTime = snapped != target ? snapped : -1;
                    target = snapped;
                } else {
                    snapGuideTime = -1;
                }
                long finalTarget = Math.max(0, target);
                for (String id : dragClips) {
                    TimelineClip c = tl.clipById(id);
                    if (c != null) {
                        c.setTimelineStart(finalTarget + (c.getTimelineStart() - moveStartTime));
                    }
                }
                tl.fire(Timeline.ChangeType.STRUCTURE);
            }
            case TRIM_START -> {
                long newTime = pxToTime(e.getX());
                boolean snap = ctx.config().getBool("snapEnabled");
                if (snap) {
                    long snapped = snapEngine.snap(tl, newTime, List.of(dragClip));
                    snapGuideTime = snapped != newTime ? snapped : -1;
                    newTime = snapped;
                }
                long sourceDuration = ctx.operations().sourceDurationMicros(dragClip);
                ctx.operations().trimClip(tl, dragClip, TimelineOperations.Edge.START, newTime, sourceDuration);
            }
            case TRIM_END -> {
                long newTime = pxToTime(e.getX());
                boolean snap = ctx.config().getBool("snapEnabled");
                if (snap) {
                    long snapped = snapEngine.snap(tl, newTime, List.of(dragClip));
                    snapGuideTime = snapped != newTime ? snapped : -1;
                    newTime = snapped;
                }
                long sourceDuration = ctx.operations().sourceDurationMicros(dragClip);
                ctx.operations().trimClip(tl, dragClip, TimelineOperations.Edge.END, newTime, sourceDuration);
            }
            case BOX -> {
                boxX0 = e.getX();
                boxY0 = e.getY();
                Timeline tl0 = ctx.project().timeline();
                double x0 = Math.min(pressX, boxX0);
                double x1 = Math.max(pressX, boxX0);
                double y0 = Math.min(pressY, boxY0);
                double y1 = Math.max(pressY, boxY0);
                List<String> hit = new ArrayList<>();
                for (Track t : tl0.tracks()) {
                    double ty = trackY(t);
                    if (ty > y1 || ty + TRACK_H < y0) {
                        continue;
                    }
                    for (TimelineClip c : t.clips()) {
                        double cx = timeToPx(c.getTimelineStart()) - scrollH;
                        double cw = Math.max(2, c.duration() / 1_000_000.0 * pxPerSecond);
                        if (cx + cw > x0 && cx < x1) {
                            hit.add(c.getId());
                        }
                    }
                }
                tl0.selectedIds().clear();
                tl0.selectedIds().addAll(hit);
                tl0.fire(Timeline.ChangeType.SELECTION);
            }
            default -> {
            }
        }
        redraw();
    }

    private void setPlayheadAt(javafx.scene.input.MouseEvent e) {
        long t = pxToTime(e.getX());
        if (ctx.config().getBool("snapEnabled")) {
            long snapped = snapEngine.snap(ctx.project().timeline(), t, null);
            if (snapped != t) {
                snapGuideTime = snapped;
            } else {
                snapGuideTime = -1;
            }
            t = snapped;
        } else {
            snapGuideTime = -1;
        }
        ctx.project().timeline().setPlayhead(Math.max(0, t));
    }

    private void onRelease(javafx.scene.input.MouseEvent e) {
        if (mode == Mode.PLAYHEAD_DRAG) {
            snapGuideTime = -1;
            mode = Mode.NONE;
            redraw();
            return;
        }
        if (pendingCommand != null) {
            pendingCommand.captureAfter();
            boolean changed = !pendingCommand.beforeJson().equals(pendingCommand.afterJson());
            if (changed) {
                ctx.undo().execute(pendingCommand);
                ctx.markDirty();
            }
            pendingCommand = null;
        }
        mode = Mode.NONE;
        snapGuideTime = -1;
        dragClip = null;
        dragClips.clear();
        redraw();
    }

    private void onMove(javafx.scene.input.MouseEvent e) {
        if (mode != Mode.NONE) {
            return;
        }
        if (e.getY() < RULER_H && e.getX() > HEADER_W) {
            canvas.getScene().getRoot().setStyle("-fx-cursor: hand");
            return;
        }
        TimelineClip clip = clipAt(e.getX(), e.getY());
        if (clip != null && !clip.isLocked()) {
            double cx = timeToPx(clip.getTimelineStart()) - scrollH;
            double cw = Math.max(2, clip.duration() / 1_000_000.0 * pxPerSecond);
            if (Math.abs(e.getX() - cx) < EDGE_SNAP_PX || Math.abs(e.getX() - (cx + cw)) < EDGE_SNAP_PX) {
                canvas.getScene().getRoot().setStyle("-fx-cursor: resize_h");
                return;
            }
            canvas.getScene().getRoot().setStyle("-fx-cursor: move");
        } else {
            canvas.getScene().getRoot().setStyle("-fx-cursor: default");
        }
    }

    // ---------- context menu ----------

    private void showContextMenu(javafx.scene.input.ContextMenuEvent e) {
        TimelineClip clip = clipAt(e.getX(), e.getY());
        ContextMenu menu = new ContextMenu();

        MenuItem addClip = new MenuItem("Add Clip Here");
        addClip.setOnAction(ev -> importAt(pxToTime(e.getX())));
        menu.getItems().add(addClip);

        if (clip != null) {
            Timeline tl = ctx.project().timeline();
            if (!tl.selectedIds().contains(clip.getId())) {
                tl.select(clip.getId());
            }
            menu.getItems().add(new SeparatorMenuItem());
            menu.getItems().add(item("Split at Playhead", ev -> splitAtPlayhead()));
            menu.getItems().add(item("Delete", ev -> deleteSelected(false)));
            menu.getItems().add(item("Ripple Delete", ev -> deleteSelected(true)));
            menu.getItems().add(item("Duplicate", ev -> duplicateSelected()));
            menu.getItems().add(item("Copy", ev -> copySelected()));
            menu.getItems().add(item("Join Adjacent Clips", ev -> joinSelected()));
            menu.getItems().add(new SeparatorMenuItem());
            if (clip.getKind() == TimelineClip.Kind.VIDEO && clip.isHasAudio() && !clip.isAudioDetached()) {
                menu.getItems().add(item("Detach Audio", ev -> detachAudio()));
            }
            menu.getItems().add(item("Restore Clip (Uncut)", ev -> restoreSelected()));
            menu.getItems().add(item("Reset Speed to 1x", ev -> resetSpeedSelected()));
            menu.getItems().add(new SeparatorMenuItem());
            menu.getItems().add(item(clip.isLocked() ? "Unlock" : "Lock", ev -> toggleLock()));
            menu.getItems().add(item(clip.isMuted() ? "Unmute" : "Mute", ev -> toggleMute()));
            menu.getItems().add(item(clip.isHidden() ? "Unhide" : "Hide", ev -> toggleHidden()));
            menu.getItems().add(new SeparatorMenuItem());
            MenuItem transIn = new MenuItem("Transition In: " + clip.getTransitionIn().label());
            transIn.setOnAction(ev -> cycleTransitionIn());
            MenuItem transOut = new MenuItem("Transition Out: " + clip.getTransitionOut().label());
            transOut.setOnAction(ev -> cycleTransitionOut());
            menu.getItems().addAll(transIn, transOut);
        }
        menu.show(canvas, e.getScreenX(), e.getScreenY());
    }

    private MenuItem item(String label, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        MenuItem m = new MenuItem(label);
        m.setOnAction(handler);
        return m;
    }

    // ---------- actions ----------

    public void splitAtPlayhead() {
        Timeline tl = ctx.project().timeline();
        UndoManager.TimelineSnapshotCommand cmd =
                new UndoManager.TimelineSnapshotCommand(tl, "Split");
        List<TimelineClip> created = ctx.operations().splitAt(tl, tl.playhead());
        if (created.isEmpty()) {
            ctx.status("Nothing to split at playhead");
            return;
        }
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        tl.select(created.get(created.size() - 1).getId());
        ctx.markDirty();
    }

    public void deleteSelected(boolean ripple) {
        Timeline tl = ctx.project().timeline();
        if (tl.selectedIds().isEmpty()) {
            ctx.status("No clip selected");
            return;
        }
        UndoManager.TimelineSnapshotCommand cmd =
                new UndoManager.TimelineSnapshotCommand(tl, ripple ? "Ripple Delete" : "Delete");
        ctx.operations().deleteSelected(tl, ripple);
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        ctx.markDirty();
    }

    public void duplicateSelected() {
        Timeline tl = ctx.project().timeline();
        if (tl.selectedIds().isEmpty()) {
            return;
        }
        UndoManager.TimelineSnapshotCommand cmd = new UndoManager.TimelineSnapshotCommand(tl, "Duplicate");
        ctx.operations().duplicateClips(tl, new ArrayList<>(tl.selectedIds()));
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        ctx.markDirty();
    }

    public void copySelected() {
        Timeline tl = ctx.project().timeline();
        List<String> ids = new ArrayList<>(tl.selectedIds());
        if (ids.isEmpty()) {
            ctx.status("No clip selected to copy");
            return;
        }
        clipboardJson = new ArrayList<>();
        for (String id : ids) {
            TimelineClip c = tl.clipById(id);
            if (c != null) {
                clipboardJson.add(videoforge.project.ProjectSerializer.clipToJsonString(c));
            }
        }
        ctx.status("Copied " + clipboardJson.size() + " clip(s)");
    }

    private List<String> clipboardJson = new ArrayList<>();

    public void pasteClips() {
        if (clipboardJson.isEmpty()) {
            ctx.status("Clipboard is empty");
            return;
        }
        Timeline tl = ctx.project().timeline();
        UndoManager.TimelineSnapshotCommand cmd = new UndoManager.TimelineSnapshotCommand(tl, "Paste");
        long base = tl.playhead();
        for (String json : clipboardJson) {
            TimelineClip clip = videoforge.project.ProjectSerializer.clipFromJsonString(json);
            clip.setTimelineStart(base);
            Track track = tl.trackById(clip.getTrackId());
            if (track == null) {
                track = tl.defaultTrackFor(clip.getKind());
            }
            tl.addClip(clip, track);
            base += clip.duration() + videoforge.utils.TimeUtils.secondsToMicros(0.1);
        }
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        ctx.markDirty();
    }

    public void joinSelected() {
        Timeline tl = ctx.project().timeline();
        UndoManager.TimelineSnapshotCommand cmd = new UndoManager.TimelineSnapshotCommand(tl, "Join Clips");
        boolean ok = ctx.operations().joinClips(tl, new ArrayList<>(tl.selectedIds()));
        if (ok) {
            cmd.captureAfter();
            ctx.undo().execute(cmd);
            ctx.markDirty();
        } else {
            ctx.status("Could not join: clips are not adjacent on the same track");
        }
    }

    public void detachAudio() {
        Timeline tl = ctx.project().timeline();
        List<TimelineClip> sel = tl.selectedClips();
        if (sel.isEmpty()) {
            return;
        }
        UndoManager.TimelineSnapshotCommand cmd = new UndoManager.TimelineSnapshotCommand(tl, "Detach Audio");
        for (TimelineClip c : sel) {
            ctx.operations().detachAudio(tl, c);
        }
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        ctx.markDirty();
    }

    public void restoreSelected() {
        Timeline tl = ctx.project().timeline();
        List<TimelineClip> sel = tl.selectedClips();
        if (sel.isEmpty()) {
            return;
        }
        UndoManager.TimelineSnapshotCommand cmd = new UndoManager.TimelineSnapshotCommand(tl, "Restore Clip");
        for (TimelineClip c : sel) {
            long full = ctx.operations().sourceDurationMicros(c);
            ctx.operations().restoreClip(tl, c, full);
        }
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        ctx.markDirty();
        ctx.status("Clip restored (uncut)");
    }

    public void resetSpeedSelected() {
        Timeline tl = ctx.project().timeline();
        List<TimelineClip> sel = tl.selectedClips();
        if (sel.isEmpty()) {
            return;
        }
        UndoManager.TimelineSnapshotCommand cmd = new UndoManager.TimelineSnapshotCommand(tl, "Reset Speed");
        for (TimelineClip c : sel) {
            ctx.operations().resetSpeed(tl, c);
        }
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        ctx.markDirty();
    }

    public void toggleLock() {
        Timeline tl = ctx.project().timeline();
        for (TimelineClip c : tl.selectedClips()) {
            c.setLocked(!c.isLocked());
        }
        tl.fire(Timeline.ChangeType.CLIP);
    }

    public void toggleMute() {
        Timeline tl = ctx.project().timeline();
        for (TimelineClip c : tl.selectedClips()) {
            c.setMuted(!c.isMuted());
        }
        tl.fire(Timeline.ChangeType.CLIP);
    }

    public void toggleHidden() {
        Timeline tl = ctx.project().timeline();
        for (TimelineClip c : tl.selectedClips()) {
            c.setHidden(!c.isHidden());
        }
        tl.fire(Timeline.ChangeType.CLIP);
    }

    public void cycleTransitionIn() {
        TimelineClip c = ctx.project().timeline().selectedClips().stream().findFirst().orElse(null);
        if (c == null) {
            return;
        }
        c.setTransitionIn(nextTransition(c.getTransitionIn()));
        ctx.project().timeline().notifyClipChanged(c);
        ctx.markDirty();
    }

    public void cycleTransitionOut() {
        TimelineClip c = ctx.project().timeline().selectedClips().stream().findFirst().orElse(null);
        if (c == null) {
            return;
        }
        c.setTransitionOut(nextTransition(c.getTransitionOut()));
        ctx.project().timeline().notifyClipChanged(c);
        ctx.markDirty();
    }

    private static videoforge.timeline.TransitionType nextTransition(videoforge.timeline.TransitionType t) {
        videoforge.timeline.TransitionType[] all = videoforge.timeline.TransitionType.values();
        return all[(t.ordinal() + 1) % all.length];
    }

    private void importAt(long time) {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Add Media");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Media", "*.*"));
        var file = chooser.showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        Timeline tl = ctx.project().timeline();
        UndoManager.TimelineSnapshotCommand cmd = new UndoManager.TimelineSnapshotCommand(tl, "Add Clip");
        MediaFile mf = ctx.library().byPath(file.toPath());
        if (mf == null) {
            var imported = ctx.library().importPaths(List.of(file.toPath()), null, null);
            mf = imported.isEmpty() ? null : imported.get(0);
        }
        if (mf != null) {
            TimelineClip clip = ctx.operations().createClip(mf, time);
            tl.addClip(clip);
            tl.select(clip.getId());
        }
        cmd.captureAfter();
        ctx.undo().execute(cmd);
        ctx.markDirty();
    }

    // ---------- drag & drop ----------

    private void installDragDrop() {
        setOnDragOver(e -> {
            if (e.getDragboard().hasFiles() || e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            long time = pxToTime(e.getX());
            boolean done = false;
            if (db.hasFiles()) {
                List<Path> paths = db.getFiles().stream().map(java.io.File::toPath)
                        .collect(java.util.stream.Collectors.toList());
                Timeline tl = ctx.project().timeline();
                UndoManager.TimelineSnapshotCommand cmd =
                        new UndoManager.TimelineSnapshotCommand(tl, "Add Media");
                long t = time;
                for (Path p : paths) {
                    MediaFile mf = ctx.library().byPath(p);
                    if (mf == null) {
                        var imported = ctx.library().importPaths(List.of(p), null, null);
                        mf = imported.isEmpty() ? null : imported.get(0);
                    }
                    if (mf != null) {
                        TimelineClip clip = ctx.operations().createClip(mf, t);
                        tl.addClip(clip);
                        t += clip.duration();
                    }
                }
                cmd.captureAfter();
                ctx.undo().execute(cmd);
                ctx.markDirty();
                done = true;
            } else if (db.hasString()) {
                // dragging from the media list
                Path p = Path.of(db.getString());
                Timeline tl = ctx.project().timeline();
                UndoManager.TimelineSnapshotCommand cmd =
                        new UndoManager.TimelineSnapshotCommand(tl, "Add Media");
                MediaFile mf = ctx.library().byPath(p);
                if (mf == null) {
                    var imported = ctx.library().importPaths(List.of(p), null, null);
                    mf = imported.isEmpty() ? null : imported.get(0);
                }
                if (mf != null) {
                    TimelineClip clip = ctx.operations().createClip(mf, time);
                    tl.addClip(clip);
                }
                cmd.captureAfter();
                ctx.undo().execute(cmd);
                ctx.markDirty();
                done = true;
            }
            e.setDropCompleted(done);
            e.consume();
        });
    }

    // ---------- public helpers ----------

    public void setPlayhead(long time) {
        ctx.project().timeline().setPlayhead(time);
        redraw();
    }

    public void scrollToTime(long time) {
        double x = timeToPx(time) - getWidth() / 2.0;
        hScroll.setValue(Math.max(0, Math.min(hScroll.getMax(), x)));
    }

    public void scrollToPlayhead() {
        double x = timeToPx(ctx.project().timeline().playhead()) - getWidth() / 2.0;
        hScroll.setValue(Math.max(0, Math.min(hScroll.getMax(), x)));
    }

    public void setZoom(double pxPerSecond) {
        this.pxPerSecond = Math.max(4, Math.min(400, pxPerSecond));
        updateScrollRange();
        redraw();
    }

    public double zoom() {
        return pxPerSecond;
    }
}
