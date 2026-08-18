import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import components.RoundedButton;
import start.Start;

public class App {

    private static final String TITLE = "ProjectHub";

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            UIManager.put("Button.font", new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
            UIManager.put("Label.font", new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));

            JFrame frame = new JFrame(TITLE);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setMinimumSize(new java.awt.Dimension(900, 620));
            frame.setSize(1100, 720);
            frame.setLayout(new BorderLayout());

            // =====================================
            // WELCOME TEXT
            // =====================================

            String html = """
                    <html>
                    <body style="
                        background-color: #23225A;
                        color: white;
                        text-align: center;
                        font-family: sans-serif;
                    ">

                        <br><br>

                        <h1 style="font-size: 42px;">
                            Welcome to ProjectHub
                        </h1>

                        <p style="font-size: 20px;">
                            Manage your coding projects in one place.
                        </p>

                    </body>
                    </html>
                    """;

            JEditorPane editorPane =
                    new JEditorPane("text/html", html);

            editorPane.setEditable(false);
            editorPane.setBackground(new java.awt.Color(35, 34, 90));

            // Remove white border
            editorPane.setBorder(
                    BorderFactory.createEmptyBorder()
            );

            frame.add(
                    editorPane,
                    BorderLayout.CENTER
            );

            // =====================================
            // BUTTON PANEL
            // =====================================

            JPanel buttonPanel =
                    new JPanel(
                            new GridLayout(
                                    1,
                                    2,
                                    10,
                                    0
                            )
                    );

            buttonPanel.setBackground(
                    new java.awt.Color(
                            35,
                            34,
                            90
                    )
            );

            buttonPanel.setBorder(
                    BorderFactory.createEmptyBorder(
                            10,
                            15,
                            15,
                            15
                    )
            );

            // =====================================
            // START BUTTON
            // =====================================

            RoundedButton startButton =
                    new RoundedButton("Start");

            startButton.addActionListener(e -> {
                new Start(frame);
            });

            // =====================================
            // DOCS BUTTON
            // =====================================

            RoundedButton docsButton =
                    new RoundedButton("Docs");

            docsButton.addActionListener(e -> {
                openDocs();
            });

            // =====================================
            // ADD BUTTONS
            // =====================================

            buttonPanel.add(startButton);
            buttonPanel.add(docsButton);

            frame.add(
                    buttonPanel,
                    BorderLayout.SOUTH
            );

            // Center window
            frame.setLocationRelativeTo(null);

            frame.setVisible(true);
        });
    }

    // =========================================
    // DOCUMENTATION WINDOW
    // =========================================

    private static void openDocs() {

        JFrame docsFrame =
                new JFrame(
                        "ProjectHub Documentation"
                );

        docsFrame.setSize(
                550,
                600
        );

        docsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        docsFrame.setLocationRelativeTo(null);

        String docsHtml = """
                <html>

                <body style="
                    background-color: #23225A;
                    color: white;
                    font-family: sans-serif;
                    padding: 20px;
                ">

                    <h1>
                        ProjectHub Docs
                    </h1>

                    <h2>
                        What is ProjectHub?
                    </h2>

                    <p>
                        ProjectHub helps you manage
                        your coding projects.
                    </p>

                    <h2>
                        Projects
                    </h2>

                    <p>
                        You can create projects,
                        organize them, and track
                        their progress.
                    </p>

                    <h2>
                        Features
                    </h2>

                    <ul>
                        <li>Create projects</li>
                        <li>Manage projects</li>
                        <li>Track progress</li>
                        <li>Store project information</li>
                        <li>Organize coding work</li>
                    </ul>

                </body>

                </html>
                """;

        JEditorPane docsPane =
                new JEditorPane(
                        "text/html",
                        docsHtml
                );

        docsPane.setEditable(false);
        docsPane.setBackground(new java.awt.Color(35, 34, 90));

        docsPane.setBorder(
                BorderFactory.createEmptyBorder()
        );

        docsFrame.add(
                docsPane,
                BorderLayout.CENTER
        );

        docsFrame.setVisible(true);
    }
}
