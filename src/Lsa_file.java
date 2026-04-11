import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Lsa_file {

    private String filename;
    private LSA_structure network;

    public Lsa_file(String filename) {
        this.filename = filename;
        this.network = new LSA_structure();
    }

    public LSA_structure load_file() throws IOException {
        File file = new File(filename);

        // if file no found, return null
        if (!file.exists()) {
            System.out.println("error in load_file: file not found");
            return null;
        }

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            parse_line(line);
        }

        reader.close();
        System.out.println("Loaded network from: " + filename);
        return network;
    }


    private void parse_line(String line) {
        String[] parts = line.split("\\s+"); // Split by whitespace

        // get source node
        String source_node = parts[0].replace(":", "");

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


    public void save_file() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {

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
        }

        System.out.println("Saved network to: " + filename);
    }



    public boolean add_node_to_file(String nodeId) throws IOException {
        boolean success = network.add_node(nodeId);
        if (success) {
            save_file();
            System.out.println("Node " + nodeId + " added and saved to file");
        }
        return success;
    }


    public void add_link_to_file(String from, String to, int weight) throws IOException {
        network.add_edge(from, to, weight);
        save_file();
        System.out.println("Link " + from + "-" + to + " (cost:" + weight + ") added and saved");
    }


    public boolean remove_link_to_file(String from, String to) throws IOException {
        boolean success = network.remove_edge(from, to);
        if (success) {
            save_file();
            System.out.println("Link " + from + "-" + to + " removed and saved");
        }
        return success;
    }

    public boolean remove_node_to_file(String nodeId) throws IOException {
        boolean success = network.remove_node(nodeId);
        if (success) {
            save_file();
            System.out.println("Node " + nodeId + " removed and saved");
        }
        return success;
    }



}
