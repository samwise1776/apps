package utilities;

import components.RoundedButton;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.security.SecureRandom;

public class PassGen extends JPanel {

    private final JTextField passwordField;

    private final JSlider lengthSlider;

    private final JCheckBox lowercaseBox;
    private final JCheckBox uppercaseBox;
    private final JCheckBox numbersBox;
    private final JCheckBox symbolsBox;

    private final SecureRandom random =
            new SecureRandom();

    public PassGen() {

        setLayout(
                new BoxLayout(
                        this,
                        BoxLayout.Y_AXIS
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
                BorderFactory.createEmptyBorder(
                        40,
                        50,
                        40,
                        50
                )
        );

        // ==========================================
        // TITLE
        // ==========================================

        JLabel title =
                new JLabel(
                        "Password Generator"
                );

        title.setForeground(
                Color.WHITE
        );

        title.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        30
                )
        );

        title.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        add(title);

        add(
                Box.createVerticalStrut(
                        30
                )
        );

        // ==========================================
        // PASSWORD FIELD
        // ==========================================

        passwordField =
                new JTextField();

        passwordField.setEditable(
                false
        );

        passwordField.setMaximumSize(
                new Dimension(
                        500,
                        50
                )
        );

        passwordField.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.BOLD,
                        18
                )
        );

        passwordField.setHorizontalAlignment(
                JTextField.CENTER
        );

        passwordField.setBackground(
                new Color(
                        38,
                        40,
                        58
                )
        );

        passwordField.setForeground(
                Color.WHITE
        );

        passwordField.setBorder(
                BorderFactory.createEmptyBorder(
                        10,
                        10,
                        10,
                        10
                )
        );

        add(passwordField);

        add(
                Box.createVerticalStrut(
                        25
                )
        );

        // ==========================================
        // LENGTH
        // ==========================================

        JLabel lengthLabel =
                new JLabel(
                        "Length: 16"
                );

        lengthLabel.setForeground(
                Color.WHITE
        );

        lengthLabel.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        16
                )
        );

        lengthLabel.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        add(lengthLabel);

        lengthSlider =
                new JSlider(
                        4,
                        64,
                        16
                );

        lengthSlider.setMaximumSize(
                new Dimension(
                        450,
                        50
                )
        );

        lengthSlider.setBackground(
                getBackground()
        );

        lengthSlider.addChangeListener(e -> {

            lengthLabel.setText(
                    "Length: "
                            +
                    lengthSlider.getValue()
            );
        });

        add(lengthSlider);

        add(
                Box.createVerticalStrut(
                        20
                )
        );

        // ==========================================
        // OPTIONS
        // ==========================================

        lowercaseBox =
                createCheckBox(
                        "Lowercase",
                        true
                );

        uppercaseBox =
                createCheckBox(
                        "Uppercase",
                        true
                );

        numbersBox =
                createCheckBox(
                        "Numbers",
                        true
                );

        symbolsBox =
                createCheckBox(
                        "Symbols",
                        true
                );

        JPanel options =
                new JPanel(
                        new GridLayout(
                                2,
                                2,
                                10,
                                10
                        )
                );

        options.setBackground(
                getBackground()
        );

        options.setMaximumSize(
                new Dimension(
                        450,
                        100
                )
        );

        options.add(
                lowercaseBox
        );

        options.add(
                uppercaseBox
        );

        options.add(
                numbersBox
        );

        options.add(
                symbolsBox
        );

        add(options);

        add(
                Box.createVerticalStrut(
                        30
                )
        );

        // ==========================================
        // GENERATE BUTTON
        // ==========================================

        RoundedButton generateButton =
                new RoundedButton(
                        "Generate"
                );

        generateButton.setBackground(
                new Color(
                        75,
                        95,
                        230
                )
        );

        generateButton.setForeground(
                Color.WHITE
        );

        generateButton.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        generateButton.setMaximumSize(
                new Dimension(
                        200,
                        45
                )
        );

        generateButton.addActionListener(e -> {

            generatePassword();
        });

        add(generateButton);

        add(
                Box.createVerticalStrut(
                        12
                )
        );

        // ==========================================
        // COPY BUTTON
        // ==========================================

        RoundedButton copyButton =
                new RoundedButton(
                        "Copy"
                );

        copyButton.setBackground(
                new Color(
                        45,
                        160,
                        100
                )
        );

        copyButton.setForeground(
                Color.WHITE
        );

        copyButton.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        copyButton.setMaximumSize(
                new Dimension(
                        200,
                        45
                )
        );

        copyButton.addActionListener(e -> {

            String password =
                    passwordField.getText();

            if (
                    password.isEmpty()
            ) {

                return;
            }

            Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(
                            new StringSelection(
                                    password
                            ),
                            null
                    );
        });

        add(copyButton);
    }

    // ==========================================
    // CREATE CHECKBOX
    // ==========================================

    private JCheckBox createCheckBox(
            String text,
            boolean selected
    ) {

        JCheckBox box =
                new JCheckBox(
                        text,
                        selected
                );

        box.setBackground(
                getBackground()
        );

        box.setForeground(
                Color.WHITE
        );

        box.setFocusPainted(
                false
        );

        return box;
    }

    // ==========================================
    // GENERATE PASSWORD
    // ==========================================

    private void generatePassword() {

        String chars = "";

        if (
                lowercaseBox.isSelected()
        ) {

            chars +=
                    "abcdefghijklmnopqrstuvwxyz";
        }

        if (
                uppercaseBox.isSelected()
        ) {

            chars +=
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        }

        if (
                numbersBox.isSelected()
        ) {

            chars +=
                    "0123456789";
        }

        if (
                symbolsBox.isSelected()
        ) {

            chars +=
                    "!@#$%^&*_-+=?";
        }

        if (
                chars.isEmpty()
        ) {

            JOptionPane.showMessageDialog(
                    this,
                    "Choose at least one character type."
            );

            return;
        }

        int length =
                lengthSlider.getValue();

        StringBuilder password =
                new StringBuilder();

        for (
                int i = 0;
                i < length;
                i++
        ) {

            int index =
                    random.nextInt(
                            chars.length()
                    );

            password.append(
                    chars.charAt(
                            index
                    )
            );
        }

        passwordField.setText(
                password.toString()
        );
    }
}