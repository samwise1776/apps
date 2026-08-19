import components.RoundedButton;

import utilities.AppGen;
import utilities.Calc;
import utilities.PassGen;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class App {

    static JFrame frame;

    static CardLayout pages;
    static JPanel root;

    static final Color BACKGROUND =
            new Color(
                    34,
                    35,
                    90
            );

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            frame =
                    new JFrame(
                            "Utilor"
                    );

            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE
            );

            frame.setSize(
                    850,
                    800
            );

            frame.setMinimumSize(
                    new Dimension(
                            650,
                            600
                    )
            );

            frame.setLocationRelativeTo(
                    null
            );

            // ==========================================
            // PAGE SYSTEM
            // ==========================================

            pages =
                    new CardLayout();

            root =
                    new JPanel(
                            pages
                    );

            // ==========================================
            // HOME
            // ==========================================

            root.add(
                    createHome(),
                    "home"
            );

            // ==========================================
            // CALCULATOR
            // ==========================================

            root.add(
                    createUtilityPage(
                            "Calculator",
                            new Calc()
                    ),
                    "calc"
            );

            // ==========================================
            // PASSWORD GENERATOR
            // ==========================================

            root.add(
                    createUtilityPage(
                            "Password Generator",
                            new PassGen()
                    ),
                    "pass"
            );

            // ==========================================
            // APP GENERATOR
            // ==========================================

            root.add(
                    createUtilityPage(
                            "App Generator",
                            new AppGen()
                    ),
                    "appgen"
            );

            frame.add(
                    root
            );

            pages.show(
                    root,
                    "home"
            );

            frame.setVisible(
                    true
            );
        });
    }

    // ==========================================
    // HOME
    // ==========================================

    static JPanel createHome() {

        JPanel home =
                new JPanel(
                        new BorderLayout()
                );

        home.setBackground(
                BACKGROUND
        );

        home.setBorder(
                new EmptyBorder(
                        30,
                        40,
                        40,
                        40
                )
        );

        // ==========================================
        // HEADER
        // ==========================================

        JPanel header =
                new JPanel();

        header.setLayout(
                new BoxLayout(
                        header,
                        BoxLayout.Y_AXIS
                )
        );

        header.setOpaque(
                false
        );

        JLabel title =
                new JLabel(
                        "Utilor"
                );

        title.setForeground(
                Color.WHITE
        );

        title.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        40
                )
        );

        title.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        JLabel subtitle =
                new JLabel(
                        "One utility, or another, and another."
                );

        subtitle.setForeground(
                new Color(
                        190,
                        195,
                        230
                )
        );

        subtitle.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        16
                )
        );

        subtitle.setAlignmentX(
                Component.CENTER_ALIGNMENT
        );

        header.add(
                title
        );

        header.add(
                Box.createVerticalStrut(
                        5
                )
        );

        header.add(
                subtitle
        );

        home.add(
                header,
                BorderLayout.NORTH
        );

        // ==========================================
        // APP GRID
        // ==========================================

        JPanel apps =
                new JPanel(
                        new GridLayout(
                                0,
                                2,
                                20,
                                20
                        )
                );

        apps.setOpaque(
                false
        );

        apps.setBorder(
                new EmptyBorder(
                        50,
                        50,
                        100,
                        50
                )
        );

        // ==========================================
        // CALCULATOR
        // ==========================================

        apps.add(
                createAppButton(
                        "Calculator",
                        new Color(
                                70,
                                90,
                                220
                        ),
                        "calc"
                )
        );

        // ==========================================
        // PASSWORD GENERATOR
        // ==========================================

        apps.add(
                createAppButton(
                        "Password Generator",
                        new Color(
                                150,
                                70,
                                200
                        ),
                        "pass"
                )
        );

        // ==========================================
        // APP GENERATOR
        // ==========================================

        apps.add(
                createAppButton(
                        "App Generator",
                        new Color(
                                45,
                                155,
                                110
                        ),
                        "appgen"
                )
        );

        home.add(
                apps,
                BorderLayout.CENTER
        );

        return home;
    }

    // ==========================================
    // APP BUTTON
    // ==========================================

    static RoundedButton createAppButton(
            String text,
            Color color,
            String page
    ) {

        RoundedButton button =
                new RoundedButton(
                        text
                );

        button.setBackground(
                color
        );

        button.setForeground(
                Color.WHITE
        );

        button.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        18
                )
        );

        button.setRadius(
                25
        );

        button.addActionListener(e -> {

            pages.show(
                    root,
                    page
            );
        });

        return button;
    }

    // ==========================================
    // UTILITY PAGE
    // ==========================================

    static JPanel createUtilityPage(
            String name,
            JPanel utility
    ) {

        JPanel page =
                new JPanel(
                        new BorderLayout()
                );

        page.setBackground(
                new Color(
                        24,
                        25,
                        40
                )
        );

        // ==========================================
        // TOP BAR
        // ==========================================

        JPanel top =
                new JPanel(
                        new BorderLayout()
                );

        top.setBackground(
                new Color(
                        29,
                        31,
                        50
                )
        );

        top.setBorder(
                new EmptyBorder(
                        10,
                        15,
                        10,
                        15
                )
        );

        RoundedButton back =
                new RoundedButton(
                        "← Back"
                );

        back.setBackground(
                new Color(
                        65,
                        70,
                        100
                )
        );

        back.setForeground(
                Color.WHITE
        );

        back.setPreferredSize(
                new Dimension(
                        100,
                        40
                )
        );

        back.addActionListener(e -> {

            pages.show(
                    root,
                    "home"
            );
        });

        JLabel title =
                new JLabel(
                        name,
                        SwingConstants.CENTER
                );

        title.setForeground(
                Color.WHITE
        );

        title.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        20
                )
        );

        top.add(
                back,
                BorderLayout.WEST
        );

        top.add(
                title,
                BorderLayout.CENTER
        );

        // This keeps the title visually centered.

        JPanel spacer =
                new JPanel();

        spacer.setOpaque(
                false
        );

        spacer.setPreferredSize(
                new Dimension(
                        100,
                        40
                )
        );

        top.add(
                spacer,
                BorderLayout.EAST
        );

        page.add(
                top,
                BorderLayout.NORTH
        );

        page.add(
                utility,
                BorderLayout.CENTER
        );

        return page;
    }
}