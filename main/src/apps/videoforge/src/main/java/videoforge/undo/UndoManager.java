package videoforge.undo;

import videoforge.timeline.Timeline;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Command-based undo/redo manager. Holds two stacks and a history of labels.
 * Ctrl+Z / Ctrl+Y and Edit > History are wired to this class.
 */
public final class UndoManager {

    private final Deque<EditCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditCommand> redoStack = new ArrayDeque<>();
    private final List<String> history = new ArrayList<>();

    private static final int MAX = 200;

    /** Execute a command, record it and clear the redo stack. */
    public void execute(EditCommand cmd) {
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
        history.add(cmd.label());
        if (history.size() > MAX) {
            history.remove(0);
        }
    }

    /** Run an existing edit operation and wrap it in a command. */
    public void executeEdit(EditOperation op) {
        EditCommand cmd = new RunnableCommand(op, null);
        execute(cmd);
    }

    @FunctionalInterface
    public interface EditOperation {
        void run();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        EditCommand cmd = undoStack.poll();
        if (cmd == null) {
            return;
        }
        cmd.undo();
        redoStack.push(cmd);
    }

    public void redo() {
        EditCommand cmd = redoStack.poll();
        if (cmd == null) {
            return;
        }
        cmd.execute();
        undoStack.push(cmd);
    }

    public String undoLabel() {
        EditCommand c = undoStack.peek();
        return c == null ? "" : c.label();
    }

    public String redoLabel() {
        EditCommand c = redoStack.peek();
        return c == null ? "" : c.label();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    public List<String> history() {
        List<String> out = new ArrayList<>(history);
        java.util.Collections.reverse(out);
        return out;
    }

    /**
     * Wraps a {@link RunnableEdit} so undo executes a symmetric restore.
     */
    public abstract static class RunnableEdit implements EditCommand {
    }

    private static final class RunnableCommand implements EditCommand {
        private final EditOperation run;
        private final EditOperation undo;

        RunnableCommand(EditOperation run, EditOperation undo) {
            this.run = run;
            this.undo = undo;
        }

        @Override
        public String label() {
            return "Edit";
        }

        @Override
        public void execute() {
            if (run != null) {
                run.run();
            }
        }

        @Override
        public void undo() {
            if (undo != null) {
                undo.run();
            }
        }
    }

    /** A command that restores the entire timeline to a previous snapshot. */
    public static final class TimelineSnapshotCommand implements EditCommand {
        private final Timeline timeline;
        private final String label;
        private final String before;
        private String after;

        public TimelineSnapshotCommand(Timeline timeline, String label) {
            this.timeline = timeline;
            this.label = label;
            this.before = videoforge.project.ProjectSerializer.timelineToJsonString(timeline);
        }

        public void captureAfter() {
            this.after = videoforge.project.ProjectSerializer.timelineToJsonString(timeline);
        }

        public String beforeJson() {
            return before;
        }

        public String afterJson() {
            return after;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public void execute() {
            if (after == null) {
                return;
            }
            restore(after);
        }

        @Override
        public void undo() {
            restore(before);
        }

        private void restore(String json) {
            try {
                videoforge.project.ProjectSerializer.timelineFromJsonString(json, timeline);
            } catch (videoforge.project.ProjectSerializer.ProjectException e) {
                // never lose the user's work on an undo failure
            }
        }
    }

    /** Snapshots a set of clips before a property edit, restores them on undo. */
    public static final class ClipSnapshotCommand implements EditCommand {
        private final Timeline timeline;
        private final String label;
        private final List<SnapClip> before;
        private final List<SnapClip> after = new ArrayList<>();
        private final List<String> clipIds;

        public ClipSnapshotCommand(Timeline timeline, String label, List<String> clipIds) {
            this.timeline = timeline;
            this.label = label;
            this.clipIds = clipIds;
            this.before = new ArrayList<>();
            for (String id : clipIds) {
                var clip = timeline.clipById(id);
                if (clip != null) {
                    before.add(new SnapClip(id, videoforge.project.ProjectSerializer.clipToJsonString(clip)));
                }
            }
        }

        public void captureAfter() {
            after.clear();
            for (String id : clipIds) {
                var clip = timeline.clipById(id);
                if (clip != null) {
                    after.add(new SnapClip(id, videoforge.project.ProjectSerializer.clipToJsonString(clip)));
                }
            }
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public void execute() {
            restore(after);
        }

        @Override
        public void undo() {
            restore(before);
        }

        private void restore(List<SnapClip> snaps) {
            for (SnapClip s : snaps) {
                var clip = timeline.clipById(s.id);
                if (clip == null) {
                    continue;
                }
                var restored = videoforge.project.ProjectSerializer.clipFromJsonString(s.json);
                copyOver(restored, clip);
            }
            timeline.fire(Timeline.ChangeType.CLIP);
        }

        private void copyOver(videoforge.timeline.TimelineClip from, videoforge.timeline.TimelineClip to) {
            to.setTimelineStart(from.getTimelineStart());
            to.setSourceStart(from.getSourceStart());
            to.setSourceEnd(from.getSourceEnd());
            to.setSpeed(from.getSpeed());
            to.setReverse(from.isReverse());
            to.setFreezeFrame(from.isFreezeFrame());
            to.setPositionX(from.getPositionX());
            to.setPositionY(from.getPositionY());
            to.setScale(from.getScale());
            to.setScaleX(from.getScaleX());
            to.setScaleY(from.getScaleY());
            to.setRotation(from.getRotation());
            to.setOpacity(from.getOpacity());
            to.setVolume(from.getVolume());
            to.setMuted(from.isMuted());
            to.setEnabled(from.isEnabled());
            to.setHidden(from.isHidden());
            to.setLocked(from.isLocked());
            to.setName(from.getName());
            to.setLabel(from.getLabel());
            to.setColor(from.getColor());
            to.setTransitionIn(from.getTransitionIn());
            to.setTransitionOut(from.getTransitionOut());
            to.setTransitionInDuration(from.getTransitionInDuration());
            to.setTransitionOutDuration(from.getTransitionOutDuration());
            to.getEffects().clear();
            to.getEffects().addAll(from.getEffects());
            to.allKeyframes().clear();
            to.allKeyframes().putAll(from.allKeyframes());
            to.setText(from.getText());
            to.setHasAudio(from.isHasAudio());
            to.setAudioDetached(from.isAudioDetached());
        }

        private record SnapClip(String id, String json) {}
    }
}
