package ui;

import app.Lsa_file;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.mxgraph.layout.*;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.model.mxGraphModel;
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
    private mxGraphComponent graphComponent;
    private mxGraphModel model;
    private Object firstVertexForEdge = null;

    public LSRDisplay(String title) {
        FlatIntelliJLaf.setup();
        frame = new JFrame(title);

        form = new LSRDisplayForm();
        frame.setContentPane(form.contentPanel);

        form.graph = new mxGraph();
        this.model = (mxGraphModel) form.graph.getModel();
        this.graphComponent = new mxGraphComponent(form.graph);
        this.graphLocked = false;
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

    public void pack() {
        try {
            mxCircleLayout layout = new mxCircleLayout(form.graph);
            layout.execute(form.graph.getDefaultParent());
        } catch (Exception e) {
            updateStatus("Error packing graph: " + e.getMessage());
        }
        frame.pack();
    }

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

        graphComponent = new mxGraphComponent(form.graph);
        graphComponent.setConnectable(false); // Disables the "connection" handle
        graphComponent.setDragEnabled(false);  // Allows moving nodes around
        this.addGraphComponent(graphComponent);

        addClickListener(graphComponent);
    }

    public void setupMouseInteractions(Lsa_file controller) {
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());

                // --- RIGHT CLICK: Remove Vertex or Edge ---
                if (SwingUtilities.isRightMouseButton(e) && cell != null) {
                    int confirm = JOptionPane.showConfirmDialog(frame, "Delete selected element?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        controller.removeCell(cell);
                    }
                }
                model.beginUpdate();
                // --- DOUBLE LEFT CLICK: Add Vertex or Create Edge ---
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    if (cell == null) {
                        // Clicked empty space: Add new Vertex
                        String name = JOptionPane.showInputDialog(frame, "New Node Name:");
                        if (name != null && !name.trim().isEmpty()) {
                            controller.addNewNode(name, e.getX(), e.getY());
                        }
                    } else if (form.graph.getModel().isVertex(cell)) {
                        // Clicked a vertex: Link logic
                        if (firstVertexForEdge == null) {
                            firstVertexForEdge = cell;
                            updateStatus("Selected " + form.graph.getModel().getValue(cell) + ". Double click another node to link.");
                        } else {
                            Object secondVertex = cell;
                            if (firstVertexForEdge != secondVertex) {
                                String weightStr = JOptionPane.showInputDialog(frame, "Enter Link Cost:");
                                try {
                                    int weight = Integer.parseInt(weightStr);
                                    String from = (String) form.graph.getModel().getValue(firstVertexForEdge);
                                    String to = (String) form.graph.getModel().getValue(secondVertex);
                                    controller.add_link_to_file(from, to, weight);
                                    // Refresh graph visuals
                                    controller.displayGraph();
                                } catch (NumberFormatException ex) {
                                    updateStatus("Invalid cost.");
                                }
                            }
                            firstVertexForEdge = null; // Reset selection
                        }
                    }
                }
                model.endUpdate();
            }
        });
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
                    if (model.isVertex(cell)) {
                        String nodeName = (String) model.getValue(cell);
                        form.sourceComboBox.setSelectedItem(nodeName);
                        highlightCell(cell);
                    }
                    else if (model.isEdge(cell)) {
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
        this.highlightCell(cell, true);
    }

    public void highlightCell(Object cell, String colorHex, boolean clearOtherColors) {
        if (clearOtherColors) clearHighlight();
        model.beginUpdate();

        try {
            // Highlight this specific one
            form.graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, colorHex, new Object[]{cell});
        } finally {
            model.endUpdate();
        }
    }

    public void highlightCell(Object cell, boolean clearOtherColors) {
        this.highlightCell(cell, Color.green, clearOtherColors);
    }

    public void highlightCell(Object cell, Color color, boolean clearOtherColors) {
        String hexCode = String.format("#%06X", (0xFFFFFF & color.getRGB()));
        this.highlightCell(cell, hexCode, clearOtherColors);
    }

    public void clearHighlight() {

        model.beginUpdate();
        form.graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, "#C3D9FF",
                form.graph.getChildVertices(form.graph.getDefaultParent()));
        model.endUpdate();
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

    public void printFileContent(final String fileContent) {
        form.fileTextArea.setText(fileContent);
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
        model.beginUpdate();
        model.clear();
        form.graph.removeCells(form.graph.getChildCells(form.graph.getDefaultParent()), true);
        model.endUpdate();

        this.graphComponent.refresh();
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
        form.graph.setSelectionCell(null);
        form.sourceComboBox.setEnabled(false);
    }

    public void selectNode(Object cell) {
        form.graph.setSelectionCell(cell);
    }
}

