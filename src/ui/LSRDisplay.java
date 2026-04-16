package ui;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.function.Consumer;

public final class LSRDisplay {
    private final JFrame frame;
    private final LSRDisplayForm form;
    private final JFileChooser chooser;
    private boolean graphLocked;

    public LSRDisplay(String title) {
        FlatIntelliJLaf.setup();
        frame = new JFrame(title);

        form = new LSRDisplayForm();
        frame.setContentPane(form.contentPanel);

        form.graph = new mxGraph();
        graphLocked = false;
        initGraph();

        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        //frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);

        chooser = new JFileChooser(System.getProperty("user.dir"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("LSA Files", "lsa"));

        this.resetSelection();
    }

    public LSRDisplay() {
        this("LSR Display");
    }

    public void pack() {frame.pack();}

    public mxGraph getGraph() {return form.graph;}

    public void initGraph() {


        form.graph.setCellsEditable(false);
        form.graph.setCellsDeletable(false);
        form.graph.setCellsDisconnectable(false);
        form.graph.setEdgeLabelsMovable(false);
        form.graph.setVertexLabelsMovable(false);
        form.graph.setCellsBendable(false);

        var edgeStyle = form.graph.getStylesheet().getDefaultEdgeStyle();
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);
        edgeStyle.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
        edgeStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
        edgeStyle.put(mxConstants.STYLE_MOVABLE, mxConstants.NONE);

        var vertexStyle = form.graph.getStylesheet().getDefaultVertexStyle();
        vertexStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);

        mxGraphComponent graphComponent = new mxGraphComponent(form.graph);
        graphComponent.setConnectable(false); // Disables the "connection" handle
        graphComponent.setDragEnabled(false);  // Allows moving nodes around
        this.addGraphComponent(graphComponent);

        addClickListener(graphComponent);
    }

    private void addClickListener(mxGraphComponent graphComponent) {
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                // Find the cell at the mouse coordinates
                if (graphLocked) return;
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());

                mxGraph graph = form.graph;
                if (cell != null) {
                    if (graph.getModel().isVertex(cell)) {
                        String nodeName = (String) graph.getModel().getValue(cell);
                        form.sourceComboBox.setSelectedItem(nodeName);
                        highlightCell(cell);
                    }
                    else if (graph.getModel().isEdge(cell)) {
                        form.sourceComboBox.setSelectedIndex(0);
                        // Example: Logic to 'Break' this link in your LSR simulation
                        graph.setCellStyles(mxConstants.STYLE_STROKECOLOR, "red", new Object[]{cell});
                    }
                } else {
                    clearHighlight();
                    form.sourceComboBox.setSelectedIndex(0);
                }
            }
        });
    }

    public void highlightCell(Object cell) {
        clearHighlight();
        form.graph.getModel().beginUpdate();
        try {
            // Highlight this specific one
            form.graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#FFA500", new Object[]{cell});
        } finally {
            form.graph.getModel().endUpdate();
        }
    }

    public void clearHighlight() {

        form.graph.getModel().beginUpdate();
        form.graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#C3D9FF",
                form.graph.getChildVertices(form.graph.getDefaultParent()));
        form.graph.getModel().endUpdate();
    }

    public void addGraphComponent(mxGraphComponent graphComponent) {
        form.topUpdatePanel.add(graphComponent);
    }



    @Deprecated
    public void showNodeInfo(final String message, final NodeInfo infoType) {
        final JTextField textField = switch (infoType) {
            case NODE_ADD -> form.nodeAddField;
            case NODE_DELETE -> form.nodeDelField;
            case NODE_LINK_BROKEN -> form.nodeBrokenField;
        };

        textField.setText(message);
    }

    public void printFileLine(final String fileLine) {
        form.fileTextArea.append(fileLine + '\n');
    }

    public void clearFileContent() {
        form.fileTextArea.setText(null);
    }

    public void updateStatus(final String message) {
        form.statusTextArea
                .insert(message.endsWith("\n") ? message : message + '\n', 0);
    }

    public void clearStatus() {
        form.statusTextArea.setText(null);
    }

    private void addEvent(final JButton component, final Runnable callback) {
        component.addActionListener(x -> callback.run());
    }

    public void setFileState(final FileProcessState fileProcessState) {
        form.statusLabel.setText(switch(fileProcessState) {
            case ERROR -> "File Loading Error";
            case LOADED ->  "File Loaded Successfully";
            case REMOVED ->  "Removed File";
        });
    }

    public void onSingleStep(final Runnable callback) {
        addEvent(form.singleStepButton, callback);
    }

    public void onComputeAll(final Runnable callback) {
        addEvent(form.computeAllButton, callback);
    }

    public void onReset(final Runnable callback) {
        addEvent(form.resetButton, callback);
    }

    public void onSelectFile(final Consumer<File> callback) {
        form.loadFileButton.addActionListener(x -> {
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                final File selectedFile = chooser.getSelectedFile();
                callback.accept(selectedFile);
            }
        });
    }

    public void onSelectSource(final Consumer<String> callback) {
        form.sourceComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                callback.accept((String)e.getItem());
            }
        });
    }

    public void addSourceOption(final String option) {
        form.sourceComboBox.addItem(option);
    }

    public void clearTopologyUpdates() {
        form.graph.removeCells(form.graph.getChildCells(form.graph.getDefaultParent()), true);
        // Refresh the UI to show the empty space
        form.topUpdatePanel.revalidate();
        form.topUpdatePanel.repaint();
//        form.nodeAddField.setText(null);
//        form.nodeDelField.setText(null);
//        form.nodeBrokenField.setText(null);
    }

    public void resetSelection() {
        form.sourceComboBox.setEnabled(false);
        form.sourceComboBox.removeAllItems();
        form.sourceComboBox.addItem("<default>");
        form.sourceComboBox.setSelectedIndex(0);
    }

    public void enableSelection() {
        graphLocked = false;
        form.graph.setCellsSelectable(true);
        form.sourceComboBox.setEnabled(true);
    }

    public void disableSelection() {
        graphLocked = true;
        form.graph.setCellsSelectable(false);
        form.sourceComboBox.setEnabled(false);
    }

    public void selectNode(Object cell) {
        form.graph.setSelectionCell(cell);
    }
}

