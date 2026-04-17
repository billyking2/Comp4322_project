package ui;

import com.mxgraph.view.mxGraph;

import javax.swing.*;

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
    JTextArea statusTextArea;
    JButton helpButton;
    mxGraph graph;
}
