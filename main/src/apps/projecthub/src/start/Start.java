package start;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import pages.Bugs;
import pages.Dashboard;
import pages.Projects;
import pages.Settings;
import pages.Tasks;
import pages.Versions;

public class Start {

    private static final Color SIDEBAR_BACKGROUND =
            new Color(28, 27, 72);

    private static final Color SELECTED_COLOR =
            new Color(65, 64, 135);

    private static final Color TEXT_COLOR =
            new Color(240, 240, 245);

    public Start(JFrame frame) {

        // =========================================
        // CLEAR WELCOME SCREEN
        // =========================================

        frame.getContentPane().removeAll();

        frame.getContentPane().setLayout(
                new BorderLayout()
        );

        frame.setTitle("ProjectHub — Workspace");


        // =========================================
        // SIDEBAR TREE NODES
        // =========================================

        DefaultMutableTreeNode root =
                new DefaultMutableTreeNode(
                        "ProjectHub"
                );

        root.add(
                new DefaultMutableTreeNode(
                        "Dashboard"
                )
        );

        root.add(
                new DefaultMutableTreeNode(
                        "Projects"
                )
        );

        root.add(
                new DefaultMutableTreeNode(
                        "Tasks"
                )
        );

        root.add(
                new DefaultMutableTreeNode(
                        "Bugs"
                )
        );

        root.add(
                new DefaultMutableTreeNode(
                        "Versions"
                )
        );

        root.add(
                new DefaultMutableTreeNode(
                        "Settings"
                )
        );


        // =========================================
        // JTREE
        // =========================================

        JTree tree =
                new JTree(root);

        tree.setRootVisible(false);

        tree.setShowsRootHandles(false);

        tree.setRowHeight(55);

        tree.setBackground(
                SIDEBAR_BACKGROUND
        );

        tree.setBorder(
                BorderFactory.createEmptyBorder(
                        20,
                        8,
                        20,
                        8
                )
        );

        UIManager.put(
                "Tree.paintLines",
                false
        );


        // =========================================
        // TREE BUTTON/TAB APPEARANCE
        // =========================================

        tree.setCellRenderer(
                new DefaultTreeCellRenderer() {

                    @Override
                    public Component getTreeCellRendererComponent(
                            JTree tree,
                            Object value,
                            boolean selected,
                            boolean expanded,
                            boolean leaf,
                            int row,
                            boolean hasFocus
                    ) {

                        JLabel label =
                                (JLabel)
                                super.getTreeCellRendererComponent(
                                        tree,
                                        value,
                                        selected,
                                        expanded,
                                        leaf,
                                        row,
                                        hasFocus
                                );

                        // Remove tree icons
                        label.setIcon(null);

                        label.setOpaque(true);

                        label.setFont(
                                new Font(
                                        "SansSerif",
                                        Font.BOLD,
                                        17
                                )
                        );

                        label.setBorder(
                                BorderFactory.createEmptyBorder(
                                        10,
                                        15,
                                        10,
                                        15
                                )
                        );

                        if (selected) {

                            label.setBackground(
                                    SELECTED_COLOR
                            );

                            label.setForeground(
                                    Color.WHITE
                            );

                        } else {

                            label.setBackground(
                                    SIDEBAR_BACKGROUND
                            );

                            label.setForeground(
                                    TEXT_COLOR
                            );
                        }

                        return label;
                    }
                }
        );


        // =========================================
        // CARD LAYOUT
        // =========================================

        CardLayout cardLayout =
                new CardLayout();

        JPanel pages =
                new JPanel(cardLayout);


        // =========================================
        // ADD PAGE CLASSES
        // =========================================

        pages.add(
                new Dashboard(),
                "Dashboard"
        );

        pages.add(
                new Projects(),
                "Projects"
        );

        pages.add(
                new Tasks(),
                "Tasks"
        );

        pages.add(
                new Bugs(),
                "Bugs"
        );

        pages.add(
                new Versions(),
                "Versions"
        );

        pages.add(
                new Settings(),
                "Settings"
        );


        // =========================================
        // SIDEBAR SELECTION
        // =========================================

        tree.addTreeSelectionListener(e -> {

            Object selected =
                    tree.getLastSelectedPathComponent();

            if (selected == null) {
                return;
            }

            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode)
                    selected;

            String pageName =
                    node.getUserObject()
                            .toString();

            cardLayout.show(
                    pages,
                    pageName
            );
        });


        // =========================================
        // SIDEBAR SCROLL PANE
        // =========================================

        JScrollPane sidebar =
                new JScrollPane(tree);

        sidebar.setPreferredSize(
                new Dimension(
                        180,
                        700
                )
        );

        sidebar.setBorder(null);

        sidebar.getViewport()
                .setBackground(
                        SIDEBAR_BACKGROUND
                );


        // =========================================
        // ADD UI
        // =========================================

        frame.getContentPane().add(
                sidebar,
                BorderLayout.WEST
        );

        frame.getContentPane().add(
                pages,
                BorderLayout.CENTER
        );


        // Dashboard selected by default
        tree.setSelectionRow(0);


        // =========================================
        // REFRESH
        // =========================================

        frame.revalidate();
        frame.repaint();
    }
}
