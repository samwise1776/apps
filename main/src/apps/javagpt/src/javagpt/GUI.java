package javagpt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class GUI extends JFrame {
    private static final Color BG = new Color(18, 18, 24);
    private static final Color PANEL = new Color(28, 28, 36);
    private static final Color TEXT = new Color(220, 220, 230);
    private static final Color MUTED = new Color(140, 140, 160);
    private static final Color ACCENT = new Color(100, 160, 255);
    private static final Color GREEN = new Color(100, 220, 140);
    private static final Color RED = new Color(220, 100, 100);

    private GPT model;
    private Tokenizer tokenizer;
    private Generator generator;
    private GPTConfig config = GPTConfig.tiny();
    private Thread trainThread;

    // UI components
    private final JTextArea chatOutput = new JTextArea();
    private final JTextField chatInput = new JTextField();
    private final JTextArea trainLog = new JTextArea();
    private final JTextArea infoArea = new JTextArea();
    private final JProgressBar trainProgress = new JProgressBar();
    private final JLabel statusLabel = new JLabel("No model loaded");
    private final JComboBox<String> configBox = new JComboBox<>(new String[]{"tiny", "small", "medium"});
    private final JSpinner stepsSpinner = new JSpinner(new SpinnerNumberModel(2000, 100, 100000, 100));
    private final JSpinner tempSpinner = new JSpinner(new SpinnerNumberModel(0.8, 0.1, 2.0, 0.1));
    private final JSpinner topKSpinner = new JSpinner(new SpinnerNumberModel(40, 1, 256, 1));
    private final JButton trainBtn = new JButton("Train");
    private final JButton stopBtn = new JButton("Stop");
    private final JButton saveBtn = new JButton("Save Model");
    private final JButton loadBtn = new JButton("Load Model");

    public GUI() {
        super("JavaGPT — Transformer Language Model");
        installLookAndFeel();
        buildUI();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (trainThread != null && trainThread.isAlive()) {
                    trainThread.interrupt();
                    trainThread = null;
                }
                dispose();
                System.exit(0);
            }
        });
        setSize(1100, 750);
        setLocationRelativeTo(null);
    }

    private void installLookAndFeel() {
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextArea.background", PANEL);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextField.background", PANEL);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("ComboBox.background", PANEL);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("Button.background", new Color(50, 60, 80));
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("ProgressBar.background", new Color(40, 40, 55));
        UIManager.put("ProgressBar.foreground", ACCENT);
    }

    private void buildUI() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setForeground(TEXT);
        tabs.addTab("Chat", buildChatPanel());
        tabs.addTab("Training", buildTrainingPanel());
        tabs.addTab("Model Settings", buildSettingsPanel());
        tabs.addTab("Dataset", buildDatasetPanel());
        tabs.addTab("Model Info", buildInfoPanel());
        add(tabs, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JComponent buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        chatOutput.setEditable(false);
        chatOutput.setBackground(PANEL);
        chatOutput.setForeground(TEXT);
        chatOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        chatOutput.setLineWrap(true);
        chatOutput.setWrapStyleWord(true);

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setBackground(BG);
        chatInput.setBackground(PANEL);
        chatInput.setForeground(TEXT);
        chatInput.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        chatInput.setToolTipText("Type your message");
        chatInput.addActionListener(e -> sendChat());

        JButton sendBtn = new JButton("Send");
        sendBtn.setBackground(ACCENT);
        sendBtn.addActionListener(e -> sendChat());

        inputRow.add(chatInput, BorderLayout.CENTER);
        inputRow.add(sendBtn, BorderLayout.EAST);

        panel.add(new JScrollPane(chatOutput), BorderLayout.CENTER);
        panel.add(inputRow, BorderLayout.SOUTH);
        return panel;
    }

    private void sendChat() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;
        if (model == null || tokenizer == null) {
            chatOutput.append("You: " + text + "\n[No model loaded. Train or load a model first.]\n\n");
            chatInput.setText("");
            return;
        }
        chatInput.setText("");
        chatOutput.append("You: " + text + "\n");

        float temp = ((Number) tempSpinner.getValue()).floatValue();
        int topK = ((Number) topKSpinner.getValue()).intValue();

        new Thread(() -> {
            String response = generator.generate(text, 200, temp, topK);
            SwingUtilities.invokeLater(() -> {
                chatOutput.append("AI: " + response + "\n\n");
                chatOutput.setCaretPosition(chatOutput.getDocument().getLength());
            });
        }).start();
    }

    private JComponent buildTrainingPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setBackground(BG);
        controls.add(new JLabel("Config:"));
        controls.add(configBox);
        controls.add(new JLabel("Steps:"));
        controls.add(stepsSpinner);
        trainBtn.addActionListener(e -> startTraining());
        stopBtn.addActionListener(e -> stopTraining());
        stopBtn.setEnabled(false);
        controls.add(trainBtn);
        controls.add(stopBtn);

        trainLog.setEditable(false);
        trainLog.setBackground(PANEL);
        trainLog.setForeground(TEXT);
        trainLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        trainProgress.setStringPainted(true);

        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(trainLog), BorderLayout.CENTER);
        panel.add(trainProgress, BorderLayout.SOUTH);
        return panel;
    }

    private void startTraining() {
        if (trainThread != null && trainThread.isAlive()) return;
        String cfgName = (String) configBox.getSelectedItem();
        config = switch (cfgName) {
            case "small" -> GPTConfig.small();
            case "medium" -> GPTConfig.medium();
            default -> GPTConfig.tiny();
        };
        config.trainingSteps = ((Number) stepsSpinner.getValue()).intValue();

        trainBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        trainLog.setText("");
        trainProgress.setValue(0);

        trainThread = new Thread(() -> {
            try {
                String dataPath = "data/training.txt";
                if (!Files.exists(Path.of(dataPath))) {
                    SwingUtilities.invokeLater(() -> {
                        trainLog.append("ERROR: " + dataPath + " not found!\n");
                        trainBtn.setEnabled(true);
                        stopBtn.setEnabled(false);
                    });
                    return;
                }
                String text = Files.readString(Path.of(dataPath));
                tokenizer = new Tokenizer();
                tokenizer.buildFromText(text);
                config.vocabSize = tokenizer.getVocabSize();
                config.recalc();

                SwingUtilities.invokeLater(() -> {
                    trainLog.append("Building tokenizer: " + tokenizer.vocabInfo() + "\n");
                    config.printInfo();
                });

                model = new GPT(config, new Random(42));
                generator = new Generator(model, tokenizer);
                Dataset dataset = Dataset.fromFile(dataPath, tokenizer, config.contextLength);

                SwingUtilities.invokeLater(() -> trainLog.append("Starting training...\n\n"));

                GPT m = model;
                GPTConfig cfg = config;
                AdamW opt = new AdamW(cfg.learningRate, cfg.beta1, cfg.beta2, cfg.epsilon, cfg.weightDecay);
                opt.registerAllWithGrads(m);

                int totalSteps = cfg.trainingSteps;
                long startTime = System.currentTimeMillis();
                int totalTokens = 0;

                for (int step = 1; step <= totalSteps; step++) {
                    if (Thread.currentThread().isInterrupted()) break;
                    dataset.reset();
                    float stepLoss = 0;
                    int batchesThisStep = 0;

                    while (dataset.hasMore() && batchesThisStep < 10) {
                        int[][][] batch = dataset.getNextBatch(cfg.batchSize);
                        Tensor logits = m.forward(batch[0]);
                        Tensor gradLogits = new Tensor(logits.shape.clone());
                        float loss = m.computeLoss(logits, batch[1], gradLogits);
                        stepLoss += loss;
                        batchesThisStep++;
                        m.backward(gradLogits);
                        opt.stepWithGrads();
                        m.zeroAllGrads();
                        totalTokens += cfg.batchSize * cfg.contextLength;
                    }

                    float avgLoss = stepLoss / Math.max(batchesThisStep, 1);

                    if (step % 10 == 0 || step == 1 || step == totalSteps) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        float tps = (elapsed > 0) ? (totalTokens * 1000f / elapsed) : 0;
                        final int s = step;
                        final float l = avgLoss;
                        final float t = tps;
                        final int pct = (int)(100L * step / totalSteps);
                        SwingUtilities.invokeLater(() -> {
                            trainLog.append(String.format("Step %,6d / %,d  loss=%.4f  [%.1f tok/s]%n", s, totalSteps, l, t));
                            trainLog.setCaretPosition(trainLog.getDocument().getLength());
                            trainProgress.setValue(pct);
                            trainProgress.setString(pct + "%");
                            statusLabel.setText(String.format("Training step %,d — loss %.4f", s, l));
                        });
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    trainLog.append("\nTraining complete.\n");
                    statusLabel.setText("Training complete — model ready");
                    trainBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    generator = new Generator(model, tokenizer);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    trainLog.append("ERROR: " + ex.getMessage() + "\n");
                    trainLog.append(ex.getClass().getName() + "\n");
                    trainBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                });
            }
        }, "javagpt-gui-trainer");
        trainThread.setDaemon(true);
        trainThread.start();
    }

    private void stopTraining() {
        if (trainThread != null) {
            trainThread.interrupt();
            trainThread = null;
        }
        trainBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        statusLabel.setText("Training stopped");
    }

    private JComponent buildSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        for (String[] pair : new String[][]{
                {"Configuration:", null},
                {"Training steps:", null},
                {"Temperature:", null},
                {"Top-k:", null}}) {
            JPanel r = new JPanel(new BorderLayout(8, 0));
            r.setBackground(PANEL);
            r.setBorder(new EmptyBorder(8, 12, 8, 12));
            JLabel l = new JLabel(pair[0]);
            l.setForeground(MUTED);
            r.add(l, BorderLayout.WEST);
            r.add(switch (pair[0]) {
                case "Configuration:" -> configBox;
                case "Training steps:" -> stepsSpinner;
                case "Temperature:" -> tempSpinner;
                case "Top-k:" -> topKSpinner;
                default -> new JLabel("");
            }, BorderLayout.EAST);
            panel.add(r);
            panel.add(Box.createVerticalStrut(4));
        }
        panel.add(Box.createVerticalGlue());

        // Save/Load
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setBackground(BG);
        saveBtn.addActionListener(e -> saveModel());
        loadBtn.addActionListener(e -> loadModel());
        btnRow.add(saveBtn);
        btnRow.add(loadBtn);
        panel.add(btnRow);

        return panel;
    }

    private JComponent buildDatasetPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(PANEL);
        area.setForeground(TEXT);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBorder(new EmptyBorder(8, 8, 8, 8));

        try {
            if (Files.exists(Path.of("data/training.txt"))) {
                String text = Files.readString(Path.of("data/training.txt"));
                area.setText("File: data/training.txt\n" +
                        "Characters: " + text.length() + "\n\n" +
                        text.substring(0, Math.min(2000, text.length())) +
                        (text.length() > 2000 ? "\n\n... (truncated)" : ""));
            } else {
                area.setText("No training data found at data/training.txt");
            }
        } catch (IOException e) {
            area.setText("Error reading training data: " + e.getMessage());
        }

        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        infoArea.setEditable(false);
        infoArea.setBackground(PANEL);
        infoArea.setForeground(TEXT);
        infoArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        infoArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> updateInfo());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(BG);
        top.add(refreshBtn);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        return panel;
    }

    private void updateInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== JavaGPT Model Information ===\n\n");

        if (model == null) {
            sb.append("No model loaded.\n");
            sb.append("Train a new model or load an existing one.\n");
        } else {
            GPTConfig c = model.config;
            long params = c.parameterCount();
            sb.append(String.format("Configuration:   %s%n", c.name));
            sb.append(String.format("Parameters:      %,d (~%.2fM)%n", params, params / 1e6));
            sb.append(String.format("Vocabulary:      %d%n", c.vocabSize));
            sb.append(String.format("Embedding dim:   %d%n", c.embedDim));
            sb.append(String.format("Layers:          %d%n", c.numLayers));
            sb.append(String.format("Attention heads: %d%n", c.numHeads));
            sb.append(String.format("Head dim:        %d%n", c.headDim));
            sb.append(String.format("FF dim:          %d%n", c.ffDim));
            sb.append(String.format("Context length:  %d%n", c.contextLength));
            sb.append(String.format("Est RAM:         ~%.1f MB%n", c.estimatedRamBytes() / 1e6));
            sb.append(String.format("Weight size:     ~%.1f MB%n", (params * 4L) / 1e6));
        }

        File modelFile = new File("models/javagpt.bin");
        if (modelFile.exists()) {
            sb.append(String.format("%nSaved model:     models/javagpt.bin%n"));
            sb.append(String.format("Model file size: %.2f MB%n", modelFile.length() / 1e6));
        } else {
            sb.append("\nNo saved model found.\n");
        }

        infoArea.setText(sb.toString());
    }

    private void saveModel() {
        if (model == null || tokenizer == null) {
            JOptionPane.showMessageDialog(this, "No model to save. Train or load a model first.");
            return;
        }
        try {
            Files.createDirectories(Paths.get("models"));
            Checkpoint.saveFull(model, tokenizer, "models/javagpt.bin", "models/tokenizer.bin");
            JOptionPane.showMessageDialog(this, "Model saved to models/javagpt.bin");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Save failed: " + e.getMessage());
        }
    }

    private void loadModel() {
        try {
            if (!Files.exists(Paths.get("models/javagpt.bin"))) {
                JOptionPane.showMessageDialog(this, "No saved model found at models/javagpt.bin");
                return;
            }
            model = Checkpoint.loadModel("models/javagpt.bin");
            tokenizer = Checkpoint.loadTokenizer("models/tokenizer.bin");
            generator = new Generator(model, tokenizer);
            config = model.config;
            statusLabel.setText("Model loaded — " + config.name + " — " + config.parameterCount() + " params");
            updateInfo();
            JOptionPane.showMessageDialog(this, "Model loaded successfully.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Load failed: " + e.getMessage());
        }
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(12, 12, 18));
        footer.setBorder(new EmptyBorder(6, 12, 6, 12));
        statusLabel.setForeground(MUTED);
        footer.add(statusLabel, BorderLayout.WEST);
        JLabel ver = new JLabel("JavaGPT v1.0");
        ver.setForeground(MUTED);
        footer.add(ver, BorderLayout.EAST);
        return footer;
    }
}
