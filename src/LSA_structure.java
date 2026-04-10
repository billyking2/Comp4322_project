import java.util.*;

public class LSA_structure {

    private Map<String, Map<String, Integer>> network;

    public LSA_structure() {
        this.network = new HashMap<>();
    }

    // add new node (add success return true)
    public boolean add_node(String nodeId) {
        if (network.putIfAbsent(nodeId, new HashMap<>()) == null){
            return true;
        }
        System.out.println("error in add_node: add node fail");
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
            System.out.println("error in remove_edge: remove_edge fail at least in one side");
            return false;
        }
    }

    // remove a node and its neighbors (remove success return true)
    public boolean remove_node(String node) {
        // get the neighbors
        Map<String, Integer> edges = network.get(node);

        // return false if node not exist
        if (edges == null) {
            System.out.println("error in remove_node: node not found");
            return false;
        }

        for (String neighbor : edges.keySet()) {
            boolean edgeRemoved = remove_edge(node, neighbor);
            if (!edgeRemoved) {
                System.out.println("error removing edge between " + node + " and " + neighbor);
            }
        }

        network.remove(node);

        return true;
    }


    // get neighbors edge
    public Map<String, Integer> get_edge(String node) {
        Map<String, Integer> neighbors_in_node = network.get(node);
        if (neighbors_in_node == null ){
            System.out.println("error in get_neighbors: node not exist");
            return null;
        }
        return neighbors_in_node;
    }

    // get all nodes
    public List<String> get_all_nodes() {
        return new ArrayList<>(network.keySet());
    }

    // check if node exists
    public boolean have_node(String nodeId) {
        return network.containsKey(nodeId);
    }

    // print network
    public void print_network() {
        for (Map.Entry<String, Map<String, Integer>> entry : network.entrySet()) {
            String node = entry.getKey();
            Map<String, Integer> neighbors = entry.getValue();

            System.out.print(node + " -> ");

            for (Map.Entry<String, Integer> neighborEntry : neighbors.entrySet()) {
                System.out.print(neighborEntry.getKey() + "(" + neighborEntry.getValue() + ") ");
            }
            System.out.println();
        }
    }

}
