package javagi.casestudies.publicationbrowser;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;

public class Main {

    private static void createAndShowGUI() {
        TreeModel model = new DefaultTreeModel(PublicationDB.root);
        JTree tree = new JTree(model);
        tree.setCellRenderer(new TreeCellRenderer() {
                public Component getTreeCellRendererComponent(JTree tree,
                                                              Object value,
                                                              boolean selected,
                                                              boolean expanded,
                                                              boolean leaf,
                                                              int row,
                                                              boolean hasFocus) {
                    ILabelProvider node = (ILabelProvider) value;
                    Icon icon = node.getIcon();
                    if (icon == null) {
                        return new JLabel(node.getText());
                    } else {
                        return new JLabel(node.getText(), icon, SwingConstants.LEFT);
                    }
                }
            });

        JFrame frame = new JFrame("Publication Database");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(tree);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });
    }
}