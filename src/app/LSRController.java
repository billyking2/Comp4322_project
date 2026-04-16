package app;

import com.mxgraph.view.mxGraph;
import ui.FileProcessState;
import ui.LSRDisplay;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LSRController {

    private File file;
    private LSRModel network;
    private final LSRDisplay display;
    private DijkstraAlgo algo;
    private final Map<String, Object> vertexMap = new java.util.HashMap<>();

    public LSRController(final LSRDisplay display) {
        this.display = display;
        this.network = null;
        this.algo = null;

        display.setupMouseInteractions(this);

        display.onSelectFile(f -> {
            this.loadLSAFile(f);
            this.displayGraph();
        });
        display.onComputeAll(() -> {
            if (algo == null) {
                display.updateStatus("Not performing action: Please select a file and starting node first.");
                return;
            }
            algo.computeALl();
            if (algo.isStarted() && algo.isEnded())
                display.enableSelection();
            else
                display.disableSelection();
        });
        display.onSingleStep(() -> {
            if (algo == null) {
                display.updateStatus("Not performing action: Please select a file and starting node first.");
                return;
            }
            algo.singleStep();
            if (algo.isStarted() && algo.isEnded())
                display.enableSelection();
            else
                display.disableSelection();
        });
        display.onSelectSource(n -> {
            if (n.equals("<none>") || network == null) {
                display.selectNode(null);
                display.clearHighlight();
                this.algo = null;
                return;
            }
            this.algo = new DijkstraAlgo(this, n, display);
            display.selectNode(vertexMap.get(n));
            display.highlightCell(vertexMap.get(n));
        });
        display.onReset(this::reset);
    }


    public void reset() {
        display.clearFileContent();
        display.clearTopologyUpdates();
        display.clearStatus();
        display.setFileState(FileProcessState.REMOVED);
        display.resetSelection();

        this.algo = null;
        this.network = null;
        this.vertexMap.clear();
    }

    public void loadLSAFile(final File file) {

        if (!file.exists()) {
            display.updateStatus("Error in loadLSAFile: %s not found".formatted(file.getAbsolutePath()));
            display.setFileState(FileProcessState.ERROR);
            return;
        }

        String line = "";
        display.clearFileContent();

        try {
            network = new LSRModel(display);
            final BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                parseLine(line);
                display.printFileLine(line);
            }
            reader.close();
            display.setFileState(FileProcessState.LOADED);
            this.file = file;
        } catch (IOException e) {
            display.updateStatus("Error parsing line: %s".formatted(line));
            display.clearFileContent();
            display.setFileState(FileProcessState.ERROR);
            display.resetSelection();

            network = null;
            this.file = null;
            return;
        }

        display.updateStatus("Loaded network from %s".formatted(file.getAbsolutePath()));
    }


    public void displayGraph() {
        mxGraph graph = display.getGraph();

        display.enableSelection();

        Object parent = graph.getDefaultParent();
        final String[] nodes = network.getAllNodes().toArray(new String[0]);

        graph.getModel().beginUpdate();
        try {
            // 2. First Pass: Create all vertices and store them in the map
            for (int i = 0; i < nodes.length; ++i) {
                String nodeName = nodes[i];

                Object vertex = graph.insertVertex(parent, null, nodeName, 0, 0, 40, 40);
                vertexMap.put(nodeName, vertex);
            }

            // 3. Second Pass: Create edges using the objects from the map
            for (String node : nodes) {
                var edges = network.getEdge(node);
                for (var entry : edges.entrySet()) {
                    String connectTo = entry.getKey();
                    int edgeCost = entry.getValue();

                    Object vSource = vertexMap.get(node);
                    Object vTarget = vertexMap.get(connectTo);

                    if (vSource != null && vTarget != null) {
                        // Check if an edge already exists between these two in either direction
                        Object[] existingEdges = graph.getEdgesBetween(vSource, vTarget);

                        if (existingEdges.length == 0) {
                            // Only insert if no edge exists yet
                            graph.insertEdge(parent, null, edgeCost, vSource, vTarget, "");
                        }
                    }
                }
            }
        } catch (Exception e) {
            display.updateStatus("Error when loading network: " + e.getMessage());
        } finally {
            graph.getModel().endUpdate();
        }

        display.pack();
    }

    private void parseLine(final String line) {
        String[] parts = line.split("\\s+"); // Split by whitespace

        // get source node
        String srcNode = parts[0].replace(":", "");
        network.addNode(srcNode);

        display.addSourceOption(srcNode);
        // parse each part
        for (String part : parts) {
            String[] linkInfo = part.split(":");

            if (linkInfo.length == 3) {
                String toNode = linkInfo[1];
                int cost = Integer.parseInt(linkInfo[2]);
                network.addEdge(srcNode, toNode, cost);
            }
            else if (linkInfo.length == 2 && !part.equals(parts[0])) {
                String toNode = linkInfo[0];
                int cost = Integer.parseInt(linkInfo[1]);
                network.addEdge(srcNode, toNode, cost);
            }
        }
    }

    public void saveFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

            List<String> nodes = network.getAllNodes();
            Collections.sort(nodes);
            StringBuilder content = new StringBuilder();
            for (String node : nodes) {
                Map<String, Integer> neighbors = network.getEdge(node);

                // build the line for this node
                StringBuilder line = new StringBuilder();
                line.append(node).append(": ");

                for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                    line.append(entry.getKey())
                            .append(":")
                            .append(entry.getValue())
                            .append(" ");
                }
                String save = line.toString();
                content.append(save).append("\n");

                writer.write(save);
                writer.newLine();
            }
            display.clearFileContent();
            display.printFileContent(content.toString());
        } catch (IOException e) {
            display.updateStatus("Error while writing file: %s".formatted(file.getAbsolutePath()));
            display.clearFileContent();
            return;
        }

        display.updateStatus("Saved network to: %s".formatted(file.getAbsolutePath()));
    }

    public void insertEdge(String from, String to, int weight) {
        network.addEdge(from, to, weight);
        saveFile();
        display.updateStatus("Link " + from + "-" + to + " (cost:" + weight + ") added and saved");
    }

    public Object getNodeCell(String node) {
        return vertexMap.get(node);
    }

    public LSRModel getNetwork() {
        return network;
    }

    public void addNewNode(String nodeId, int x, int y) {
        if (network.addNode(nodeId)) {
            Object parent = display.getGraph().getDefaultParent();
            display.getGraph().getModel().beginUpdate();
            try {
                Object v = display.getGraph().insertVertex(parent, nodeId, nodeId, x, y, 40, 40);
                vertexMap.put(nodeId, v);
            } finally {
                display.getGraph().getModel().endUpdate();
            }
            display.addSourceOption(nodeId);
            saveFile();
            display.updateStatus("Node " + nodeId + " created.");
        } else {
            display.updateStatus("Node " + nodeId + " already exists.");
        }
    }

    public void removeCell(Object cell) {
        mxGraph graph = display.getGraph();
        graph.getModel().beginUpdate();
        try {
            if (graph.getModel().isVertex(cell)) {
                String id = (String) graph.getModel().getValue(cell);
                if (network.removeNode(id)) {
                    graph.removeCells(new Object[]{cell}, true);
                    vertexMap.remove(id);
                    display.updateStatus("Node " + id + " removed.");
                }
                else display.updateStatus("Node " + id + " remove failed.");

            } else if (graph.getModel().isEdge(cell)) {
                com.mxgraph.model.mxCell edge = (com.mxgraph.model.mxCell) cell;
                String from = (String) edge.getSource().getValue();
                String to = (String) edge.getTarget().getValue();
                if (network.removeEdge(from, to)) {
                    graph.removeCells(new Object[]{cell}, true);
                    display.updateStatus("Edge " + from + " to " + to + " removed.");
                }
                else display.updateStatus("Edge " + from + " to " + to + " remove failed.");
            }
            saveFile();
        } catch (Exception ex) {
            display.updateStatus("Error while trying to remove node " + cell + ": " + ex.getMessage());
        } finally {
            graph.getModel().endUpdate();
            display.refreshGraph();
        }
    }
}