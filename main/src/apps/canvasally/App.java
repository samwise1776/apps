import javax.swing.*;
import javax.imageio.ImageIO;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class App {

    // =========================================================
    // TOOL TYPES
    // =========================================================

    enum Tool {
        BRUSH,
        ERASER,
        SHAPE
    }

    static Tool currentTool =
            Tool.BRUSH;

    // =========================================================
    // DRAWING STATE
    // =========================================================

    static int oldX;
    static int oldY;

    static int drawSize = 5;

    static int shapeSize = 100;

    static BufferedImage canvas;

    static Color brushColor =
            Color.BLACK;

    static float brushOpacity =
            1.0f;

    static int transparencyStep =
            0;

    static boolean fillShape =
            false;

    static String selectedShape =
            "Circle";

    // =========================================================
    // THEME
    // =========================================================

    static final Color BG =
            new Color(20, 22, 30);

    static final Color TOOLBAR_BG =
            new Color(30, 33, 45);

    static final Color ACCENT =
            new Color(90, 105, 255);

    static final Color TOOL_COLOR =
            new Color(75, 85, 115);

    static final Color TOOL_ACTIVE =
            new Color(230, 145, 55);

    static final Color SHAPE_COLOR =
            new Color(55, 130, 190);

    static final Color FILL_COLOR =
            new Color(160, 80, 190);

    static final Color SAVE_GREEN =
            new Color(55, 180, 105);

    static final Color CLEAR_RED =
            new Color(180, 70, 70);

    static final Color TEXT =
            new Color(235, 238, 245);

    static final Color BORDER =
            new Color(55, 60, 75);

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame =
                    new JFrame(
                            "Canvasally"
                    );

            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE
            );

            frame.setSize(
                    1150,
                    750
            );

            frame.setMinimumSize(
                    new Dimension(
                            800,
                            550
                    )
            );

            frame.setLayout(
                    new BorderLayout()
            );

            frame.getContentPane()
                    .setBackground(
                            BG
                    );

            // =================================================
            // DRAW PANEL
            // =================================================

            JPanel drawPanel =
                    new JPanel() {

                        @Override
                        protected void paintComponent(
                                Graphics g
                        ) {

                            super.paintComponent(g);

                            if (canvas == null) {

                                canvas =
                                        new BufferedImage(
                                                getWidth(),
                                                getHeight(),
                                                BufferedImage.TYPE_INT_ARGB
                                        );

                                Graphics2D g2 =
                                        canvas.createGraphics();

                                g2.setColor(
                                        Color.WHITE
                                );

                                g2.fillRect(
                                        0,
                                        0,
                                        canvas.getWidth(),
                                        canvas.getHeight()
                                );

                                g2.dispose();
                            }

                            g.drawImage(
                                    canvas,
                                    0,
                                    0,
                                    null
                            );
                        }
                    };

            drawPanel.setBackground(
                    Color.WHITE
            );

            drawPanel.setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.CROSSHAIR_CURSOR
                    )
            );

            // =================================================
            // MOUSE PRESSED
            // =================================================

            drawPanel.addMouseListener(
                    new MouseAdapter() {

                        @Override
                        public void mousePressed(
                                MouseEvent e
                        ) {

                            oldX =
                                    e.getX();

                            oldY =
                                    e.getY();

                            // ---------------------------------
                            // BRUSH
                            // ---------------------------------

                            if (
                                    currentTool
                                    ==
                                    Tool.BRUSH
                            ) {

                                drawDot(
                                        oldX,
                                        oldY,
                                        drawPanel,
                                        false
                                );
                            }

                            // ---------------------------------
                            // ERASER
                            // ---------------------------------

                            else if (
                                    currentTool
                                    ==
                                    Tool.ERASER
                            ) {

                                drawDot(
                                        oldX,
                                        oldY,
                                        drawPanel,
                                        true
                                );
                            }

                            // ---------------------------------
                            // SHAPE
                            // ---------------------------------

                            else if (
                                    currentTool
                                    ==
                                    Tool.SHAPE
                            ) {

                                drawShape(
                                        selectedShape,
                                        oldX,
                                        oldY,
                                        drawPanel
                                );
                            }
                        }
                    }
            );

            // =================================================
            // MOUSE DRAGGED
            // =================================================

            drawPanel.addMouseMotionListener(
                    new MouseMotionAdapter() {

                        @Override
                        public void mouseDragged(
                                MouseEvent e
                        ) {

                            if (canvas == null) {
                                return;
                            }

                            // Shapes are placed with clicks.
                            if (
                                    currentTool
                                    ==
                                    Tool.SHAPE
                            ) {

                                return;
                            }

                            int newX =
                                    e.getX();

                            int newY =
                                    e.getY();

                            Graphics2D g2 =
                                    canvas.createGraphics();

                            setupGraphics(
                                    g2
                            );

                            if (
                                    currentTool
                                    ==
                                    Tool.ERASER
                            ) {

                                g2.setComposite(
                                        AlphaComposite.SrcOver
                                );

                                g2.setColor(
                                        Color.WHITE
                                );

                            } else {

                                useBrushColor(
                                        g2
                                );
                            }

                            g2.setStroke(
                                    new BasicStroke(
                                            drawSize,
                                            BasicStroke.CAP_ROUND,
                                            BasicStroke.JOIN_ROUND
                                    )
                            );

                            g2.drawLine(
                                    oldX,
                                    oldY,
                                    newX,
                                    newY
                            );

                            g2.dispose();

                            oldX =
                                    newX;

                            oldY =
                                    newY;

                            drawPanel.repaint();
                        }
                    }
            );

            // =================================================
            // TOOLBAR CONTAINER
            // =================================================

            JPanel toolbar =
                    new JPanel();

            toolbar.setLayout(
                    new BoxLayout(
                            toolbar,
                            BoxLayout.Y_AXIS
                    )
            );

            toolbar.setBackground(
                    TOOLBAR_BG
            );

            // =================================================
            // TOOLBAR ROW 1
            // =================================================

            JPanel row1 =
                    new JPanel(
                            new FlowLayout(
                                    FlowLayout.LEFT,
                                    8,
                                    7
                            )
                    );

            row1.setBackground(
                    TOOLBAR_BG
            );

            // =================================================
            // TITLE
            // =================================================

            JLabel title =
                    new JLabel(
                            "Canvasally"
                    );

            title.setForeground(
                    TEXT
            );

            title.setFont(
                    new Font(
                            Font.SANS_SERIF,
                            Font.BOLD,
                            20
                    )
            );

            row1.add(
                    title
            );

            // =================================================
            // BRUSH BUTTON
            // =================================================

            RoundedButton brushButton =
                    makeButton(
                            "Brush",
                            TOOL_ACTIVE,
                            85
                    );

            // =================================================
            // ERASER BUTTON
            // =================================================

            RoundedButton eraserButton =
                    makeButton(
                            "Eraser",
                            TOOL_COLOR,
                            85
                    );

            // =================================================
            // CIRCLE BUTTON
            // =================================================

            RoundedButton circleButton =
                    makeButton(
                            "○ Circle",
                            SHAPE_COLOR,
                            95
                    );

            // =================================================
            // RECTANGLE BUTTON
            // =================================================

            RoundedButton rectangleButton =
                    makeButton(
                            "[] Rectangle",
                            SHAPE_COLOR,
                            115
                    );

            // =================================================
            // FILL BUTTON
            // =================================================

            RoundedButton fillButton =
                    makeButton(
                            "Fill Shape OFF",
                            FILL_COLOR,
                            125
                    );

            // =================================================
            // COLOR BUTTON
            // =================================================

            RoundedButton colorButton =
                    makeButton(
                            "Color",
                            ACCENT,
                            80
                    );

            JPanel colorPreview =
                    new JPanel();

            colorPreview.setPreferredSize(
                    new Dimension(
                            30,
                            30
                    )
            );

            colorPreview.setBackground(
                    brushColor
            );

            colorPreview.setBorder(
                    BorderFactory.createLineBorder(
                            Color.GRAY,
                            2
                    )
            );

            // =================================================
            // OPACITY
            // =================================================

            RoundedButton opacityButton =
                    makeButton(
                            "Opacity 100%",
                            new Color(
                                    120,
                                    80,
                                    190
                            ),
                            115
                    );

            // =================================================
            // CLEAR
            // =================================================

            RoundedButton clearButton =
                    makeButton(
                            "Clear",
                            CLEAR_RED,
                            75
                    );

            // =================================================
            // SAVE
            // =================================================

            RoundedButton saveButton =
                    makeButton(
                            "Save",
                            SAVE_GREEN,
                            75
                    );

            // =================================================
            // BRUSH ACTION
            // =================================================

            brushButton.addActionListener(e -> {

                currentTool =
                        Tool.BRUSH;

                brushButton.setBackground(
                        TOOL_ACTIVE
                );

                eraserButton.setBackground(
                        TOOL_COLOR
                );
            });

            // =================================================
            // ERASER ACTION
            // =================================================

            eraserButton.addActionListener(e -> {

                currentTool =
                        Tool.ERASER;

                eraserButton.setBackground(
                        TOOL_ACTIVE
                );

                brushButton.setBackground(
                        TOOL_COLOR
                );
            });

            // =================================================
            // CIRCLE ACTION
            // =================================================

            circleButton.addActionListener(e -> {

                currentTool =
                        Tool.SHAPE;

                selectedShape =
                        "Circle";

                brushButton.setBackground(
                        TOOL_COLOR
                );

                eraserButton.setBackground(
                        TOOL_COLOR
                );
            });

            // =================================================
            // RECTANGLE ACTION
            // =================================================

            rectangleButton.addActionListener(e -> {

                currentTool =
                        Tool.SHAPE;

                selectedShape =
                        "Rectangle";

                brushButton.setBackground(
                        TOOL_COLOR
                );

                eraserButton.setBackground(
                        TOOL_COLOR
                );
            });

            // =================================================
            // FILL ACTION
            // =================================================

            fillButton.addActionListener(e -> {

                fillShape =
                        !fillShape;

                if (fillShape) {

                    fillButton.setText(
                            "Fill Shape ON"
                    );

                    fillButton.setBackground(
                            TOOL_ACTIVE
                    );

                } else {

                    fillButton.setText(
                            "Fill Shape OFF"
                    );

                    fillButton.setBackground(
                            FILL_COLOR
                    );
                }
            });

            // =================================================
            // COLOR ACTION
            // =================================================

            colorButton.addActionListener(e -> {

                Color chosen =
                        JColorChooser.showDialog(
                                frame,
                                "Choose Color",
                                brushColor
                        );

                if (chosen != null) {

                    brushColor =
                            chosen;

                    colorPreview.setBackground(
                            brushColor
                    );
                }
            });

            // =================================================
            // OPACITY ACTION
            // =================================================

            opacityButton.addActionListener(e -> {

                transparencyStep++;

                if (
                        transparencyStep
                        >
                        3
                ) {

                    transparencyStep =
                            0;
                }

                switch (
                        transparencyStep
                ) {

                    case 0 -> {

                        brushOpacity =
                                1.0f;

                        opacityButton.setText(
                                "Opacity 100%"
                        );
                    }

                    case 1 -> {

                        brushOpacity =
                                0.75f;

                        opacityButton.setText(
                                "Opacity 75%"
                        );
                    }

                    case 2 -> {

                        brushOpacity =
                                0.50f;

                        opacityButton.setText(
                                "Opacity 50%"
                        );
                    }

                    case 3 -> {

                        brushOpacity =
                                0.25f;

                        opacityButton.setText(
                                "Opacity 25%"
                        );
                    }
                }
            });

            // =================================================
            // CLEAR ACTION
            // =================================================

            clearButton.addActionListener(e -> {

                if (canvas == null) {
                    return;
                }

                Graphics2D g2 =
                        canvas.createGraphics();

                g2.setComposite(
                        AlphaComposite.Src
                );

                g2.setColor(
                        Color.WHITE
                );

                g2.fillRect(
                        0,
                        0,
                        canvas.getWidth(),
                        canvas.getHeight()
                );

                g2.dispose();

                drawPanel.repaint();
            });

            // =================================================
            // SAVE ACTION
            // =================================================

            saveButton.addActionListener(e -> {

                if (canvas == null) {
                    return;
                }

                JFileChooser chooser =
                        new JFileChooser();

                chooser.setSelectedFile(
                        new File(
                                "drawing.png"
                        )
                );

                int result =
                        chooser.showSaveDialog(
                                frame
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
                                    .toLowerCase()
                                    .endsWith(".png")
                    ) {

                        file =
                                new File(
                                        file.getAbsolutePath()
                                                +
                                                ".png"
                                );
                    }

                    ImageIO.write(
                            canvas,
                            "png",
                            file
                    );

                    JOptionPane.showMessageDialog(
                            frame,
                            "Saved:\n"
                                    +
                                    file.getAbsolutePath()
                    );

                } catch (
                        Exception ex
                ) {

                    JOptionPane.showMessageDialog(
                            frame,
                            ex.getMessage(),
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            });

            // =================================================
            // ROW 1 COMPONENTS
            // =================================================

            row1.add(
                    brushButton
            );

            row1.add(
                    eraserButton
            );

            row1.add(
                    circleButton
            );

            row1.add(
                    rectangleButton
            );

            row1.add(
                    fillButton
            );

            row1.add(
                    colorButton
            );

            row1.add(
                    colorPreview
            );

            row1.add(
                    opacityButton
            );

            row1.add(
                    clearButton
            );

            row1.add(
                    saveButton
            );

            // =================================================
            // TOOLBAR ROW 2
            // =================================================

            JPanel row2 =
                    new JPanel(
                            new FlowLayout(
                                    FlowLayout.LEFT,
                                    8,
                                    7
                            )
                    );

            row2.setBackground(
                    TOOLBAR_BG
            );

            // =================================================
            // SHAPE MENU
            // =================================================

            JLabel shapeLabel =
                    new JLabel(
                            "Shape:"
                    );

            shapeLabel.setForeground(
                    TEXT
            );

            JComboBox<String> shapeMenu =
                    new JComboBox<>(
                            createShapeNames()
                    );

            shapeMenu.setPreferredSize(
                    new Dimension(
                            170,
                            32
                    )
            );

            shapeMenu.addActionListener(e -> {

                String shape =
                        (String)
                                shapeMenu.getSelectedItem();

                if (shape != null) {

                    selectedShape =
                            shape;

                    currentTool =
                            Tool.SHAPE;

                    brushButton.setBackground(
                            TOOL_COLOR
                    );

                    eraserButton.setBackground(
                            TOOL_COLOR
                    );
                }
            });

            // =================================================
            // SHAPE SIZE
            // =================================================

            JLabel shapeSizeLabel =
                    new JLabel(
                            "Shape Size:"
                    );

            shapeSizeLabel.setForeground(
                    TEXT
            );

            JLabel shapeValue =
                    new JLabel(
                            shapeSize
                                    +
                                    " px"
                    );

            shapeValue.setForeground(
                    TEXT
            );

            JSlider shapeSlider =
                    new JSlider(
                            20,
                            300,
                            shapeSize
                    );

            shapeSlider.setPreferredSize(
                    new Dimension(
                            180,
                            35
                    )
            );

            shapeSlider.setBackground(
                    TOOLBAR_BG
            );

            shapeSlider.addChangeListener(e -> {

                shapeSize =
                        shapeSlider.getValue();

                shapeValue.setText(
                        shapeSize
                                +
                                " px"
                );
            });

            // =================================================
            // BRUSH SIZE
            // =================================================

            JLabel brushSizeLabel =
                    new JLabel(
                            "Brush Size:"
                    );

            brushSizeLabel.setForeground(
                    TEXT
            );

            JLabel brushValue =
                    new JLabel(
                            drawSize
                                    +
                                    " px"
                    );

            brushValue.setForeground(
                    TEXT
            );

            JSlider brushSlider =
                    new JSlider(
                            1,
                            1000,
                            drawSize
                    );

            brushSlider.setPreferredSize(
                    new Dimension(
                            150,
                            35
                    )
            );

            brushSlider.setBackground(
                    TOOLBAR_BG
            );

            brushSlider.addChangeListener(e -> {

                drawSize =
                        brushSlider.getValue();

                brushValue.setText(
                        drawSize
                                +
                                " px"
                );
            });

            row2.add(
                    shapeLabel
            );

            row2.add(
                    shapeMenu
            );

            row2.add(
                    shapeSizeLabel
            );

            row2.add(
                    shapeSlider
            );

            row2.add(
                    shapeValue
            );

            row2.add(
                    brushSizeLabel
            );

            row2.add(
                    brushSlider
            );

            row2.add(
                    brushValue
            );

            toolbar.add(
                    row1
            );

            toolbar.add(
                    row2
            );

            // =================================================
            // CANVAS CONTAINER
            // =================================================

            JPanel canvasContainer =
                    new JPanel(
                            new BorderLayout()
                    );

            canvasContainer.setBackground(
                    BG
            );

            canvasContainer.setBorder(
                    BorderFactory.createEmptyBorder(
                            15,
                            15,
                            15,
                            15
                    )
            );

            drawPanel.setBorder(
                    BorderFactory.createLineBorder(
                            BORDER,
                            2
                    )
            );

            canvasContainer.add(
                    drawPanel,
                    BorderLayout.CENTER
            );

            // =================================================
            // FRAME
            // =================================================

            frame.add(
                    toolbar,
                    BorderLayout.NORTH
            );

            frame.add(
                    canvasContainer,
                    BorderLayout.CENTER
            );

            frame.setLocationRelativeTo(
                    null
            );

            frame.setVisible(
                    true
            );
        });
    }

    // =========================================================
    // CREATE 100+ SHAPES
    // =========================================================

    static String[] createShapeNames() {

        String[] special = {

                "Circle",
                "Rectangle",
                "Ellipse",
                "Square",
                "Diamond",
                "Star",
                "Heart"
        };

        // Polygon 3 through Polygon 100
        // = 98 polygon shapes.

        String[] names =
                new String[
                        special.length
                                +
                                98
                ];

        int index =
                0;

        for (
                String shape :
                special
        ) {

            names[index++] =
                    shape;
        }

        for (
                int sides = 3;
                sides <= 100;
                sides++
        ) {

            names[index++] =
                    "Polygon "
                            +
                            sides;
        }

        return names;
    }

    // =========================================================
    // DRAW SHAPE
    // =========================================================

    static void drawShape(
            String shape,
            int centerX,
            int centerY,
            JPanel panel
    ) {

        if (canvas == null) {
            return;
        }

        Graphics2D g2 =
                canvas.createGraphics();

        setupGraphics(
                g2
        );

        useBrushColor(
                g2
        );

        g2.setStroke(
                new BasicStroke(
                        drawSize,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND
                )
        );

        int size =
                shapeSize;

        int half =
                size / 2;

        // =====================================================
        // CIRCLE
        // =====================================================

        if (
                shape.equals(
                        "Circle"
                )
        ) {

            if (fillShape) {

                g2.fillOval(
                        centerX - half,
                        centerY - half,
                        size,
                        size
                );

            } else {

                g2.drawOval(
                        centerX - half,
                        centerY - half,
                        size,
                        size
                );
            }
        }

        // =====================================================
        // RECTANGLE
        // =====================================================

        else if (
                shape.equals(
                        "Rectangle"
                )
        ) {

            int width =
                    size;

            int height =
                    (int)
                            (
                                    size
                                    *
                                    0.65
                            );

            if (fillShape) {

                g2.fillRect(
                        centerX - width / 2,
                        centerY - height / 2,
                        width,
                        height
                );

            } else {

                g2.drawRect(
                        centerX - width / 2,
                        centerY - height / 2,
                        width,
                        height
                );
            }
        }

        // =====================================================
        // SQUARE
        // =====================================================

        else if (
                shape.equals(
                        "Square"
                )
        ) {

            if (fillShape) {

                g2.fillRect(
                        centerX - half,
                        centerY - half,
                        size,
                        size
                );

            } else {

                g2.drawRect(
                        centerX - half,
                        centerY - half,
                        size,
                        size
                );
            }
        }

        // =====================================================
        // ELLIPSE
        // =====================================================

        else if (
                shape.equals(
                        "Ellipse"
                )
        ) {

            int height =
                    size / 2;

            if (fillShape) {

                g2.fillOval(
                        centerX - half,
                        centerY - height / 2,
                        size,
                        height
                );

            } else {

                g2.drawOval(
                        centerX - half,
                        centerY - height / 2,
                        size,
                        height
                );
            }
        }

        // =====================================================
        // DIAMOND
        // =====================================================

        else if (
                shape.equals(
                        "Diamond"
                )
        ) {

            Path2D diamond =
                    new Path2D.Double();

            diamond.moveTo(
                    centerX,
                    centerY - half
            );

            diamond.lineTo(
                    centerX + half,
                    centerY
            );

            diamond.lineTo(
                    centerX,
                    centerY + half
            );

            diamond.lineTo(
                    centerX - half,
                    centerY
            );

            diamond.closePath();

            drawOrFill(
                    g2,
                    diamond
            );
        }

        // =====================================================
        // STAR
        // =====================================================

        else if (
                shape.equals(
                        "Star"
                )
        ) {

            Path2D star =
                    createStar(
                            centerX,
                            centerY,
                            half,
                            half / 2
                    );

            drawOrFill(
                    g2,
                    star
            );
        }

        // =====================================================
        // HEART
        // =====================================================

        else if (
                shape.equals(
                        "Heart"
                )
        ) {

            Path2D heart =
                    createHeart(
                            centerX,
                            centerY,
                            size
                    );

            drawOrFill(
                    g2,
                    heart
            );
        }

        // =====================================================
        // POLYGONS 3 - 100
        // =====================================================

        else if (
                shape.startsWith(
                        "Polygon "
                )
        ) {

            try {

                int sides =
                        Integer.parseInt(
                                shape.substring(
                                        8
                                )
                        );

                Path2D polygon =
                        createPolygon(
                                centerX,
                                centerY,
                                half,
                                sides
                        );

                drawOrFill(
                        g2,
                        polygon
                );

            } catch (
                    NumberFormatException ignored
            ) {
            }
        }

        g2.dispose();

        panel.repaint();
    }

    // =========================================================
    // POLYGON GENERATOR
    // =========================================================

    static Path2D createPolygon(
            double centerX,
            double centerY,
            double radius,
            int sides
    ) {

        Path2D path =
                new Path2D.Double();

        for (
                int i = 0;
                i < sides;
                i++
        ) {

            double angle =
                    -Math.PI / 2
                            +
                            (
                                    Math.PI
                                    *
                                    2
                                    *
                                    i
                                    /
                                    sides
                            );

            double x =
                    centerX
                            +
                            Math.cos(
                                    angle
                            )
                            *
                            radius;

            double y =
                    centerY
                            +
                            Math.sin(
                                    angle
                            )
                            *
                            radius;

            if (i == 0) {

                path.moveTo(
                        x,
                        y
                );

            } else {

                path.lineTo(
                        x,
                        y
                );
            }
        }

        path.closePath();

        return path;
    }

    // =========================================================
    // STAR GENERATOR
    // =========================================================

    static Path2D createStar(
            double centerX,
            double centerY,
            double outerRadius,
            double innerRadius
    ) {

        Path2D path =
                new Path2D.Double();

        int points =
                10;

        for (
                int i = 0;
                i < points;
                i++
        ) {

            double radius =
                    i % 2 == 0
                            ?
                            outerRadius
                            :
                            innerRadius;

            double angle =
                    -Math.PI / 2
                            +
                            Math.PI
                            *
                            i
                            /
                            5;

            double x =
                    centerX
                            +
                            Math.cos(
                                    angle
                            )
                            *
                            radius;

            double y =
                    centerY
                            +
                            Math.sin(
                                    angle
                            )
                            *
                            radius;

            if (i == 0) {

                path.moveTo(
                        x,
                        y
                );

            } else {

                path.lineTo(
                        x,
                        y
                );
            }
        }

        path.closePath();

        return path;
    }

    // =========================================================
    // HEART GENERATOR
    // =========================================================

    static Path2D createHeart(
            double centerX,
            double centerY,
            double size
    ) {

        double s =
                size / 100.0;

        Path2D heart =
                new Path2D.Double();

        heart.moveTo(
                centerX,
                centerY + 35 * s
        );

        heart.curveTo(
                centerX - 60 * s,
                centerY,
                centerX - 50 * s,
                centerY - 45 * s,
                centerX - 22 * s,
                centerY - 45 * s
        );

        heart.curveTo(
                centerX - 5 * s,
                centerY - 45 * s,
                centerX,
                centerY - 30 * s,
                centerX,
                centerY - 20 * s
        );

        heart.curveTo(
                centerX,
                centerY - 30 * s,
                centerX + 5 * s,
                centerY - 45 * s,
                centerX + 22 * s,
                centerY - 45 * s
        );

        heart.curveTo(
                centerX + 50 * s,
                centerY - 45 * s,
                centerX + 60 * s,
                centerY,
                centerX,
                centerY + 35 * s
        );

        heart.closePath();

        return heart;
    }

    // =========================================================
    // DRAW OR FILL PATH
    // =========================================================

    static void drawOrFill(
            Graphics2D g2,
            Shape shape
    ) {

        if (fillShape) {

            g2.fill(
                    shape
            );

        } else {

            g2.draw(
                    shape
            );
        }
    }

    // =========================================================
    // DRAW SINGLE DOT
    // =========================================================

    static void drawDot(
            int x,
            int y,
            JPanel panel,
            boolean erase
    ) {

        if (canvas == null) {
            return;
        }

        Graphics2D g2 =
                canvas.createGraphics();

        setupGraphics(
                g2
        );

        if (erase) {

            g2.setComposite(
                    AlphaComposite.SrcOver
            );

            g2.setColor(
                    Color.WHITE
            );

        } else {

            useBrushColor(
                    g2
            );
        }

        int size =
                Math.max(
                        1,
                        drawSize
                );

        g2.fillOval(
                x - size / 2,
                y - size / 2,
                size,
                size
        );

        g2.dispose();

        panel.repaint();
    }

    // =========================================================
    // GRAPHICS SETTINGS
    // =========================================================

    static void setupGraphics(
            Graphics2D g2
    ) {

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE
        );
    }

    // =========================================================
    // APPLY BRUSH COLOR + OPACITY
    // =========================================================

    static void useBrushColor(
            Graphics2D g2
    ) {

        g2.setComposite(
                AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER,
                        brushOpacity
                )
        );

        g2.setColor(
                brushColor
        );
    }

    // =========================================================
    // BUTTON HELPER
    // =========================================================

    static RoundedButton makeButton(
            String text,
            Color color,
            int width
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
                        12
                )
        );

        button.setPreferredSize(
                new Dimension(
                        width,
                        36
                )
        );

        return button;
    }

    // =========================================================
    // ROUNDED BUTTON
    // =========================================================

    static class RoundedButton
            extends JButton {

        RoundedButton(
                String text
        ) {

            super(
                    text
            );

            setFocusPainted(
                    false
            );

            setBorderPainted(
                    false
            );

            setContentAreaFilled(
                    false
            );

            setOpaque(
                    false
            );

            setCursor(
                    Cursor.getPredefinedCursor(
                            Cursor.HAND_CURSOR
                    )
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

            Color color =
                    getBackground();

            if (
                    getModel()
                            .isPressed()
            ) {

                color =
                        color.darker();

            } else if (
                    getModel()
                            .isRollover()
            ) {

                color =
                        color.brighter();
            }

            g2.setColor(
                    color
            );

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    16,
                    16
            );

            g2.dispose();

            super.paintComponent(
                    g
            );
        }
    }
}