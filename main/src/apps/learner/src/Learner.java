import character.NernalMood;
import character.NernalPanel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import lessons.Curriculum;
import lessons.Lesson;
import lessons.LessonCatalog;
import profile.AppSettings;
import profile.Profile;
import quiz.Question;
import quiz.QuizManager;
import storage.SaveManager;
import ui.Theme;

/** Main window and navigation coordinator for Learner. */
public final class Learner extends JFrame {
  private static final long serialVersionUID = 1L;
  private final transient LessonCatalog catalog = new LessonCatalog();
  private final transient QuizManager quizManager = new QuizManager();
  private final transient SaveManager saves = new SaveManager();
  private transient AppSettings settings;
  private transient Profile profile;
  private String librarySubject = "All";

  public static void main(String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          Learner app = new Learner();
          app.setVisible(true);
        });
  }

  public Learner() {
    super("Learner");
    settings = saves.loadSettings();
    Theme.install(settings.fontSize());
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    setSize(1050, 780);
    setMinimumSize(new Dimension(800, 620));
    setLocationRelativeTo(null);
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            saveAll();
            dispose();
          }
        });
    showProfileChooser();
  }

  private void showProfileChooser() {
    JPanel page = Theme.page();
    page.add(heading("Welcome to Learner"), BorderLayout.NORTH);
    NernalPanel nernal = nernal(NernalMood.HAPPY);

    DefaultListModel<String> model = new DefaultListModel<>();
    saves.profileNames().forEach(model::addElement);
    JList<String> profiles = new JList<>(model);
    profiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    profiles.setBackground(Theme.PANEL);
    profiles.setForeground(Theme.TEXT);
    profiles.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

    JTextField name = new JTextField();
    name.setToolTipText("1-24 letters, numbers, spaces, underscores, or hyphens");
    JButton create = Theme.button("Create Profile");
    JButton open = Theme.button("Open Selected Profile");
    create.addActionListener(
        e -> {
          try {
            profile = new Profile(name.getText());
            saveProfile();
            showDashboard();
          } catch (IllegalArgumentException ex) {
            warning(ex.getMessage());
          }
        });
    open.addActionListener(
        e -> {
          if (profiles.getSelectedValue() == null) {
            warning("Select a profile first.");
            return;
          }
          profile = saves.loadProfile(profiles.getSelectedValue());
          showDashboard();
        });
    name.addActionListener(e -> create.doClick());

    JPanel controls = new JPanel(new GridLayout(0, 1, 8, 8));
    controls.setOpaque(false);
    controls.add(new JLabel("Local learner profiles (no personal information required):"));
    controls.add(new JScrollPane(profiles));
    controls.add(name);
    controls.add(create);
    controls.add(open);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nernal, controls);
    split.setOpaque(false);
    split.setBorder(null);
    split.setDividerLocation(430);
    split.setResizeWeight(.45);
    page.add(split, BorderLayout.CENTER);
    JLabel note = new JLabel("Progress is saved locally in ~/.learner", SwingConstants.CENTER);
    note.setForeground(Theme.MUTED);
    page.add(note, BorderLayout.SOUTH);
    swap(page);
  }

  private void showDashboard() {
    Lesson current = catalog.byId(profile.currentLessonId());
    JPanel page = Theme.page();
    page.add(heading("Welcome back, " + profile.name() + "!"), BorderLayout.NORTH);

    JPanel summary = card(new GridLayout(1, 4, 12, 0));
    summary.add(stat("Level", profile.level() + ""));
    summary.add(stat("Points", String.format("%,d", profile.points())));
    summary.add(stat("Lessons", profile.completedLessonIds().size() + " / 10,000"));
    summary.add(stat("Quiz Average", profile.averageQuizScore() + "%"));

    JPanel courses = new JPanel(new GridLayout(5, 2, 10, 10));
    courses.setOpaque(false);
    for (String subject : Curriculum.SUBJECTS) {
      int completed = profile.completedForSubject(subject);
      JButton course = Theme.button(subject + "  " + completed + " / 1,000");
      course.addActionListener(e -> showCourse(subject));
      courses.add(course);
    }
    JProgressBar overall =
        progress(
            profile.completedLessonIds().size(),
            10_000,
            "Overall Progress — " + profile.completedLessonIds().size() + " / 10,000");
    JPanel middle = new JPanel(new BorderLayout(10, 10));
    middle.setOpaque(false);
    middle.add(summary, BorderLayout.NORTH);
    middle.add(courses, BorderLayout.CENTER);
    middle.add(overall, BorderLayout.SOUTH);

    JButton resume = Theme.button("Continue: " + current.subject() + " #" + current.id());
    JButton library = Theme.button("Lesson Library");
    JButton progress = Theme.button("Progress");
    JButton achievements = Theme.button("Achievements");
    JButton leaderboard = Theme.button("Local Leaderboard");
    JButton settingsButton = Theme.button("Settings");
    JButton profiles = Theme.button("Switch Profile");
    resume.addActionListener(e -> showLesson(current));
    library.addActionListener(e -> showLibrary("All"));
    progress.addActionListener(e -> showProgress());
    achievements.addActionListener(e -> showAchievements());
    leaderboard.addActionListener(e -> showLeaderboard());
    settingsButton.addActionListener(e -> showSettings());
    profiles.addActionListener(
        e -> {
          saveProfile();
          showProfileChooser();
        });
    JPanel nav = new JPanel(new GridLayout(2, 3, 8, 8));
    nav.setOpaque(false);
    for (JButton b :
        new JButton[] {resume, library, progress, achievements, leaderboard, settingsButton})
      nav.add(b);
    JPanel south = new JPanel(new BorderLayout(8, 8));
    south.setOpaque(false);
    south.add(nav);
    south.add(profiles, BorderLayout.SOUTH);
    page.add(middle);
    page.add(south, BorderLayout.SOUTH);
    swap(page);
  }

  private void showCourse(String subject) {
    int completed = profile.completedForSubject(subject);
    JPanel page = Theme.page();
    page.add(heading(subject + " Course"), BorderLayout.NORTH);
    JPanel center = new JPanel(new BorderLayout(10, 10));
    center.setOpaque(false);
    JProgressBar bar = progress(completed, 1000, completed + " of 1,000 lessons completed");
    center.add(bar, BorderLayout.NORTH);
    JPanel topics = new JPanel(new GridLayout(5, 2, 10, 10));
    topics.setOpaque(false);
    List<String> topicNames = Curriculum.topics(subject);
    for (int i = 0; i < topicNames.size(); i++) {
      int start = i * 100 + 1;
      JButton button = Theme.button(start + "–" + (start + 99) + "  " + topicNames.get(i));
      String topic = topicNames.get(i);
      button.addActionListener(e -> showLibrary(subject, topic));
      topics.add(button);
    }
    center.add(topics);
    page.add(center);
    JButton back = Theme.button("Dashboard");
    JButton browse = Theme.button("Browse All 1,000");
    JButton quiz = Theme.button("Take Quiz");
    back.addActionListener(e -> showDashboard());
    browse.addActionListener(e -> showLibrary(subject));
    quiz.addActionListener(e -> startQuiz(subject));
    page.add(buttonRow(back, browse, quiz), BorderLayout.SOUTH);
    swap(page);
  }

  private void showLibrary(String subject) {
    showLibrary(subject, "All");
  }

  private void showLibrary(String subject, String initialTopic) {
    librarySubject = subject;
    JPanel page = Theme.page();
    page.add(heading("10,000 Lesson Library"), BorderLayout.NORTH);
    JTextField query = new JTextField();
    JComboBox<String> subjects = new JComboBox<>(withAll(Curriculum.SUBJECTS));
    subjects.setSelectedItem(subject);
    JComboBox<String> topics = new JComboBox<>();
    JComboBox<String> difficulty =
        new JComboBox<>(new String[] {"All", "Beginner", "Intermediate", "Advanced"});
    JComboBox<String> completion =
        new JComboBox<>(new String[] {"All", "Completed", "Not Completed"});
    DefaultListModel<Lesson> model = new DefaultListModel<>();
    JList<Lesson> list = new JList<>(model);
    list.setBackground(Theme.PANEL);
    list.setForeground(Theme.TEXT);
    list.setFixedCellHeight(30);
    list.setCellRenderer(
        new DefaultListCellRenderer() {
          @Override
          public Component getListCellRendererComponent(
              JList<?> owner, Object value, int index, boolean selected, boolean focus) {
            JLabel label =
                (JLabel) super.getListCellRendererComponent(owner, value, index, selected, focus);
            Lesson lesson = (Lesson) value;
            String status =
                profile.completedLessonIds().contains(lesson.id())
                    ? "Completed ✓"
                    : lesson.id() == profile.currentLessonId() ? "Current" : "Not Started";
            label.setText("[" + status + "]  " + lesson);
            if (!selected) {
              label.setBackground(Theme.PANEL);
              label.setForeground(Theme.TEXT);
            }
            return label;
          }
        });
    JLabel count = new JLabel();
    count.setForeground(Theme.MUTED);
    Runnable fillTopics =
        () -> {
          String selected = (String) subjects.getSelectedItem();
          topics.removeAllItems();
          topics.addItem("All");
          if (!"All".equals(selected)) Curriculum.topics(selected).forEach(topics::addItem);
          topics.setSelectedItem(initialTopic);
        };
    Runnable search =
        () -> {
          model.clear();
          List<Lesson> found =
              catalog.search(
                  query.getText(),
                  (String) subjects.getSelectedItem(),
                  (String) topics.getSelectedItem(),
                  (String) difficulty.getSelectedItem(),
                  (String) completion.getSelectedItem(),
                  profile.completedLessonIds());
          found.forEach(model::addElement);
          count.setText(String.format("%,d lessons found", found.size()));
          if (!model.isEmpty()) list.setSelectedIndex(0);
        };
    subjects.addActionListener(
        e -> {
          fillTopics.run();
          search.run();
        });
    topics.addActionListener(e -> search.run());
    difficulty.addActionListener(e -> search.run());
    completion.addActionListener(e -> search.run());
    query.addActionListener(e -> search.run());
    fillTopics.run();
    search.run();
    JPanel filters = new JPanel(new GridLayout(2, 3, 8, 8));
    filters.setOpaque(false);
    filters.add(query);
    filters.add(subjects);
    filters.add(topics);
    filters.add(difficulty);
    filters.add(completion);
    filters.add(count);
    JPanel center = new JPanel(new BorderLayout(8, 8));
    center.setOpaque(false);
    center.add(filters, BorderLayout.NORTH);
    center.add(new JScrollPane(list));
    page.add(center);
    JButton back = Theme.button("Dashboard");
    JButton favorites = Theme.button("★ Favorites");
    JButton open = Theme.button("Open Lesson");
    back.addActionListener(e -> showDashboard());
    favorites.addActionListener(
        e -> {
          model.clear();
          profile.favoriteLessonIds().stream()
              .sorted()
              .map(catalog::byId)
              .forEach(model::addElement);
          count.setText(model.size() + " favorites");
        });
    open.addActionListener(
        e -> {
          if (list.getSelectedValue() != null) showLesson(list.getSelectedValue());
        });
    list.addMouseListener(
        new java.awt.event.MouseAdapter() {
          @Override
          public void mouseClicked(java.awt.event.MouseEvent e) {
            if (e.getClickCount() == 2 && list.getSelectedValue() != null)
              showLesson(list.getSelectedValue());
          }
        });
    page.add(buttonRow(back, favorites, open), BorderLayout.SOUTH);
    swap(page);
  }

  private void showLesson(Lesson lesson) {
    profile.viewLesson(lesson.id(), lesson.subject());
    saveProfile();
    JPanel page = Theme.page();
    page.add(heading(lesson.title()), BorderLayout.NORTH);
    JTextArea text = new JTextArea(lesson.displayText());
    text.setEditable(false);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    text.setBackground(Theme.PANEL);
    text.setForeground(Theme.TEXT);
    text.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    page.add(new JScrollPane(text));
    JButton previous = Theme.button("← Previous");
    JButton favorite =
        Theme.button(
            profile.favoriteLessonIds().contains(lesson.id()) ? "★ Favorited" : "☆ Favorite");
    JButton complete =
        Theme.button(
            profile.completedLessonIds().contains(lesson.id()) ? "Completed ✓" : "Complete +10");
    JButton next = Theme.button("Next →");
    previous.setEnabled(lesson.level() > 1);
    next.setEnabled(lesson.level() < 1000);
    previous.addActionListener(e -> showLesson(catalog.byId(lesson.id() - 1)));
    next.addActionListener(e -> showLesson(catalog.byId(lesson.id() + 1)));
    favorite.addActionListener(
        e -> {
          profile.toggleFavorite(lesson.id());
          saveProfile();
          showLesson(lesson);
        });
    complete.addActionListener(
        e -> {
          boolean awarded = profile.completeLesson(lesson.id(), lesson.subject());
          saveProfile();
          JOptionPane.showMessageDialog(
              this,
              awarded
                  ? "Lesson completed — 10 points earned!"
                  : "Already completed — no duplicate points awarded.");
          showLesson(lesson);
        });
    JButton library = Theme.button("Course Library");
    library.addActionListener(e -> showLibrary(lesson.subject()));
    JPanel bottom = new JPanel(new GridLayout(1, 5, 7, 0));
    bottom.setOpaque(false);
    for (JButton b : new JButton[] {previous, favorite, complete, next, library}) bottom.add(b);
    page.add(bottom, BorderLayout.SOUTH);
    swap(page);
  }

  private void startQuiz(String subject) {
    int level = Math.max(1, Math.min(1000, profile.completedForSubject(subject) + 1));
    List<Question> questions = quizManager.create(subject, level);
    runQuestion(subject, questions, 0, 0);
  }

  private void runQuestion(String subject, List<Question> questions, int index, int score) {
    Question q = questions.get(index);
    JPanel page = Theme.page();
    page.add(
        heading(subject + " Quiz — " + (index + 1) + " / " + questions.size()), BorderLayout.NORTH);
    JTextArea prompt = new JTextArea(q.prompt());
    prompt.setEditable(false);
    prompt.setLineWrap(true);
    prompt.setWrapStyleWord(true);
    prompt.setOpaque(false);
    prompt.setForeground(Theme.TEXT);
    JPanel answers = new JPanel(new GridLayout(q.options().length, 1, 8, 8));
    answers.setOpaque(false);
    JLabel feedback = new JLabel("Choose the best answer.", SwingConstants.CENTER);
    JButton next = Theme.button(index == questions.size() - 1 ? "See Results" : "Next Question");
    next.setEnabled(false);
    String[] options = q.options();
    for (int i = 0; i < options.length; i++) {
      JButton answer = Theme.button(options[i]);
      final int selected = i;
      answer.addActionListener(
          e -> {
            boolean correct = selected == q.correctIndex();
            feedback.setText((correct ? "Correct ✓ — " : "Incorrect ✗ — ") + q.explanation());
            feedback.setForeground(correct ? Theme.SUCCESS : Theme.ERROR);
            for (Component c : answers.getComponents()) c.setEnabled(false);
            next.setEnabled(true);
            next.putClientProperty("score", score + (correct ? 1 : 0));
          });
      answers.add(answer);
    }
    next.addActionListener(
        e -> {
          int newScore = (int) next.getClientProperty("score");
          if (index + 1 < questions.size()) runQuestion(subject, questions, index + 1, newScore);
          else showResults(subject, newScore, questions.size());
        });
    JPanel center = new JPanel(new BorderLayout(12, 12));
    center.setOpaque(false);
    center.add(prompt, BorderLayout.NORTH);
    center.add(answers);
    center.add(feedback, BorderLayout.SOUTH);
    page.add(center);
    page.add(next, BorderLayout.SOUTH);
    swap(page);
  }

  private void showResults(String subject, int score, int total) {
    int percent = Math.round(score * 100f / total);
    int awarded = profile.recordQuiz(subject, percent);
    saveProfile();
    JPanel page = Theme.page();
    page.add(heading(subject + " Results"), BorderLayout.NORTH);
    NernalPanel nernal = nernal(percent >= 80 ? NernalMood.CELEBRATING : NernalMood.ENCOURAGING);
    JLabel result =
        new JLabel(
            "<html><center><h1>"
                + score
                + " / "
                + total
                + " — "
                + percent
                + "%</h1><p>Best: "
                + profile.bestQuizScores().get(subject)
                + "%</p><p>"
                + awarded
                + " points earned from improvement.</p></center></html>",
            SwingConstants.CENTER);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nernal, result);
    split.setBorder(null);
    split.setOpaque(false);
    split.setDividerLocation(480);
    page.add(split);
    JButton course = Theme.button("Course");
    JButton retry = Theme.button("Retry");
    JButton dashboard = Theme.button("Dashboard");
    course.addActionListener(e -> showCourse(subject));
    retry.addActionListener(e -> startQuiz(subject));
    dashboard.addActionListener(e -> showDashboard());
    page.add(buttonRow(course, retry, dashboard), BorderLayout.SOUTH);
    swap(page);
  }

  private void showProgress() {
    JPanel page = Theme.page();
    page.add(heading("Learning Progress"), BorderLayout.NORTH);
    JPanel bars = new JPanel();
    bars.setLayout(new BoxLayout(bars, BoxLayout.Y_AXIS));
    bars.setOpaque(false);
    bars.add(
        progress(
            profile.completedLessonIds().size(),
            10_000,
            "Overall — " + profile.completedLessonIds().size() + " / 10,000"));
    for (String subject : Curriculum.SUBJECTS) {
      bars.add(Box.createVerticalStrut(10));
      int n = profile.completedForSubject(subject);
      bars.add(
          progress(
              n,
              1000,
              subject
                  + " — "
                  + n
                  + " / 1,000 • Best quiz "
                  + profile.bestQuizScores().getOrDefault(subject, 0)
                  + "%"));
    }
    page.add(new JScrollPane(bars));
    JButton dashboard = Theme.button("Dashboard");
    JButton history = Theme.button("Recent History");
    history.addActionListener(e -> showHistory());
    dashboard.addActionListener(e -> showDashboard());
    page.add(buttonRow(dashboard, history), BorderLayout.SOUTH);
    swap(page);
  }

  private void showHistory() {
    JPanel page = Theme.page();
    page.add(heading("Recently Viewed"), BorderLayout.NORTH);
    DefaultListModel<Lesson> model = new DefaultListModel<>();
    profile.history().forEach(id -> model.addElement(catalog.byId(id)));
    JList<Lesson> list = new JList<>(model);
    list.setBackground(Theme.PANEL);
    list.setForeground(Theme.TEXT);
    page.add(new JScrollPane(list));
    JButton back = Theme.button("Progress");
    JButton open = Theme.button("Open");
    back.addActionListener(e -> showProgress());
    open.addActionListener(
        e -> {
          if (list.getSelectedValue() != null) showLesson(list.getSelectedValue());
        });
    page.add(buttonRow(back, open), BorderLayout.SOUTH);
    swap(page);
  }

  private void showAchievements() {
    String[][] all = {
      {"First Step", "Complete your first lesson."},
      {"Math Starter", "Complete 10 Math lessons."},
      {"Coder", "Complete 25 Coding lessons."},
      {"Perfect!", "Get 100% on a quiz."},
      {"Explorer", "Open all 10 courses."},
      {"Century", "Complete 100 lessons."},
      {"Dedicated Learner", "Complete 500 lessons."}
    };
    JPanel page = Theme.page();
    page.add(heading("Achievements"), BorderLayout.NORTH);
    JPanel list = new JPanel(new GridLayout(0, 1, 8, 8));
    list.setOpaque(false);
    for (String[] a : all) {
      boolean earned = profile.achievements().contains(a[0]);
      JLabel row = new JLabel((earned ? "✓ " : "○ ") + a[0] + " — " + a[1]);
      row.setOpaque(true);
      row.setBackground(Theme.PANEL);
      row.setForeground(earned ? Theme.ACCENT : Theme.MUTED);
      row.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
      list.add(row);
    }
    page.add(list);
    JButton back = Theme.button("Dashboard");
    back.addActionListener(e -> showDashboard());
    page.add(back, BorderLayout.SOUTH);
    swap(page);
  }

  private void showLeaderboard() {
    List<Profile> local = new ArrayList<>();
    for (String name : saves.profileNames()) local.add(saves.loadProfile(name));
    local.sort(Comparator.comparingInt(Profile::points).reversed());
    JPanel page = Theme.page();
    page.add(heading("Local Leaderboard"), BorderLayout.NORTH);
    String[] columns = {"Rank", "Name", "Points", "Lessons", "Quiz Average"};
    Object[][] data = new Object[local.size()][5];
    for (int i = 0; i < local.size(); i++) {
      Profile p = local.get(i);
      data[i] =
          new Object[] {
            i + 1, p.name(), p.points(), p.completedLessonIds().size(), p.averageQuizScore() + "%"
          };
    }
    JTable table = new JTable(data, columns);
    table.setRowHeight(32);
    table.setEnabled(false);
    page.add(new JScrollPane(table));
    JLabel note =
        new JLabel("Local profiles only — no online or simulated users.", SwingConstants.CENTER);
    note.setForeground(Theme.MUTED);
    JButton back = Theme.button("Dashboard");
    back.addActionListener(e -> showDashboard());
    JPanel south = new JPanel(new BorderLayout());
    south.setOpaque(false);
    south.add(note);
    south.add(back, BorderLayout.SOUTH);
    page.add(south, BorderLayout.SOUTH);
    swap(page);
  }

  private void showSettings() {
    JPanel page = Theme.page();
    page.add(heading("Settings & Profile"), BorderLayout.NORTH);
    JSpinner font = new JSpinner(new SpinnerNumberModel(settings.fontSize(), 14, 28, 1));
    JCheckBox animation = new JCheckBox("Animate Nernal", settings.animation());
    animation.setOpaque(false);
    animation.setForeground(Theme.TEXT);
    JCheckBox sound = new JCheckBox("Sound (placeholder)", settings.sound());
    sound.setOpaque(false);
    sound.setForeground(Theme.TEXT);
    JTextField name = new JTextField(profile.name());
    JPanel form = card(new GridLayout(0, 2, 12, 12));
    form.add(new JLabel("Display name"));
    form.add(name);
    form.add(new JLabel("Font size (restart to fully apply)"));
    form.add(font);
    form.add(animation);
    form.add(sound);
    page.add(form);
    JButton save = Theme.button("Save Settings");
    JButton reset = Theme.button("Reset This Profile");
    JButton back = Theme.button("Dashboard");
    save.addActionListener(
        e -> {
          try {
            String oldName = profile.name();
            profile.rename(name.getText());
            if (!oldName.equals(profile.name())) saves.deleteProfile(new Profile(oldName));
            settings.setFontSize((int) font.getValue());
            settings.setAnimation(animation.isSelected());
            settings.setSound(sound.isSelected());
            saves.saveSettings(settings);
            saveProfile();
            showDashboard();
          } catch (IllegalArgumentException | IOException ex) {
            warning("Could not save settings: " + ex.getMessage());
          }
        });
    reset.addActionListener(
        e -> {
          int answer =
              JOptionPane.showConfirmDialog(
                  this,
                  "Permanently reset " + profile.name() + "?",
                  "Confirm reset",
                  JOptionPane.YES_NO_OPTION);
          if (answer == JOptionPane.YES_OPTION)
            try {
              saves.deleteProfile(profile);
              profile = new Profile(profile.name());
              saveProfile();
              showDashboard();
            } catch (IOException ex) {
              warning("Could not reset profile: " + ex.getMessage());
            }
        });
    back.addActionListener(e -> showDashboard());
    page.add(buttonRow(back, reset, save), BorderLayout.SOUTH);
    swap(page);
  }

  private NernalPanel nernal(NernalMood mood) {
    NernalPanel panel = new NernalPanel();
    panel.setMood(mood);
    panel.setAnimationEnabled(settings.animation());
    return panel;
  }

  private JLabel heading(String text) {
    JLabel label = new JLabel(text, SwingConstants.CENTER);
    label.setForeground(Theme.TEXT);
    label.setFont(new Font("SansSerif", Font.BOLD, 32));
    return label;
  }

  private JPanel card(LayoutManager layout) {
    JPanel p = new JPanel(layout);
    p.setBackground(Theme.PANEL);
    p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
    return p;
  }

  private JPanel stat(String name, String value) {
    JPanel p = new JPanel(new GridLayout(2, 1));
    p.setOpaque(false);
    JLabel a = new JLabel(name, SwingConstants.CENTER);
    a.setForeground(Theme.MUTED);
    JLabel b = new JLabel(value, SwingConstants.CENTER);
    b.setForeground(Theme.ACCENT);
    b.setFont(b.getFont().deriveFont(Font.BOLD, 22f));
    p.add(a);
    p.add(b);
    return p;
  }

  private JProgressBar progress(int value, int max, String text) {
    JProgressBar bar = new JProgressBar(0, max);
    bar.setValue(value);
    bar.setStringPainted(true);
    bar.setString(text);
    bar.setPreferredSize(new Dimension(200, 32));
    return bar;
  }

  private JPanel buttonRow(JButton... buttons) {
    JPanel p = new JPanel(new GridLayout(1, buttons.length, 8, 0));
    p.setOpaque(false);
    for (JButton b : buttons) p.add(b);
    return p;
  }

  private String[] withAll(List<String> values) {
    String[] result = new String[values.size() + 1];
    result[0] = "All";
    for (int i = 0; i < values.size(); i++) result[i + 1] = values.get(i);
    return result;
  }

  private void swap(JPanel page) {
    setContentPane(page);
    revalidate();
    repaint();
  }

  private void saveProfile() {
    if (profile == null) return;
    try {
      saves.saveProfile(profile);
    } catch (IOException ex) {
      warning("Could not save progress. Your current session can continue.\n" + ex.getMessage());
    }
  }

  private void saveAll() {
    saveProfile();
    try {
      saves.saveSettings(settings);
    } catch (IOException ex) {
      System.err.println("Could not save settings: " + ex.getMessage());
    }
  }

  private void warning(String message) {
    JOptionPane.showMessageDialog(this, message, "Learner", JOptionPane.WARNING_MESSAGE);
  }
}
