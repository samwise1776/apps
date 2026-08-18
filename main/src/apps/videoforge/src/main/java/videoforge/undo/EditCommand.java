package videoforge.undo;

/**
 * A single undoable edit. Commands capture the before/after state at creation
 * time and apply/restore it. Structural timeline edits use whole-timeline
 * snapshots; property edits snapshot only the affected clips.
 */
public interface EditCommand {

    /** Human-readable description shown in the Edit History window. */
    String label();

    void execute();

    void undo();
}
