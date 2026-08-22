(function () {
  const modules = [
    {
      name: "Core components",
      kind: "component",
      note: "Choose the smallest component that expresses the user's intent, then configure it before adding it to a container.",
      lessons: [
        ["Labels that explain", "Master JLabel", "JLabel", "Use labels for concise, non-editable text and pair them clearly with the control they describe.", "Label selected", "Label mastered"],
        ["Buttons with purpose", "Design JButton actions", "JButton", "Give buttons verb-first names so the result of clicking is predictable before the click.", "Button clicked", "Action confirmed"],
        ["Icons and text", "Work with ImageIcon", "ImageIcon", "Combine imagery with accessible text instead of asking an icon to communicate meaning alone.", "Icon loaded", "Icon and text ready"],
        ["Single-line input", "Capture JTextField values", "JTextField", "Use a text field for short input and read its current value inside the event that needs it.", "Text captured", "Input accepted"],
        ["Private input", "Use JPasswordField", "JPasswordField", "Password fields obscure entered characters and expose a character array that can be cleared after use.", "Password checked", "Credentials handled safely"],
        ["Multi-line notes", "Explore JTextArea", "JTextArea", "Text areas support longer input and usually belong inside a JScrollPane for predictable sizing.", "Note saved", "Multi-line note saved"],
        ["Independent choices", "Toggle JCheckBox", "JCheckBox", "Check boxes represent independent yes-or-no choices and can be selected in any combination.", "Option toggled", "Preference updated"],
        ["One choice only", "Group JRadioButtons", "JRadioButton", "A ButtonGroup makes related radio buttons mutually exclusive while each button remains a normal component.", "Choice selected", "Single choice locked in"],
        ["Compact selection", "Build JComboBox menus", "JComboBox", "Combo boxes conserve space by revealing a list of choices only when the user opens them.", "Item chosen", "Selection applied"],
        ["Visible collections", "Present data with JList", "JList", "Lists display a collection through a model and maintain selection separately from the underlying data.", "List item selected", "List selection understood"]
      ]
    },
    {
      name: "Layout mastery",
      kind: "layout",
      note: "Layout managers calculate geometry from preferred sizes. Combine simple layouts in nested panels before reaching for manual coordinates.",
      lessons: [
        ["Natural rows", "Arrange with FlowLayout", "FlowLayout", "FlowLayout places components in a row and wraps them when horizontal space runs out.", "Flow layout active", "Flow layout tuned"],
        ["Five useful regions", "Divide with BorderLayout", "BorderLayout", "BorderLayout gives the center remaining space while edge regions keep their preferred dimension.", "Border regions placed", "Five regions balanced"],
        ["Equal-sized cells", "Organize with GridLayout", "GridLayout", "GridLayout creates a uniform matrix where every component receives the same-sized cell.", "Grid created", "Grid spacing improved"],
        ["Vertical stacks", "Stack with BoxLayout", "BoxLayout.Y_AXIS", "A vertical BoxLayout respects component alignment and works well for settings or form sections.", "Vertical stack ready", "Vertical rhythm refined"],
        ["Horizontal tool rows", "Align along the X axis", "BoxLayout.X_AXIS", "Horizontal BoxLayout supports glue and struts for deliberate spacing in toolbars and action rows.", "Action row ready", "Action row aligned"],
        ["Flexible forms", "Control GridBagLayout", "GridBagLayout", "GridBagLayout places components on a weighted grid using constraints that control fill, span, and insets.", "Form constraints set", "Flexible form complete"],
        ["Switchable screens", "Navigate with CardLayout", "CardLayout", "CardLayout keeps several panels in one region and reveals exactly one card at a time.", "First card shown", "Card navigation connected"],
        ["Constraint-based placement", "Explore SpringLayout", "SpringLayout", "SpringLayout expresses relationships between component edges for compact generated forms.", "Springs connected", "Constraints resolved"],
        ["Layouts inside layouts", "Compose nested panels", "Nested JPanel", "Nested panels let each section use the layout best suited to its local responsibility.", "Panels nested", "Composite layout complete"],
        ["Whitespace that works", "Use borders and struts", "EmptyBorder", "Insets, gaps, struts, and glue create hierarchy without fragile blank labels or fixed coordinates.", "Spacing added", "Whitespace feels intentional"]
      ]
    },
    {
      name: "Events and input",
      kind: "event",
      note: "An event handler should be short: capture the event, update application state, refresh the view, and hand slow work elsewhere.",
      lessons: [
        ["Action events", "Connect ActionListener", "ActionListener", "Action events provide the standard path from buttons, menu items, and text-field submissions into your code.", "Action received", "Action listener connected"],
        ["Selection changes", "Respond with ItemListener", "ItemListener", "Item listeners tell you when selectable controls move into or out of the selected state.", "Item changed", "Selection event handled"],
        ["Continuous values", "Track ChangeListener", "ChangeListener", "Change listeners observe models that change continuously, including sliders, spinners, and tabs.", "Value changed", "Continuous value tracked"],
        ["Keyboard detail", "Listen with KeyAdapter", "KeyListener", "Key events expose physical key activity, though higher-level actions and key bindings are often more robust.", "Key released", "Keyboard input interpreted"],
        ["Pointer interaction", "Use MouseAdapter", "MouseListener", "Mouse listeners expose clicks, presses, movement, and entry for custom interaction beyond ordinary buttons.", "Pointer clicked", "Pointer event mapped"],
        ["Focus transitions", "Validate on focus", "FocusListener", "Focus events reveal when keyboard attention moves and can trigger gentle, non-blocking validation.", "Focus changed", "Focus transition validated"],
        ["Window lifecycle", "Handle WindowAdapter", "WindowListener", "Window events let applications confirm closure, persist geometry, or release resources at the right moment.", "Window event seen", "Window lifecycle handled"],
        ["Live document edits", "Observe DocumentListener", "DocumentListener", "Document events report insertions, removals, and style changes without depending on individual keys.", "Document updated", "Live text synchronized"],
        ["Scheduled behavior", "Animate with Timer", "javax.swing.Timer", "A Swing Timer delivers ticks on the Event Dispatch Thread, making small interface updates safe.", "Timer ticked", "Timer animation running"],
        ["Reliable shortcuts", "Prefer key bindings", "InputMap / ActionMap", "Key bindings connect keystrokes to named actions and work more reliably than raw key listeners.", "Shortcut invoked", "Keyboard shortcut active"]
      ]
    },
    {
      name: "Models and state",
      kind: "model",
      note: "Swing components become easier to reason about when the model owns data and the view only presents and edits that model.",
      lessons: [
        ["Button state", "Inspect ButtonModel", "ButtonModel", "ButtonModel records armed, pressed, enabled, rollover, and selected states behind a button.", "Button model read", "Button state separated"],
        ["Dynamic lists", "Use DefaultListModel", "DefaultListModel", "DefaultListModel provides mutable list data with notifications that keep JList synchronized.", "Model item added", "Dynamic list updated"],
        ["Editable choices", "Build DefaultComboBoxModel", "DefaultComboBoxModel", "A combo-box model manages available items and the current selected item independently.", "Combo model changed", "Choice model refreshed"],
        ["Tabular data", "Extend AbstractTableModel", "TableModel", "Table models answer row, column, value, and editability questions for a JTable.", "Table value updated", "Table model understood"],
        ["Numeric boundaries", "Configure SpinnerNumberModel", "SpinnerModel", "Spinner models centralize the current value, minimum, maximum, and step size.", "Spinner advanced", "Numeric model constrained"],
        ["Shared ranges", "Use BoundedRangeModel", "BoundedRangeModel", "Sliders, progress bars, and scrollbars share a model for a bounded integer range.", "Range adjusted", "Bounded value synchronized"],
        ["Hierarchical data", "Build DefaultTreeModel", "TreeModel", "Tree models expose parent-child relationships while JTree handles expansion and selection.", "Tree node added", "Hierarchy model updated"],
        ["Text as a model", "Work with Document", "Document", "Swing text components store content in a Document that can be shared, filtered, and observed.", "Document shared", "Text model connected"],
        ["Selection as state", "Inspect ListSelectionModel", "ListSelectionModel", "Selection models define single, interval, or multiple selection without changing list data.", "Selection mode set", "Selection state isolated"],
        ["Domain-first design", "Create a custom model", "Custom model", "A focused domain model keeps business rules outside listeners and makes the interface easier to test.", "Domain state changed", "Custom model driving the view"]
      ]
    },
    {
      name: "Dialogs and commands",
      kind: "dialog",
      note: "Use dialogs for brief, interruptive decisions. Keep routine feedback in the main window where it does not break the user's flow.",
      lessons: [
        ["Helpful messages", "Show message dialogs", "showMessageDialog", "Message dialogs communicate a short result and can signal information, warnings, or errors.", "Operation finished", "Message delivered clearly"],
        ["Quick prompts", "Request dialog input", "showInputDialog", "Input dialogs are convenient for one small value but scale poorly for forms or validation-heavy tasks.", "Name requested", "Dialog input captured"],
        ["Confirm risky actions", "Use confirm dialogs", "showConfirmDialog", "Confirmation dialogs should explain the consequence and reserve the safest choice as the default.", "Confirmation requested", "Risky action confirmed"],
        ["Choose a file", "Configure JFileChooser", "JFileChooser", "File choosers provide platform-aware navigation and return an approved file without reading it for you.", "File chosen", "File selection ready"],
        ["Pick a color", "Open JColorChooser", "JColorChooser", "Color choosers return a Color value that can immediately update a preview or stored preference.", "Color selected", "Theme color applied"],
        ["Application menus", "Assemble JMenuBar", "JMenuBar", "Menu bars organize infrequent commands into familiar categories and support keyboard navigation.", "Menu command chosen", "Application menu connected"],
        ["Keyboard accelerators", "Add menu shortcuts", "KeyStroke", "Accelerators expose common commands without requiring users to open a menu first.", "Accelerator pressed", "Menu shortcut registered"],
        ["Context actions", "Create JPopupMenu", "JPopupMenu", "Popup menus place a small set of context-specific actions beside the object they affect.", "Context menu opened", "Context action available"],
        ["Visible commands", "Build JToolBar", "JToolBar", "Toolbars surface frequent actions with icons, labels, tooltips, and shared Action objects.", "Toolbar action clicked", "Frequent command surfaced"],
        ["Quiet feedback", "Design a status bar", "Status JLabel", "A status bar reports durable, non-urgent feedback without interrupting the current task.", "Status changed", "Status feedback anchored"]
      ]
    },
    {
      name: "Tables, trees, and panes",
      kind: "data",
      note: "Data-rich components depend on models, renderers, editors, and selection policies. Change one layer at a time.",
      lessons: [
        ["Your first table", "Display JTable data", "JTable", "JTable renders a two-dimensional model with columns, rows, selection, and optional editing.", "Table loaded", "Table data visible"],
        ["Useful columns", "Configure TableColumn", "TableColumn", "Column objects control width, identifiers, renderers, editors, and display order.", "Column resized", "Columns configured"],
        ["Custom cell display", "Implement TableCellRenderer", "TableCellRenderer", "Renderers reuse a component to paint each cell without storing thousands of child components.", "Renderer installed", "Cells styled by value"],
        ["Purposeful editing", "Implement TableCellEditor", "TableCellEditor", "Editors temporarily place an interactive component over a cell and commit a typed value to the model.", "Editor opened", "Cell edit committed"],
        ["Sortable rows", "Enable TableRowSorter", "TableRowSorter", "A row sorter maps model rows to view rows and adds sortable column headers.", "Rows sorted", "Sorting enabled"],
        ["Filter visible rows", "Apply RowFilter", "RowFilter", "Row filters decide which model entries appear without deleting data from the underlying model.", "Filter applied", "Visible rows narrowed"],
        ["Table selection", "Read selected rows", "JTable selection", "Selected view indexes may need conversion back to model indexes after sorting or filtering.", "Row selected", "Selection mapped to model"],
        ["Your first tree", "Display JTree data", "JTree", "JTree presents hierarchical nodes and delegates expansion, selection, and rendering to dedicated models.", "Tree loaded", "Hierarchy displayed"],
        ["Tree navigation", "Handle TreeSelectionListener", "TreeSelectionListener", "Tree selection paths identify every node from the root to the current selection.", "Tree path selected", "Tree navigation connected"],
        ["Resizable regions", "Divide with JSplitPane", "JSplitPane", "Split panes let users allocate space between two important regions while preserving both contexts.", "Divider moved", "Workspace regions balanced"]
      ]
    },
    {
      name: "Styling and painting",
      kind: "style",
      note: "Style should clarify hierarchy and state. Respect the active look and feel before overriding low-level colors or dimensions.",
      lessons: [
        ["Meaningful borders", "Compose Swing borders", "BorderFactory", "Borders add spacing, grouping, titles, or focus without requiring wrapper components.", "Border applied", "Section boundary clarified"],
        ["Readable type", "Set fonts carefully", "Font", "Derive fonts from the look and feel so size or emphasis changes preserve the platform family.", "Font changed", "Type hierarchy improved"],
        ["Color with intent", "Apply foreground and background", "Color", "Colors should reinforce state and remain readable across enabled, disabled, and selected conditions.", "Color updated", "Color communicates state"],
        ["Global defaults", "Customize UIManager", "UIManager", "UIManager defaults adjust an application consistently when set before components are created.", "Default changed", "Global style applied"],
        ["Custom drawing", "Override paintComponent", "paintComponent", "Custom components draw inside paintComponent after honoring Swing's painting lifecycle.", "Panel painted", "Custom painting visible"],
        ["Sharper graphics", "Use Graphics2D", "Graphics2D", "Graphics2D adds transforms, strokes, shapes, and rendering hints to ordinary component painting.", "Shape drawn", "Graphics rendered smoothly"],
        ["Scalable iconography", "Implement Icon", "Icon", "The Icon interface can paint lightweight, reusable graphics at a known width and height.", "Icon painted", "Reusable icon complete"],
        ["Transparent layers", "Understand opacity", "setOpaque", "Opaque components fill their backgrounds; transparent components allow parent painting to show through.", "Opacity changed", "Layering behaves correctly"],
        ["Disabled states", "Communicate availability", "setEnabled", "Disabled controls should look unavailable and have an explanation when the reason is not obvious.", "Control disabled", "Availability communicated"],
        ["Platform appearance", "Choose a LookAndFeel", "LookAndFeel", "A look and feel supplies delegates that paint standard components consistently across the application.", "Theme selected", "Look and feel initialized"]
      ]
    },
    {
      name: "Responsive applications",
      kind: "thread",
      note: "The Event Dispatch Thread must remain free to process paint and input events. Move blocking work away, then publish small updates back.",
      lessons: [
        ["The EDT contract", "Respect the UI thread", "Event Dispatch Thread", "Most Swing component access belongs on the single Event Dispatch Thread to avoid subtle race conditions.", "EDT confirmed", "UI thread rule applied"],
        ["Small scheduled updates", "Use Swing Timer", "Timer", "Swing timers are ideal for brief periodic UI changes because their callbacks already run on the EDT.", "Progress advanced", "Timer update scheduled"],
        ["Background work", "Start SwingWorker", "SwingWorker", "SwingWorker performs slow work off the EDT and delivers completion back on the UI thread.", "Worker started", "Background task completed"],
        ["Visible progress", "Update JProgressBar", "JProgressBar", "Progress bars communicate determinate completion or indeterminate activity without blocking interaction.", "Progress updated", "Progress feedback visible"],
        ["Safe cancellation", "Cancel a worker", "SwingWorker.cancel", "Cooperative cancellation checks interruption between units of work and leaves state consistent.", "Cancellation requested", "Background work cancelled safely"],
        ["Streaming results", "Publish and process chunks", "publish / process", "SwingWorker can publish intermediate chunks and process them in batches on the EDT.", "Chunk published", "Incremental results displayed"],
        ["Avoid noisy updates", "Debounce text changes", "Debounce Timer", "A restarting timer waits for a pause in rapid input before launching expensive work.", "Search delayed", "Rapid input debounced"],
        ["Asynchronous loading", "Populate views later", "Async loading", "Load data in the background, preserve a clear loading state, then swap in results on completion.", "Loading started", "Data loaded without freezing"],
        ["Smooth transitions", "Animate lightweight state", "Animation Timer", "Small timer-driven interpolation can add clarity when every tick remains cheap to paint.", "Animation frame drawn", "Transition running smoothly"],
        ["Find frozen UIs", "Diagnose thread mistakes", "Thread diagnostics", "Thread dumps and EDT assertions reveal blocking calls and component access from the wrong thread.", "Thread inspected", "EDT problem identified"]
      ]
    },
    {
      name: "Architecture and quality",
      kind: "architecture",
      note: "Structure is valuable when it makes behavior easier to change, test, and explain. Introduce boundaries around real responsibilities.",
      lessons: [
        ["Model–view–controller", "Separate responsibilities", "MVC", "MVC keeps domain state, visual presentation, and user coordination in distinct roles.", "MVC wired", "Responsibilities separated"],
        ["Reusable commands", "Share Action objects", "AbstractAction", "An Action shares text, icon, enabled state, and behavior between buttons, menus, and shortcuts.", "Action executed", "Command reused"],
        ["Undoable changes", "Use command history", "UndoManager", "Undo support records reversible edits and exposes consistent undo and redo commands.", "Edit recorded", "Undo history working"],
        ["Friendly validation", "Validate form values", "InputVerifier", "Validation should explain how to recover and avoid trapping keyboard focus without necessity.", "Input checked", "Form values validated"],
        ["Form composition", "Build reusable fields", "Form panel", "A reusable field row binds label, input, help, and validation while preserving consistent alignment.", "Form assembled", "Reusable form complete"],
        ["Screen navigation", "Coordinate view changes", "Card navigation", "A small navigation controller keeps CardLayout names and transition logic out of individual screens.", "Screen changed", "Navigation centralized"],
        ["Remember preferences", "Persist lightweight settings", "Preferences API", "The Preferences API stores small user choices such as window position, theme, or recent paths.", "Preference stored", "User setting restored"],
        ["International text", "Load ResourceBundle", "ResourceBundle", "Resource bundles separate translated strings from component construction and support locale changes.", "Bundle loaded", "Interface text localized"],
        ["Accessible interfaces", "Add names and mnemonics", "AccessibleContext", "Accessible names, label relationships, mnemonics, and keyboard order help more users operate the app.", "Accessible name set", "Keyboard access improved"],
        ["Testable behavior", "Test models and actions", "UI testing", "Test domain models and Action behavior directly, then reserve robot tests for essential end-to-end flows.", "Behavior tested", "Interaction verified"]
      ]
    },
    {
      name: "Portfolio projects",
      kind: "project",
      note: "Build a small complete loop first—input, state, output—then add one feature at a time while the application remains runnable.",
      lessons: [
        ["Calculator", "Compose numeric actions", "Calculator", "Combine a display, digit actions, operation state, and predictable error handling in a compact utility.", "Calculation ready", "Calculator result produced"],
        ["Pomodoro timer", "Coordinate time and state", "Pomodoro", "Model work and rest phases explicitly, then let a Swing Timer refresh the visible countdown.", "Focus session ready", "Pomodoro cycle started"],
        ["Notes editor", "Save editable text", "Notes", "Place a JTextArea at the center, connect file actions, and track whether the document has unsaved edits.", "Note is unsaved", "Note saved successfully"],
        ["Contact manager", "Edit structured records", "Contacts", "Use a list for identity, a form for details, and a model that owns add, edit, and delete rules.", "Contact drafted", "Contact added"],
        ["Expense tracker", "Summarize table data", "Expenses", "Capture transactions into a table model and derive totals from model values instead of label text.", "Expense entered", "Running total updated"],
        ["Weather dashboard", "Present loading states", "Weather mock", "Design loading, result, empty, and error states even when the sample data is local.", "Forecast loading", "Forecast displayed"],
        ["Image viewer", "Navigate visual files", "Image viewer", "Fit icons into a scrollable viewport and keep file navigation separate from image decoding.", "Image selected", "Image view updated"],
        ["Markdown editor", "Coordinate editor and preview", "Markdown editor", "Use a split pane for source and preview, then debounce rendering while the user types.", "Preview waiting", "Markdown preview refreshed"],
        ["Quiz game", "Model questions and score", "Quiz", "Represent each question as data and let one controller advance state, evaluate answers, and update score.", "Answer selected", "Score advanced"],
        ["Inventory manager", "Bring the course together", "Inventory", "Combine validated forms, a sortable table model, reusable actions, persistence, and background loading.", "Inventory item drafted", "Inventory item saved"]
      ]
    }
  ];

  const javaString = (value) => value.replaceAll("\\", "\\\\").replaceAll('"', '\\"');

  function componentExpression(focus) {
    const expressions = {
      JLabel: 'new JLabel("A helpful label")',
      JButton: 'new JButton("Primary action")',
      ImageIcon: 'new JLabel("Icon + accessible text", new ImageIcon(), JLabel.CENTER)',
      JTextField: 'new JTextField("Editable text", 15)',
      JPasswordField: 'new JPasswordField("secret", 15)',
      JTextArea: 'new JTextArea("A longer note can live here.", 3, 18)',
      JCheckBox: 'new JCheckBox("Send me updates", true)',
      JRadioButton: 'new JRadioButton("Choice A", true)',
      JComboBox: 'new JComboBox<>(new String[]{"Small", "Medium", "Large"})',
      JList: 'new JList<>(new String[]{"Alpha", "Beta", "Gamma"})'
    };
    return expressions[focus] || `new JLabel("${javaString(focus)} demo")`;
  }

  function componentCode(spec) {
    const [title, , focus, , starter] = spec;
    return `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${javaString(title)} Lab");
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 14));
            JComponent demo = ${componentExpression(focus)};
            JLabel status = new JLabel("Ready");
            JButton apply = new JButton("Try ${javaString(focus)}");

            apply.addActionListener(e -> status.setText("${javaString(starter)}"));
            panel.add(demo);
            panel.add(apply);
            panel.add(status);
            frame.add(panel);
            frame.setSize(420, 220);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`;
  }

  function layoutCode(spec) {
    const [title, , focus, , starter] = spec;
    let layout = "new FlowLayout(FlowLayout.CENTER, 8, 8)";
    if (focus === "BorderLayout") layout = "new BorderLayout(8, 8)";
    if (focus === "GridLayout") layout = "new GridLayout(2, 2, 8, 8)";
    if (focus === "GridBagLayout") layout = "new GridBagLayout()";
    if (focus === "CardLayout") layout = "new CardLayout()";
    if (focus === "SpringLayout") layout = "new SpringLayout()";

    const boxSetup = focus.includes("BoxLayout")
      ? `JPanel demo = new JPanel();\n            demo.setLayout(new BoxLayout(demo, ${focus}));`
      : `JPanel demo = new JPanel(${layout});`;

    return `import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${javaString(title)}");
            ${boxSetup}
            demo.setBorder(new EmptyBorder(10, 10, 10, 10));
            demo.add(new JButton("One"));
            demo.add(new JButton("Two"));
            demo.add(new JButton("Three"));
            demo.add(new JButton("Four"));

            JLabel status = new JLabel("Ready", SwingConstants.CENTER);
            JButton apply = new JButton("Inspect layout");
            apply.addActionListener(e -> status.setText("${javaString(starter)}"));
            JPanel footer = new JPanel(new FlowLayout());
            footer.add(apply);
            footer.add(status);
            frame.add(demo, BorderLayout.CENTER);
            frame.add(footer, BorderLayout.SOUTH);
            frame.setSize(430, 260);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`;
  }

  function eventCode(spec) {
    const [title, , focus, , starter] = spec;
    return `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${javaString(title)}");
            JLabel status = new JLabel("Waiting for an event", SwingConstants.CENTER);
            JTextField input = new JTextField("Edit or press the button", 18);
            JButton trigger = new JButton("Trigger ${javaString(focus)}");

            // ${focus} turns a user gesture into application behavior.
            trigger.addActionListener(e -> status.setText("${javaString(starter)}"));

            JPanel panel = new JPanel(new FlowLayout());
            panel.add(input);
            panel.add(trigger);
            frame.add(status, BorderLayout.CENTER);
            frame.add(panel, BorderLayout.SOUTH);
            frame.setSize(430, 210);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`;
  }

  function modelCode(spec) {
    const [title, , focus, , starter] = spec;
    return `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${javaString(title)}");
            DefaultListModel<String> model = new DefaultListModel<>();
            model.addElement("First model value");
            model.addElement("Second model value");
            JList<String> view = new JList<>(model);
            JLabel status = new JLabel("Model and view are separate");
            JButton update = new JButton("Update ${javaString(focus)}");

            update.addActionListener(e -> {
                model.addElement("${javaString(starter)}");
                status.setText("${javaString(starter)}");
            });

            frame.add(new JScrollPane(view), BorderLayout.CENTER);
            JPanel footer = new JPanel(new FlowLayout());
            footer.add(update);
            footer.add(status);
            frame.add(footer, BorderLayout.SOUTH);
            frame.setSize(440, 260);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`;
  }

  function dialogCode(spec) {
    const [title, , focus, , starter] = spec;
    return `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${javaString(title)}");
            JLabel status = new JLabel("Dialog closed", SwingConstants.CENTER);
            JButton open = new JButton("Open ${javaString(focus)}");

            open.addActionListener(e -> {
                JOptionPane.showMessageDialog(frame, "${javaString(starter)}");
                status.setText("${javaString(starter)}");
            });

            frame.add(status, BorderLayout.CENTER);
            frame.add(open, BorderLayout.SOUTH);
            frame.setSize(390, 190);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`;
  }

  function dataCode(spec) {
    const [title, , focus, , starter] = spec;
    const isTree = focus.includes("Tree") || focus === "JTree";
    const dataView = isTree
      ? `JTree data = new JTree();`
      : `String[] columns = {"Name", "Status"};
            Object[][] rows = {{"Alpha", "Ready"}, {"Beta", "Learning"}};
            JTable data = new JTable(rows, columns);`;
    return `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${javaString(title)}");
            ${dataView}
            JLabel status = new JLabel("Data view ready");
            JButton inspect = new JButton("Inspect ${javaString(focus)}");
            inspect.addActionListener(e -> status.setText("${javaString(starter)}"));

            frame.add(new JScrollPane(data), BorderLayout.CENTER);
            JPanel footer = new JPanel(new FlowLayout());
            footer.add(inspect);
            footer.add(status);
            frame.add(footer, BorderLayout.SOUTH);
            frame.setSize(460, 280);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`;
  }

  function styleCode(spec) {
    const [title, , focus, , starter] = spec;
    return `import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${javaString(title)}");
            JLabel sample = new JLabel("${javaString(focus)} preview", SwingConstants.CENTER);
            sample.setOpaque(true);
            sample.setBackground(new Color(225, 238, 230));
            sample.setForeground(new Color(32, 91, 67));
            sample.setFont(sample.getFont().deriveFont(Font.BOLD, 18f));
            sample.setBorder(new EmptyBorder(18, 18, 18, 18));
            JButton apply = new JButton("Apply style");
            apply.addActionListener(e -> sample.setText("${javaString(starter)}"));

            frame.add(sample, BorderLayout.CENTER);
            frame.add(apply, BorderLayout.SOUTH);
            frame.setSize(420, 220);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`;
  }

  function threadCode(spec) {
    const [title, , focus, , starter] = spec;
    return `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${javaString(title)}");
            JProgressBar progress = new JProgressBar(0, 100);
            progress.setValue(20);
            progress.setStringPainted(true);
            JLabel status = new JLabel("UI remains responsive");
            JButton work = new JButton("Run ${javaString(focus)} demo");

            work.addActionListener(e -> {
                progress.setValue(progress.getValue() + 20);
                status.setText("${javaString(starter)}");
            });

            JPanel panel = new JPanel(new FlowLayout());
            panel.add(progress);
            panel.add(work);
            panel.add(status);
            frame.add(panel);
            frame.setSize(470, 200);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`;
  }

  function architectureCode(spec) {
    const [title, , focus, , starter] = spec;
    return `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${javaString(title)}");
            JTextField input = new JTextField("Sample value", 16);
            JLabel status = new JLabel("${javaString(focus)} ready");
            JButton save = new JButton("Validate and save");

            save.addActionListener(e -> {
                String value = input.getText().trim();
                status.setText(value.isEmpty() ? "Value required" : "${javaString(starter)}");
            });

            JPanel form = new JPanel(new FlowLayout());
            form.add(new JLabel("Value:"));
            form.add(input);
            form.add(save);
            frame.add(form, BorderLayout.CENTER);
            frame.add(status, BorderLayout.SOUTH);
            frame.setSize(460, 210);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`;
  }

  function projectCode(spec) {
    const [title, , focus, , starter] = spec;
    return `import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("${javaString(title)}");
            DefaultListModel<String> model = new DefaultListModel<>();
            model.addElement("${javaString(focus)} starter item");
            JList<String> items = new JList<>(model);
            JTextField input = new JTextField("New item", 15);
            JLabel status = new JLabel("Project ready");
            JButton add = new JButton("Add");

            add.addActionListener(e -> {
                String value = input.getText().trim();
                if (!value.isEmpty()) model.addElement(value);
                status.setText("${javaString(starter)}");
                input.setText("");
            });

            JPanel composer = new JPanel(new FlowLayout());
            composer.add(input);
            composer.add(add);
            composer.add(status);
            frame.add(new JScrollPane(items), BorderLayout.CENTER);
            frame.add(composer, BorderLayout.SOUTH);
            frame.setSize(470, 290);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}`;
  }

  const factories = {
    component: componentCode,
    layout: layoutCode,
    event: eventCode,
    model: modelCode,
    dialog: dialogCode,
    data: dataCode,
    style: styleCode,
    thread: threadCode,
    architecture: architectureCode,
    project: projectCode
  };

  function buildLesson(spec, module, moduleIndex, lessonIndex) {
    const [title, subtitle, focus, detail, starter, goal] = spec;
    return {
      module: module.name,
      moduleIndex: moduleIndex + 2,
      navTitle: title,
      navSubtitle: subtitle,
      time: `${6 + ((moduleIndex + lessonIndex) % 5)} MIN`,
      title,
      description: `${detail} In this lab, you will change the example and inspect the result immediately in the Swing simulator.`,
      tags: [focus, module.name.split(" ")[0], "Hands-on"],
      conceptTitle: `${focus}: ${subtitle}`,
      conceptBody: [
        `${detail}`,
        `Run the starter first, interact with the preview, and then make one focused change. The short feedback loop is the fastest way to turn <code>${focus}</code> from a name into a usable mental model.`
      ],
      mentorNote: module.note,
      points: [
        ["Purpose", `Know what problem ${focus} is meant to solve.`],
        ["Connection", "Notice how the component, model, layout, and listener cooperate."],
        ["Feedback", "Run after one small edit so the result is easy to explain."]
      ],
      challengeTitle: `Change the ${focus} result`,
      challengeText: `In the editor, replace <code>${starter}</code> with <code>${goal}</code>, then run the code.`,
      challengeTest: (code) => code.includes(`"${goal}"`),
      challengeSolution: (code) => code.replaceAll(`"${starter}"`, `"${goal}"`),
      code: factories[module.kind](spec)
    };
  }

  window.buildSwingcraftExtraLessons = function () {
    return modules.flatMap((module, moduleIndex) =>
      module.lessons.map((spec, lessonIndex) => buildLesson(spec, module, moduleIndex, lessonIndex))
    );
  };
})();
