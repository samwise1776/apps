package utilities;

import components.RoundedButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;

public class AppGen extends JPanel {

    private final JTextField classNameField;
    private final JTextField titleField;

    private final JSpinner widthSpinner;
    private final JSpinner heightSpinner;

    private final JCheckBox buttonBox;
    private final JCheckBox labelBox;
    private final JCheckBox textFieldBox;

    private final JTextArea codeArea;

    private Color appBackground =
            new Color(35, 34, 59);

    public AppGen() {

        setLayout(
                new BorderLayout(
                        15,
                        15
                )
        );

        setBackground(
                new Color(
                        24,
                        25,
                        40
                )
        );

        setBorder(
                new EmptyBorder(
                        20,
                        20,
                        20,
                        20
                )
        );

        // ==========================================
        // TITLE
        // ==========================================

        JLabel header =
                new JLabel(
                        "App Generator"
                );

        header.setForeground(
                Color.WHITE
        );

        header.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        28
                )
        );

        add(
                header,
                BorderLayout.NORTH
        );

        // ==========================================
        // SETTINGS
        // ==========================================

        JPanel settings =
                new JPanel();

        settings.setLayout(
                new BoxLayout(
                        settings,
                        BoxLayout.Y_AXIS
                )
        );

        settings.setBackground(
                new Color(
                        32,
                        34,
                        50
                )
        );

        settings.setBorder(
                new EmptyBorder(
                        20,
                        20,
                        20,
                        20
                )
        );

        settings.setPreferredSize(
                new Dimension(
                        280,
                        500
                )
        );

        // ==========================================
        // CLASS NAME
        // ==========================================

        settings.add(
                makeLabel(
                        "Class Name"
                )
        );

        classNameField =
                new JTextField(
                        "MyApp"
                );

        styleField(
                classNameField
        );

        settings.add(
                classNameField
        );

        settings.add(
                Box.createVerticalStrut(
                        15
                )
        );

        // ==========================================
        // WINDOW TITLE
        // ==========================================

        settings.add(
                makeLabel(
                        "Window Title"
                )
        );

        titleField =
                new JTextField(
                        "My App"
                );

        styleField(
                titleField
        );

        settings.add(
                titleField
        );

        settings.add(
                Box.createVerticalStrut(
                        15
                )
        );

        // ==========================================
        // WIDTH
        // ==========================================

        settings.add(
                makeLabel(
                        "Width"
                )
        );

        widthSpinner =
                new JSpinner(
                        new SpinnerNumberModel(
                                700,
                                200,
                                3000,
                                10
                        )
                );

        settings.add(
                widthSpinner
        );

        settings.add(
                Box.createVerticalStrut(
                        10
                )
        );

        // ==========================================
        // HEIGHT
        // ==========================================

        settings.add(
                makeLabel(
                        "Height"
                )
        );

        heightSpinner =
                new JSpinner(
                        new SpinnerNumberModel(
                                600,
                                200,
                                3000,
                                10
                        )
                );

        settings.add(
                heightSpinner
        );

        settings.add(
                Box.createVerticalStrut(
                        15
                )
        );

        // ==========================================
        // BACKGROUND
        // ==========================================

        RoundedButton backgroundButton =
                new RoundedButton(
                        "Background Color"
                );

        backgroundButton.setBackground(
                new Color(
                        75,
                        90,
                        220
                )
        );

        backgroundButton.setForeground(
                Color.WHITE
        );

        backgroundButton.setMaximumSize(
                new Dimension(
                        240,
                        42
                )
        );

        backgroundButton.addActionListener(e -> {

            Color selected =
                    JColorChooser.showDialog(
                            this,
                            "Choose App Background",
                            appBackground
                    );

            if (selected != null) {

                appBackground =
                        selected;
            }
        });

        settings.add(
                backgroundButton
        );

        settings.add(
                Box.createVerticalStrut(
                        20
                )
        );

        // ==========================================
        // COMPONENT OPTIONS
        // ==========================================

        settings.add(
                makeLabel(
                        "Components"
                )
        );

        buttonBox =
                makeCheckBox(
                        "Button"
                );

        labelBox =
                makeCheckBox(
                        "Label"
                );

        textFieldBox =
                makeCheckBox(
                        "Text Field"
                );

        settings.add(
                buttonBox
        );

        settings.add(
                labelBox
        );

        settings.add(
                textFieldBox
        );

        settings.add(
                Box.createVerticalStrut(
                        20
                )
        );

        // ==========================================
        // GENERATE
        // ==========================================

        RoundedButton generateButton =
                new RoundedButton(
                        "Generate Code"
                );

        generateButton.setBackground(
                new Color(
                        55,
                        155,
                        100
                )
        );

        generateButton.setForeground(
                Color.WHITE
        );

        generateButton.setMaximumSize(
                new Dimension(
                        240,
                        45
                )
        );

        generateButton.addActionListener(e ->
                generateCode()
        );

        settings.add(
                generateButton
        );

        settings.add(
                Box.createVerticalStrut(
                        10
                )
        );

        // ==========================================
        // SAVE
        // ==========================================

        RoundedButton saveButton =
                new RoundedButton(
                        "Save Java File"
                );

        saveButton.setBackground(
                new Color(
                        175,
                        90,
                        50
                )
        );

        saveButton.setForeground(
                Color.WHITE
        );

        saveButton.setMaximumSize(
                new Dimension(
                        240,
                        45
                )
        );

        saveButton.addActionListener(e ->
                saveCode()
        );

        settings.add(
                saveButton
        );

        // ==========================================
        // CODE AREA
        // ==========================================

        codeArea =
                new JTextArea();

        codeArea.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        15
                )
        );

        codeArea.setBackground(
                new Color(
                        18,
                        20,
                        30
                )
        );

        codeArea.setForeground(
                new Color(
                        225,
                        230,
                        240
                )
        );

        codeArea.setCaretColor(
                Color.WHITE
        );

        codeArea.setTabSize(
                4
        );

        JScrollPane scroll =
                new JScrollPane(
                        codeArea
                );

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        new Color(
                                60,
                                65,
                                85
                        )
                )
        );

        // ==========================================
        // ADD
        // ==========================================

        add(
                settings,
                BorderLayout.WEST
        );

        add(
                scroll,
                BorderLayout.CENTER
        );

        generateCode();
    }

    // ==========================================
    // GENERATE JAVA
    // ==========================================

    private void generateCode() {

        String className =
                classNameField
                        .getText()
                        .trim();

        if (className.isEmpty()) {

            className =
                    "MyApp";
        }

        // Remove spaces and invalid simple characters.

        className =
                className.replaceAll(
                        "[^A-Za-z0-9_$]",
                        ""
                );

        if (
                className.isEmpty()
                        ||
                Character.isDigit(
                        className.charAt(0)
                )
        ) {

            className =
                    "MyApp";
        }

        String title =
                titleField
                        .getText()
                        .replace(
                                "\"",
                                "\\\""
                        );

        int width =
                (Integer)
                        widthSpinner.getValue();

        int height =
                (Integer)
                        heightSpinner.getValue();

        StringBuilder code =
                new StringBuilder();

        code.append(
                "import javax.swing.*;\n"
        );

        code.append(
                "import java.awt.*;\n\n"
        );

        code.append(
                "public class "
                        + className
                        + " {\n\n"
        );

        code.append(
                "    public static void main(String[] args) {\n\n"
        );

        code.append(
                "        JFrame frame = new JFrame(\""
                        + title
                        + "\");\n"
        );

        code.append(
                "        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);\n"
        );

        code.append(
                "        frame.setSize("
                        + width
                        + ", "
                        + height
                        + ");\n"
        );

        code.append(
                "        frame.setLocationRelativeTo(null);\n\n"
        );

        code.append(
                "        JPanel panel = new JPanel();\n"
        );

        code.append(
                "        panel.setBackground(new Color("
                        + appBackground.getRed()
                        + ", "
                        + appBackground.getGreen()
                        + ", "
                        + appBackground.getBlue()
                        + "));\n\n"
        );

        // ==========================================
        // LABEL
        // ==========================================

        if (
                labelBox.isSelected()
        ) {

            code.append(
                    "        JLabel label = new JLabel(\"Hello World\");\n"
            );

            code.append(
                    "        label.setForeground(Color.WHITE);\n"
            );

            code.append(
                    "        panel.add(label);\n\n"
            );
        }

        // ==========================================
        // TEXT FIELD
        // ==========================================

        if (
                textFieldBox.isSelected()
        ) {

            code.append(
                    "        JTextField textField = new JTextField(20);\n"
            );

            code.append(
                    "        panel.add(textField);\n\n"
            );
        }

        // ==========================================
        // BUTTON
        // ==========================================

        if (
                buttonBox.isSelected()
        ) {

            code.append(
                    "        JButton button = new JButton(\"Click Me\");\n"
            );

            code.append(
                    "        button.addActionListener(e -> {\n"
            );

            code.append(
                    "            System.out.println(\"Button clicked!\");\n"
            );

            code.append(
                    "        });\n"
            );

            code.append(
                    "        panel.add(button);\n\n"
            );
        }

        code.append(
                "        frame.add(panel);\n\n"
        );

        code.append(
                "        frame.setVisible(true);\n"
        );

        code.append(
                "    }\n"
        );

        code.append(
                "}\n"
        );

        codeArea.setText(
                code.toString()
        );

        codeArea.setCaretPosition(
                0
        );
    }

    // ==========================================
    // SAVE
    // ==========================================

    private void saveCode() {

        generateCode();

        JFileChooser chooser =
                new JFileChooser();

        String className =
                classNameField
                        .getText()
                        .trim()
                        .replaceAll(
                                "[^A-Za-z0-9_$]",
                                ""
                        );

        if (
                className.isEmpty()
        ) {

            className =
                    "MyApp";
        }

        chooser.setSelectedFile(
                new File(
                        className
                                +
                        ".java"
                )
        );

        int result =
                chooser.showSaveDialog(
                        this
                );

        if (
                result
                        !=
                JFileChooser.APPROVE_OPTION
        ) {

            return;
        }

        try {

            File file =
                    chooser.getSelectedFile();

            if (
                    !file.getName()
                            .endsWith(
                                    ".java"
                            )
            ) {

                file =
                        new File(
                                file.getAbsolutePath()
                                        +
                                ".java"
                        );
            }

            FileWriter writer =
                    new FileWriter(
                            file
                    );

            writer.write(
                    codeArea.getText()
            );

            writer.close();

            JOptionPane.showMessageDialog(
                    this,
                    "Saved:\n"
                            +
                    file.getAbsolutePath()
            );

        } catch (
                Exception e
        ) {

            JOptionPane.showMessageDialog(
                    this,
                    "Could not save file:\n"
                            +
                    e.getMessage()
            );
        }
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private JLabel makeLabel(
            String text
    ) {

        JLabel label =
                new JLabel(
                        text
                );

        label.setForeground(
                Color.WHITE
        );

        label.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        14
                )
        );

        return label;
    }

    private JCheckBox makeCheckBox(
            String text
    ) {

        JCheckBox box =
                new JCheckBox(
                        text
                );

        box.setBackground(
                new Color(
                        32,
                        34,
                        50
                )
        );

        box.setForeground(
                Color.WHITE
        );

        box.setFocusPainted(
                false
        );

        return box;
    }

    private void styleField(
            JTextField field
    ) {

        field.setMaximumSize(
                new Dimension(
                        240,
                        38
                )
        );

        field.setBackground(
                new Color(
                        45,
                        48,
                        65
                )
        );

        field.setForeground(
                Color.WHITE
        );

        field.setCaretColor(
                Color.WHITE
        );
    }
}