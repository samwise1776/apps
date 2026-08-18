package videoforge.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import videoforge.effects.BlurEffect;
import videoforge.effects.ChromaKeyEffect;
import videoforge.effects.ColorEffect;
import videoforge.effects.CropEffect;
import videoforge.effects.Effect;
import videoforge.effects.TextEffect;
import videoforge.timeline.Timeline;
import videoforge.timeline.TimelineClip;
import videoforge.timeline.Keyframe;
import videoforge.undo.UndoManager;
import videoforge.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * Right-hand inspector: live editors for the selected clip (transform, audio,
 * speed), a full text editor for TEXT clips, an effect stack, and project-level
 * settings (canvas, fps, background).
 */
public final class InspectorPanel extends VBox {

    private final AppContext ctx;
    private final TabPane tabs = new TabPane();
    private final Label emptyLabel = new Label("Select a clip to edit its properties.");

    private final VBox clipBox = new VBox(8);
    private final VBox textBox = new VBox(8);
    private final VBox effectsBox = new VBox(8);
    private final VBox projectBox = new VBox(8);

    public InspectorPanel(AppContext ctx) {
        this.ctx = ctx;
        setPrefWidth(300);
        setMinWidth(240);
        getStyleClass().add("app-panel");
        getStyleClass().add("app-panel-border");

        tabs.getTabs().addAll(
                tab("Clip", clipBox),
                tab("Text", textBox),
                tab("Effects", effectsBox),
                tab("Project", projectBox));
        VBox.setVgrow(tabs, Priority.ALWAYS);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        getChildren().add(tabs);

        ctx.project().timeline().addListener(this::onTimelineEvent);
        rebuild();
    }

    private Tab tab(String name, VBox content) {
        Tab t = new Tab(name, wrap(content));
        t.setClosable(false);
        return t;
    }

    private static ScrollPane wrap(VBox content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    private void onTimelineEvent(Timeline.ChangeType type) {
        if (type == Timeline.ChangeType.SELECTION || type == Timeline.ChangeType.STRUCTURE) {
            javafx.application.Platform.runLater(this::rebuild);
        } else if (type == Timeline.ChangeType.CLIP) {
            javafx.application.Platform.runLater(this::rebuild);
        }
    }

    private void rebuild() {
        List<TimelineClip> sel = ctx.project().timeline().selectedClips();
        if (sel.isEmpty()) {
            tabs.setVisible(false);
            tabs.setManaged(false);
            if (!getChildren().contains(emptyLabel)) {
                getChildren().clear();
                getChildren().add(emptyLabel);
            }
            return;
        }
        if (!getChildren().contains(tabs)) {
            getChildren().clear();
            getChildren().add(tabs);
            tabs.setVisible(true);
            tabs.setManaged(true);
        }
        TimelineClip clip = sel.get(0);
        boolean multi = sel.size() > 1;
        rebuildClip(clip, multi);
        rebuildText(clip);
        rebuildEffects(clip);
        rebuildProject();
    }

    // ---------- helpers ----------

    private static Slider slider(double min, double max, double value, DoubleConsumer onChange) {
        Slider s = new Slider(min, max, value);
        s.setMaxWidth(Double.MAX_VALUE);
        s.valueProperty().addListener((o, a, b) -> onChange.accept(b.doubleValue()));
        return s;
    }

    private static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        return l;
    }

    private HBox labeledSlider(String name, double min, double max, double value, boolean intVal,
                               DoubleConsumer onChange, Runnable onCommit) {
        Slider s = slider(min, max, value, onChange);
        Label val = new Label(intVal ? String.valueOf(Math.round(value)) : String.format("%.2f", value));
        s.valueProperty().addListener((o, a, b) -> {
            val.setText(intVal ? String.valueOf(Math.round(b.doubleValue())) : String.format("%.2f", b.doubleValue()));
        });
        s.valueChangingProperty().addListener((o, was, now) -> {
            if (!now && onCommit != null) {
                onCommit.run();
            }
        });
        HBox row = new HBox(6, fieldLabel(name), s, val);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(s, Priority.ALWAYS);
        return row;
    }

    private void rebuildClip(TimelineClip clip, boolean multi) {
        clipBox.getChildren().clear();
        if (multi) {
            clipBox.getChildren().add(new Label("Editing " + ctx.project().timeline().selectedIds().size() + " clips"));
        }
        clipBox.getChildren().add(new Label("Transform"));

        clipBox.getChildren().add(labeledSlider("X", -2000, 2000, safeVal(clip.getPositionX(), 960), false,
                v -> setClipProp(clip, c -> c.setPositionX(v)), commitClip(clip)));
        clipBox.getChildren().add(labeledSlider("Y", -2000, 2000, safeVal(clip.getPositionY(), 540), false,
                v -> setClipProp(clip, c -> c.setPositionY(v)), commitClip(clip)));
        clipBox.getChildren().add(labeledSlider("Scale", 0.05, 4, clip.getScale(), false,
                v -> setClipProp(clip, c -> c.setScale(v)), commitClip(clip)));
        clipBox.getChildren().add(labeledSlider("Scale X", 0.05, 4, clip.getScaleX(), false,
                v -> setClipProp(clip, c -> c.setScaleX(v)), commitClip(clip)));
        clipBox.getChildren().add(labeledSlider("Scale Y", 0.05, 4, clip.getScaleY(), false,
                v -> setClipProp(clip, c -> c.setScaleY(v)), commitClip(clip)));
        clipBox.getChildren().add(labeledSlider("Rotation", -360, 360, clip.getRotation(), false,
                v -> setClipProp(clip, c -> c.setRotation(v)), commitClip(clip)));
        clipBox.getChildren().add(labeledSlider("Opacity", 0, 1, clip.getOpacity(), false,
                v -> setClipProp(clip, c -> c.setOpacity(v)), commitClip(clip)));

        HBox fitRow = new HBox(6);
        Button fit = new Button("Fit");
        Button fill = new Button("Fill");
        Button center = new Button("Center");
        Button reset = new Button("Reset Transform");
        fit.setOnAction(e -> {
            clip.setScale(1);
            clip.setScaleX(1);
            clip.setScaleY(1);
            centerClip(clip);
            commitClip(clip).run();
        });
        fill.setOnAction(e -> {
            clip.setScale(2);
            clip.setScaleX(1);
            clip.setScaleY(1);
            centerClip(clip);
            commitClip(clip).run();
        });
        center.setOnAction(e -> {
            centerClip(clip);
            commitClip(clip).run();
        });
        reset.setOnAction(e -> {
            clip.setPositionX(Double.NaN);
            clip.setPositionY(Double.NaN);
            clip.setScale(1);
            clip.setScaleX(1);
            clip.setScaleY(1);
            clip.setRotation(0);
            clip.setOpacity(1);
            commitClip(clip).run();
        });
        fitRow.getChildren().addAll(fit, fill, center, reset);
        clipBox.getChildren().add(fitRow);

        clipBox.getChildren().add(new Label("Audio"));
        clipBox.getChildren().add(labeledSlider("Volume", 0, 2, clip.getVolume(), false,
                v -> setClipProp(clip, c -> c.setVolume(v)), commitClip(clip)));
        CheckBox mute = new CheckBox("Mute");
        mute.setSelected(clip.isMuted());
        mute.setOnAction(e -> setClipProp(clip, c -> c.setMuted(mute.isSelected())));
        CheckBox hidden = new CheckBox("Hidden");
        hidden.setSelected(clip.isHidden());
        hidden.setOnAction(e -> setClipProp(clip, c -> c.setHidden(hidden.isSelected())));
        CheckBox locked = new CheckBox("Locked");
        locked.setSelected(clip.isLocked());
        locked.setOnAction(e -> setClipProp(clip, c -> c.setLocked(locked.isSelected())));
        HBox flags = new HBox(10, mute, hidden, locked);
        clipBox.getChildren().add(flags);

        if (clip.getKind() == TimelineClip.Kind.VIDEO || clip.getKind() == TimelineClip.Kind.AUDIO) {
            clipBox.getChildren().add(new Label("Speed"));
            ComboBox<String> speeds = new ComboBox<>(FXCollections.observableArrayList(
                    "0.25x", "0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x", "4x"));
            speeds.setValue(formatSpeed(clip.getSpeed()));
            speeds.setOnAction(e -> {
                try {
                    double sp = Double.parseDouble(speeds.getValue().replace("x", ""));
                    ctx.undo().execute(new UndoManager.ClipSnapshotCommand(ctx.project().timeline(),
                            "Set Speed", List.of(clip.getId())));
                    clip.setSpeed(sp);
                    commitClip(clip).run();
                } catch (Exception ex) {
                }
            });
            clipBox.getChildren().add(speeds);

            HBox extra = new HBox(6);
            CheckBox reverse = new CheckBox("Reverse");
            reverse.setSelected(clip.isReverse());
            reverse.setOnAction(e -> setClipProp(clip, c -> c.setReverse(reverse.isSelected())));
            CheckBox freeze = new CheckBox("Freeze Frame");
            freeze.setSelected(clip.isFreezeFrame());
            freeze.setOnAction(e -> setClipProp(clip, c -> c.setFreezeFrame(freeze.isSelected())));
            extra.getChildren().addAll(reverse, freeze);
            clipBox.getChildren().add(extra);
        }

        // timing info + restore
        Label info = new Label("In: " + TimeUtils.toHMS(clip.getSourceStart())
                + "  Out: " + TimeUtils.toHMS(clip.getSourceEnd()));
        info.getStyleClass().add("muted");
        clipBox.getChildren().add(info);

        Button restore = new Button("Restore Clip (Uncut)");
        restore.setOnAction(e -> {
            ctx.operations().restoreClip(ctx.project().timeline(), clip,
                    ctx.operations().sourceDurationMicros(clip));
            commitClip(clip).run();
        });
        clipBox.getChildren().add(restore);

        // keyframes
        clipBox.getChildren().add(new Label("Keyframes"));
        HBox kfRow = new HBox(6);
        ComboBox<String> props = new ComboBox<>(FXCollections.observableArrayList(
                "positionX", "positionY", "scale", "rotation", "opacity", "volume"));
        props.setValue("positionX");
        Button addKf = new Button("Add Keyframe");
        Button delKf = new Button("Clear Keyframes");
        addKf.setOnAction(e -> {
            String prop = props.getValue();
            long local = ctx.project().timeline().playhead() - clip.getTimelineStart();
            local = Math.max(0, Math.min(local, clip.duration()));
            double base = switch (prop) {
                case "positionX" -> clip.getPositionX();
                case "positionY" -> clip.getPositionY();
                case "scale" -> clip.getScale();
                case "rotation" -> clip.getRotation();
                case "opacity" -> clip.getOpacity();
                default -> clip.getVolume();
            };
            final List<Keyframe> kf = clip.keyframes(prop);
            final long atTime = local;
            kf.removeIf(k -> Math.abs(k.getTimeMicros() - atTime) < TimeUtils.secondsToMicros(0.05));
            kf.add(new videoforge.timeline.Keyframe(local, base));
            kf.sort(java.util.Comparator.comparingLong(videoforge.timeline.Keyframe::getTimeMicros));
            commitClip(clip).run();
            ctx.status("Keyframe added on " + prop);
        });
        delKf.setOnAction(e -> {
            clip.allKeyframes().clear();
            commitClip(clip).run();
        });
        kfRow.getChildren().addAll(props, addKf, delKf);
        clipBox.getChildren().add(kfRow);
    }

    private void centerClip(TimelineClip clip) {
        clip.setPositionX(ctx.project().timeline().canvasWidth() / 2.0);
        clip.setPositionY(ctx.project().timeline().canvasHeight() / 2.0);
    }

    private static double safeVal(double v, double fallback) {
        return Double.isNaN(v) ? fallback : v;
    }

    private static String formatSpeed(double s) {
        if (s == 0.25) return "0.25x";
        if (s == 0.5) return "0.5x";
        if (s == 0.75) return "0.75x";
        if (s == 1) return "1x";
        if (s == 1.25) return "1.25x";
        if (s == 1.5) return "1.5x";
        if (s == 2) return "2x";
        if (s == 4) return "4x";
        return s + "x";
    }

    private void setClipProp(TimelineClip clip, java.util.function.Consumer<TimelineClip> mut) {
        mut.accept(clip);
        ctx.project().timeline().notifyClipChanged(clip);
    }

    private Runnable commitClip(TimelineClip clip) {
        return () -> {
            ctx.markDirty();
            ctx.project().timeline().notifyClipChanged(clip);
        };
    }

    // ---------- text editor ----------

    private void rebuildText(TimelineClip clip) {
        textBox.getChildren().clear();
        TextEffect t = clip.getText();
        if (clip.getKind() != TimelineClip.Kind.TEXT || t == null) {
            textBox.getChildren().add(new Label("Select a text clip to edit text."));
            return;
        }
        TextArea area = new TextArea(t.getText());
        area.setPrefRowCount(3);
        area.textProperty().addListener((o, a, b) -> {
            t.setText(b);
            commitClip(clip).run();
        });
        textBox.getChildren().add(area);

        TextField font = new TextField(t.getFont());
        font.setOnAction(e -> {
            t.setFont(font.getText());
            commitClip(clip).run();
        });
        textBox.getChildren().add(row("Font", font));

        textBox.getChildren().add(labeledSlider("Font size", 8, 300, t.getFontSize(), false,
                v -> {
                    t.setFontSize(v);
                    commitClip(clip).run();
                }, commitClip(clip)));
        textBox.getChildren().add(labeledSlider("Opacity", 0, 1, t.getOpacity(), false,
                v -> {
                    t.setOpacity(v);
                    commitClip(clip).run();
                }, commitClip(clip)));

        CheckBox bold = new CheckBox("Bold");
        bold.setSelected(t.isBold());
        bold.setOnAction(e -> {
            t.setBold(bold.isSelected());
            commitClip(clip).run();
        });
        CheckBox italic = new CheckBox("Italic");
        italic.setSelected(t.isItalic());
        italic.setOnAction(e -> {
            t.setItalic(italic.isSelected());
            commitClip(clip).run();
        });
        CheckBox underline = new CheckBox("Underline");
        underline.setSelected(t.isUnderline());
        underline.setOnAction(e -> {
            t.setUnderline(underline.isSelected());
            commitClip(clip).run();
        });
        textBox.getChildren().add(new HBox(10, bold, italic, underline));

        ColorPicker color = new ColorPicker(rgbToFx(t.getColor()));
        color.setOnAction(e -> {
            t.setColor(fxToRgb(color.getValue()));
            commitClip(clip).run();
        });
        textBox.getChildren().add(row("Color", color));

        CheckBox stroke = new CheckBox("Stroke");
        stroke.setSelected(t.isStrokeEnabled());
        stroke.setOnAction(e -> {
            t.setStrokeEnabled(stroke.isSelected());
            commitClip(clip).run();
        });
        CheckBox shadow = new CheckBox("Shadow");
        shadow.setSelected(t.isShadowEnabled());
        shadow.setOnAction(e -> {
            t.setShadowEnabled(shadow.isSelected());
            commitClip(clip).run();
        });
        CheckBox bg = new CheckBox("Background");
        bg.setSelected(t.isBackgroundEnabled());
        bg.setOnAction(e -> {
            t.setBackgroundEnabled(bg.isSelected());
            commitClip(clip).run();
        });
        textBox.getChildren().add(new HBox(10, stroke, shadow, bg));

        textBox.getChildren().add(labeledSlider("Stroke width", 0, 20, t.getStrokeWidth(), false,
                v -> {
                    t.setStrokeWidth(v);
                    commitClip(clip).run();
                }, commitClip(clip)));
        textBox.getChildren().add(labeledSlider("Shadow distance", 0, 30, t.getShadowDistance(), false,
                v -> {
                    t.setShadowDistance(v);
                    commitClip(clip).run();
                }, commitClip(clip)));

        ComboBox<String> align = new ComboBox<>(FXCollections.observableArrayList("left", "center", "right"));
        align.setValue(t.getAlign());
        align.setOnAction(e -> {
            t.setAlign(align.getValue());
            commitClip(clip).run();
        });
        textBox.getChildren().add(row("Align", align));

        textBox.getChildren().add(new Label("Animations"));
        addCheck(textBox, "Fade in", t.isFadeIn(), v -> {
            t.setFadeIn(v);
            commitClip(clip).run();
        });
        addCheck(textBox, "Fade out", t.isFadeOut(), v -> {
            t.setFadeOut(v);
            commitClip(clip).run();
        });
        addCheck(textBox, "Slide in", t.isSlideIn(), v -> {
            t.setSlideIn(v);
            commitClip(clip).run();
        });
        addCheck(textBox, "Slide out", t.isSlideOut(), v -> {
            t.setSlideOut(v);
            commitClip(clip).run();
        });
        addCheck(textBox, "Typewriter", t.isTypewriter(), v -> {
            t.setTypewriter(v);
            commitClip(clip).run();
        });
        addCheck(textBox, "Pop in", t.isPopIn(), v -> {
            t.setPopIn(v);
            commitClip(clip).run();
        });
        addCheck(textBox, "Zoom in", t.isZoomIn(), v -> {
            t.setZoomIn(v);
            commitClip(clip).run();
        });
        textBox.getChildren().add(labeledSlider("Animation duration (s)", 0.1, 3, t.getAnimationDuration(), false,
                v -> {
                    t.setAnimationDuration(v);
                    commitClip(clip).run();
                }, commitClip(clip)));
    }

    private static void addCheck(VBox box, String label, boolean value, java.util.function.Consumer<Boolean> on) {
        CheckBox cb = new CheckBox(label);
        cb.setSelected(value);
        cb.setOnAction(e -> on.accept(cb.isSelected()));
        box.getChildren().add(cb);
    }

    private static HBox row(String label, javafx.scene.Node node) {
        HBox row = new HBox(6, fieldLabel(label), node);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(node, Priority.ALWAYS);
        return row;
    }

    private static Color rgbToFx(int rgb) {
        return Color.rgb((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff);
    }

    private static int fxToRgb(Color c) {
        return (int) Math.round(c.getRed() * 255) << 16
                | (int) Math.round(c.getGreen() * 255) << 8
                | (int) Math.round(c.getBlue() * 255);
    }

    // ---------- effects ----------

    private void rebuildEffects(TimelineClip clip) {
        effectsBox.getChildren().clear();
        if (clip.getEffects().isEmpty()) {
            effectsBox.getChildren().add(new Label("No effects. Add one below."));
        }
        for (Effect e : clip.getEffects()) {
            effectsBox.getChildren().add(effectCard(clip, e));
        }
        Button addBlur = new Button("+ Blur");
        addBlur.setOnAction(ev -> {
            clip.addEffect(new BlurEffect());
            commitClip(clip).run();
            rebuildEffects(clip);
        });
        Button addColor = new Button("+ Color");
        addColor.setOnAction(ev -> {
            clip.addEffect(new ColorEffect());
            commitClip(clip).run();
            rebuildEffects(clip);
        });
        Button addCrop = new Button("+ Crop");
        addCrop.setOnAction(ev -> {
            clip.addEffect(new CropEffect());
            commitClip(clip).run();
            rebuildEffects(clip);
        });
        Button addChroma = new Button("+ Chroma Key");
        addChroma.setOnAction(ev -> {
            clip.addEffect(new ChromaKeyEffect());
            commitClip(clip).run();
            rebuildEffects(clip);
        });
        effectsBox.getChildren().add(new HBox(6, addBlur, addColor, addCrop, addChroma));
    }

    private VBox effectCard(TimelineClip clip, Effect e) {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: #262c38; -fx-padding: 6; -fx-background-radius: 4;");
        HBox head = new HBox(6);
        Label title = new Label(e.getName());
        title.getStyleClass().add("section-title");
        CheckBox enabled = new CheckBox();
        enabled.setSelected(e.isEnabled());
        enabled.setOnAction(ev -> {
            e.setEnabled(enabled.isSelected());
            commitClip(clip).run();
        });
        Button remove = new Button("Remove");
        remove.setOnAction(ev -> {
            clip.removeEffect(e.getId());
            commitClip(clip).run();
            rebuildEffects(clip);
        });
        head.getChildren().addAll(title, enabled, remove);
        card.getChildren().add(head);

        if (e instanceof BlurEffect b) {
            ComboBox<String> mode = new ComboBox<>(FXCollections.observableArrayList("WHOLE", "REGION"));
            mode.setValue(b.getMode().name());
            mode.setOnAction(ev -> {
                b.setMode(BlurEffect.Mode.valueOf(mode.getValue()));
                commitClip(clip).run();
            });
            card.getChildren().add(row("Mode", mode));
            card.getChildren().add(labeledSlider("Strength", 1, 100, b.getStrength(), false,
                    v -> {
                        b.setStrength(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            if (b.getMode() == BlurEffect.Mode.REGION) {
                card.getChildren().add(labeledSlider("X", 0, 1, b.getRegionX(), false,
                        v -> {
                            b.setRegionX(v);
                            commitClip(clip).run();
                        }, commitClip(clip)));
                card.getChildren().add(labeledSlider("Y", 0, 1, b.getRegionY(), false,
                        v -> {
                            b.setRegionY(v);
                            commitClip(clip).run();
                        }, commitClip(clip)));
                card.getChildren().add(labeledSlider("W", 0.05, 1, b.getRegionW(), false,
                        v -> {
                            b.setRegionW(v);
                            commitClip(clip).run();
                        }, commitClip(clip)));
                card.getChildren().add(labeledSlider("H", 0.05, 1, b.getRegionH(), false,
                        v -> {
                            b.setRegionH(v);
                            commitClip(clip).run();
                        }, commitClip(clip)));
                card.getChildren().add(labeledSlider("Feather", 0, 1, b.getFeather(), false,
                        v -> {
                            b.setFeather(v);
                            commitClip(clip).run();
                        }, commitClip(clip)));
            }
        } else if (e instanceof ColorEffect c) {
            card.getChildren().add(labeledSlider("Brightness", -100, 100, c.getBrightness(), true,
                    v -> {
                        c.setBrightness(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            card.getChildren().add(labeledSlider("Contrast", -100, 100, c.getContrast(), true,
                    v -> {
                        c.setContrast(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            card.getChildren().add(labeledSlider("Saturation", -100, 100, c.getSaturation(), true,
                    v -> {
                        c.setSaturation(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            card.getChildren().add(labeledSlider("Temperature", -100, 100, c.getTemperature(), true,
                    v -> {
                        c.setTemperature(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            card.getChildren().add(labeledSlider("Gamma", 0.1, 5, c.getGamma(), false,
                    v -> {
                        c.setGamma(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            card.getChildren().add(labeledSlider("Exposure", -3, 3, c.getExposure(), false,
                    v -> {
                        c.setExposure(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            ComboBox<String> preset = new ComboBox<>(FXCollections.observableArrayList("NONE", "BLACK_WHITE", "SEPIA"));
            preset.setValue(c.getPreset().name());
            preset.setOnAction(ev -> {
                c.setPreset(ColorEffect.Preset.valueOf(preset.getValue()));
                commitClip(clip).run();
            });
            card.getChildren().add(row("Preset", preset));
        } else if (e instanceof CropEffect c) {
            card.getChildren().add(labeledSlider("Top", 0, 0.95, c.getTop(), false,
                    v -> {
                        c.setTop(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            card.getChildren().add(labeledSlider("Bottom", 0, 0.95, c.getBottom(), false,
                    v -> {
                        c.setBottom(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            card.getChildren().add(labeledSlider("Left", 0, 0.95, c.getLeft(), false,
                    v -> {
                        c.setLeft(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            card.getChildren().add(labeledSlider("Right", 0, 0.95, c.getRight(), false,
                    v -> {
                        c.setRight(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
        } else if (e instanceof ChromaKeyEffect ck) {
            ColorPicker picker = new ColorPicker(rgbToFx(ck.getKeyColor()));
            picker.setOnAction(ev -> {
                ck.setKeyColor(fxToRgb(picker.getValue()));
                commitClip(clip).run();
            });
            card.getChildren().add(row("Key color", picker));
            card.getChildren().add(labeledSlider("Tolerance", 0, 1, ck.getTolerance(), false,
                    v -> {
                        ck.setTolerance(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            card.getChildren().add(labeledSlider("Softness", 0, 1, ck.getSoftness(), false,
                    v -> {
                        ck.setSoftness(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
            card.getChildren().add(labeledSlider("Spill", 0, 1, ck.getSpill(), false,
                    v -> {
                        ck.setSpill(v);
                        commitClip(clip).run();
                    }, commitClip(clip)));
        }
        return card;
    }

    // ---------- project ----------

    private void rebuildProject() {
        projectBox.getChildren().clear();
        Timeline tl = ctx.project().timeline();

        ComboBox<String> res = new ComboBox<>(FXCollections.observableArrayList(
                "1920x1080", "2560x1440", "3840x2160", "1080x1920", "1080x1080", "Custom"));
        res.setValue(tl.canvasWidth() + "x" + tl.canvasHeight());
        res.setOnAction(e -> {
            String v = res.getValue();
            if (!"Custom".equals(v)) {
                String[] parts = v.split("x");
                tl.setCanvasSize(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                ctx.markDirty();
            }
        });
        projectBox.getChildren().add(row("Resolution", res));

        TextField w = new TextField(String.valueOf(tl.canvasWidth()));
        TextField h = new TextField(String.valueOf(tl.canvasHeight()));
        Button apply = new Button("Apply");
        apply.setOnAction(e -> {
            try {
                tl.setCanvasSize(Integer.parseInt(w.getText()), Integer.parseInt(h.getText()));
                ctx.markDirty();
            } catch (NumberFormatException ex) {
            }
        });
        HBox custom = new HBox(6, w, h, apply);
        projectBox.getChildren().add(custom);

        ComboBox<String> fps = new ComboBox<>(FXCollections.observableArrayList("24", "25", "30", "50", "60"));
        fps.setValue(String.valueOf(Math.round(tl.fps())));
        fps.setOnAction(e -> {
            tl.setFps(Double.parseDouble(fps.getValue()));
            ctx.project().exportSettings().fps = Double.parseDouble(fps.getValue());
            ctx.markDirty();
        });
        projectBox.getChildren().add(row("Timeline FPS", fps));

        projectBox.getChildren().add(new Label("Background"));
        var bg = ctx.project().background();
        ComboBox<String> bgType = new ComboBox<>(FXCollections.observableArrayList(
                "SOLID", "GRADIENT", "IMAGE", "TRANSPARENT"));
        bgType.setValue(bg.getType().name());
        bgType.setOnAction(e -> {
            bg.setType(videoforge.effects.BackgroundEffect.Type.valueOf(bgType.getValue()));
            ctx.markDirty();
            rebuildProject();
        });
        projectBox.getChildren().add(row("Type", bgType));

        if (bg.getType() == videoforge.effects.BackgroundEffect.Type.SOLID) {
            ColorPicker c = new ColorPicker(rgbToFx(bg.getSolidColor()));
            c.setOnAction(e -> {
                bg.setSolidColor(fxToRgb(c.getValue()));
                ctx.markDirty();
                ctx.preview().setBackgroundOverride(bg);
            });
            projectBox.getChildren().add(row("Color", c));
        }
        if (bg.getType() == videoforge.effects.BackgroundEffect.Type.GRADIENT) {
            ColorPicker a = new ColorPicker(rgbToFx(bg.getGradientColorA()));
            a.setOnAction(e -> {
                bg.setGradientColorA(fxToRgb(a.getValue()));
                ctx.markDirty();
            });
            ColorPicker b = new ColorPicker(rgbToFx(bg.getGradientColorB()));
            b.setOnAction(e -> {
                bg.setGradientColorB(fxToRgb(b.getValue()));
                ctx.markDirty();
            });
            projectBox.getChildren().add(row("Color A", a));
            projectBox.getChildren().add(row("Color B", b));
        }
        if (bg.getType() == videoforge.effects.BackgroundEffect.Type.IMAGE) {
            Button choose = new Button("Choose Image...");
            choose.setOnAction(e -> {
                javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
                var file = fc.showOpenDialog(getScene().getWindow());
                if (file != null) {
                    bg.setImagePath(file.getPath());
                    ctx.markDirty();
                }
            });
            projectBox.getChildren().add(choose);
        }

        ComboBox<String> fill = new ComboBox<>(FXCollections.observableArrayList(
                "NONE", "FIT", "FILL", "STRETCH", "BLUR_BEHIND"));
        fill.setValue(bg.getFillMode().name());
        fill.setOnAction(e -> {
            bg.setFillMode(videoforge.effects.BackgroundEffect.FillMode.valueOf(fill.getValue()));
            ctx.markDirty();
        });
        projectBox.getChildren().add(row("Source fill", fill));
        if (bg.getFillMode() == videoforge.effects.BackgroundEffect.FillMode.BLUR_BEHIND) {
            projectBox.getChildren().add(labeledSlider("Blur strength", 5, 80, bg.getBlurStrength(), false,
                    v -> {
                        bg.setBlurStrength(v);
                        ctx.markDirty();
                    }, () -> {
                    }));
        }

        Button resetBg = new Button("Reset Background");
        resetBg.setOnAction(e -> {
            ctx.project().background().load(new videoforge.effects.BackgroundEffect().toJson());
            ctx.markDirty();
            rebuildProject();
        });
        projectBox.getChildren().add(resetBg);

        Button markers = new Button("Open Markers / Chapters...");
        markers.setOnAction(e -> MarkersWindow.show(ctx));
        Button chapters = new Button("Copy Chapters Text");
        chapters.setOnAction(e -> {
            String text = ctx.project().timeline().chapterText();
            if (text.isBlank()) {
                ctx.status("No chapter markers defined");
            } else {
                javafx.scene.input.Clipboard.getSystemClipboard()
                        .setContent(java.util.Map.of(javafx.scene.input.DataFormat.PLAIN_TEXT, text));
                ctx.status("Chapters copied to clipboard");
            }
        });
        projectBox.getChildren().addAll(markers, chapters);
    }
}
