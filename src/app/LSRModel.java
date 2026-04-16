package app;

import ui.LSRDisplay;

import java.util.*;

public class LSRModel {

    private final Map<String, Map<String, Integer>> network;
    private final LSRDisplay display;

    public LSRModel(LSRDisplay display) {
        this.network = new HashMap<>();
        this.display = display;
    }

    // add new node (add success return true)
    public boolean addNode(String nodeId) {
        if (network.putIfAbsent(nodeId, new HashMap<>()) == null){
            return true;
        }
        return false;
    }

    // add bidirectional edge
    public void addEdge(String startNode, String endNode, int weight) {
        // check if startNode exists
        if (!network.containsKey(startNode)) {
            network.put(startNode, new HashMap<>());
        }
        // add the edge into startNode
        network.get(startNode).put(endNode, weight);

        // check if endNode exists
        if (!network.containsKey(endNode)) {
            network.put(endNode, new HashMap<>());
        }

        // add the edge into endNode
        network.get(endNode).put(startNode, weight);
    }

    // remove edge (remove success return true)
    public boolean removeEdge(String startNode, String endNode) {
        int edgesRemoved = 0;
        Map<String, Integer> startNeighbors = network.get(startNode);
        Map<String, Integer> endNeighbors = network.get(endNode);

        if (startNeighbors != null) {
            endNeighbors.remove(startNode);
            edgesRemoved++;
        }
        if (endNeighbors != null) {
            startNeighbors.remove(endNode);
            edgesRemoved++;
        }
        if (edgesRemoved == 2){
            return true;
        }
        else {
            display.updateStatus("error in removeEdge: removeEdge fail at least in one side");
            display.updateStatus("error in removeEdge: edgesRemoved " + edgesRemoved);
            return false;
        }
    }

    // remove a node and its neighbors (remove success return true)
    public boolean removeNode(String node) {
        // get the neighbors
        Map<String, Integer> edges = network.get(node);
        // choose node (B), and have B{A:5 C:4 E:3 }

        // return false if node not exist
        if (edges == null) {
            display.updateStatus("error in removeNode: node not found");
            return false;
        }
        List<String> edgeList = new ArrayList<>(edges.keySet());

        for (String neighbor : edgeList) {
            boolean edgeRemoved = removeEdge(node, neighbor);
            if (!edgeRemoved) {
                display.updateStatus("error removing edge between " + node + " and " + neighbor);
            }
        }

        network.remove(node);

        return true;
    }


    // get neighbors edge
    public Map<String, Integer> getEdge(String node) {
        Map<String, Integer> neighborsInNode = network.get(node);
        if (neighborsInNode == null ){
            display.updateStatus("error in getEdge: node " + node + " not exist");
            return null;
        }
        return neighborsInNode;
    }

    // get all nodes
    public List<String> getAllNodes() {
        return new ArrayList<>(network.keySet());
    }
}
