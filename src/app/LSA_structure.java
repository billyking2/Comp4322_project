package app;

import ui.LSRDisplay;

import java.util.*;

public class LSA_structure {

    private final Map<String, Map<String, Integer>> network;
    private final LSRDisplay display;

    public LSA_structure(LSRDisplay display) {
        this.network = new HashMap<>();
        this.display = display;
    }

    // add new node (add success return true)
    public boolean add_node(String nodeId) {
        if (network.putIfAbsent(nodeId, new HashMap<>()) == null){
            return true;
        }
        display.updateStatus("error in add_node: add node fail");
        return false;
    }

    // add bidirectional edge
    public void add_edge(String start_node, String end_node, int weight) {
        // check if start_node exists
        if (!network.containsKey(start_node)) {
            network.put(start_node, new HashMap<>());
        }
        // add the edge into start_node
        network.get(start_node).put(end_node, weight);

        // check if end_node exists
        if (!network.containsKey(end_node)) {
            network.put(end_node, new HashMap<>());
        }

        // add the edge into end_node
        network.get(end_node).put(start_node, weight);
    }

    // remove edge (remove success return true)
    public boolean remove_edge(String start_node, String end_node) {
        int success_counter_in_remove_edge=0;
        Map<String, Integer> start_node_neighbors = network.get(start_node);
        Map<String, Integer> end_node_neighbors = network.get(end_node);

        if (start_node_neighbors != null) {
            end_node_neighbors.remove(start_node);
            success_counter_in_remove_edge++;
        }
        if (end_node_neighbors != null) {
            start_node_neighbors.remove(end_node);
            success_counter_in_remove_edge++;
        }
        if (success_counter_in_remove_edge == 2){
            return true;
        }
        else {
            display.updateStatus("error in remove_edge: remove_edge fail at least in one side");
            display.updateStatus("error in remove_edge: success_counter_in_remove_edge " + success_counter_in_remove_edge);
            return false;
        }
    }

    // remove a node and its neighbors (remove success return true)
    public boolean remove_node(String node) {
        // get the neighbors
        Map<String, Integer> edges = network.get(node);
        // choose node (B), and have B{A:5 C:4 E:3 }

        // return false if node not exist
        if (edges == null) {
            display.updateStatus("error in remove_node: node not found");
            return false;
        }
        List<String> edges_list = new ArrayList<>(edges.keySet());

        for (String neighbor : edges_list) {
            boolean edgeRemoved = remove_edge(node, neighbor);
            if (!edgeRemoved) {
                display.updateStatus("error removing edge between " + node + " and " + neighbor);
            }
        }

        network.remove(node);

        return true;
    }


    // get neighbors edge
    public Map<String, Integer> get_edge(String node) {
        Map<String, Integer> neighbors_in_node = network.get(node);
        if (neighbors_in_node == null ){
            display.updateStatus("error in get_neighbors: node not exist");
            return null;
        }
        return neighbors_in_node;
    }

    // get all nodes
    public List<String> get_all_nodes() {
        return new ArrayList<>(network.keySet());
    }


    // print network
    public void print_network() {
        // print node
        for (Map.Entry<String, Map<String, Integer>> entry : network.entrySet()) {
            StringBuilder node = new StringBuilder(entry.getKey());
            Map<String, Integer> neighbors = entry.getValue();

            node.append(" -> ");
            // print edge
            for (Map.Entry<String, Integer> neighborEntry : neighbors.entrySet()) {
                node.append(neighborEntry.getKey()).append("(").append(neighborEntry.getValue()).append(") ");
            }

            display.updateStatus(node.toString());
        }
    }

}
