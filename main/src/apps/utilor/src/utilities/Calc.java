package utilities;

import components.RoundedButton;

import javax.swing.*;
import java.awt.*;

public class Calc extends JPanel {

    private final JTextField display;

    private double firstNumber = 0;
    private String operator = "";
    private boolean startNewNumber = true;

    public Calc() {

        setLayout(
                new BorderLayout(
                        10,
                        10
                )
        );

        setBackground(
                new Color(
                        24,
                        25,
                        35
                )
        );

        setBorder(
                BorderFactory.createEmptyBorder(
                        20,
                        20,
                        20,
                        20
                )
        );

        // ==========================================
        // TITLE
        // ==========================================

        JLabel title =
                new JLabel(
                        "Calculator"
                );

        title.setForeground(
                Color.WHITE
        );

        title.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        24
                )
        );

        // ==========================================
        // DISPLAY
        // ==========================================

        display =
                new JTextField(
                        "0"
                );

        display.setEditable(
                false
        );

        display.setHorizontalAlignment(
                JTextField.RIGHT
        );

        display.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.BOLD,
                        28
                )
        );

        display.setBackground(
                new Color(
                        35,
                        37,
                        50
                )
        );

        display.setForeground(
                Color.WHITE
        );

        display.setCaretColor(
                Color.WHITE
        );

        display.setBorder(
                BorderFactory.createEmptyBorder(
                        15,
                        15,
                        15,
                        15
                )
        );

        // ==========================================
        // TOP
        // ==========================================

        JPanel top =
                new JPanel(
                        new BorderLayout(
                                0,
                                10
                        )
                );

        top.setOpaque(
                false
        );

        top.add(
                title,
                BorderLayout.NORTH
        );

        top.add(
                display,
                BorderLayout.CENTER
        );

        add(
                top,
                BorderLayout.NORTH
        );

        // ==========================================
        // BUTTON GRID
        // ==========================================

        JPanel buttons =
                new JPanel(
                        new GridLayout(
                                5,
                                4,
                                8,
                                8
                        )
                );

        buttons.setOpaque(
                false
        );

        String[] keys = {

                "C",
                "±",
                "%",
                "/",

                "7",
                "8",
                "9",
                "*",

                "4",
                "5",
                "6",
                "-",

                "1",
                "2",
                "3",
                "+",

                "0",
                ".",
                "⌫",
                "="
        };

        for (String key : keys) {

            RoundedButton button =
                    new RoundedButton(
                            key
                    );

            button.setRadius(
                    18
            );

            button.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.BOLD,
                            18
                    )
            );

            if (
                    key.equals("+")
                            ||
                    key.equals("-")
                            ||
                    key.equals("*")
                            ||
                    key.equals("/")
                            ||
                    key.equals("=")
            ) {

                button.setBackground(
                        new Color(
                                85,
                                100,
                                240
                        )
                );

            } else if (
                    key.equals("C")
            ) {

                button.setBackground(
                        new Color(
                                190,
                                65,
                                75
                        )
                );

            } else {

                button.setBackground(
                        new Color(
                                45,
                                48,
                                65
                        )
                );
            }

            button.setForeground(
                    Color.WHITE
            );

            button.addActionListener(
                    e -> handleButton(
                            key
                    )
            );

            buttons.add(
                    button
            );
        }

        add(
                buttons,
                BorderLayout.CENTER
        );
    }

    // ==========================================
    // BUTTON HANDLER
    // ==========================================

    private void handleButton(
            String key
    ) {

        if (
                key.matches(
                        "[0-9]"
                )
        ) {

            addNumber(
                    key
            );

            return;
        }

        switch (key) {

            case "." ->
                    addDecimal();

            case "+" ->
                    setOperator("+");

            case "-" ->
                    setOperator("-");

            case "*" ->
                    setOperator("*");

            case "/" ->
                    setOperator("/");

            case "=" ->
                    calculate();

            case "C" ->
                    clear();

            case "±" ->
                    toggleSign();

            case "%" ->
                    percentage();

            case "⌫" ->
                    backspace();
        }
    }

    // ==========================================
    // NUMBERS
    // ==========================================

    private void addNumber(
            String number
    ) {

        if (
                startNewNumber
                        ||
                display.getText()
                        .equals("0")
        ) {

            display.setText(
                    number
            );

            startNewNumber =
                    false;

        } else {

            display.setText(
                    display.getText()
                            +
                    number
            );
        }
    }

    // ==========================================
    // DECIMAL
    // ==========================================

    private void addDecimal() {

        if (
                startNewNumber
        ) {

            display.setText(
                    "0."
            );

            startNewNumber =
                    false;

            return;
        }

        if (
                !display.getText()
                        .contains(".")
        ) {

            display.setText(
                    display.getText()
                            +
                    "."
            );
        }
    }

    // ==========================================
    // OPERATOR
    // ==========================================

    private void setOperator(
            String newOperator
    ) {

        firstNumber =
                getDisplayNumber();

        operator =
                newOperator;

        startNewNumber =
                true;
    }

    // ==========================================
    // CALCULATE
    // ==========================================

    private void calculate() {

        if (
                operator.isEmpty()
        ) {

            return;
        }

        double secondNumber =
                getDisplayNumber();

        double result;

        switch (operator) {

            case "+" ->

                    result =
                            firstNumber
                                    +
                            secondNumber;

            case "-" ->

                    result =
                            firstNumber
                                    -
                            secondNumber;

            case "*" ->

                    result =
                            firstNumber
                                    *
                            secondNumber;

            case "/" -> {

                if (
                        secondNumber == 0
                ) {

                    display.setText(
                            "Cannot divide by zero"
                    );

                    operator = "";

                    startNewNumber =
                            true;

                    return;
                }

                result =
                        firstNumber
                                /
                        secondNumber;
            }

            default -> {

                return;
            }
        }

        display.setText(
                formatNumber(
                        result
                )
        );

        operator = "";

        startNewNumber =
                true;
    }

    // ==========================================
    // CLEAR
    // ==========================================

    private void clear() {

        display.setText(
                "0"
        );

        firstNumber =
                0;

        operator =
                "";

        startNewNumber =
                true;
    }

    // ==========================================
    // SIGN
    // ==========================================

    private void toggleSign() {

        double number =
                getDisplayNumber();

        number =
                -number;

        display.setText(
                formatNumber(
                        number
                )
        );
    }

    // ==========================================
    // PERCENT
    // ==========================================

    private void percentage() {

        double number =
                getDisplayNumber();

        number =
                number / 100.0;

        display.setText(
                formatNumber(
                        number
                )
        );

        startNewNumber =
                true;
    }

    // ==========================================
    // BACKSPACE
    // ==========================================

    private void backspace() {

        String text =
                display.getText();

        if (
                text.length() <= 1
                        ||
                startNewNumber
        ) {

            display.setText(
                    "0"
            );

            return;
        }

        display.setText(
                text.substring(
                        0,
                        text.length() - 1
                )
        );
    }

    // ==========================================
    // DISPLAY NUMBER
    // ==========================================

    private double getDisplayNumber() {

        try {

            return Double.parseDouble(
                    display.getText()
            );

        } catch (
                NumberFormatException e
        ) {

            return 0;
        }
    }

    // ==========================================
    // FORMAT
    // ==========================================

    private String formatNumber(
            double number
    ) {

        if (
                number
                        ==
                Math.rint(
                        number
                )
        ) {

            return String.valueOf(
                    (long) number
            );
        }

        return String.valueOf(
                number
        );
    }
}