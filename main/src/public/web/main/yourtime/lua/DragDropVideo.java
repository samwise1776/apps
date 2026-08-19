import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class DragDropVideo {

    private static Point mouseOffset;

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            // =========================================================
            // COLORS
            // =========================================================

            Color background = new Color(243, 243, 243);
            Color card = Color.WHITE;
            Color border = new Color(210, 210, 210);
            Color text = new Color(32, 32, 32);
            Color secondaryText = new Color(95, 95, 95);
            Color blue = new Color(0, 120, 212);
            Color error = new Color(196, 43, 28);

            // =========================================================
            // FONTS
            // =========================================================

            Font normalFont = new Font(
                    "Segoe UI",
                    Font.PLAIN,
                    15
            );

            Font titleFont = new Font(
                    "Segoe UI",
                    Font.PLAIN,
                    13
            );

            Font headingFont = new Font(
                    "Segoe UI",
                    Font.BOLD,
                    26
            );

            Font dropFont = new Font(
                    "Segoe UI",
                    Font.BOLD,
                    22
            );

            // =========================================================
            // FRAME
            // =========================================================

            JFrame frame = new JFrame();

            frame.setSize(
                    650,
                    430
            );

            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE
            );

            frame.setLocationRelativeTo(null);

            // Custom Windows-style title bar
            frame.setUndecorated(true);

            frame.getContentPane().setBackground(
                    background
            );

            frame.setLayout(
                    new BorderLayout()
            );

            // =========================================================
            // TITLE BAR
            // =========================================================

            JPanel titleBar = new JPanel(
                    new BorderLayout()
            );

            titleBar.setBackground(
                    card
            );

            titleBar.setPreferredSize(
                    new Dimension(
                            650,
                            42
                    )
            );

            JLabel title = new JLabel(
                    "  Yourtime - Add Video"
            );

            title.setFont(
                    titleFont
            );

            title.setForeground(
                    text
            );

            titleBar.add(
                    title,
                    BorderLayout.WEST
            );

            // =========================================================
            // WINDOW BUTTONS
            // =========================================================

            JPanel windowButtons = new JPanel(
                    new FlowLayout(
                            FlowLayout.RIGHT,
                            0,
                            0
                    )
            );

            windowButtons.setOpaque(false);

            JButton minimizeButton =
                    createWindowButton(
                            "—",
                            false
                    );

            JButton closeButton =
                    createWindowButton(
                            "×",
                            true
                    );

            minimizeButton.addActionListener(
                    e -> frame.setState(
                            Frame.ICONIFIED
                    )
            );

            closeButton.addActionListener(
                    e -> frame.dispose()
            );

            windowButtons.add(
                    minimizeButton
            );

            windowButtons.add(
                    closeButton
            );

            titleBar.add(
                    windowButtons,
                    BorderLayout.EAST
            );

            // =========================================================
            // WINDOW DRAGGING
            // =========================================================

            MouseAdapter dragListener =
                    new MouseAdapter() {

                        @Override
                        public void mousePressed(
                                MouseEvent e
                        ) {

                            mouseOffset =
                                    e.getPoint();
                        }

                        @Override
                        public void mouseDragged(
                                MouseEvent e
                        ) {

                            Point location =
                                    e.getLocationOnScreen();

                            frame.setLocation(
                                    location.x
                                            - mouseOffset.x,

                                    location.y
                                            - mouseOffset.y
                            );
                        }
                    };

            titleBar.addMouseListener(
                    dragListener
            );

            titleBar.addMouseMotionListener(
                    dragListener
            );

            title.addMouseListener(
                    dragListener
            );

            title.addMouseMotionListener(
                    dragListener
            );

            // =========================================================
            // MAIN CONTENT
            // =========================================================

            JPanel content =
                    new JPanel();

            content.setLayout(
                    new BoxLayout(
                            content,
                            BoxLayout.Y_AXIS
                    )
            );

            content.setBackground(
                    background
            );

            content.setBorder(
                    new EmptyBorder(
                            30,
                            35,
                            30,
                            35
                    )
            );

            // =========================================================
            // HEADING
            // =========================================================

            JLabel heading =
                    new JLabel(
                            "Add a video"
                    );

            heading.setFont(
                    headingFont
            );

            heading.setForeground(
                    text
            );

            heading.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            // =========================================================
            // DESCRIPTION
            // =========================================================

            JLabel description =
                    new JLabel(
                            "Drag and drop a video file below."
                    );

            description.setFont(
                    normalFont
            );

            description.setForeground(
                    secondaryText
            );

            description.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            // =========================================================
            // DROP AREA
            // =========================================================

            JPanel dropArea =
                    new RoundedPanel(
                            18,
                            card,
                            border
                    );

            dropArea.setLayout(
                    new BoxLayout(
                            dropArea,
                            BoxLayout.Y_AXIS
                    )
            );

            dropArea.setPreferredSize(
                    new Dimension(
                            570,
                            220
                    )
            );

            dropArea.setMaximumSize(
                    new Dimension(
                            Integer.MAX_VALUE,
                            220
                    )
            );

            dropArea.setAlignmentX(
                    Component.LEFT_ALIGNMENT
            );

            // =========================================================
            // UPLOAD ICON
            // =========================================================

            JLabel iconLabel =
                    new JLabel(
                            "⬆"
                    );

            iconLabel.setFont(
                    new Font(
                            "Segoe UI Symbol",
                            Font.PLAIN,
                            42
                    )
            );

            iconLabel.setForeground(
                    blue
            );

            iconLabel.setAlignmentX(
                    Component.CENTER_ALIGNMENT
            );

            // =========================================================
            // DROP LABEL
            // =========================================================

            JLabel label =
                    new JLabel(
                            "Drag a video here"
                    );

            label.setFont(
                    dropFont
            );

            label.setForeground(
                    text
            );

            label.setAlignmentX(
                    Component.CENTER_ALIGNMENT
            );

            // =========================================================
            // SUPPORTED FORMATS
            // =========================================================

            JLabel formats =
                    new JLabel(
                            "MP4, WebM, MOV, MKV or AVI"
                    );

            formats.setFont(
                    normalFont
            );

            formats.setForeground(
                    secondaryText
            );

            formats.setAlignmentX(
                    Component.CENTER_ALIGNMENT
            );

            // =========================================================
            // BUILD DROP AREA
            // =========================================================

            dropArea.add(
                    Box.createVerticalGlue()
            );

            dropArea.add(
                    iconLabel
            );

            dropArea.add(
                    Box.createVerticalStrut(8)
            );

            dropArea.add(
                    label
            );

            dropArea.add(
                    Box.createVerticalStrut(7)
            );

            dropArea.add(
                    formats
            );

            dropArea.add(
                    Box.createVerticalGlue()
            );

            // =========================================================
            // DRAG AND DROP
            // =========================================================

            dropArea.setTransferHandler(
                    new TransferHandler() {

                        @Override
                        public boolean canImport(
                                TransferSupport support
                        ) {

                            return support
                                    .isDataFlavorSupported(
                                            DataFlavor
                                                    .javaFileListFlavor
                                    );
                        }

                        @Override
                        public boolean importData(
                                TransferSupport support
                        ) {

                            if (!canImport(support)) {

                                return false;
                            }

                            try {

                                @SuppressWarnings("unchecked")
                                List<File> files =
                                        (List<File>)
                                                support
                                                        .getTransferable()
                                                        .getTransferData(
                                                                DataFlavor
                                                                        .javaFileListFlavor
                                                        );

                                if (files.isEmpty()) {

                                    return false;
                                }

                                // Only use the first dropped file
                                File file =
                                        files.get(0);

                                // Make sure it really exists
                                if (!file.exists()) {

                                    label.setText(
                                            "File does not exist"
                                    );

                                    label.setForeground(
                                            error
                                    );

                                    return false;
                                }

                                // Don't allow directories
                                if (!file.isFile()) {

                                    label.setText(
                                            "Please drop a video file"
                                    );

                                    label.setForeground(
                                            error
                                    );

                                    return false;
                                }

                                String name =
                                        file
                                                .getName()
                                                .toLowerCase();

                                // =================================================
                                // VIDEO CHECK
                                // =================================================

                                boolean supported =
                                        name.endsWith(".mp4")
                                                ||
                                        name.endsWith(".webm")
                                                ||
                                        name.endsWith(".mov")
                                                ||
                                        name.endsWith(".mkv")
                                                ||
                                        name.endsWith(".avi");

                                if (!supported) {

                                    label.setText(
                                            "That isn't a supported video"
                                    );

                                    label.setForeground(
                                            error
                                    );

                                    return false;
                                }

                                // =================================================
                                // VIDEO ACCEPTED
                                // =================================================

                                label.setText(
                                        "Video selected"
                                );

                                label.setForeground(
                                        blue
                                );

                                formats.setText(
                                        file.getName()
                                );

                                // =================================================
                                // PASS FILE TO WHENPOSTED
                                // =================================================

                                try {

                                    new WhenPosted(
                                            file
                                    );

                                } catch (Exception exception) {

                                    exception.printStackTrace();

                                    label.setText(
                                            "Could not open post"
                                    );

                                    label.setForeground(
                                            error
                                    );

                                    return false;
                                }

                                // Close this drag/drop window
                                frame.dispose();

                                return true;

                            } catch (Exception e) {

                                e.printStackTrace();

                                label.setText(
                                        "Could not load video"
                                );

                                label.setForeground(
                                        error
                                );

                                return false;
                            }
                        }
                    }
            );

            // =========================================================
            // BUILD CONTENT
            // =========================================================

            content.add(
                    heading
            );

            content.add(
                    Box.createVerticalStrut(5)
            );

            content.add(
                    description
            );

            content.add(
                    Box.createVerticalStrut(20)
            );

            content.add(
                    dropArea
            );

            // =========================================================
            // ADD TO FRAME
            // =========================================================

            frame.add(
                    titleBar,
                    BorderLayout.NORTH
            );

            frame.add(
                    content,
                    BorderLayout.CENTER
            );

            // =========================================================
            // SHOW WINDOW
            // =========================================================

            frame.setVisible(true);
        });
    }

    // =============================================================
    // WINDOW BUTTON
    // =============================================================

    private static JButton createWindowButton(
            String text,
            boolean close
    ) {

        JButton button =
                new JButton(
                        text
                );

        button.setPreferredSize(
                new Dimension(
                        48,
                        42
                )
        );

        button.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        close
                                ? 20
                                : 15
                )
        );

        button.setForeground(
                new Color(
                        32,
                        32,
                        32
                )
        );

        button.setBackground(
                Color.WHITE
        );

        button.setBorderPainted(
                false
        );

        button.setFocusPainted(
                false
        );

        button.setContentAreaFilled(
                true
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        button.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseEntered(
                            MouseEvent e
                    ) {

                        if (close) {

                            button.setBackground(
                                    new Color(
                                            232,
                                            17,
                                            35
                                    )
                            );

                            button.setForeground(
                                    Color.WHITE
                            );

                        } else {

                            button.setBackground(
                                    new Color(
                                            232,
                                            232,
                                            232
                                    )
                            );
                        }
                    }

                    @Override
                    public void mouseExited(
                            MouseEvent e
                    ) {

                        button.setBackground(
                                Color.WHITE
                        );

                        button.setForeground(
                                new Color(
                                        32,
                                        32,
                                        32
                                )
                        );
                    }
                }
        );

        return button;
    }

    // =============================================================
    // ROUNDED PANEL
    // =============================================================

    static class RoundedPanel
            extends JPanel {

        private final int radius;
        private final Color background;
        private final Color border;

        public RoundedPanel(
                int radius,
                Color background,
                Color border
        ) {

            this.radius =
                    radius;

            this.background =
                    background;

            this.border =
                    border;

            setOpaque(
                    false
            );
        }

        @Override
        protected void paintComponent(
                Graphics g
        ) {

            Graphics2D g2 =
                    (Graphics2D)
                            g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            // Background
            g2.setColor(
                    background
            );

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    radius,
                    radius
            );

            // Border
            g2.setColor(
                    border
            );

            g2.drawRoundRect(
                    0,
                    0,
                    getWidth() - 1,
                    getHeight() - 1,
                    radius,
                    radius
            );

            g2.dispose();

            super.paintComponent(
                    g
            );
        }
    }
}