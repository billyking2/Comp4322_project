package ui;

import app.LSRController;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.mxgraph.layout.*;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
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
    private mxGraphComponent graphComponent;private final mxGraphModel model;
    private Object firstVertexForEdge = null;
    private final StyledDocument styledDoc;
    private final Style docStyle;

    public LSRDisplay(String title) {
        FlatIntelliJLaf.setup();
        frame = new JFrame(title);

        form = new LSRDisplayForm();
        frame.setContentPane(form.contentPanel);

        styledDoc = form.statusTextPane.getStyledDocument();
        docStyle = form.statusTextPane.addStyle("StatusStyle", null);
        form.graph = new mxGraph();
        this.model = (mxGraphModel) form.graph.getModel();
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

        this.resetSelectOptions();

        addEvent(form.helpButton, () -> {

            JOptionPane.showMessageDialog(frame,
            """
            Click LMB on a node to select the node;
            Similarly, select a node in the dropbox below to select a node;
            Hold LMB on a node to drag the node around;
            
            Click RMB on a node to delete the node and all its edges;
            Click RMB on an edge to remove the edge;
            Double click LMB on empty space to create a new node;
            Double click LMB on 2 different nodes to create an edge between the two nodes;
            
            When selecting a node, press Single Step button or Compute All button to start performing packet broadcasting;
            Pressing Single Step button forwards to next step, until all the node is visited;
            Pressing Compute All button completes the visit immediately;
            
            The status will be displayed on the Status panel at the bottom;
            The real time file content update will be shown on the File panel on the right;
            """, "How to basic", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public LSRDisplay() {
        this("LSR Display");
    }

    public void pack() {
        try {
            mxCircleLayout layout = new mxCircleLayout(form.graph);
            layout.execute(form.graph.getDefaultParent());
        } catch (Exception e) {
            logErr("Error packing graph: " + e.getMessage());
        }
        frame.pack();
    }

    public mxGraph getGraph() {return form.graph;}

    public void refreshGraph() {
        form.graph.refresh();
        form.graph.repaint();
        if (this.graphComponent != null) {
            graphComponent.refresh();
            graphComponent.repaint();
        }

        form.topUpdatePanel.revalidate();
        form.topUpdatePanel.repaint();
    }

    public void initGraph() {
        form.graph.setCellsEditable(false);
        //form.graph.setCellsDeletable(false);
        form.graph.setCellsDisconnectable(false);
        form.graph.setEdgeLabelsMovable(false);
        form.graph.setVertexLabelsMovable(false);
        form.graph.setCellsBendable(false);
        form.graph.setCellsSelectable(true);

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

    public void setupMouseInteractions(LSRController controller) {
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (graphLocked) return;
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());

                // --- RIGHT CLICK: Remove Vertex or Edge ---
                if (SwingUtilities.isRightMouseButton(e) && cell != null) {
                    int confirm = JOptionPane.showConfirmDialog(frame, "Delete selected element?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        controller.removeCell(cell);
                    }
                }

                // --- DOUBLE LEFT CLICK: Add Vertex or Create Edge ---
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    model.beginUpdate();
                    if (cell == null) {
                        // Clicked empty space: Add new Vertex
                        String name = JOptionPane.showInputDialog(frame, "New Node Name:");
                        if (name != null && !name.trim().isEmpty()) {
                            controller.addNewNode(name, e.getX(), e.getY());
                        }
                    } else if (model.isVertex(cell)) {
                        // Clicked a vertex: Link logic
                        if (firstVertexForEdge == null) {
                            firstVertexForEdge = cell;
                            logInfo("Selected " + model.getValue(cell) + ". Double click another node to link.");
                        } else if (firstVertexForEdge.equals(cell)) {
                            logWarn("Node " + model.getValue(cell) + " is same as previous node. Please select another node.");
                        }
                        else {
                            try {
                                String from = (String) model.getValue(firstVertexForEdge);
                                String to = (String) model.getValue(cell);
                                Object fromNode = controller.getNodeCell(from);
                                Object toNode = controller.getNodeCell(to);
                                if (fromNode != null && toNode != null) {
                                    // Check if an edge already exists between these two in either direction
                                    Object[] existingEdges = form.graph.getEdgesBetween(fromNode, toNode);

                                    if (existingEdges.length == 0) {
                                        // Only insert if no edge exists yet
                                        final String weightStr = JOptionPane.showInputDialog(frame, "Enter Link Cost larger than 0:");
                                        final int weight = Integer.parseInt(weightStr);

                                        // Only allow > 1 for Dih algo
                                        if (weight <= 0) {
                                            logErr("Invalid Link Cost: " + weight + ", weight must be greater than 0.");
                                            return;
                                        }
                                        controller.insertEdge(from, to, weight);
                                        form.graph.insertEdge(form.graph.getDefaultParent(), null, weight, fromNode, toNode, "");
                                    } else {
                                        logErr("Edge between " + from + " - " + to + " already exists.");
                                        return;
                                    }
                                }
                            } catch (NumberFormatException ex) {
                                logErr("Invalid cost.");
                            }
                            firstVertexForEdge = null; // Reset selection
                        }
                    }
                    model.endUpdate();
                    refreshGraph();
                }
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

                selectNode(cell);
                if (cell != null) {
                    if (model.isVertex(cell)) {
                        String nodeName = (String) model.getValue(cell);
                        form.sourceComboBox.setSelectedItem(nodeName);
                        highlightVertex(cell);
                    }
                    else if (model.isEdge(cell)) {
                        form.sourceComboBox.setSelectedIndex(0);
                    }
                } else {
                    clearHighlight();
                    form.sourceComboBox.setSelectedIndex(0);
                }
            }
        });
    }

    public void highlightVertex(Object cell) {
        this.highlightVertex(cell, true);
    }

    public void highlightVertex(Object cell, String colorHex, boolean clearOtherColors) {
        if (clearOtherColors) clearHighlight();
        model.beginUpdate();

        try {
            // Highlight this specific one
            form.graph.setCellStyles(mxConstants.STYLE_FILLCOLOR, colorHex, new Object[]{cell});
        } finally {
            model.endUpdate();
        }
    }

    public void highlightVertex(Object cell, boolean clearOtherColors) {
        this.highlightVertex(cell, Color.green, clearOtherColors);
    }

    public void highlightVertex(Object cell, Color color, boolean clearOtherColors) {
        String hexCode = String.format("#%06X", (0xFFFFFF & color.getRGB()));
        this.highlightVertex(cell, hexCode, clearOtherColors);
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

    public void printFileLine(final String fileLine) {
        form.fileTextArea.append(fileLine + '\n');
    }

    public void printFileContent(final String fileContent) {
        form.fileTextArea.setText(fileContent);
    }

    public void clearFileContent() {
        form.fileTextArea.setText(null);
    }

    private void updateStatus(final String message) {
        try {
            styledDoc.insertString(0,message + '\n', docStyle);
        } catch (BadLocationException ex) {
            System.err.println("Error in inserting to status: " + message + "\n" + ex);
        }
    }

    private static final Color WARN_COLOR = new Color(191, 128, 2);
    private static final Color INFO_COLOR = Color.black;
    private static final Color ERR_COLOR = new Color(191, 2, 2);

    public void logWarn(final String warn) {
        StyleConstants.setForeground(docStyle, WARN_COLOR);
        updateStatus("WARNING: " + warn);
    }

    public void logInfo(final String info) {
        StyleConstants.setForeground(docStyle, INFO_COLOR);
        updateStatus("INFO: " + info);
    }

    public void logErr(final String errMsg) {
        StyleConstants.setForeground(docStyle, ERR_COLOR);
        updateStatus("ERROR: " + errMsg);
    }

    public void clearStatus() {
        form.statusTextPane.setText(null);
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

        graphComponent.refresh();
        form.topUpdatePanel.revalidate();
        form.topUpdatePanel.repaint();
    }

    public void resetSelectOptions() {
        form.sourceComboBox.setEnabled(false);
        form.sourceComboBox.removeAllItems();
        form.sourceComboBox.addItem("<none>");
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

