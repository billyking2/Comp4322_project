package app;

import com.mxgraph.view.mxGraph;
import ui.FileProcessState;
import ui.LSRDisplay;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Lsa_file {

    private File file;
    private LSA_structure network;
    private final LSRDisplay display;
    private Dijkstra_Algorithm algo;
    private final Map<String, Object> vertexMap = new java.util.HashMap<>();

    public Lsa_file(final LSRDisplay display) {
        this.display = display;
        this.network = null;
        this.algo = null;

        display.setupMouseInteractions(this);

        display.onSelectFile(f -> {
            this.load_file(f);
            this.displayGraph();
        });
        display.onComputeAll(() -> {
            if (algo == null) {
                display.updateStatus("Not performing action: Please select a file and starting node first.");
                return;
            }
            algo.compute_all();
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
            algo.single_step();
            if (algo.isStarted() && algo.isEnded())
                display.enableSelection();
            else
                display.disableSelection();
        });
        display.onSelectSource(n -> {
            if (n.length() != 1 || network == null) {
                display.selectNode(null);
                display.clearHighlight();
                this.algo = null;
                return;
            }
            this.algo = new Dijkstra_Algorithm(this, n, display);
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

    public void load_file(final File file) {

        if (!file.exists()) {
            display.updateStatus("Error in load_file: %s not found".formatted(file.getAbsolutePath()));
            display.setFileState(FileProcessState.ERROR);
            return;
        }

        String line = "";
        display.clearFileContent();

        try {
            network = new LSA_structure(display);
            final BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                parse_line(line);
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
        final String[] nodes = network.get_all_nodes().toArray(new String[0]);

        graph.getModel().beginUpdate();
        try {
            // 2. First Pass: Create all vertices and store them in the map
            for (int i = 0; i < nodes.length; ++i) {
                String nodeName = nodes[i];

                // Note: your x and y logic might need scaling (e.g., * 50)
                // otherwise nodes will overlap at (0,0), (1,0) etc.
                int x = (i % 5) * 50;
                int y = (i / 5) * 50;

                Object vertex = graph.insertVertex(parent, null, nodeName, x, y, 40, 40);
                vertexMap.put(nodeName, vertex);
            }

            // 3. Second Pass: Create edges using the objects from the map
            for (String node : nodes) {
                var edges = network.get_edge(node);
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

    private void parse_line(final String line) {
        String[] parts = line.split("\\s+"); // Split by whitespace

        // get source node
        String source_node = parts[0].replace(":", "");

        display.addSourceOption(source_node);
        // parse each part
        for (String part : parts) {
            String[] linkInfo = part.split(":");

            if (linkInfo.length == 3) {
                String to_node = linkInfo[1];
                int cost = Integer.parseInt(linkInfo[2]);
                network.add_edge(source_node, to_node, cost);
            }
            else if (linkInfo.length == 2 && !part.equals(parts[0])) {
                String to_node = linkInfo[0];
                int cost = Integer.parseInt(linkInfo[1]);
                network.add_edge(source_node, to_node, cost);
            }
        }
    }

    public void save_file() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {

            List<String> all_nodes = network.get_all_nodes();
            Collections.sort(all_nodes);
            StringBuilder content = new StringBuilder();
            for (String node : all_nodes) {
                Map<String, Integer> neighbors = network.get_edge(node);

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

    public void add_link_to_file(String from, String to, int weight) {
        network.add_edge(from, to, weight);
        save_file();
        display.updateStatus("Link " + from + "-" + to + " (cost:" + weight + ") added and saved");
    }

    public Object getNodeCell(String node) {
        return vertexMap.get(node);
    }

    public LSA_structure getNetwork() {
        return network;
    }

    public void addNewNode(String nodeId, int x, int y) {
        if (network.add_node(nodeId)) {
            Object parent = display.getGraph().getDefaultParent();
            display.getGraph().getModel().beginUpdate();
            try {
                Object v = display.getGraph().insertVertex(parent, null, nodeId, x, y, 40, 40);
                vertexMap.put(nodeId, v);
            } finally {
                display.getGraph().getModel().endUpdate();
            }
            save_file();
            display.updateStatus("Node " + nodeId + " created.");
        }
    }

    public void removeCell(Object cell) {
        mxGraph graph = display.getGraph();
        graph.getModel().beginUpdate();
        try {
            if (graph.getModel().isVertex(cell)) {
                String id = (String) graph.getModel().getValue(cell);
                if (network.remove_node(id)) {
                    graph.removeCells(new Object[]{cell});
                    vertexMap.remove(id);
                } else {
                    display.updateStatus("Node " + id + " removed.");
                }
            } else if (graph.getModel().isEdge(cell)) {
                com.mxgraph.model.mxCell edge = (com.mxgraph.model.mxCell) cell;
                String from = (String) edge.getSource().getValue();
                String to = (String) edge.getTarget().getValue();
                if (network.remove_edge(from, to)) {
                    graph.removeCells(new Object[]{cell});
                } else {
                    display.updateStatus("Edge " + from + " to " + to + " removed.");
                }
            }
        } finally {
            graph.getModel().endUpdate();
        }
        save_file();
    }
}