package ui;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import java.awt.event.MouseEvent;

public final class LSRDisplayForm {
    JPanel contentPanel;
    JButton singleStepButton;
    JButton computeAllButton;
    JButton loadFileButton;
    JButton resetButton;
    JPanel topUpdatePanel;
    JPanel filePanel;
    JLabel statusLabel;
    JTextArea fileTextArea;
    JComboBox<String> sourceComboBox;
    JButton helpButton;
    JTextPane statusTextPane;
    mxGraph graph;

    public static class LSRGraphComponent extends mxGraphComponent {

        public LSRGraphComponent(mxGraph graph) {
            super(graph);
        }

        @Override
        public boolean isPanningEvent(MouseEvent event) {
            return event != null && SwingUtilities.isRightMouseButton(event) && event.getClickCount() < 2;
        }
    }
}
