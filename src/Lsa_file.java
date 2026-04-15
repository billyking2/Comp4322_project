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

    public Lsa_file(final LSRDisplay display) {
        this.display = display;
        this.network = null;
        this.algo = null;

        display.onSelectFile(this::load_file);
        display.onComputeAll(() -> {
            if (algo == null) {
                display.updateStatus("Not performing action: Please select a file and starting node first.");
                return;
            }
            algo.compute_all();
        });
        display.onSingleStep(() -> {
            if (algo == null) {
                display.updateStatus("Not performing action: Please select a file and starting node first.");
                return;
            }
            algo.single_step();
        });
        display.onSelectSource(n -> {
            if (n.length() != 1 && network == null) {
                return;
            }
            this.algo = new Dijkstra_Algorithm(network, n, display);
        });
        display.onReset(this::reset);
    }

    public void reset() {
        display.clearFileContent();
        display.clearTopologyUpdates();
        display.setFileState(FileProcessState.REMOVED);

        this.algo = null;
        this.network = null;
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
            network = null;
            this.file = null;
            return;
        }

        display.updateStatus("Loaded network from %s".formatted(file.getAbsolutePath()));
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

            for (String node : all_nodes) {
                Map<String, Integer> neighbors = network.get_edge(node);

                // build the line for this node
                StringBuilder line = new StringBuilder();
                line.append(node);

                for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                    line.append(":")
                            .append(entry.getKey())
                            .append(":")
                            .append(entry.getValue());
                }

                writer.write(line.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            display.updateStatus("Error while writing file: %s".formatted(file.getAbsolutePath()));
            display.clearFileContent();
            return;
        }

        display.updateStatus("Saved network to: %s".formatted(file.getAbsolutePath()));
    }

    public boolean add_node_to_file(String nodeId) throws IOException {
        boolean success = network.add_node(nodeId);
        if (success) {
            save_file();
            display.updateStatus("Node " + nodeId + " added and saved to file");
        }
        return success;
    }


    public void add_link_to_file(String from, String to, int weight) throws IOException {
        network.add_edge(from, to, weight);
        save_file();
        display.updateStatus("Link " + from + "-" + to + " (cost:" + weight + ") added and saved");
    }

    public boolean remove_link_to_file(String from, String to) throws IOException {
        boolean success = network.remove_edge(from, to);
        if (success) {
            save_file();
            display.updateStatus("Link " + from + "-" + to + " removed and saved");
        }
        return success;
    }

    public boolean remove_node_to_file(String nodeId) throws IOException {
        boolean success = network.remove_node(nodeId);
        if (success) {
            save_file();
            display.updateStatus("Node " + nodeId + " removed and saved");
        }
        return success;
    }
}