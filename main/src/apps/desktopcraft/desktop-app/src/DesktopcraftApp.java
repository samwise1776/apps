import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Base64;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class DesktopcraftApp {
    private static Color NAV = new Color(23, 37, 31);
    private static Color NAV_ACTIVE = new Color(42, 67, 56);
    private static Color CANVAS = new Color(245, 243, 236);
    private static Color PAPER = new Color(255, 254, 249);
    private static Color INK = new Color(31, 41, 37);
    private static Color MUTED = new Color(104, 115, 109);
    private static Color GREEN = new Color(46, 125, 91);
    private static Color GREEN_SOFT = new Color(223, 238, 230);
    private static Color LIME = new Color(216, 239, 114);
    private static Color ORANGE_SOFT = new Color(255, 240, 228);
    private static Color EDITOR = new Color(22, 33, 28);
    private static Color EDITOR_BAR = new Color(32, 46, 39);
    private static final String CUSTOM_LESSON_COUNT_KEY = "custom.lesson.count.v1";
    private static final String CREATION_BAN_KEY = "custom.lesson.creation.banned.v1";
    private static final String FORUM_TOPIC_COUNT_KEY = "forum.topic.count.v1";
    private static final int PREFERENCE_CHUNK_SIZE = 6000;
    private static final int PASSWORD_ITERATIONS = 210000;
    private static final Pattern PROFANITY_PATTERN = Pattern.compile(
        "(?:^|[^a-z0-9])(?:f[\\W_]*u[\\W_]*c[\\W_]*k(?:er|ing|ed|s)?|s[\\W_]*h[\\W_]*i[\\W_]*t(?:ty|s)?|bitch(?:es|ing)?|ass(?:hole|holes|es)?|bastard(?:s)?|cunt(?:s)?|dick(?:s)?|pussy|whore(?:s)?|slut(?:s)?|damn|crap)(?=$|[^a-z0-9])",
        Pattern.CASE_INSENSITIVE
    );

    private final Preferences prefs = Preferences.userNodeForPackage(DesktopcraftApp.class);
    private final List<Course> courses = loadCourses();
    private final Map<String, Set<Integer>> completed = new HashMap<>();
    private final Map<String, Map<Integer, String>> editedCode = new HashMap<>();
    private final Map<String, Integer> builtInLessonCounts = new HashMap<>();

    private JFrame frame;
    private JComboBox<Course> courseBox;
    private JTextField searchField;
    private DefaultListModel<LessonRef> lessonModel;
    private JList<LessonRef> lessonList;
    private JLabel courseTitle;
    private JLabel progressCopy;
    private JLabel xpLabel;
    private JButton userButton;
    private JProgressBar courseProgress;
    private JEditorPane lessonPane;
    private JTextArea codeArea;
    private JPanel previewHost;
    private JLabel consoleLabel;
    private JLabel fileLabel;
    private JButton completeButton;
    private JButton quizButton;
    private JButton previousButton;
    private JButton nextButton;
    private JButton createLessonButton;
    private Course currentCourse;
    private int activeLesson;
    private String currentUserName = "Guest learner";
    private String currentUsername = "";
    private boolean dataLoaded;

    public static void main(String[] args) {
        if (args.length > 0 && "--verify".equals(args[0])) {
            List<Course> verifiedCourses = loadCourses();
            int lessonCount = verifiedCourses.stream().mapToInt(course -> course.lessons.size()).sum();
            Lesson creatorLesson = customLesson(verifiedCourses.get(0), "Greeting challenge", "starter", "answer", "Hello, builder!");
            boolean creatorVerified = "starter".equals(creatorLesson.code)
                && "answer".equals(creatorLesson.goal)
                && "Hello, builder!".equals(creatorLesson.starter)
                && containsProfanity("crap")
                && !containsProfanity("class JFrameBuilder");
            AppBlueprint makerTest = new AppBlueprint("Java Swing", "Hello Builder", "Test Window", "Ready", "Run", "Working", 520, 320);
            boolean appMakerVerified = buildMakerSource(makerTest).contains("new JFrame(\"Test Window\")")
                && makerFileName(makerTest).equals("HelloBuilder.java")
                && buildMakerSource(new AppBlueprint("Python Tkinter", "Demo", "Window", "Ready", "Run", "Done", 520, 320)).contains("tk.Tk()")
                && buildMakerSource(new AppBlueprint("C# WinForms", "Demo", "Window", "Ready", "Run", "Done", 520, 320)).contains("Application.Run")
                && buildMakerSource(new AppBlueprint("C++ Qt Widgets", "Demo", "Window", "Ready", "Run", "Done", 520, 320)).contains("QApplication")
                && buildMakerSource(new AppBlueprint("JavaScript Electron", "Demo", "Window", "Ready", "Run", "Done", 520, 320)).contains("BrowserWindow");
            boolean accountStorageVerified = verifyPasswordStorage();
            boolean curriculumVerified = verifiedCourses.stream().allMatch(course -> course.lessons.size() == 500);
            boolean quizzesVerified = verifiedCourses.stream().allMatch(course -> {
                List<QuizQuestion> questions = buildLessonQuiz(course, 499);
                return questions.size() == 20 && questions.stream().allMatch(question -> question.options.size() == 3 && !question.explanation.isBlank());
            });
            System.out.println("Desktopcraft courses=" + verifiedCourses.size() + " lessons=" + lessonCount + " quizzes=" + (quizzesVerified ? "verified" : "failed") + " creator=" + (creatorVerified ? "verified" : "failed") + " app-maker=" + (appMakerVerified ? "verified" : "failed") + " account-storage=" + (accountStorageVerified ? "verified" : "failed"));
            if (verifiedCourses.size() != 5 || lessonCount != 2500 || !curriculumVerified || !quizzesVerified || !creatorVerified || !appMakerVerified || !accountStorageVerified) System.exit(1);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            UIManager.put("Label.font", new Font("SansSerif", Font.PLAIN, 13));
            UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 12));
            new DesktopcraftApp().show();
        });
    }

    private void show() {
        applyThemePreferences();
        showLoginDialog();
        if (!dataLoaded) {
            courses.forEach(course -> builtInLessonCounts.put(course.id, course.lessons.size()));
            loadCustomLessons();
            loadProgress();
            dataLoaded = true;
        }
        String preferred = prefs.get("activeCourse", "java-swing");
        currentCourse = courses.stream().filter(course -> course.id.equals(preferred)).findFirst().orElse(courses.get(0));
        activeLesson = Math.min(prefs.getInt("activeLesson." + currentCourse.id, 0), currentCourse.lessons.size() - 1);

        frame = new JFrame("Desktopcraft — Desktop App Tutor");
        frame.setIconImages(buildAppIcons());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1040, 700));
        frame.setSize(1360, 860);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.add(buildSidebar(), BorderLayout.WEST);
        frame.add(buildWorkspace(), BorderLayout.CENTER);
        bindKeyboardShortcuts();
        switchCourse(currentCourse, false);
        frame.setVisible(true);
    }

    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(300, 0));
        sidebar.setBackground(NAV);
        sidebar.setLayout(new BorderLayout());
        sidebar.setBorder(new EmptyBorder(22, 16, 18, 16));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel brand = new JLabel("  Desktopcraft");
        brand.setOpaque(true);
        brand.setBackground(LIME);
        brand.setForeground(NAV);
        brand.setFont(new Font("SansSerif", Font.BOLD, 18));
        brand.setBorder(new EmptyBorder(10, 9, 10, 9));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(brand);
        top.add(Box.createVerticalStrut(20));

        JLabel switchLabel = smallLabel("CURRENT COURSE", LIME);
        switchLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(switchLabel);
        top.add(Box.createVerticalStrut(5));
        courseBox = new JComboBox<>(courses.toArray(new Course[0]));
        courseBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        courseBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        courseBox.addActionListener(event -> {
            Course selected = (Course) courseBox.getSelectedItem();
            if (selected != null && selected != currentCourse) switchCourse(selected, true);
        });
        top.add(courseBox);
        top.add(Box.createVerticalStrut(15));

        progressCopy = new JLabel("0 of 0 lessons");
        progressCopy.setForeground(new Color(173, 188, 178));
        progressCopy.setFont(new Font("SansSerif", Font.PLAIN, 11));
        progressCopy.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(progressCopy);
        top.add(Box.createVerticalStrut(5));
        courseProgress = new JProgressBar(0, 100);
        courseProgress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        courseProgress.setPreferredSize(new Dimension(260, 6));
        courseProgress.setForeground(LIME);
        courseProgress.setBackground(new Color(53, 70, 61));
        courseProgress.setBorderPainted(false);
        courseProgress.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(courseProgress);
        top.add(Box.createVerticalStrut(16));

        searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        searchField.putClientProperty("JTextField.placeholderText", "Search lessons or APIs…");
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { filterLessons(); }
            public void removeUpdate(DocumentEvent event) { filterLessons(); }
            public void changedUpdate(DocumentEvent event) { filterLessons(); }
        });
        top.add(searchField);
        sidebar.add(top, BorderLayout.NORTH);

        lessonModel = new DefaultListModel<>();
        lessonList = new JList<>(lessonModel);
        lessonList.setBackground(NAV);
        lessonList.setForeground(new Color(188, 201, 192));
        lessonList.setSelectionBackground(NAV_ACTIVE);
        lessonList.setSelectionForeground(Color.WHITE);
        lessonList.setFixedCellHeight(54);
        lessonList.setCellRenderer(new LessonRenderer());
        lessonList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && lessonList.getSelectedValue() != null) {
                saveCurrentEdit();
                activeLesson = lessonList.getSelectedValue().index;
                showLesson();
            }
        });
        JScrollPane lessonScroll = new JScrollPane(lessonList);
        lessonScroll.setBorder(new EmptyBorder(16, 0, 12, 0));
        lessonScroll.getViewport().setBackground(NAV);
        lessonScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.add(lessonScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(0, 1, 0, 6));
        bottom.setOpaque(false);
        createLessonButton = darkButton(prefs.getBoolean(CREATION_BAN_KEY, false) ? "Lesson creation banned" : "+  Create lesson");
        createLessonButton.setEnabled(!prefs.getBoolean(CREATION_BAN_KEY, false));
        createLessonButton.addActionListener(event -> showCreateLessonDialog());
        JButton appMaker = darkButton("🛠  App Maker");
        appMaker.addActionListener(event -> showAppMakerDialog());
        JButton tutorials = darkButton("▣  Tutorials");
        tutorials.addActionListener(event -> showTutorialsDialog());
        JButton forum = darkButton("☰  Builder forum");
        forum.addActionListener(event -> showForumDialog());
        JButton helper = darkButton("✦  Helper AI");
        helper.addActionListener(event -> showHelperDialog());
        JButton customization = darkButton("◉  Customize");
        customization.addActionListener(event -> showCustomizationDialog());
        JButton leaderboard = darkButton("🏆  Leaderboard");
        leaderboard.addActionListener(event -> showLeaderboard());
        JButton projects = darkButton("▤  Projects");
        projects.addActionListener(event -> showProjectsDialog());
        JButton community = darkButton("⬇  Community apps");
        community.addActionListener(event -> showCommunityDialog());
        JButton feedback = darkButton("✉  Feedback");
        feedback.addActionListener(event -> showFeedbackDialog());
        JButton about = darkButton("ⓘ  About Desktopcraft");
        about.addActionListener(event -> showAboutDialog());
        JButton reset = darkButton("Reset current course");
        reset.addActionListener(event -> resetCourse());
        bottom.add(createLessonButton);
        bottom.add(appMaker);
        bottom.add(tutorials);
        bottom.add(forum);
        bottom.add(helper);
        bottom.add(customization);
        bottom.add(leaderboard);
        bottom.add(projects);
        bottom.add(community);
        bottom.add(feedback);
        bottom.add(about);
        bottom.add(reset);
        JScrollPane toolScroll = new JScrollPane(bottom);
        toolScroll.setPreferredSize(new Dimension(0, 315));
        toolScroll.setBorder(null);
        toolScroll.getViewport().setBackground(NAV);
        toolScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        toolScroll.getVerticalScrollBar().setUnitIncrement(14);
        sidebar.add(toolScroll, BorderLayout.SOUTH);
        return sidebar;
    }

    private JComponent buildWorkspace() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CANVAS);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CANVAS);
        header.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, new Color(222, 222, 213)), new EmptyBorder(15, 25, 15, 25)));
        courseTitle = new JLabel("Desktop course");
        courseTitle.setForeground(INK);
        courseTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        xpLabel = new JLabel("0 XP");
        xpLabel.setForeground(GREEN);
        xpLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        userButton = secondaryButton(currentUserName);
        userButton.setToolTipText(currentUsername.isBlank() ? "Guest profile" : "@" + currentUsername + " · click to switch account");
        userButton.addActionListener(event -> {
            prefs.remove("session.username");
            prefs.remove("session.email");
            prefs.remove("session.name");
            showLoginDialog();
            userButton.setText(currentUserName);
            userButton.setToolTipText(currentUsername.isBlank() ? "Guest profile" : "@" + currentUsername + " · click to switch account");
        });
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.setOpaque(false);
        headerActions.add(xpLabel);
        headerActions.add(userButton);
        header.add(courseTitle, BorderLayout.WEST);
        header.add(headerActions, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        lessonPane = new JEditorPane("text/html", "");
        lessonPane.setEditable(false);
        lessonPane.setBackground(PAPER);
        lessonPane.setBorder(new EmptyBorder(16, 16, 16, 16));
        JScrollPane lessonScroll = new JScrollPane(lessonPane);
        lessonScroll.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBackground(EDITOR);
        JPanel editorToolbar = new JPanel(new BorderLayout());
        editorToolbar.setBackground(EDITOR_BAR);
        editorToolbar.setBorder(new EmptyBorder(9, 13, 9, 10));
        fileLabel = new JLabel("Main.java");
        fileLabel.setForeground(Color.WHITE);
        fileLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JPanel editorActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        editorActions.setOpaque(false);
        JButton copy = toolbarButton("Copy");
        copy.addActionListener(event -> copyCode());
        JButton reset = toolbarButton("Reset");
        reset.addActionListener(event -> resetCode());
        editorActions.add(copy);
        editorActions.add(reset);
        editorToolbar.add(fileLabel, BorderLayout.WEST);
        editorToolbar.add(editorActions, BorderLayout.EAST);
        editorPanel.add(editorToolbar, BorderLayout.NORTH);

        codeArea = new JTextArea();
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        codeArea.setBackground(EDITOR);
        codeArea.setForeground(new Color(223, 233, 227));
        codeArea.setCaretColor(LIME);
        codeArea.setSelectionColor(GREEN);
        codeArea.setTabSize(4);
        codeArea.setBorder(new EmptyBorder(14, 16, 14, 16));
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(null);
        editorPanel.add(codeScroll, BorderLayout.CENTER);

        JPanel runBar = new JPanel(new BorderLayout());
        runBar.setBackground(EDITOR_BAR);
        runBar.setBorder(new EmptyBorder(8, 12, 8, 12));
        consoleLabel = new JLabel("Ready. Run the code to update the preview.");
        consoleLabel.setForeground(new Color(161, 177, 167));
        consoleLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        JButton run = primaryButton("▶  Run code");
        run.addActionListener(event -> runSimulation());
        runBar.add(consoleLabel, BorderLayout.CENTER);
        runBar.add(run, BorderLayout.EAST);
        editorPanel.add(runBar, BorderLayout.SOUTH);

        previewHost = new JPanel(new GridBagLayout());
        previewHost.setBackground(new Color(231, 233, 228));
        previewHost.setBorder(new TitledBorder(new LineBorder(new Color(209, 213, 208)), " Desktop simulator "));

        JSplitPane practiceSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPanel, previewHost);
        practiceSplit.setResizeWeight(0.62);
        practiceSplit.setDividerSize(7);
        practiceSplit.setBorder(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lessonScroll, practiceSplit);
        mainSplit.setResizeWeight(0.38);
        mainSplit.setDividerSize(7);
        mainSplit.setBorder(new EmptyBorder(20, 22, 12, 22));
        mainSplit.setBackground(CANVAS);
        root.add(mainSplit, BorderLayout.CENTER);

        JPanel navigation = new JPanel(new BorderLayout());
        navigation.setBackground(CANVAS);
        navigation.setBorder(new EmptyBorder(0, 22, 18, 22));
        previousButton = secondaryButton("← Previous");
        previousButton.addActionListener(event -> moveLesson(-1));
        nextButton = secondaryButton("Next →");
        nextButton.addActionListener(event -> moveLesson(1));
        completeButton = primaryButton("Complete lesson ✓");
        completeButton.addActionListener(event -> completeLesson());
        quizButton = secondaryButton("Take lesson quiz");
        quizButton.addActionListener(event -> showLessonQuiz());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(quizButton);
        right.add(completeButton);
        right.add(nextButton);
        navigation.add(previousButton, BorderLayout.WEST);
        navigation.add(right, BorderLayout.EAST);
        root.add(navigation, BorderLayout.SOUTH);
        return root;
    }

    private void switchCourse(Course course, boolean savePrevious) {
        if (savePrevious) {
            saveCurrentEdit();
            saveProgress();
        }
        currentCourse = course;
        prefs.put("activeCourse", course.id);
        activeLesson = Math.min(prefs.getInt("activeLesson." + course.id, 0), course.lessons.size() - 1);
        courseBox.setSelectedItem(course);
        searchField.setText("");
        filterLessons();
        showLesson();
    }

    private void filterLessons() {
        if (currentCourse == null || lessonModel == null) return;
        String query = searchField.getText().trim().toLowerCase();
        lessonModel.clear();
        for (int index = 0; index < currentCourse.lessons.size(); index++) {
            Lesson lesson = currentCourse.lessons.get(index);
            String searchable = (lesson.title + " " + lesson.api + " " + lesson.module).toLowerCase();
            if (query.isEmpty() || searchable.contains(query)) lessonModel.addElement(new LessonRef(index, lesson));
        }
        for (int row = 0; row < lessonModel.size(); row++) {
            if (lessonModel.get(row).index == activeLesson) {
                lessonList.setSelectedIndex(row);
                lessonList.ensureIndexIsVisible(row);
                break;
            }
        }
    }

    private void showLesson() {
        Lesson lesson = currentCourse.lessons.get(activeLesson);
        courseTitle.setText(currentCourse.language + "  /  " + simpleLessonText(lesson.module) + "  /  " + lesson.api);
        fileLabel.setText(currentCourse.fileName);
        codeArea.setText(editedCode.getOrDefault(currentCourse.id, Map.of()).getOrDefault(activeLesson, lesson.code));
        codeArea.setCaretPosition(0);
        lessonPane.setText(lessonHtml(lesson));
        lessonPane.setCaretPosition(0);
        renderPreview(codeArea.getText());
        consoleLabel.setText("Ready. Run the code to update the preview.");
        previousButton.setEnabled(activeLesson > 0);
        nextButton.setEnabled(activeLesson < currentCourse.lessons.size() - 1);
        completeButton.setText(isCompleted(activeLesson) ? "Completed ✓" : "Complete lesson ✓");
        quizButton.setText(isCompleted(activeLesson) ? "Retake quiz · complete" : "Take lesson quiz");
        prefs.putInt("activeLesson." + currentCourse.id, activeLesson);
        updateProgress();
    }

    private String lessonHtml(Lesson lesson) {
        if ("Custom lessons".equals(lesson.module)) {
            return "<html><body style='font-family:sans-serif;color:#1f2925;background:#fffef9;padding:20px'>" +
                "<p style='font-size:10px;color:#2e7d5b;font-weight:bold'>CUSTOM LESSON · CREATOR STUDIO</p>" +
                "<h1 style='font-family:serif;font-size:34px;font-weight:normal;margin:8px 0'>" + html(lesson.title) + "</h1>" +
                "<p style='color:#68736d;font-size:14px'>" + html(simpleLessonText(lesson.description)) + "</p>" +
                "<hr style='color:#e5e5dd'><h2 style='font-size:17px'>Build toward the expected result</h2>" +
                "<p>Start with the given code. Change it until it matches the saved answer.</p>" +
                "<div style='background:#dfeee6;padding:12px;margin:18px 0'><b>EXPECTED OUTPUT</b><br>" + html(lesson.starter) + "</div>" +
                "<h2 style='font-size:17px'>Quick challenge</h2>" +
                "<div style='background:#fff0e4;padding:12px'>Make the starter code match the creator's answer, then run it.</div>" +
                "</body></html>";
        }
        String level = prefs.get("custom.explanation", "balanced");
        String explanation = "concise".equals(level)
            ? ""
            : "<hr style='color:#e5e5dd'><h2 style='font-size:17px'>The big idea</h2>" +
              "<p>Layouts place the controls. Callbacks make the screen react when someone clicks or types.</p>" +
              "<div style='background:#dfeee6;padding:12px;margin:18px 0'><b>HELPFUL TIP</b><br>Keep callbacks short: read the input, change the data, then show the result.</div>";
        String detail = "detailed".equals(level)
            ? "<h2 style='font-size:17px'>Detailed reference</h2><p><b>API:</b> " + html(lesson.api) + "<br><b>Module:</b> " + html(simpleLessonText(lesson.module)) + "<br><b>Flow:</b> create the control, place it with a layout, connect its event, update state, and confirm visible feedback.</p>"
            : "";
        return "<html><body style='font-family:sans-serif;color:#1f2925;background:#fffef9;padding:20px'>" +
            "<p style='font-size:10px;color:#2e7d5b;font-weight:bold'>LESSON " + String.format("%03d", activeLesson + 1) + " · " + html(lesson.api) + "</p>" +
            "<h1 style='font-family:serif;font-size:34px;font-weight:normal;margin:8px 0'>" + html(lesson.title) + "</h1>" +
            "<p style='color:#68736d;font-size:14px'>" + html(simpleLessonText(lesson.description)) + "</p>" +
            explanation + detail +
            "<h2 style='font-size:17px'>Quick challenge</h2>" +
            "<div style='background:#fff0e4;padding:12px'>Replace <b>" + html(lesson.starter) + "</b> with <b>" + html(lesson.goal) + "</b>, then run the code.</div>" +
            "</body></html>";
    }

    private void runSimulation() {
        String code = codeArea.getText();
        if (!balanced(code, '{', '}') || !balanced(code, '(', ')') || !balanced(code, '[', ']')) {
            consoleLabel.setForeground(new Color(255, 170, 164));
            consoleLabel.setText("Structure check failed: delimiters are unbalanced.");
            return;
        }
        renderPreview(code);
        Lesson lesson = currentCourse.lessons.get(activeLesson);
        boolean passed = "Custom lessons".equals(lesson.module)
            ? normalizeLessonAnswer(code).equals(normalizeLessonAnswer(lesson.goal))
            : code.contains(lesson.goal);
        consoleLabel.setForeground(passed ? LIME : new Color(161, 177, 167));
        consoleLabel.setText(passed && "Custom lessons".equals(lesson.module)
            ? "Answer matched. Expected output: " + lesson.starter
            : passed ? "Challenge complete. Preview updated." : "Simulation ready. Interact with the preview.");
        editedCode.computeIfAbsent(currentCourse.id, ignored -> new HashMap<>()).put(activeLesson, code);
    }

    private void renderPreview(String code) {
        previewHost.removeAll();
        JPanel simulatedWindow = new JPanel(new BorderLayout());
        simulatedWindow.setPreferredSize(new Dimension(400, 180));
        simulatedWindow.setBackground(new Color(244, 244, 244));
        simulatedWindow.setBorder(new CompoundBorder(new LineBorder(new Color(143, 150, 145)), new EmptyBorder(0, 0, 0, 0)));
        JLabel title = new JLabel("  ●  ●  ●                         " + extractTitle(code));
        title.setOpaque(true);
        title.setBackground(new Color(215, 217, 216));
        title.setBorder(new EmptyBorder(7, 8, 7, 8));
        title.setFont(new Font("SansSerif", Font.BOLD, 10));
        simulatedWindow.add(title, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 20));
        controls.setBackground(new Color(244, 244, 244));
        JTextField input = new JTextField("Desktop app", 12);
        JButton action = new JButton("Try " + currentCourse.lessons.get(activeLesson).api);
        JLabel status = new JLabel("Ready");
        action.addActionListener(event -> {
            status.setText(extractResult(code));
            consoleLabel.setText("Desktop event handled in the simulated window.");
        });
        controls.add(input);
        controls.add(action);
        controls.add(status);
        simulatedWindow.add(controls, BorderLayout.CENTER);
        previewHost.add(simulatedWindow);
        previewHost.revalidate();
        previewHost.repaint();
    }

    private String extractTitle(String code) {
        String[] patterns = {
            "new\\s+JFrame\\s*\\(\\s*\"([^\"]*)\"",
            "\\.title\\s*\\(\\s*\"([^\"]*)\"",
            "(?:^|\\s)Text\\s*=\\s*\"([^\"]*)\"",
            "setWindowTitle\\s*\\(\\s*\"([^\"]*)\"",
            "document\\.title\\s*=\\s*\"([^\"]*)\""
        };
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE).matcher(code);
            if (matcher.find()) return matcher.group(1);
        }
        return currentCourse.shortTitle + " Preview";
    }

    private String extractResult(String code) {
        String[] patterns = {
            "(?:setText|config)\\s*\\([^\"]*\"([^\"]*)\"",
            "status\\.Text\\s*=\\s*\"([^\"]*)\"",
            "status->setText\\s*\\(\\s*\"([^\"]*)\"",
            "status\\.textContent\\s*=\\s*\"([^\"]*)\""
        };
        String result = currentCourse.lessons.get(activeLesson).starter;
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(code);
            while (matcher.find()) result = matcher.group(1);
        }
        return result;
    }

    private void completeLesson() {
        completed.computeIfAbsent(currentCourse.id, ignored -> new HashSet<>()).add(activeLesson);
        saveProgress();
        completeButton.setText("Completed ✓");
        updateProgress();
        lessonList.repaint();
        if (activeLesson < currentCourse.lessons.size() - 1) moveLesson(1);
    }

    private void showLessonQuiz() {
        List<QuizQuestion> questions = buildLessonQuiz(currentCourse, activeLesson);
        JDialog dialog = new JDialog(frame, "Lesson quiz · " + currentCourse.lessons.get(activeLesson).title, true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(PAPER);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(PAPER);
        content.setBorder(new EmptyBorder(22, 24, 20, 24));
        JLabel progress = new JLabel();
        progress.setForeground(GREEN);
        progress.setFont(new Font("SansSerif", Font.BOLD, 11));
        JLabel prompt = new JLabel();
        prompt.setFont(new Font("SansSerif", Font.BOLD, 15));
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setOpaque(false);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        prompt.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.add(progress);
        heading.add(Box.createVerticalStrut(10));
        heading.add(prompt);
        content.add(heading, BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBackground(PAPER);
        content.add(optionsPanel, BorderLayout.CENTER);

        JLabel feedback = new JLabel(" ");
        feedback.setVerticalAlignment(SwingConstants.TOP);
        feedback.setForeground(MUTED);
        JButton check = primaryButton("Check answer");
        JButton next = primaryButton("Next question →");
        next.setVisible(false);
        JButton close = secondaryButton("Close");
        close.addActionListener(event -> dialog.dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(close);
        actions.add(check);
        actions.add(next);
        JPanel footer = new JPanel(new BorderLayout(0, 12));
        footer.setOpaque(false);
        footer.add(feedback, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.SOUTH);
        content.add(footer, BorderLayout.SOUTH);
        dialog.add(content, BorderLayout.CENTER);

        int[] questionIndex = {0};
        int[] score = {0};
        boolean[] answered = {false};
        boolean[] finished = {false};
        ButtonGroup[] currentGroup = {new ButtonGroup()};
        List<JRadioButton> optionButtons = new ArrayList<>();
        Runnable[] renderQuestion = new Runnable[1];
        renderQuestion[0] = () -> {
            QuizQuestion question = questions.get(questionIndex[0]);
            progress.setText("QUESTION " + (questionIndex[0] + 1) + " OF " + questions.size() + "  ·  " + score[0] + " CORRECT");
            prompt.setText("<html><body style='width:540px'>" + html(simpleLessonText(question.question)) + "</body></html>");
            optionsPanel.removeAll();
            currentGroup[0] = new ButtonGroup();
            optionButtons.clear();
            for (int optionIndex = 0; optionIndex < question.options.size(); optionIndex++) {
                JRadioButton option = new JRadioButton("<html><body style='width:500px'>" + html(simpleLessonText(question.options.get(optionIndex))) + "</body></html>");
                option.setActionCommand(String.valueOf(optionIndex));
                option.setBackground(PAPER);
                option.setBorder(new CompoundBorder(new LineBorder(new Color(222, 225, 218)), new EmptyBorder(10, 11, 10, 11)));
                option.setAlignmentX(Component.LEFT_ALIGNMENT);
                option.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
                currentGroup[0].add(option);
                optionButtons.add(option);
                optionsPanel.add(option);
                optionsPanel.add(Box.createVerticalStrut(8));
            }
            feedback.setText(" ");
            feedback.setForeground(MUTED);
            check.setVisible(true);
            next.setVisible(false);
            next.setText(questionIndex[0] == questions.size() - 1 ? "Finish quiz →" : "Next question →");
            answered[0] = false;
            optionsPanel.revalidate();
            optionsPanel.repaint();
        };

        check.addActionListener(event -> {
            if (answered[0] || finished[0]) return;
            ButtonModel selected = currentGroup[0].getSelection();
            if (selected == null) {
                feedback.setForeground(new Color(136, 70, 47));
                feedback.setText("Choose an answer before checking.");
                return;
            }
            QuizQuestion question = questions.get(questionIndex[0]);
            int selectedIndex = Integer.parseInt(selected.getActionCommand());
            boolean correct = selectedIndex == question.answer;
            if (correct) score[0]++;
            answered[0] = true;
            for (int index = 0; index < optionButtons.size(); index++) {
                JRadioButton option = optionButtons.get(index);
                option.setEnabled(false);
                if (index == question.answer) option.setBackground(GREEN_SOFT);
                else if (index == selectedIndex) option.setBackground(ORANGE_SOFT);
            }
            progress.setText("QUESTION " + (questionIndex[0] + 1) + " OF " + questions.size() + "  ·  " + score[0] + " CORRECT");
            feedback.setForeground(correct ? GREEN : new Color(136, 70, 47));
            feedback.setText(correct
                ? "<html><body style='width:540px'><b>Correct.</b> " + html(simpleLessonText(question.explanation)) + "</body></html>"
                : "<html><body style='width:540px'><b>Not quite. The correct answer is “" + html(simpleLessonText(question.options.get(question.answer))) + ".”</b><br>" + html(simpleLessonText(question.explanation)) + "</body></html>");
            check.setVisible(false);
            next.setVisible(true);
        });

        next.addActionListener(event -> {
            if (finished[0]) {
                questionIndex[0] = 0;
                score[0] = 0;
                finished[0] = false;
                renderQuestion[0].run();
                return;
            }
            if (!answered[0]) return;
            if (questionIndex[0] < questions.size() - 1) {
                questionIndex[0]++;
                renderQuestion[0].run();
                return;
            }
            finished[0] = true;
            boolean perfect = score[0] == questions.size();
            progress.setText("QUIZ COMPLETE  ·  " + score[0] + " OF " + questions.size() + " CORRECT");
            prompt.setText("<html><body style='width:540px'><b>" + score[0] + " / " + questions.size() + "</b><br>" + (perfect ? "Perfect score—this lesson is complete." : "Review the explanations, then retake the quiz when you are ready.") + "</body></html>");
            optionsPanel.removeAll();
            optionsPanel.revalidate();
            optionsPanel.repaint();
            feedback.setText(perfect ? "Excellent work. You answered every question correctly." : (questions.size() - score[0]) + " answer" + (questions.size() - score[0] == 1 ? "" : "s") + " to revisit.");
            feedback.setForeground(perfect ? GREEN : new Color(136, 70, 47));
            check.setVisible(false);
            next.setText("Retake quiz");
            next.setVisible(true);
            if (perfect) {
                completed.computeIfAbsent(currentCourse.id, ignored -> new HashSet<>()).add(activeLesson);
                saveProgress();
                completeButton.setText("Completed ✓");
                quizButton.setText("Retake quiz · complete");
                updateProgress();
                lessonList.repaint();
            }
        });

        renderQuestion[0].run();
        dialog.setSize(650, 590);
        dialog.setMinimumSize(new Dimension(610, 540));
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private static List<QuizQuestion> buildLessonQuiz(Course course, int lessonIndex) {
        Lesson lesson = course.lessons.get(lessonIndex);
        List<QuizQuestion> questions = new ArrayList<>();
        String[][] prompts = {
            {"Which topic is the focus of this lesson?", "Which API or concept is central to the lesson?", "Which result completes the coding challenge?", "Which curriculum module contains this lesson?", "Which summary describes the active lesson?", "Which response appears in the starter code?"},
            {"Select the title of the lesson you are reviewing.", "Select the API practiced in the code example.", "Select the challenge's required finished result.", "Select the lesson's module.", "Select the matching lesson description.", "Select the starter response before it is edited."},
            {"Review: what is this lesson teaching?", "Review: which API should you recognize?", "Review: what must the response become?", "Review: where is this lesson organized?", "Review: which description matches this work?", "Review: which text is the starting point?"}
        };
        for (int index = 0; index < 18; index++) {
            int valueType = index % 6;
            int round = index / 6;
            String correct = lessonValue(lesson, valueType);
            String explanation = switch (valueType) {
                case 0 -> "The active lesson is titled “" + lesson.title + ".”";
                case 1 -> lesson.api + " is the API or concept named beside this lesson and used by its example.";
                case 2 -> "The challenge asks you to change the response to “" + lesson.goal + ".”";
                case 3 -> "This lesson is organized under the “" + lesson.module + "” module.";
                case 4 -> "This description matches the goal and example shown in the active lesson.";
                default -> "The starter code begins with “" + lesson.starter + "” before the challenge edit.";
            };
            questions.add(quizQuestion(prompts[round][valueType], correct, nearbyLessonValues(course, lessonIndex, valueType, index + 1), lessonIndex + index, explanation));
        }
        questions.add(quizQuestion(
            "Which programming language is used by this course?",
            course.language,
            List.of("Java", "Python", "C#", "C++", "JavaScript"),
            lessonIndex + 18,
            "This course uses " + course.language + " for its editable desktop examples."
        ));
        questions.add(quizQuestion(
            "Which source filename belongs to this course?",
            course.fileName,
            List.of("Main.java", "app.py", "MainForm.cs", "main.cpp", "renderer.js"),
            lessonIndex + 19,
            "The editor labels this course's source file as “" + course.fileName + ".”"
        ));
        return questions;
    }

    private static List<String> nearbyLessonValues(Course course, int lessonIndex, int valueType, int questionOffset) {
        List<String> values = new ArrayList<>();
        values.add(lessonValue(course.lessons.get(lessonIndex), valueType));
        for (int step = 1; values.size() < 3 && step < course.lessons.size() + 2; step++) {
            int index = Math.floorMod(lessonIndex + questionOffset + step * 7, course.lessons.size());
            String value = lessonValue(course.lessons.get(index), valueType);
            if (!value.isBlank() && !values.contains(value)) values.add(value);
        }
        for (String fallback : List.of("A different desktop concept", "An unrelated project step", "A separate toolkit feature")) {
            if (values.size() < 3 && !values.contains(fallback)) values.add(fallback);
        }
        return values;
    }

    private static String lessonValue(Lesson lesson, int valueType) {
        return switch (valueType) {
            case 0 -> lesson.title;
            case 1 -> lesson.api;
            case 2 -> lesson.goal;
            case 3 -> lesson.module;
            case 4 -> lesson.description;
            default -> lesson.starter;
        };
    }

    private static QuizQuestion quizQuestion(String prompt, String correct, List<String> values, int rotation, String explanation) {
        List<String> options = new ArrayList<>();
        options.add(correct);
        for (String value : values) if (!options.contains(value) && options.size() < 3) options.add(value);
        while (options.size() < 3) options.add("Alternative " + (options.size() + 1));
        int shift = Math.floorMod(rotation, options.size());
        List<String> rotated = new ArrayList<>();
        rotated.addAll(options.subList(shift, options.size()));
        rotated.addAll(options.subList(0, shift));
        return new QuizQuestion(prompt, rotated, rotated.indexOf(correct), explanation);
    }

    private void moveLesson(int delta) {
        saveCurrentEdit();
        activeLesson = Math.max(0, Math.min(activeLesson + delta, currentCourse.lessons.size() - 1));
        filterLessons();
        showLesson();
    }

    private void updateProgress() {
        int count = completed.getOrDefault(currentCourse.id, Set.of()).size();
        int percent = Math.round(count * 100f / currentCourse.lessons.size());
        progressCopy.setText(count + " of " + currentCourse.lessons.size() + " lessons");
        courseProgress.setValue(percent);
        int totalLessons = courses.stream().mapToInt(course -> course.lessons.size()).sum();
        xpLabel.setText(totalCompleted() * 100 + " XP  ·  " + totalCompleted() + " / " + totalLessons + " lessons");
    }

    private void showLeaderboard() {
        saveProgress();
        List<Builder> builders = new ArrayList<>();
        Set<String> usernames = new HashSet<>();
        try {
            for (String key : prefs.keys()) {
                if (!key.startsWith("account.") || !key.endsWith(".username")) continue;
                String id = key.substring("account.".length(), key.length() - ".username".length());
                String username = prefs.get(key, "");
                String name = prefs.get("account." + id + ".name", username);
                if (!username.isBlank() && usernames.add(username)) builders.add(leaderboardBuilder(name, username));
            }
        } catch (Exception ignored) {
            // The current signed-in account can still be shown if preference enumeration is unavailable.
        }
        if (!currentUsername.isBlank() && usernames.add(currentUsername)) {
            builders.add(leaderboardBuilder(currentUserName, currentUsername));
        }
        if (builders.isEmpty()) {
            JOptionPane.showMessageDialog(
                frame,
                "Create a Desktopcraft account and complete a lesson to claim the first leaderboard spot.\nNo sample builders or invented scores are used.",
                "Desktopcraft Real Account Leaderboard",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        builders.sort(Comparator.comparingInt(Builder::xp).reversed().thenComparing(Builder::username));
        String[] columns = {"Rank", "Builder", "Username", "Focus", "XP"};
        Object[][] rows = new Object[builders.size()][5];
        for (int index = 0; index < builders.size(); index++) {
            Builder builder = builders.get(index);
            rows[index] = new Object[]{index + 1, builder.name, "@" + builder.username, builder.focus, String.format("%,d", builder.xp)};
        }
        JTable table = new JTable(rows, columns);
        table.setRowHeight(30);
        table.setEnabled(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(720, Math.min(420, 68 + builders.size() * 30)));
        JOptionPane.showMessageDialog(frame, scroll, "Desktopcraft Real Account Leaderboard", JOptionPane.PLAIN_MESSAGE);
    }

    private Builder leaderboardBuilder(String name, String username) {
        String id = accountId(username);
        int total = 0;
        int strongestCount = 0;
        String focus = "Start a course";
        for (Course course : courses) {
            int courseCompleted = Math.max(0, Math.min(500, prefs.getInt("leaderboard." + id + ".completed." + course.id, 0)));
            total += courseCompleted;
            if (courseCompleted > strongestCount) {
                strongestCount = courseCompleted;
                focus = course.shortTitle;
            }
        }
        return new Builder(name, username, focus, total * 100);
    }

    private String strongestCourse() {
        return courses.stream().max(Comparator.comparingInt(course -> completed.getOrDefault(course.id, Set.of()).size()))
            .map(course -> course.shortTitle).orElse("Start a course");
    }

    private int totalCompleted() {
        return completed.values().stream().mapToInt(Set::size).sum();
    }

    private boolean isCompleted(int index) {
        return completed.getOrDefault(currentCourse.id, Set.of()).contains(index);
    }

    private void resetCourse() {
        int answer = JOptionPane.showConfirmDialog(frame, "Reset progress and edited code for " + currentCourse.shortTitle + "?", "Reset course", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        completed.remove(currentCourse.id);
        editedCode.remove(currentCourse.id);
        prefs.remove("completed." + currentCourse.id);
        activeLesson = 0;
        saveProgress();
        filterLessons();
        showLesson();
    }

    private void resetCode() {
        editedCode.getOrDefault(currentCourse.id, new HashMap<>()).remove(activeLesson);
        codeArea.setText(currentCourse.lessons.get(activeLesson).code);
        renderPreview(codeArea.getText());
        consoleLabel.setText("Starter code restored.");
    }

    private void copyCode() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(codeArea.getText()), null);
        consoleLabel.setText(currentCourse.language + " code copied to the clipboard.");
    }

    private void saveCurrentEdit() {
        if (currentCourse == null || codeArea == null) return;
        editedCode.computeIfAbsent(currentCourse.id, ignored -> new HashMap<>()).put(activeLesson, codeArea.getText());
    }

    private void showTutorialsDialog() {
        JDialog dialog = new JDialog(frame, "Desktopcraft tutorials", true);
        dialog.setIconImages(buildAppIcons());
        dialog.setLayout(new BorderLayout(12, 12));
        dialog.getContentPane().setBackground(CANVAS);

        DefaultListModel<Course> model = new DefaultListModel<>();
        courses.forEach(model::addElement);
        JList<Course> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedValue(currentCourse, true);
        list.setFixedCellHeight(44);
        JScrollPane courseScroll = new JScrollPane(list);
        courseScroll.setPreferredSize(new Dimension(245, 500));

        JLabel heading = new JLabel();
        heading.setFont(new Font("Serif", Font.PLAIN, 28));
        JLabel progress = smallLabel("STEP 1 OF 5", GREEN);
        JTextArea guide = new JTextArea();
        guide.setEditable(false);
        guide.setLineWrap(true);
        guide.setWrapStyleWord(true);
        guide.setFont(new Font("SansSerif", Font.PLAIN, 14));
        guide.setBackground(PAPER);
        guide.setBorder(new EmptyBorder(14, 14, 14, 14));
        JTextArea example = new JTextArea();
        example.setEditable(false);
        example.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        example.setBackground(EDITOR);
        example.setForeground(new Color(223, 233, 227));
        example.setBorder(new EmptyBorder(14, 14, 14, 14));
        JSplitPane lesson = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(guide), new JScrollPane(example));
        lesson.setResizeWeight(0.46);
        lesson.setBorder(null);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(CANVAS);
        JPanel tutorialHeading = new JPanel();
        tutorialHeading.setLayout(new BoxLayout(tutorialHeading, BoxLayout.Y_AXIS));
        tutorialHeading.setOpaque(false);
        tutorialHeading.add(progress);
        tutorialHeading.add(Box.createVerticalStrut(5));
        tutorialHeading.add(heading);
        center.add(tutorialHeading, BorderLayout.NORTH);
        center.add(lesson, BorderLayout.CENTER);

        JButton previous = secondaryButton("← Previous step");
        JButton next = primaryButton("Next step →");
        JButton open = secondaryButton("Open in lesson studio");
        JButton close = secondaryButton("Close");
        int[] step = {0};
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            Course course = list.getSelectedValue() == null ? courses.get(0) : list.getSelectedValue();
            Lesson source = course.lessons.get(Math.min(step[0], course.lessons.size() - 1));
            heading.setText(course.shortTitle + " quick start");
            progress.setText("STEP " + (step[0] + 1) + " OF 5" + (prefs.getBoolean("tutorial.complete." + course.id, false) ? "  ·  COMPLETE" : ""));
            guide.setText(tutorialGuide(course, step[0], source));
            guide.setCaretPosition(0);
            example.setText(source.code);
            example.setCaretPosition(0);
            previous.setEnabled(step[0] > 0);
            next.setText(step[0] == 4 ? "Complete tutorial ✓" : "Next step →");
        };
        list.addListSelectionListener(event -> { if (!event.getValueIsAdjusting()) { step[0] = 0; render[0].run(); } });
        previous.addActionListener(event -> { step[0] = Math.max(0, step[0] - 1); render[0].run(); });
        next.addActionListener(event -> {
            if (step[0] < 4) step[0]++;
            else prefs.putBoolean("tutorial.complete." + list.getSelectedValue().id, true);
            render[0].run();
        });
        open.addActionListener(event -> {
            Course course = list.getSelectedValue();
            if (course != null) switchCourse(course, true);
            dialog.dispose();
        });
        close.addActionListener(event -> dialog.dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(previous); actions.add(next); actions.add(open); actions.add(close);
        center.add(actions, BorderLayout.SOUTH);

        dialog.add(courseScroll, BorderLayout.WEST);
        dialog.add(center, BorderLayout.CENTER);
        dialog.getRootPane().setBorder(new EmptyBorder(18, 18, 18, 18));
        render[0].run();
        dialog.setSize(940, 680);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private String tutorialGuide(Course course, int step, Lesson lesson) {
        String[] titles = {"Create the window", "Add useful controls", "Connect an event", "Choose a flexible layout", "Run, check, and keep learning"};
        String[] directions = {
            "Start with one main window. Give it a clear title and a sensible size before adding the rest of the interface.",
            "Add a label, input, and button. Each control should have one obvious job and a label people can understand.",
            "Connect the button to a short callback. Read the input, change the app data, and show visible feedback.",
            "Let the toolkit's layout system handle resizing. Group related controls instead of relying on fixed coordinates.",
            "Run the smallest version first, inspect the result, and change one thing at a time. The full course continues with 500 lessons."
        };
        return titles[step] + "\n\n" + directions[step] + "\n\n" + course.shortTitle + " focus: " + simpleLessonText(lesson.api) + ".\n\n" + explanationExtra(course, lesson);
    }

    private String explanationExtra(Course course, Lesson lesson) {
        String level = prefs.get("custom.explanation", "balanced");
        if ("concise".equals(level)) return "Try the example, then continue.";
        String base = "Why it matters: " + simpleLessonText(lesson.description);
        if ("detailed".equals(level)) {
            return base + "\n\nDetailed check: identify the control, the event that runs, the data that changes, and the visible result that proves it worked.";
        }
        return base + "\n\nRun the example and confirm the visible result before moving on.";
    }

    private void showHelperDialog() {
        JDialog dialog = new JDialog(frame, "Desktopcraft Helper AI", true);
        dialog.setIconImages(buildAppIcons());
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(CANVAS);
        JComboBox<Course> course = new JComboBox<>(courses.toArray(new Course[0]));
        course.setSelectedItem(currentCourse);
        JLabel heading = new JLabel("Helper AI · built-in course guide");
        heading.setFont(new Font("Serif", Font.PLAIN, 25));
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(heading, BorderLayout.WEST);
        top.add(course, BorderLayout.EAST);

        JTextArea transcript = new JTextArea(getChunked("helper.history.v1."));
        transcript.setEditable(false);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);
        transcript.setBackground(PAPER);
        transcript.setFont(new Font("SansSerif", Font.PLAIN, 13));
        transcript.setBorder(new EmptyBorder(14, 14, 14, 14));
        if (transcript.getText().isBlank()) transcript.setText("Desktopcraft Helper\nAsk about a window, event, layout, input, frozen interface, error, or what to learn next.\n");

        JTextArea question = new JTextArea(3, 54);
        question.setLineWrap(true);
        question.setWrapStyleWord(true);
        JButton ask = primaryButton("Ask Helper AI");
        JButton clear = secondaryButton("Clear conversation");
        JButton close = secondaryButton("Close");
        ask.addActionListener(event -> {
            String prompt = question.getText().trim();
            if (prompt.length() < 2) return;
            Course selected = (Course) course.getSelectedItem();
            String answer = helperAnswer(selected == null ? currentCourse : selected, prompt);
            transcript.append("\nYou\n" + prompt + "\n\nDesktopcraft Helper\n" + answer + "\n");
            transcript.setCaretPosition(transcript.getDocument().getLength());
            question.setText("");
            putChunked("helper.history.v1.", transcript.getText());
        });
        clear.addActionListener(event -> {
            transcript.setText("Desktopcraft Helper\nConversation cleared. What would you like to build?\n");
            putChunked("helper.history.v1.", transcript.getText());
        });
        close.addActionListener(event -> dialog.dispose());
        JPanel input = new JPanel(new BorderLayout(8, 0));
        input.setOpaque(false);
        input.add(new JScrollPane(question), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
        buttons.setOpaque(false);
        buttons.add(ask); buttons.add(clear); buttons.add(close);
        input.add(buttons, BorderLayout.EAST);

        dialog.add(top, BorderLayout.NORTH);
        dialog.add(new JScrollPane(transcript), BorderLayout.CENTER);
        dialog.add(input, BorderLayout.SOUTH);
        dialog.getRootPane().setBorder(new EmptyBorder(18, 18, 18, 18));
        dialog.setSize(820, 650);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private String helperAnswer(Course course, String question) {
        String query = question.toLowerCase(Locale.ROOT);
        String answer;
        if (query.matches(".*(freeze|frozen|slow|thread|hang|responsive).*")) {
            answer = "Long work blocks the " + course.shortTitle + " interface thread. Move slow I/O or calculations into the toolkit's background-work pattern, then return only the result needed to update the screen.";
        } else if (query.matches(".*(layout|grid|pack|flow|resize|align).*")) {
            answer = "Start with a row, column, or grid layout. Nest small panels for larger forms and test by resizing the window. Avoid fixed coordinates unless you are deliberately drawing an overlay.";
        } else if (query.matches(".*(click|event|listener|signal|slot|callback|command).*")) {
            answer = "Connect one visible control to one short event callback. Inside it, read current input, validate it, update app data, and show feedback.";
        } else if (query.matches(".*(input|textbox|entry|field|validate|value).*")) {
            answer = "Read the input when the action runs, trim it, validate it, and explain exactly what needs fixing. Update saved data only after validation succeeds.";
        } else if (query.matches(".*(debug|error|broken|not work|doesn't).*")) {
            answer = "Debug in small checks: read the first error, confirm the window starts, add visible feedback at the beginning of the callback, inspect the current input, then reduce the example to one window and one action.";
        } else if (query.matches(".*(next|roadmap|learn|order).*")) {
            answer = "Continue with " + course.lessons.get(Math.min(activeLesson + 1, course.lessons.size() - 1)).api + ". Build a tiny working version, take its quiz, then combine it with the current lesson.";
        } else {
            answer = course.shortTitle + " uses " + course.language + " to build desktop interfaces. A good next step is to practice " + course.lessons.get(Math.min(activeLesson, course.lessons.size() - 1)).api + " in one small window.";
        }
        String level = prefs.get("custom.explanation", "balanced");
        if ("concise".equals(level)) return answer;
        String check = "\n\nQuick check: name the control, the event, the data change, and the visible result.";
        return "detailed".equals(level) ? answer + check + " Then change one value and run the example again so you can see which line controls the behavior." : answer + check;
    }

    private void showCustomizationDialog() {
        Color initial = parseColor(prefs.get("custom.theme", "#2e7d5b"), new Color(46, 125, 91));
        JButton colorButton = new JButton("Choose theme color");
        colorButton.setBackground(initial);
        colorButton.setForeground(contrastColor(initial));
        Color[] selectedColor = {initial};
        colorButton.addActionListener(event -> {
            Color chosen = JColorChooser.showDialog(frame, "Choose Desktopcraft color", selectedColor[0]);
            if (chosen != null) {
                selectedColor[0] = chosen;
                colorButton.setBackground(chosen);
                colorButton.setForeground(contrastColor(chosen));
            }
        });
        JSlider brightness = new JSlider(70, 130, prefs.getInt("custom.brightness", 100));
        brightness.setMajorTickSpacing(10);
        brightness.setPaintTicks(true);
        brightness.setPaintLabels(true);
        JComboBox<String> explanations = new JComboBox<>(new String[]{"concise", "balanced", "detailed"});
        explanations.setSelectedItem(prefs.get("custom.explanation", "balanced"));
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints layout = new GridBagConstraints();
        layout.gridx = 0; layout.gridy = 0; layout.anchor = GridBagConstraints.WEST; layout.insets = new Insets(7, 7, 7, 14);
        panel.add(new JLabel("Theme color"), layout); layout.gridx = 1; layout.fill = GridBagConstraints.HORIZONTAL; layout.weightx = 1; panel.add(colorButton, layout);
        layout.gridx = 0; layout.gridy++; layout.fill = GridBagConstraints.NONE; layout.weightx = 0; panel.add(new JLabel("Brightness"), layout); layout.gridx = 1; layout.fill = GridBagConstraints.HORIZONTAL; layout.weightx = 1; panel.add(brightness, layout);
        layout.gridx = 0; layout.gridy++; layout.fill = GridBagConstraints.NONE; layout.weightx = 0; panel.add(new JLabel("Explanation detail"), layout); layout.gridx = 1; layout.fill = GridBagConstraints.HORIZONTAL; layout.weightx = 1; panel.add(explanations, layout);
        int result = JOptionPane.showConfirmDialog(frame, panel, "Customize every Desktopcraft screen", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, new ImageIcon(buildAppIcon(48)));
        if (result != JOptionPane.OK_OPTION) return;
        prefs.put("custom.theme", colorHex(selectedColor[0]));
        prefs.putInt("custom.brightness", brightness.getValue());
        prefs.put("custom.explanation", String.valueOf(explanations.getSelectedItem()));
        saveCurrentEdit();
        saveProgress();
        frame.dispose();
        show();
    }

    private void applyThemePreferences() {
        Color accent = parseColor(prefs.get("custom.theme", "#2e7d5b"), new Color(46, 125, 91));
        double light = prefs.getInt("custom.brightness", 100) / 100d;
        GREEN = adjustBrightness(accent, light);
        NAV = adjustBrightness(blend(accent, Color.BLACK, 0.72), light);
        NAV_ACTIVE = adjustBrightness(blend(accent, Color.BLACK, 0.47), light);
        CANVAS = adjustBrightness(blend(accent, Color.WHITE, 0.94), light);
        PAPER = adjustBrightness(blend(accent, Color.WHITE, 0.985), light);
        INK = adjustBrightness(blend(accent, Color.BLACK, 0.76), Math.min(1.15, light));
        MUTED = adjustBrightness(blend(accent, new Color(105, 105, 105), 0.72), light);
        GREEN_SOFT = adjustBrightness(blend(accent, Color.WHITE, 0.84), light);
        LIME = adjustBrightness(blend(accent, new Color(238, 255, 120), 0.75), light);
        ORANGE_SOFT = adjustBrightness(new Color(255, 240, 228), light);
        EDITOR = adjustBrightness(blend(accent, Color.BLACK, 0.82), Math.min(1.08, light));
        EDITOR_BAR = adjustBrightness(blend(accent, Color.BLACK, 0.70), Math.min(1.08, light));
    }

    private static Color parseColor(String value, Color fallback) {
        try { return Color.decode(value); } catch (NumberFormatException exception) { return fallback; }
    }

    private static Color blend(Color first, Color second, double secondWeight) {
        double weight = Math.max(0, Math.min(1, secondWeight));
        return new Color(
            (int) Math.round(first.getRed() * (1 - weight) + second.getRed() * weight),
            (int) Math.round(first.getGreen() * (1 - weight) + second.getGreen() * weight),
            (int) Math.round(first.getBlue() * (1 - weight) + second.getBlue() * weight)
        );
    }

    private static Color adjustBrightness(Color color, double multiplier) {
        return new Color(
            Math.max(0, Math.min(255, (int) Math.round(color.getRed() * multiplier))),
            Math.max(0, Math.min(255, (int) Math.round(color.getGreen() * multiplier))),
            Math.max(0, Math.min(255, (int) Math.round(color.getBlue() * multiplier)))
        );
    }

    private static Color contrastColor(Color color) {
        return color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114 > 150000 ? Color.BLACK : Color.WHITE;
    }

    private static String colorHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void putChunked(String prefix, String value) {
        int previous = prefs.getInt(prefix + "chunks", 0);
        int chunks = Math.max(1, (value.length() + PREFERENCE_CHUNK_SIZE - 1) / PREFERENCE_CHUNK_SIZE);
        prefs.putInt(prefix + "chunks", chunks);
        for (int index = 0; index < chunks; index++) {
            int start = index * PREFERENCE_CHUNK_SIZE;
            prefs.put(prefix + index, value.substring(start, Math.min(value.length(), start + PREFERENCE_CHUNK_SIZE)));
        }
        for (int index = chunks; index < previous; index++) prefs.remove(prefix + index);
    }

    private String getChunked(String prefix) {
        int chunks = prefs.getInt(prefix + "chunks", 0);
        StringBuilder value = new StringBuilder();
        for (int index = 0; index < chunks; index++) value.append(prefs.get(prefix + index, ""));
        return value.toString();
    }

    private void showForumDialog() {
        List<ForumTopic> topics = loadForumTopics();
        JDialog dialog = new JDialog(frame, "Desktopcraft builder forum", true);
        dialog.setIconImages(buildAppIcons());
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(CANVAS);
        DefaultListModel<ForumTopic> model = new DefaultListModel<>();
        topics.forEach(model::addElement);
        JList<ForumTopic> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (!topics.isEmpty()) list.setSelectedIndex(0);
        JTextArea detail = new JTextArea();
        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setBackground(PAPER);
        detail.setBorder(new EmptyBorder(14, 14, 14, 14));
        Runnable render = () -> detail.setText(forumTopicText(list.getSelectedValue()));
        list.addListSelectionListener(event -> { if (!event.getValueIsAdjusting()) render.run(); });

        JButton create = primaryButton("New topic");
        JButton reply = secondaryButton("Reply");
        JButton like = secondaryButton("Like / unlike");
        JButton close = secondaryButton("Close");
        create.addActionListener(event -> {
            if (!requireForumAccount()) return;
            JTextField title = new JTextField(32);
            JComboBox<String> category = new JComboBox<>(new String[]{"Java Swing", "Python Tkinter", "C# WinForms", "C++ Qt", "Electron", "App Maker", "General"});
            JTextArea body = new JTextArea(6, 32); body.setLineWrap(true); body.setWrapStyleWord(true);
            JTextArea code = new JTextArea(5, 32); code.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            JPanel fields = new JPanel(); fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
            fields.add(new JLabel("Title")); fields.add(title); fields.add(Box.createVerticalStrut(7)); fields.add(new JLabel("Category")); fields.add(category); fields.add(Box.createVerticalStrut(7)); fields.add(new JLabel("Question or idea")); fields.add(new JScrollPane(body)); fields.add(Box.createVerticalStrut(7)); fields.add(new JLabel("Optional code")); fields.add(new JScrollPane(code));
            if (JOptionPane.showConfirmDialog(dialog, fields, "Create forum topic", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            if (title.getText().trim().length() < 4 || body.getText().trim().length() < 8) { showCreatorError("Use a title with at least four characters and a message with at least eight."); return; }
            if (containsProfanity(title.getText(), body.getText(), code.getText())) { showCreatorError("The forum post contains blocked language. Nothing was saved."); return; }
            ForumTopic topic = new ForumTopic(System.currentTimeMillis(), title.getText().trim(), String.valueOf(category.getSelectedItem()), body.getText().trim(), code.getText().trim(), currentUserName, currentUsername, new HashSet<>(), new ArrayList<>());
            topics.add(topic); saveForumTopics(topics); model.addElement(topic); list.setSelectedValue(topic, true); render.run();
        });
        reply.addActionListener(event -> {
            ForumTopic topic = list.getSelectedValue();
            if (topic == null || !requireForumAccount()) return;
            JTextArea body = new JTextArea(5, 34); body.setLineWrap(true); body.setWrapStyleWord(true);
            if (JOptionPane.showConfirmDialog(dialog, new JScrollPane(body), "Reply as @" + currentUsername, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            String copy = body.getText().trim();
            if (copy.length() < 3 || containsProfanity(copy)) { showCreatorError("Write a useful reply with at least three characters and no blocked language."); return; }
            topic.replies.add(new ForumReply(currentUserName, currentUsername, copy, System.currentTimeMillis())); saveForumTopics(topics); render.run();
        });
        like.addActionListener(event -> {
            ForumTopic topic = list.getSelectedValue();
            if (topic == null || !requireForumAccount()) return;
            if (!topic.likedBy.add(currentUsername)) topic.likedBy.remove(currentUsername);
            saveForumTopics(topics); list.repaint(); render.run();
        });
        close.addActionListener(event -> dialog.dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false); actions.add(create); actions.add(reply); actions.add(like); actions.add(close);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), new JScrollPane(detail)); split.setDividerLocation(285); split.setResizeWeight(0); split.setBorder(null);
        JLabel title = new JLabel("Builder forum · real Desktopcraft accounts only"); title.setFont(new Font("Serif", Font.PLAIN, 25));
        dialog.add(title, BorderLayout.NORTH); dialog.add(split, BorderLayout.CENTER); dialog.add(actions, BorderLayout.SOUTH);
        dialog.getRootPane().setBorder(new EmptyBorder(18, 18, 18, 18));
        render.run(); dialog.setSize(920, 640); dialog.setLocationRelativeTo(frame); dialog.setVisible(true);
    }

    private boolean requireForumAccount() {
        if (!currentUsername.isBlank()) return true;
        JOptionPane.showMessageDialog(frame, "Create or sign in to a Desktopcraft account before posting, replying, or liking.", "Account required", JOptionPane.INFORMATION_MESSAGE);
        return false;
    }

    private String forumTopicText(ForumTopic topic) {
        if (topic == null) return "No forum topics yet. Create the first real Desktopcraft discussion.";
        StringBuilder text = new StringBuilder(topic.title).append("\n").append(topic.category).append(" · @").append(topic.authorUsername).append(" · ").append(topic.likedBy.size()).append(" likes\n\n").append(topic.body);
        if (!topic.code.isBlank()) text.append("\n\nCODE\n").append(topic.code);
        text.append("\n\nREPLIES (" + topic.replies.size() + ")");
        for (ForumReply reply : topic.replies) text.append("\n\n").append(reply.authorName).append(" · @").append(reply.authorUsername).append("\n").append(reply.body);
        return text.toString();
    }

    private List<ForumTopic> loadForumTopics() {
        List<ForumTopic> topics = new ArrayList<>();
        int count = prefs.getInt(FORUM_TOPIC_COUNT_KEY, 0);
        for (int index = 0; index < count; index++) {
            String saved = getChunked("forum.topic.v1." + index + ".");
            if (saved.isBlank()) continue;
            try { topics.add(deserializeForumTopic(saved)); } catch (RuntimeException ignored) { }
        }
        return topics;
    }

    private void saveForumTopics(List<ForumTopic> topics) {
        int oldCount = prefs.getInt(FORUM_TOPIC_COUNT_KEY, 0);
        for (int index = 0; index < topics.size(); index++) putChunked("forum.topic.v1." + index + ".", serializeForumTopic(topics.get(index)));
        for (int index = topics.size(); index < oldCount; index++) putChunked("forum.topic.v1." + index + ".", "");
        prefs.putInt(FORUM_TOPIC_COUNT_KEY, topics.size());
    }

    private static String serializeForumTopic(ForumTopic topic) {
        String replies = topic.replies.stream().map(reply -> encodeField(reply.authorName) + ":" + encodeField(reply.authorUsername) + ":" + reply.createdAt + ":" + encodeField(reply.body)).reduce((left, right) -> left + ";" + right).orElse("");
        return topic.id + "\t" + encodeField(topic.title) + "\t" + encodeField(topic.category) + "\t" + encodeField(topic.body) + "\t" + encodeField(topic.code) + "\t" + encodeField(topic.authorName) + "\t" + encodeField(topic.authorUsername) + "\t" + encodeField(String.join(",", topic.likedBy)) + "\t" + encodeField(replies);
    }

    private static ForumTopic deserializeForumTopic(String value) {
        String[] fields = value.split("\\t", -1);
        if (fields.length != 9) throw new IllegalArgumentException("Invalid forum topic");
        Set<String> likes = new HashSet<>();
        String liked = decodeField(fields[7]);
        if (!liked.isBlank()) for (String username : liked.split(",")) if (!username.isBlank()) likes.add(username);
        List<ForumReply> replies = new ArrayList<>();
        String savedReplies = decodeField(fields[8]);
        if (!savedReplies.isBlank()) for (String item : savedReplies.split(";")) {
            String[] parts = item.split(":", 4);
            if (parts.length == 4) replies.add(new ForumReply(decodeField(parts[0]), decodeField(parts[1]), decodeField(parts[3]), Long.parseLong(parts[2])));
        }
        return new ForumTopic(Long.parseLong(fields[0]), decodeField(fields[1]), decodeField(fields[2]), decodeField(fields[3]), decodeField(fields[4]), decodeField(fields[5]), decodeField(fields[6]), likes, replies);
    }

    private void showProjectsDialog() {
        JDialog dialog = new JDialog(frame, "Desktopcraft Projects", true);
        dialog.setIconImages(buildAppIcons());
        dialog.setSize(1050, 720);
        dialog.setLocationRelativeTo(frame);

        DefaultListModel<Path> files = new DefaultListModel<>();
        JList<Path> fileList = new JList<>(files);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof Path path) label.setText(path.getFileName().toString());
                return label;
            }
        });
        JTextArea projectEditor = new JTextArea();
        projectEditor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        projectEditor.setTabSize(4);
        JLabel pathLabel = new JLabel("Choose a source file to begin");
        pathLabel.setBorder(new EmptyBorder(8, 10, 8, 10));
        final Path[] activeFile = {null};
        final String[] savedCopy = {""};

        Runnable loadSelected = () -> {
            Path selected = fileList.getSelectedValue();
            if (selected == null) return;
            try {
                activeFile[0] = selected;
                savedCopy[0] = Files.readString(selected, StandardCharsets.UTF_8);
                projectEditor.setText(savedCopy[0]);
                projectEditor.setCaretPosition(0);
                pathLabel.setText(selected.toAbsolutePath().toString());
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(dialog, exception.getMessage(), "Could not open file", JOptionPane.ERROR_MESSAGE);
            }
        };
        fileList.addListSelectionListener(event -> { if (!event.getValueIsAdjusting()) loadSelected.run(); });

        JButton addFiles = secondaryButton("Add files…");
        JButton save = primaryButton("Save");
        JButton snapshot = secondaryButton("Save version");
        JButton restore = secondaryButton("Restore saved version");
        JButton openFolder = secondaryButton("Open containing folder");
        addFiles.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(true);
            if (chooser.showOpenDialog(dialog) != JFileChooser.APPROVE_OPTION) return;
            for (File file : chooser.getSelectedFiles()) if (!files.contains(file.toPath())) files.addElement(file.toPath());
            if (!files.isEmpty() && fileList.getSelectedIndex() < 0) fileList.setSelectedIndex(0);
        });
        save.addActionListener(event -> {
            if (activeFile[0] == null) return;
            try {
                Files.writeString(activeFile[0], projectEditor.getText(), StandardCharsets.UTF_8);
                savedCopy[0] = projectEditor.getText();
                pathLabel.setText(activeFile[0].toAbsolutePath() + " · saved");
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(dialog, exception.getMessage(), "Could not save file", JOptionPane.ERROR_MESSAGE);
            }
        });
        snapshot.addActionListener(event -> {
            if (activeFile[0] == null) return;
            String id = accountId(activeFile[0].toAbsolutePath().toString());
            putChunked("project.version.v1." + id + ".", projectEditor.getText());
            prefs.putLong("project.version.time.v1." + id, System.currentTimeMillis());
            JOptionPane.showMessageDialog(dialog, "A restorable version was saved.", "Version saved", JOptionPane.INFORMATION_MESSAGE);
        });
        restore.addActionListener(event -> {
            if (activeFile[0] == null) return;
            String id = accountId(activeFile[0].toAbsolutePath().toString());
            String version = getChunked("project.version.v1." + id + ".");
            if (version.isEmpty()) JOptionPane.showMessageDialog(dialog, "No saved version exists for this file.");
            else projectEditor.setText(version);
        });
        openFolder.addActionListener(event -> {
            if (activeFile[0] == null || !Desktop.isDesktopSupported()) return;
            try { Desktop.getDesktop().open(activeFile[0].toAbsolutePath().getParent().toFile()); }
            catch (IOException exception) { JOptionPane.showMessageDialog(dialog, exception.getMessage(), "Could not open folder", JOptionPane.ERROR_MESSAGE); }
        });

        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setBorder(new EmptyBorder(12, 12, 12, 6));
        left.add(addFiles, BorderLayout.NORTH);
        left.add(new JScrollPane(fileList), BorderLayout.CENTER);
        JPanel editorCard = new JPanel(new BorderLayout());
        editorCard.setBorder(new EmptyBorder(12, 6, 12, 12));
        editorCard.add(pathLabel, BorderLayout.NORTH);
        editorCard.add(new JScrollPane(projectEditor), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(openFolder); actions.add(restore); actions.add(snapshot); actions.add(save);
        editorCard.add(actions, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, editorCard);
        split.setDividerLocation(250);
        dialog.setContentPane(split);
        dialog.setVisible(true);
    }

    private void showCommunityDialog() {
        JDialog dialog = new JDialog(frame, "Desktopcraft Community Apps", true);
        dialog.setIconImages(buildAppIcons());
        dialog.setSize(900, 650);
        dialog.setLocationRelativeTo(frame);
        JTextField server = new JTextField(prefs.get("server.url", "http://localhost:8000"), 28);
        DefaultListModel<CommunityApp> apps = new DefaultListModel<>();
        JList<CommunityApp> list = new JList<>(apps);
        JTextArea details = new JTextArea("Connect to the Desktopcraft server to browse free source and installable Debian packages.");
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        list.addListSelectionListener(event -> {
            CommunityApp app = list.getSelectedValue();
            if (app != null) details.setText(app.title + "\n\n" + app.description + "\n\nBy @" + app.creator + "\nDownload: " + app.fileName);
        });
        Runnable refresh = () -> {
            prefs.put("server.url", server.getText().trim());
            details.setText("Loading community apps…");
            new SwingWorker<List<CommunityApp>, Void>() {
                protected List<CommunityApp> doInBackground() throws Exception { return loadCommunityApps(server.getText().trim()); }
                protected void done() {
                    try {
                        apps.clear();
                        for (CommunityApp app : get()) apps.addElement(app);
                        details.setText(apps.isEmpty() ? "No community apps have been published yet." : "Choose an app to see its details.");
                    } catch (Exception exception) { details.setText("Could not load the community library. Start the site server with npm start.\n\n" + exception.getMessage()); }
                }
            }.execute();
        };
        JButton refreshButton = secondaryButton("Refresh");
        JButton download = primaryButton("Download selected");
        JButton publish = secondaryButton("Publish an app");
        refreshButton.addActionListener(event -> refresh.run());
        download.addActionListener(event -> {
            CommunityApp app = list.getSelectedValue();
            if (app != null) openWebPage(server.getText().trim().replaceAll("/+$", "") + "/api/apps/" + app.id + "/download");
        });
        publish.addActionListener(event -> openWebPage(server.getText().trim().replaceAll("/+$", "") + "/make.html"));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Server")); top.add(server); top.add(refreshButton); top.add(publish);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.add(download);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), new JScrollPane(details));
        split.setDividerLocation(330);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.add(top, BorderLayout.NORTH); root.add(split, BorderLayout.CENTER); root.add(actions, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        refresh.run();
        dialog.setVisible(true);
    }

    private List<CommunityApp> loadCommunityApps(String server) throws Exception {
        String base = server.replaceAll("/+$", "");
        HttpResponse<String> response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(base + "/api/apps")).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Server returned " + response.statusCode());
        List<CommunityApp> apps = new ArrayList<>();
        Pattern item = Pattern.compile("\\{\"id\":(\\d+),\"title\":\"((?:\\\\.|[^\"])*)\",\"description\":\"((?:\\\\.|[^\"])*)\",\"toolkit\":\"[^\"]+\",\"toolkitLabel\":\"[^\"]+\",\"fileName\":\"((?:\\\\.|[^\"])*)\".*?\"username\":\"((?:\\\\.|[^\"])*)\"", Pattern.DOTALL);
        Matcher matcher = item.matcher(response.body());
        while (matcher.find()) apps.add(new CommunityApp(Long.parseLong(matcher.group(1)), unescapeJson(matcher.group(2)), unescapeJson(matcher.group(3)), unescapeJson(matcher.group(4)), unescapeJson(matcher.group(5))));
        return apps;
    }

    private void showFeedbackDialog() {
        JTextField name = new JTextField(currentUsername.isBlank() ? "" : currentUserName, 28);
        JTextField email = new JTextField(28);
        JTextArea message = new JTextArea(8, 34);
        message.setLineWrap(true); message.setWrapStyleWord(true);
        JPanel form = new JPanel(); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(new JLabel("Name (optional)")); form.add(name); form.add(Box.createVerticalStrut(8));
        form.add(new JLabel("Reply email (optional)")); form.add(email); form.add(Box.createVerticalStrut(8));
        form.add(new JLabel("Feedback")); form.add(new JScrollPane(message));
        if (JOptionPane.showConfirmDialog(frame, form, "Send Desktopcraft feedback", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (message.getText().trim().length() < 2) { JOptionPane.showMessageDialog(frame, "Write a feedback message first."); return; }
        String base = prefs.get("server.url", "http://localhost:8000").replaceAll("/+$", "");
        String payload = "{\"name\":\"" + jsonString(name.getText()) + "\",\"email\":\"" + jsonString(email.getText()) + "\",\"message\":\"" + jsonString(message.getText()) + "\",\"website\":\"\"}";
        new SwingWorker<Integer, Void>() {
            protected Integer doInBackground() throws Exception {
                return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/api/feedback")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build(), HttpResponse.BodyHandlers.ofString()).statusCode();
            }
            protected void done() {
                try { JOptionPane.showMessageDialog(frame, get() < 300 ? "Feedback sent. Thank you." : "The server could not accept feedback."); }
                catch (Exception exception) { JOptionPane.showMessageDialog(frame, "Could not connect to the feedback server: " + exception.getMessage()); }
            }
        }.execute();
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(frame,
            "<html><div style='width:420px'><h1>Desktopcraft</h1><p>Learn desktop development through 2,500 lessons, projects, tutorials, quizzes, community apps, and native building tools.</p><p>Five tracks: Java Swing, Python Tkinter, C# WinForms, C++ Qt Widgets, and JavaScript Electron.</p></div></html>",
            "About Desktopcraft", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(buildAppIcon(64)));
    }

    private void openWebPage(String address) {
        if (!Desktop.isDesktopSupported()) { JOptionPane.showMessageDialog(frame, address, "Open this address", JOptionPane.INFORMATION_MESSAGE); return; }
        try { Desktop.getDesktop().browse(URI.create(address)); }
        catch (Exception exception) { JOptionPane.showMessageDialog(frame, exception.getMessage(), "Could not open page", JOptionPane.ERROR_MESSAGE); }
    }

    private static String jsonString(String value) {
        return String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String unescapeJson(String value) {
        return value.replace("\\n", "\n").replace("\\r", "\r").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private void showAppMakerDialog() {
        JDialog dialog = new JDialog(frame, "Desktopcraft App Maker", true);
        dialog.setIconImages(buildAppIcons());
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(1120, 760);
        dialog.setMinimumSize(new Dimension(900, 640));
        dialog.setLocationRelativeTo(frame);

        JComboBox<String> toolkitBox = new JComboBox<>(new String[]{"Java Swing", "Python Tkinter", "C# WinForms", "C++ Qt Widgets", "JavaScript Electron"});
        JTextField appNameField = new JTextField("Hello Builder", 24);
        JTextField titleField = new JTextField("My Desktop App", 24);
        JTextField messageField = new JTextField("Ready to build something great.", 24);
        JTextField buttonField = new JTextField("Launch", 24);
        JTextField actionField = new JTextField("Your desktop app is working!", 24);
        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(520, 320, 1200, 10));
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(320, 220, 900, 10));

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBackground(PAPER);
        controls.setBorder(new EmptyBorder(22, 22, 22, 22));
        JLabel kicker = smallLabel("DESKTOP APP WORKSHOP", GREEN);
        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel heading = new JLabel("Build an app starter");
        heading.setFont(new Font("Serif", Font.PLAIN, 27));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel description = new JLabel("<html><span style='color:#68736d'>Choose a toolkit, edit the blueprint,<br>then copy or save the generated source.</span></html>");
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(kicker);
        controls.add(Box.createVerticalStrut(3));
        controls.add(heading);
        controls.add(Box.createVerticalStrut(4));
        controls.add(description);
        controls.add(Box.createVerticalStrut(18));
        addMakerControl(controls, "Desktop toolkit", toolkitBox);
        addMakerControl(controls, "App name", appNameField);
        addMakerControl(controls, "Window title", titleField);
        addMakerControl(controls, "Starting message", messageField);
        addMakerControl(controls, "Button label", buttonField);
        addMakerControl(controls, "Message after click", actionField);
        JPanel dimensions = new JPanel(new GridLayout(1, 2, 8, 0));
        dimensions.setOpaque(false);
        dimensions.add(labeledMakerComponent("WIDTH", widthSpinner));
        dimensions.add(labeledMakerComponent("HEIGHT", heightSpinner));
        dimensions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        dimensions.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(dimensions);
        controls.add(Box.createVerticalGlue());
        JLabel localNote = new JLabel("Generated locally · No source is uploaded");
        localNote.setForeground(MUTED);
        localNote.setFont(new Font("SansSerif", Font.PLAIN, 9));
        localNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.add(localNote);

        JTextArea generatedArea = new JTextArea();
        generatedArea.setEditable(false);
        generatedArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        generatedArea.setForeground(new Color(220, 231, 224));
        generatedArea.setBackground(EDITOR);
        generatedArea.setCaretColor(Color.WHITE);
        generatedArea.setBorder(new EmptyBorder(14, 16, 14, 16));
        JScrollPane codeScroll = new JScrollPane(generatedArea);
        codeScroll.setBorder(null);

        JLabel generatedFile = new JLabel("HelloBuilder.java");
        generatedFile.setForeground(Color.WHITE);
        generatedFile.setFont(new Font("Monospaced", Font.BOLD, 11));
        JPanel codeHeader = new JPanel(new BorderLayout());
        codeHeader.setBackground(EDITOR_BAR);
        codeHeader.setBorder(new EmptyBorder(10, 14, 10, 14));
        codeHeader.add(generatedFile, BorderLayout.WEST);
        JPanel codeCard = new JPanel(new BorderLayout());
        codeCard.setBackground(EDITOR);
        codeCard.add(codeHeader, BorderLayout.NORTH);
        codeCard.add(codeScroll, BorderLayout.CENTER);

        JPanel previewStage = new JPanel(new GridBagLayout());
        previewStage.setBackground(new Color(235, 238, 232));
        previewStage.setBorder(new EmptyBorder(18, 18, 18, 18));
        JLabel previewTitle = new JLabel("  ●  ●  ●                         My Desktop App");
        previewTitle.setOpaque(true);
        previewTitle.setBackground(new Color(218, 222, 218));
        previewTitle.setBorder(new EmptyBorder(7, 9, 7, 9));
        previewTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel previewToolkit = smallLabel("JAVA SWING", GREEN);
        JLabel previewAppName = new JLabel("Hello Builder");
        previewAppName.setFont(new Font("Serif", Font.PLAIN, 25));
        JLabel previewMessage = new JLabel("Ready to build something great.");
        previewMessage.setForeground(MUTED);
        JButton previewAction = primaryButton("Launch");
        JPanel previewBody = new JPanel();
        previewBody.setLayout(new BoxLayout(previewBody, BoxLayout.Y_AXIS));
        previewBody.setBackground(new Color(248, 248, 246));
        previewBody.setBorder(new EmptyBorder(24, 34, 24, 34));
        for (JComponent component : new JComponent[]{previewToolkit, previewAppName, previewMessage, previewAction}) {
            component.setAlignmentX(Component.CENTER_ALIGNMENT);
            previewBody.add(component);
            previewBody.add(Box.createVerticalStrut(9));
        }
        JPanel previewWindow = new JPanel(new BorderLayout());
        previewWindow.setPreferredSize(new Dimension(470, 230));
        previewWindow.setBorder(new LineBorder(new Color(150, 158, 153)));
        previewWindow.add(previewTitle, BorderLayout.NORTH);
        previewWindow.add(previewBody, BorderLayout.CENTER);
        previewStage.add(previewWindow);

        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBackground(CANVAS);
        right.setBorder(new EmptyBorder(14, 14, 14, 14));
        JSplitPane outputSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, codeCard, previewStage);
        outputSplit.setResizeWeight(0.58);
        outputSplit.setBorder(null);
        right.add(outputSplit, BorderLayout.CENTER);

        JButton copyButton = secondaryButton("Copy code");
        JButton saveButton = primaryButton("Save source…");
        JButton closeButton = secondaryButton("Close");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(copyButton);
        actions.add(saveButton);
        actions.add(closeButton);
        right.add(actions, BorderLayout.SOUTH);

        Runnable refresh = () -> {
            AppBlueprint blueprint = new AppBlueprint(
                String.valueOf(toolkitBox.getSelectedItem()),
                appNameField.getText().trim().isEmpty() ? "Desktop App" : appNameField.getText().trim(),
                titleField.getText().trim().isEmpty() ? "My Desktop App" : titleField.getText().trim(),
                messageField.getText().trim().isEmpty() ? "Ready." : messageField.getText().trim(),
                buttonField.getText().trim().isEmpty() ? "Run" : buttonField.getText().trim(),
                actionField.getText().trim().isEmpty() ? "It works!" : actionField.getText().trim(),
                (Integer) widthSpinner.getValue(),
                (Integer) heightSpinner.getValue()
            );
            generatedArea.setText(buildMakerSource(blueprint));
            generatedArea.setCaretPosition(0);
            generatedFile.setText(makerFileName(blueprint));
            previewToolkit.setText(blueprint.toolkit.toUpperCase(Locale.ROOT));
            previewAppName.setText(blueprint.appName);
            previewMessage.setText(blueprint.message);
            previewAction.setText(blueprint.buttonLabel);
            previewTitle.setText("  ●  ●  ●                         " + blueprint.windowTitle);
            previewWindow.setPreferredSize(new Dimension(Math.min(650, blueprint.width), Math.min(360, blueprint.height)));
            previewStage.revalidate();
        };

        DocumentListener liveRefresh = new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { refresh.run(); }
            public void removeUpdate(DocumentEvent event) { refresh.run(); }
            public void changedUpdate(DocumentEvent event) { refresh.run(); }
        };
        for (JTextField field : new JTextField[]{appNameField, titleField, messageField, buttonField, actionField}) {
            field.getDocument().addDocumentListener(liveRefresh);
        }
        toolkitBox.addActionListener(event -> refresh.run());
        widthSpinner.addChangeListener(event -> refresh.run());
        heightSpinner.addChangeListener(event -> refresh.run());
        previewAction.addActionListener(event -> previewMessage.setText(actionField.getText().trim().isEmpty() ? "It works!" : actionField.getText().trim()));
        copyButton.addActionListener(event -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(generatedArea.getText()), null);
            copyButton.setText("Copied ✓");
            Timer timer = new Timer(1400, resetEvent -> copyButton.setText("Copy code"));
            timer.setRepeats(false);
            timer.start();
        });
        saveButton.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save generated app source");
            chooser.setSelectedFile(new File(generatedFile.getText()));
            if (chooser.showSaveDialog(dialog) != JFileChooser.APPROVE_OPTION) return;
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), generatedArea.getText(), StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(dialog, "Saved " + chooser.getSelectedFile().getName(), "Source saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(dialog, "Could not save the source: " + exception.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE);
            }
        });
        closeButton.addActionListener(event -> dialog.dispose());

        JSplitPane layout = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls, right);
        layout.setDividerLocation(340);
        layout.setResizeWeight(0);
        layout.setBorder(null);
        dialog.setContentPane(layout);
        refresh.run();
        dialog.setVisible(true);
    }

    private static void addMakerControl(JPanel panel, String label, JComponent component) {
        JLabel fieldLabel = new JLabel(label.toUpperCase(Locale.ROOT));
        fieldLabel.setForeground(MUTED);
        fieldLabel.setFont(new Font("SansSerif", Font.BOLD, 9));
        fieldLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        panel.add(fieldLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(component);
        panel.add(Box.createVerticalStrut(10));
    }

    private static JPanel labeledMakerComponent(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel copy = smallLabel(label, MUTED);
        panel.add(copy, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private static String buildMakerSource(AppBlueprint values) {
        String name = makerEscape(values.appName);
        String title = makerEscape(values.windowTitle);
        String message = makerEscape(values.message);
        String button = makerEscape(values.buttonLabel);
        String action = makerEscape(values.actionMessage);
        String className = makerClassName(values.appName);
        return switch (values.toolkit) {
            case "Python Tkinter" -> """
                import tkinter as tk
                from tkinter import ttk

                root = tk.Tk()
                root.title("%s")
                root.geometry("%dx%d")

                content = ttk.Frame(root, padding=32)
                content.pack(fill="both", expand=True)
                ttk.Label(content, text="%s", font=("TkDefaultFont", 18, "bold")).pack(pady=8)
                message = ttk.Label(content, text="%s")
                message.pack(pady=12)

                def handle_action():
                    message.config(text="%s")

                ttk.Button(content, text="%s", command=handle_action).pack(pady=8)
                root.mainloop()
                """.formatted(title, values.width, values.height, name, message, action, button);
            case "C# WinForms" -> """
                using System;
                using System.Drawing;
                using System.Windows.Forms;

                public class %s : Form
                {
                    private readonly Label message = new();

                    public %s()
                    {
                        Text = "%s";
                        ClientSize = new Size(%d, %d);
                        StartPosition = FormStartPosition.CenterScreen;
                        var heading = new Label { Text = "%s", AutoSize = true };
                        message.Text = "%s";
                        message.AutoSize = true;
                        var action = new Button { Text = "%s", AutoSize = true };
                        action.Click += (_, _) => message.Text = "%s";
                        var layout = new FlowLayoutPanel { Dock = DockStyle.Fill, FlowDirection = FlowDirection.TopDown, Padding = new Padding(32) };
                        layout.Controls.AddRange(new Control[] { heading, message, action });
                        Controls.Add(layout);
                    }

                    [STAThread]
                    public static void Main()
                    {
                        ApplicationConfiguration.Initialize();
                        Application.Run(new %s());
                    }
                }
                """.formatted(className, className, title, values.width, values.height, name, message, button, action, className);
            case "C++ Qt Widgets" -> """
                #include <QApplication>
                #include <QLabel>
                #include <QPushButton>
                #include <QVBoxLayout>
                #include <QWidget>

                int main(int argc, char *argv[]) {
                    QApplication app(argc, argv);
                    QWidget window;
                    window.setWindowTitle("%s");
                    window.resize(%d, %d);
                    auto *heading = new QLabel("%s");
                    auto *message = new QLabel("%s");
                    auto *action = new QPushButton("%s");
                    auto *layout = new QVBoxLayout(&window);
                    layout->addWidget(heading);
                    layout->addWidget(message);
                    layout->addWidget(action);
                    QObject::connect(action, &QPushButton::clicked, [message]() {
                        message->setText("%s");
                    });
                    window.show();
                    return app.exec();
                }
                """.formatted(title, values.width, values.height, name, message, button, action);
            case "JavaScript Electron" -> {
                String safeAction = makerEscape(values.actionMessage).replace("</script", "<\\/script");
                String page = "<!doctype html><html><body style='font-family:system-ui;display:grid;place-items:center;height:100vh;margin:0'><main style='text-align:center'><h1>" + makerHtml(values.appName) + "</h1><p id='message'>" + makerHtml(values.message) + "</p><button id='action'>" + makerHtml(values.buttonLabel) + "</button></main><script>document.querySelector('#action').addEventListener('click',()=>document.querySelector('#message').textContent=\"" + safeAction + "\")<" + "/script></body></html>";
                yield """
                    const { app, BrowserWindow } = require("electron");

                    function createWindow() {
                        const window = new BrowserWindow({ width: %d, height: %d, title: "%s" });
                        const page = "%s";
                        window.loadURL("data:text/html;charset=utf-8," + encodeURIComponent(page));
                    }

                    app.whenReady().then(createWindow);
                    app.on("window-all-closed", () => {
                        if (process.platform !== "darwin") app.quit();
                    });
                    """.formatted(values.width, values.height, title, makerEscape(page));
            }
            default -> """
                import javax.swing.*;
                import java.awt.*;

                public class %s {
                    public static void main(String[] args) {
                        SwingUtilities.invokeLater(() -> {
                            JFrame frame = new JFrame("%s");
                            JLabel heading = new JLabel("%s", SwingConstants.CENTER);
                            JLabel message = new JLabel("%s", SwingConstants.CENTER);
                            JButton action = new JButton("%s");
                            heading.setFont(heading.getFont().deriveFont(Font.BOLD, 22f));
                            action.addActionListener(event -> message.setText("%s"));
                            JPanel content = new JPanel(new GridLayout(3, 1, 8, 8));
                            content.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
                            content.add(heading);
                            content.add(message);
                            content.add(action);
                            frame.setContentPane(content);
                            frame.setSize(%d, %d);
                            frame.setLocationRelativeTo(null);
                            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            frame.setVisible(true);
                        });
                    }
                }
                """.formatted(className, title, name, message, button, action, values.width, values.height);
        };
    }

    private static String makerFileName(AppBlueprint values) {
        String extension = switch (values.toolkit) {
            case "Python Tkinter" -> "py";
            case "C# WinForms" -> "cs";
            case "C++ Qt Widgets" -> "cpp";
            case "JavaScript Electron" -> "js";
            default -> "java";
        };
        return makerClassName(values.appName) + "." + extension;
    }

    private static String makerClassName(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("[^A-Za-z0-9]+")) {
            if (part.isEmpty()) continue;
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        if (result.isEmpty()) result.append("DesktopApp");
        if (Character.isDigit(result.charAt(0))) result.insert(0, "App");
        return result.toString();
    }

    private static String makerEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
    }

    private static String makerHtml(String value) {
        return html(value).replace("'", "&#039;");
    }

    private void showCreateLessonDialog() {
        if (prefs.getBoolean(CREATION_BAN_KEY, false)) {
            JOptionPane.showMessageDialog(frame, "Lesson creation is banned on this local app profile.", "Creation unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField nameField = new JTextField(36);
        JTextArea starterArea = creatorTextArea(7, true);
        JTextArea answerArea = creatorTextArea(7, true);
        JTextArea outputArea = creatorTextArea(4, false);
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel heading = new JLabel("<html><b>Create a " + html(currentCourse.shortTitle) + " lesson</b><br><span style='color:#68736d'>Write the lesson name, starter code, answer, and expected output yourself.</span></html>");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(heading);
        fields.add(Box.createVerticalStrut(10));
        JLabel warning = new JLabel("<html><div style='background:#fff0e4;padding:8px;color:#68392e'><b>Profanity-free studio</b><br>Submitting profanity permanently disables creation on this app profile and erases every custom lesson and this draft.</div></html>");
        warning.setAlignmentX(Component.LEFT_ALIGNMENT);
        fields.add(warning);
        fields.add(Box.createVerticalStrut(12));
        addCreatorField(fields, "Lesson name", nameField);
        addCreatorField(fields, "Starter code", new JScrollPane(starterArea));
        addCreatorField(fields, "Answer", new JScrollPane(answerArea));
        addCreatorField(fields, "Expected output", new JScrollPane(outputArea));

        JScrollPane formScroll = new JScrollPane(fields);
        formScroll.setPreferredSize(new Dimension(700, 610));
        formScroll.setBorder(null);
        int choice = JOptionPane.showConfirmDialog(
            frame,
            formScroll,
            "Desktopcraft Lesson Studio",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            new ImageIcon(buildAppIcon(48))
        );
        if (choice != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        String starterCode = starterArea.getText().trim();
        String answer = answerArea.getText().trim();
        String expectedOutput = outputArea.getText().trim();
        if (name.isBlank() || starterCode.isBlank() || answer.isBlank() || expectedOutput.isBlank()) {
            showCreatorError("Complete the lesson name, starter code, answer, and expected output.");
            return;
        }
        if (name.length() > 80 || starterCode.length() > 12000 || answer.length() > 12000 || expectedOutput.length() > 1000) {
            showCreatorError("Lesson content is too long. Keep the name under 80 characters, code fields under 12,000, and output under 1,000.");
            return;
        }
        if (containsProfanity(name, starterCode, answer, expectedOutput)) {
            nameField.setText("");
            starterArea.setText("");
            answerArea.setText("");
            outputArea.setText("");
            eraseCustomLessonsAndBan();
            JOptionPane.showMessageDialog(
                frame,
                "Profanity was detected. All custom lessons and this draft were erased, and lesson creation is now banned on this app profile.",
                "Lesson creation banned",
                JOptionPane.ERROR_MESSAGE,
                new ImageIcon(buildAppIcon(32))
            );
            return;
        }

        Lesson lesson = customLesson(currentCourse, name, starterCode, answer, expectedOutput);
        saveCustomLessonRecord(currentCourse.id, name, starterCode, answer, expectedOutput);
        currentCourse.lessons.add(lesson);
        activeLesson = currentCourse.lessons.size() - 1;
        filterLessons();
        showLesson();
        JOptionPane.showMessageDialog(frame, "Your custom lesson was published to " + currentCourse.shortTitle + ".", "Lesson published", JOptionPane.INFORMATION_MESSAGE);
    }

    private static JTextArea creatorTextArea(int rows, boolean code) {
        JTextArea area = new JTextArea(rows, 52);
        area.setLineWrap(!code);
        area.setWrapStyleWord(!code);
        area.setTabSize(4);
        if (code) area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return area;
    }

    private void showCreatorError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Could not create lesson", JOptionPane.ERROR_MESSAGE, new ImageIcon(buildAppIcon(32)));
    }

    private static void addCreatorField(JPanel fields, String label, JComponent component) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        fieldLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component instanceof JScrollPane ? 150 : 34));
        fields.add(fieldLabel);
        fields.add(Box.createVerticalStrut(4));
        fields.add(component);
        fields.add(Box.createVerticalStrut(10));
    }

    private static Lesson customLesson(Course course, String name, String starterCode, String answer, String expectedOutput) {
        return new Lesson(
            "Custom lessons",
            name,
            "Creator lesson",
            "A custom " + course.shortTitle + " challenge created in the Desktopcraft lesson studio. Expected output: " + expectedOutput,
            expectedOutput,
            answer,
            starterCode
        );
    }

    private static boolean containsProfanity(String... values) {
        for (String value : values) {
            String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('0', 'o')
                .replace('1', 'i')
                .replace('3', 'e')
                .replace('4', 'a')
                .replace('5', 's')
                .replace('7', 't');
            if (PROFANITY_PATTERN.matcher(normalized).find()) return true;
        }
        return false;
    }

    private void saveCustomLessonRecord(String courseId, String name, String starterCode, String answer, String expectedOutput) {
        int index = prefs.getInt(CUSTOM_LESSON_COUNT_KEY, 0);
        String serialized = courseId + "\t" + encodeField(name) + "\t" + encodeField(starterCode) + "\t" + encodeField(answer) + "\t" + encodeField(expectedOutput);
        String prefix = "custom.lesson.v1." + index + ".";
        int chunks = Math.max(1, (serialized.length() + PREFERENCE_CHUNK_SIZE - 1) / PREFERENCE_CHUNK_SIZE);
        prefs.putInt(prefix + "chunks", chunks);
        for (int chunk = 0; chunk < chunks; chunk++) {
            int start = chunk * PREFERENCE_CHUNK_SIZE;
            prefs.put(prefix + chunk, serialized.substring(start, Math.min(serialized.length(), start + PREFERENCE_CHUNK_SIZE)));
        }
        prefs.putInt(CUSTOM_LESSON_COUNT_KEY, index + 1);
    }

    private void loadCustomLessons() {
        int count = prefs.getInt(CUSTOM_LESSON_COUNT_KEY, 0);
        for (int index = 0; index < count; index++) {
            String prefix = "custom.lesson.v1." + index + ".";
            int chunks = prefs.getInt(prefix + "chunks", 0);
            StringBuilder serialized = new StringBuilder();
            for (int chunk = 0; chunk < chunks; chunk++) serialized.append(prefs.get(prefix + chunk, ""));
            String[] fields = serialized.toString().split("\\t", -1);
            if (fields.length != 5) continue;
            Course course = courses.stream().filter(candidate -> candidate.id.equals(fields[0])).findFirst().orElse(null);
            if (course == null) continue;
            try {
                String name = decodeField(fields[1]);
                String starterCode = decodeField(fields[2]);
                String answer = decodeField(fields[3]);
                String expectedOutput = decodeField(fields[4]);
                if (!name.isBlank() && !starterCode.isBlank() && !answer.isBlank() && !expectedOutput.isBlank()) {
                    course.lessons.add(customLesson(course, name, starterCode, answer, expectedOutput));
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore a malformed local record and keep the built-in catalog available.
            }
        }
    }

    private void eraseCustomLessonsAndBan() {
        prefs.putBoolean(CREATION_BAN_KEY, true);
        int count = prefs.getInt(CUSTOM_LESSON_COUNT_KEY, 0);
        for (int index = 0; index < count; index++) {
            String prefix = "custom.lesson.v1." + index + ".";
            int chunks = prefs.getInt(prefix + "chunks", 0);
            for (int chunk = 0; chunk < chunks; chunk++) prefs.remove(prefix + chunk);
            prefs.remove(prefix + "chunks");
        }
        prefs.remove(CUSTOM_LESSON_COUNT_KEY);

        for (Course course : courses) {
            int builtInCount = builtInLessonCounts.getOrDefault(course.id, course.lessons.size());
            if (course.lessons.size() > builtInCount) course.lessons.subList(builtInCount, course.lessons.size()).clear();
            completed.computeIfAbsent(course.id, ignored -> new HashSet<>()).removeIf(index -> index >= builtInCount);
            editedCode.computeIfAbsent(course.id, ignored -> new HashMap<>()).keySet().removeIf(index -> index >= builtInCount);
            String stored = completed.get(course.id).stream().sorted().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("");
            prefs.put("completed." + course.id, stored);
            prefs.putInt("activeLesson." + course.id, Math.min(prefs.getInt("activeLesson." + course.id, 0), builtInCount - 1));
        }

        activeLesson = Math.min(activeLesson, currentCourse.lessons.size() - 1);
        createLessonButton.setText("Lesson creation banned");
        createLessonButton.setEnabled(false);
        filterLessons();
        showLesson();
    }

    private static String encodeField(String value) {
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeField(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private void loadProgress() {
        for (Course course : courses) {
            Set<Integer> values = new HashSet<>();
            String stored = prefs.get("completed." + course.id, "");
            for (String value : stored.split(",")) {
                try {
                    if (!value.isBlank()) values.add(Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
            completed.put(course.id, values);
        }
    }

    private void saveProgress() {
        Set<Integer> values = completed.getOrDefault(currentCourse.id, Set.of());
        String stored = values.stream().sorted().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("");
        prefs.put("completed." + currentCourse.id, stored);
        prefs.putInt("activeLesson." + currentCourse.id, activeLesson);
        if (!currentUsername.isBlank()) {
            String id = accountId(currentUsername);
            prefs.put("leaderboard." + id + ".name", currentUserName);
            prefs.put("leaderboard." + id + ".username", currentUsername);
            prefs.putLong("leaderboard." + id + ".updated", System.currentTimeMillis());
            for (Course course : courses) {
                int count = completed.getOrDefault(course.id, Set.of()).size();
                prefs.putInt("leaderboard." + id + ".completed." + course.id, Math.max(0, Math.min(500, count)));
            }
        }
    }

    private void showLoginDialog() {
        String savedUsername = prefs.get("session.username", "");
        String savedName = prefs.get("session.name", "");
        if (!savedUsername.isBlank() && !savedName.isBlank()) {
            currentUsername = savedUsername;
            currentUserName = savedName;
            return;
        }

        JTextField nameField = new JTextField(22);
        JTextField usernameField = new JTextField(22);
        JPasswordField passwordField = new JPasswordField(22);
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBorder(new EmptyBorder(6, 6, 6, 6));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 12, 0);
        JLabel intro = new JLabel("<html><b>Desktopcraft profile</b><br><span style='color:#68736d'>Your account stays registered in this desktop app.<br>Use a Desktopcraft-only password—never your email password.</span></html>");
        fields.add(intro, constraints);
        constraints.gridwidth = 1;
        constraints.gridy++;
        constraints.insets = new Insets(4, 0, 4, 10);
        fields.add(new JLabel("Display name"), constraints);
        constraints.gridx = 1;
        constraints.insets = new Insets(4, 0, 4, 0);
        fields.add(nameField, constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.insets = new Insets(4, 0, 4, 10);
        fields.add(new JLabel("Desktopcraft username"), constraints);
        constraints.gridx = 1;
        constraints.insets = new Insets(4, 0, 4, 0);
        fields.add(usernameField, constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.insets = new Insets(4, 0, 4, 10);
        fields.add(new JLabel("Desktopcraft password"), constraints);
        constraints.gridx = 1;
        constraints.insets = new Insets(4, 0, 4, 0);
        fields.add(passwordField, constraints);

        Object[] options = {"Sign in", "Create account", "Continue as guest"};
        while (true) {
            int choice = JOptionPane.showOptionDialog(
                frame,
                fields,
                "Welcome to Desktopcraft",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                new ImageIcon(buildAppIcon(64)),
                options,
                options[0]
            );
            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                currentUserName = "Guest learner";
                currentUsername = "";
                return;
            }

            String username = usernameField.getText().trim().toLowerCase();
            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);
            java.util.Arrays.fill(passwordChars, '\0');
            if (!username.matches("^[a-z0-9_.-]{3,24}$")) {
                showLoginError("Use 3–24 letters, numbers, dots, dashes, or underscores for your username.");
                continue;
            }
            if (password.length() < 8) {
                showLoginError("Use at least eight characters for your Desktopcraft password.");
                continue;
            }

            String accountId = accountId(username);
            String hashKey = "account." + accountId + ".hash";
            String saltKey = "account." + accountId + ".salt";
            if (choice == 1) {
                String name = nameField.getText().trim();
                if (name.length() < 2) {
                    showLoginError("Enter a display name with at least two characters.");
                    continue;
                }
                if (prefs.get(hashKey, null) != null) {
                    showLoginError("That Desktopcraft username is already taken.");
                    continue;
                }
                String salt = newPasswordSalt();
                prefs.put(saltKey, salt);
                prefs.put(hashKey, passwordVerifier(password, salt));
                prefs.put("account." + accountId + ".name", name);
                prefs.put("account." + accountId + ".username", username);
                prefs.putLong("account." + accountId + ".created", System.currentTimeMillis());
                currentUserName = name;
                currentUsername = username;
            } else {
                String expected = prefs.get(hashKey, null);
                String salt = prefs.get(saltKey, "");
                boolean valid = expected != null && (!salt.isBlank()
                    ? expected.equals(passwordVerifier(password, salt))
                    : expected.equals(passwordHash(password)));
                if (!valid) {
                    showLoginError("Desktopcraft username or password is incorrect.");
                    continue;
                }
                if (salt.isBlank()) {
                    salt = newPasswordSalt();
                    prefs.put(saltKey, salt);
                    prefs.put(hashKey, passwordVerifier(password, salt));
                }
                currentUserName = prefs.get("account." + accountId + ".name", "Builder");
                currentUsername = username;
            }
            prefs.put("session.username", currentUsername);
            prefs.put("session.name", currentUserName);
            return;
        }
    }

    private void showLoginError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Could not sign in", JOptionPane.ERROR_MESSAGE, new ImageIcon(buildAppIcon(32)));
    }

    private static String accountId(String username) {
        return passwordHash("username:" + username).substring(0, 32);
    }

    private static String newPasswordSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(salt);
    }

    private static String passwordVerifier(String password, String encodedSalt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getUrlDecoder().decode(encodedSalt), PASSWORD_ITERATIONS, 256);
        try {
            byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Secure password storage is unavailable", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean verifyPasswordStorage() {
        String salt = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
        String first = passwordVerifier("desktop-pass", salt);
        return first.length() >= 40 && first.equals(passwordVerifier("desktop-pass", salt)) && !first.equals(passwordVerifier("wrong-pass", salt));
    }

    private static String passwordHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(("desktopcraft-local-v2:" + value).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static List<Image> buildAppIcons() {
        return List.of(buildAppIcon(16), buildAppIcon(32), buildAppIcon(64), buildAppIcon(128));
    }

    private static Image buildAppIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float scale = size / 64f;
        graphics.scale(scale, scale);
        graphics.setColor(NAV);
        graphics.fillRoundRect(0, 0, 64, 64, 14, 14);
        graphics.setColor(PAPER);
        graphics.fillRoundRect(11, 12, 42, 31, 5, 5);
        graphics.setColor(LIME);
        graphics.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawRoundRect(11, 12, 42, 31, 5, 5);
        graphics.setColor(GREEN);
        graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(24, 24, 18, 29);
        graphics.drawLine(18, 29, 24, 34);
        graphics.drawLine(40, 24, 46, 29);
        graphics.drawLine(46, 29, 40, 34);
        graphics.drawLine(35, 20, 29, 38);
        graphics.setColor(LIME);
        graphics.drawLine(32, 43, 32, 51);
        graphics.drawLine(23, 53, 41, 53);
        graphics.setColor(new Color(238, 148, 93));
        graphics.fillOval(44, 13, 8, 8);
        graphics.dispose();
        return image;
    }

    private void bindKeyboardShortcuts() {
        KeyStroke run = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(run, "run-code");
        frame.getRootPane().getActionMap().put("run-code", new AbstractAction() {
            public void actionPerformed(ActionEvent event) { runSimulation(); }
        });
        KeyStroke search = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(search, "search-lessons");
        frame.getRootPane().getActionMap().put("search-lessons", new AbstractAction() {
            public void actionPerformed(ActionEvent event) { searchField.requestFocusInWindow(); }
        });
    }

    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(GREEN);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(new LineBorder(new Color(32, 91, 67)), new EmptyBorder(8, 13, 8, 13)));
        return button;
    }

    private static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(PAPER);
        button.setForeground(MUTED);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(new LineBorder(new Color(216, 217, 209)), new EmptyBorder(8, 13, 8, 13)));
        return button;
    }

    private static JButton toolbarButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(new Color(190, 202, 194));
        button.setBackground(EDITOR_BAR);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(4, 8, 4, 8));
        return button;
    }

    private static JButton darkButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(Color.WHITE);
        button.setBackground(NAV_ACTIVE);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(new LineBorder(new Color(63, 87, 76)), new EmptyBorder(8, 10, 8, 10)));
        return button;
    }

    private static JLabel smallLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        return label;
    }

    private static boolean balanced(String source, char open, char close) {
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (char value : source.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }
            if (value == '\\') {
                escaped = true;
                continue;
            }
            if (value == '"') quoted = !quoted;
            if (quoted) continue;
            if (value == open) depth++;
            if (value == close && --depth < 0) return false;
        }
        return depth == 0;
    }

    private static String normalizeLessonAnswer(String value) {
        return value.replace("\r\n", "\n").trim();
    }

    private static String simpleLessonText(String value) {
        String result = String.valueOf(value == null ? "" : value);
        String[][] replacements = {
            {"Event Dispatch Thread", "Swing UI thread"}, {"operating-system", "system"}, {"top-level", "main"},
            {"non-editable", "read-only"}, {"configuration", "settings"}, {"Configuration", "Settings"},
            {"configures", "sets up"}, {"configure", "set up"}, {"Configure", "Set up"},
            {"initializes", "starts"}, {"initialize", "start"}, {"Initialize", "Start"},
            {"constructs", "creates"}, {"construct", "create"}, {"Construct", "Create"},
            {"invokes", "runs"}, {"invoked", "run"}, {"dispatches", "sends"}, {"dispatch", "send"},
            {"supplied", "given"}, {"reusable", "shared"}, {"constrained", "limited"},
            {"hierarchical", "tree-like"}, {"periodic", "repeated"}, {"privileged", "system-level"},
            {"asynchronous", "background"}, {"dynamically", "as it runs"}, {"immediately", "right away"},
            {"subsequent", "next"}, {"corresponding", "matching"}, {"appropriate", "right"},
            {"explicit", "clear"}, {"concise", "short"}, {"responsibility", "job"},
            {"composition", "putting parts together"}, {"architecture", "design"}, {"functionality", "features"},
            {"curriculum", "course"}, {"estimated", "planned"}, {"primary", "main"}, {"central", "main"},
            {"geometry", "size and position"}, {"interface state", "screen data"}, {"application state", "app data"},
            {"interfaces", "screens"}, {"interface", "screen"}, {"components", "controls"}, {"component", "control"},
            {"containers", "holders"}, {"container", "holder"}, {"reflect the result", "show the result"},
            {"represents", "shows"}, {"persist", "save"}, {"lifecycle", "start-to-finish steps"}
        };
        boolean startsUppercase = !result.isEmpty() && Character.isUpperCase(result.charAt(0));
        for (String[] replacement : replacements) {
            result = result.replaceAll("(?i)(?<![A-Za-z])" + Pattern.quote(replacement[0]) + "(?![A-Za-z])", Matcher.quoteReplacement(replacement[1]));
        }
        if (startsUppercase && !result.isEmpty()) result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        return result;
    }

    private static String html(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private class LessonRenderer extends JPanel implements ListCellRenderer<LessonRef> {
        private final JLabel number = new JLabel();
        private final JLabel title = new JLabel();
        private final JLabel subtitle = new JLabel();

        LessonRenderer() {
            setLayout(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(5, 8, 5, 8));
            number.setHorizontalAlignment(SwingConstants.CENTER);
            number.setPreferredSize(new Dimension(40, 34));
            JPanel copy = new JPanel(new GridLayout(2, 1));
            copy.setOpaque(false);
            title.setFont(new Font("SansSerif", Font.BOLD, 11));
            subtitle.setFont(new Font("SansSerif", Font.PLAIN, 9));
            copy.add(title);
            copy.add(subtitle);
            add(number, BorderLayout.WEST);
            add(copy, BorderLayout.CENTER);
        }

        public Component getListCellRendererComponent(JList<? extends LessonRef> list, LessonRef value, int index, boolean selected, boolean focus) {
            boolean done = completed.getOrDefault(currentCourse.id, Set.of()).contains(value.index);
            setBackground(selected ? NAV_ACTIVE : NAV);
            number.setText(done ? "✓" : String.format("%03d", value.index + 1));
            number.setOpaque(true);
            number.setBackground(selected ? LIME : new Color(43, 59, 51));
            number.setForeground(selected ? NAV : new Color(201, 213, 205));
            title.setText(value.lesson.title);
            title.setForeground(selected ? Color.WHITE : new Color(199, 210, 203));
            subtitle.setText(value.lesson.api);
            subtitle.setForeground(new Color(126, 145, 134));
            return this;
        }
    }

    private static List<Course> loadCourses() {
        List<Course> result = new ArrayList<>();
        List<Lesson> swing = new ArrayList<>();
        swing.add(new Lesson("Swing foundations", "Give your app a window", "JFrame", "Create and reveal the top-level Swing window.", "Hello, Swing!", "My Swing App", javaCode("Give your app a window", "JFrame", "Hello, Swing!")));
        swing.add(new Lesson("Swing foundations", "Let layouts do the measuring", "GridLayout", "Arrange controls with flexible layout rules.", "Grid created", "Grid spacing improved", javaCode("Layout managers", "GridLayout", "Grid created")));
        swing.add(new Lesson("Swing foundations", "Make the interface respond", "ActionListener", "Connect user actions to application behavior.", "Action received", "Action listener connected", javaCode("Events", "ActionListener", "Action received")));
        swing.add(new Lesson("Swing foundations", "Turn input into an answer", "JTextField", "Read current input inside its event callback.", "Hello, learner", "Welcome, learner", javaCode("Inputs", "JTextField", "Hello, learner")));
        swing.add(new Lesson("Swing foundations", "Ship a tiny task list", "DefaultListModel", "Combine input, models, events, and a visible collection.", "Learn Swing", "Build my first app", javaCode("Task list", "DefaultListModel", "Learn Swing")));
        parseSwingResource("/lessons-extra.js", swing);
        result.add(new Course("java-swing", "Java Swing: Zero to Builder", "Java Swing", "Java", "Main.java", swing));

        parseDesktopCourse("/desktop-courses.js", "python-tkinter", "Python Tkinter Desktop Apps", "Tkinter", "Python", "app.py", result);
        parseDesktopCourse("/desktop-courses.js", "csharp-winforms", "C# Windows Forms Apps", "WinForms", "C#", "MainForm.cs", result);
        parseDesktopCourse("/desktop-courses.js", "cpp-qt", "C++ Qt Widgets Apps", "Qt Widgets", "C++", "main.cpp", result);
        parseDesktopCourse("/desktop-courses.js", "javascript-electron", "JavaScript Electron Desktop Apps", "Electron", "JavaScript", "renderer.js", result);
        expandCourses(result, 500);
        return result;
    }

    private static void expandCourses(List<Course> courses, int target) {
        for (Course course : courses) {
            if (course.lessons.size() > target) course.lessons.subList(target, course.lessons.size()).clear();
            while (course.lessons.size() < target) {
                course.lessons.add(generatedLesson(course, course.lessons.size() + 1));
            }
        }
    }

    private static Lesson generatedLesson(Course course, int lessonNumber) {
        List<String> modules = switch (course.language) {
            case "Java" -> List.of("Component mastery", "Layouts and composition", "Events and state", "Models and data", "Polish and accessibility", "Projects and architecture");
            case "Python" -> List.of("Widget mastery", "Geometry and composition", "Events and state", "Data-driven interfaces", "Polish and accessibility", "Projects and architecture");
            case "C#" -> List.of("Control mastery", "Layout and composition", "Events and state", "Data-bound interfaces", "Polish and accessibility", "Projects and architecture");
            case "C++" -> List.of("Widget mastery", "Layout and composition", "Signals and state", "Model/view interfaces", "Polish and accessibility", "Projects and architecture");
            default -> List.of("Renderer mastery", "Layout and composition", "Events and state", "Secure desktop data", "Polish and accessibility", "Projects and architecture");
        };
        List<String> apis = switch (course.language) {
            case "Java" -> List.of("JFrame", "JPanel", "JLabel", "JButton", "JTextField", "JTextArea", "JCheckBox", "JRadioButton", "JComboBox", "JList", "JTable", "JTree", "JTabbedPane", "JSplitPane", "JScrollPane", "BorderLayout", "GridBagLayout", "BoxLayout", "Action", "KeyStroke", "DocumentListener", "SwingWorker", "Timer", "TableModel", "ListModel");
            case "Python" -> List.of("Tk", "ttk.Frame", "ttk.Label", "ttk.Button", "ttk.Entry", "Text", "ttk.Checkbutton", "ttk.Radiobutton", "ttk.Combobox", "Listbox", "ttk.Treeview", "ttk.Notebook", "ttk.Panedwindow", "Canvas", "pack", "grid", "place", "StringVar", "bind", "after", "messagebox", "filedialog", "ttk.Progressbar", "Menu", "Toplevel");
            case "C#" -> List.of("Form", "Panel", "Label", "Button", "TextBox", "RichTextBox", "CheckBox", "RadioButton", "ComboBox", "ListBox", "DataGridView", "TreeView", "TabControl", "SplitContainer", "FlowLayoutPanel", "TableLayoutPanel", "Click", "KeyDown", "ErrorProvider", "BindingSource", "Timer", "async / await", "MenuStrip", "ProgressBar", "UserControl");
            case "C++" -> List.of("QApplication", "QWidget", "QLabel", "QPushButton", "QLineEdit", "QTextEdit", "QCheckBox", "QRadioButton", "QComboBox", "QListWidget", "QTableView", "QTreeView", "QTabWidget", "QSplitter", "QVBoxLayout", "QGridLayout", "signals and slots", "QAction", "QValidator", "QStringListModel", "QTimer", "QThread", "QMenuBar", "QProgressBar", "QDialog");
            default -> List.of("BrowserWindow", "HTMLElement", "HTMLButtonElement", "HTMLInputElement", "HTMLTextAreaElement", "checkbox input", "radio input", "select element", "dialog element", "CSS Grid", "Flexbox", "addEventListener", "CustomEvent", "FormData", "contextBridge", "ipcRenderer.invoke", "ipcMain.handle", "Menu", "Tray", "Notification", "globalShortcut", "fs/promises", "localStorage", "ARIA", "Electron Forge");
        };
        List<String> projects = List.of("profile editor", "task board", "notes desk", "budget panel", "timer dashboard", "contact book", "inventory tool", "study planner", "music queue", "weather console");
        List<String> skills = List.of("Create", "Configure", "Connect", "Validate", "Update", "Organize", "Test", "Refactor", "Reuse", "Ship");
        int offset = lessonNumber - 1;
        int moduleIndex = Math.floorDiv(offset, (int) Math.ceil(500d / modules.size())) % modules.size();
        String module = modules.get(moduleIndex);
        String api = apis.get(offset % apis.size());
        String project = projects.get((offset * 3 + moduleIndex) % projects.size());
        String skill = skills.get((offset + moduleIndex) % skills.size());
        String title = skill + " " + api + " in a " + project + " — Lab " + lessonNumber;
        String starter = "Lab " + lessonNumber + " ready";
        String goal = api + " lab " + lessonNumber + " complete";
        String description = "Practice how " + api + " supports a " + project + ", connect it to one clear piece of interface state, and verify the result in the desktop simulator.";
        String code = "Java".equals(course.language)
            ? javaCode(title, api, starter)
            : languageCode(course.language, title, api, starter);
        return new Lesson(module, title, api, description, starter, goal, code);
    }

    private static void parseSwingResource(String resource, List<Lesson> target) {
        String source = readResource(resource);
        String module = "Swing practice";
        Pattern modulePattern = Pattern.compile("^\\s*name:\\s*\"([^\"]+)\"", Pattern.MULTILINE);
        Pattern lessonPattern = Pattern.compile("^\\s*\\[\"([^\"]+)\",\\s*\"([^\"]+)\",\\s*\"([^\"]+)\",\\s*\"([^\"]+)\",\\s*\"([^\"]+)\",\\s*\"([^\"]+)\"\\],?", Pattern.MULTILINE);
        List<PositionedModule> modules = new ArrayList<>();
        Matcher moduleMatcher = modulePattern.matcher(source);
        while (moduleMatcher.find()) modules.add(new PositionedModule(moduleMatcher.start(), moduleMatcher.group(1)));
        Matcher matcher = lessonPattern.matcher(source);
        while (matcher.find()) {
            for (PositionedModule positioned : modules) if (positioned.position <= matcher.start()) module = positioned.name;
            String title = matcher.group(1), api = matcher.group(3), detail = matcher.group(4), starter = matcher.group(5), goal = matcher.group(6);
            target.add(new Lesson(module, title, api, detail, starter, goal, javaCode(title, api, starter)));
        }
    }

    private static void parseDesktopCourse(String resource, String id, String title, String shortTitle, String language, String fileName, List<Course> target) {
        String source = readResource(resource);
        int start = source.indexOf("id: \"" + id + "\"");
        if (start < 0) return;
        int next = source.indexOf("id: \"", start + 8);
        int definitionsEnd = source.indexOf("const escapeText", start);
        int end = next > 0 ? next : definitionsEnd > 0 ? definitionsEnd : source.length();
        String block = source.substring(start, end);
        Pattern modulePattern = Pattern.compile("^\\s*\\[\"([^\"]+)\",\\s*\\[$", Pattern.MULTILINE);
        Pattern lessonPattern = Pattern.compile("^\\s*\\[\"([^\"]+)\",\\s*\"([^\"]+)\",\\s*\"([^\"]+)\",\\s*\"([^\"]+)\",\\s*\"([^\"]+)\"\\],?", Pattern.MULTILINE);
        List<PositionedModule> modules = new ArrayList<>();
        Matcher moduleMatcher = modulePattern.matcher(block);
        while (moduleMatcher.find()) modules.add(new PositionedModule(moduleMatcher.start(), moduleMatcher.group(1)));
        List<Lesson> lessons = new ArrayList<>();
        Matcher matcher = lessonPattern.matcher(block);
        while (matcher.find()) {
            String module = "Desktop foundations";
            for (PositionedModule positioned : modules) if (positioned.position <= matcher.start()) module = positioned.name;
            String lessonTitle = matcher.group(1), api = matcher.group(2), detail = matcher.group(3), starter = matcher.group(4), goal = matcher.group(5);
            lessons.add(new Lesson(module, lessonTitle, api, detail, starter, goal, languageCode(language, lessonTitle, api, starter)));
        }
        target.add(new Course(id, title, shortTitle, language, fileName, lessons));
    }

    private static String readResource(String path) {
        try (InputStream stream = DesktopcraftApp.class.getResourceAsStream(path)) {
            if (stream == null) return "";
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private static String languageCode(String language, String title, String api, String starter) {
        return switch (language) {
            case "Python" -> "import tkinter as tk\nfrom tkinter import ttk\n\nroot = tk.Tk()\nroot.title(\"" + title + "\")\npanel = ttk.Frame(root, padding=18)\npanel.pack(fill=\"both\", expand=True)\nstatus = ttk.Label(panel, text=\"Ready\")\nentry = ttk.Entry(panel)\nentry.insert(0, \"Desktop app\")\n\ndef run_demo():\n    status.config(text=\"" + starter + "\")\n\naction = ttk.Button(panel, text=\"Try " + api + "\", command=run_demo)\nentry.pack(pady=6)\naction.pack(pady=6)\nstatus.pack(pady=6)\nroot.mainloop()";
            case "C#" -> "using System;\nusing System.Windows.Forms;\n\npublic class MainForm : Form\n{\n    public MainForm()\n    {\n        Text = \"" + title + "\";\n        var panel = new FlowLayoutPanel { Dock = DockStyle.Fill };\n        var input = new TextBox { Text = \"Desktop app\" };\n        var action = new Button { Text = \"Try " + api + "\" };\n        var status = new Label { Text = \"Ready\" };\n        action.Click += (sender, args) => status.Text = \"" + starter + "\";\n        panel.Controls.Add(input);\n        panel.Controls.Add(action);\n        panel.Controls.Add(status);\n        Controls.Add(panel);\n    }\n    [STAThread] public static void Main() => Application.Run(new MainForm());\n}";
            case "C++" -> "#include <QApplication>\n#include <QLabel>\n#include <QLineEdit>\n#include <QPushButton>\n#include <QVBoxLayout>\n#include <QWidget>\n\nint main(int argc, char *argv[]) {\n    QApplication app(argc, argv);\n    QWidget window;\n    window.setWindowTitle(\"" + title + "\");\n    auto *layout = new QVBoxLayout(&window);\n    auto *input = new QLineEdit(\"Desktop app\");\n    auto *action = new QPushButton(\"Try " + api + "\");\n    auto *status = new QLabel(\"Ready\");\n    layout->addWidget(input); layout->addWidget(action); layout->addWidget(status);\n    QObject::connect(action, &QPushButton::clicked, [=]() { status->setText(\"" + starter + "\"); });\n    window.show();\n    return app.exec();\n}";
            case "JavaScript" -> "document.title = \"" + title + "\";\nconst app = document.querySelector(\"#app\");\napp.innerHTML = `\n  <input id=\"input\" value=\"Desktop app\" />\n  <button id=\"action\">Try " + api + "</button>\n  <p id=\"status\">Ready</p>\n`;\nconst status = document.querySelector(\"#status\");\ndocument.querySelector(\"#action\").addEventListener(\"click\", () => {\n  status.textContent = \"" + starter + "\";\n});";
            default -> "";
        };
    }

    private static String javaCode(String title, String api, String starter) {
        return "import javax.swing.*;\nimport java.awt.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        SwingUtilities.invokeLater(() -> {\n            JFrame frame = new JFrame(\"" + title + "\");\n            JTextField input = new JTextField(\"Desktop app\", 14);\n            JButton action = new JButton(\"Try " + api + "\");\n            JLabel status = new JLabel(\"Ready\");\n            action.addActionListener(e -> status.setText(\"" + starter + "\"));\n            JPanel panel = new JPanel(new FlowLayout());\n            panel.add(input); panel.add(action); panel.add(status);\n            frame.add(panel);\n            frame.setSize(430, 220);\n            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n            frame.setVisible(true);\n        });\n    }\n}";
    }

    private record Course(String id, String title, String shortTitle, String language, String fileName, List<Lesson> lessons) {
        public String toString() { return language + " · " + shortTitle; }
    }
    private record AppBlueprint(String toolkit, String appName, String windowTitle, String message, String buttonLabel, String actionMessage, int width, int height) {}
    private record Lesson(String module, String title, String api, String description, String starter, String goal, String code) {}
    private record QuizQuestion(String question, List<String> options, int answer, String explanation) {}
    private record LessonRef(int index, Lesson lesson) { public String toString() { return lesson.title; } }
    private record Builder(String name, String username, String focus, int xp) {}
    private record CommunityApp(long id, String title, String description, String fileName, String creator) {
        public String toString() { return title + "  ·  @" + creator; }
    }
    private record ForumReply(String authorName, String authorUsername, String body, long createdAt) {}
    private record ForumTopic(long id, String title, String category, String body, String code, String authorName, String authorUsername, Set<String> likedBy, List<ForumReply> replies) {
        public String toString() { return title + "  ·  " + category + "  ·  " + likedBy.size() + " likes"; }
    }
    private record PositionedModule(int position, String name) {}
}
