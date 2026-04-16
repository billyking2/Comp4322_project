package app;

import ui.LSRDisplay;

import java.awt.*;
import java.util.*;
import java.util.List;

public class DijkstraAlgo {

    private LSRModel network;
    private String srcNode;
    private Map<String, Integer> distancesMap;
    private Map<String, String> prevNodes;
    private Set<String> visitedList;
    private final LSRDisplay display;
    private boolean started = false, ended = false;
    private LSRController control;

    public DijkstraAlgo(LSRController control, String sourceNode, final LSRDisplay display) {
        this.control = control;
        this.network = control.getNetwork();
        this.srcNode = sourceNode;
        this.distancesMap = new HashMap<>();
        this.prevNodes = new HashMap<>();
        this.visitedList = new HashSet<>();
        this.display = display;

        for (String node : network.getAllNodes()) {
            distancesMap.put(node, Integer.MAX_VALUE);
            //distance={A inf,B inf ,C inf,D inf,E inf....}
        }
        distancesMap.put(sourceNode, 0);
    }

    public void computeALl() {
        started = true;
        run(false);
    }

    public void singleStep() {
        started = true;
        run(true);
    }

    public boolean isStarted() {return started;}
    public boolean isEnded() {return ended;}

    private void run(boolean isSingleStep) {
        if (ended) {
            display.updateStatus("The dijkstra algorithm has already ended!");
            return;
        }
        int totalNodesSize = network.getAllNodes().size();
        // visit all the node
        while (visitedList.size() < totalNodesSize) {
            String unvisitedNode = getClosestUnvisitedNode();
            if (unvisitedNode == null) break;

            visitedList.add(unvisitedNode);

            updateNeighbors(unvisitedNode);
            Object cell = control.getNodeCell(unvisitedNode);
            if (!unvisitedNode.equals(srcNode))
                display.highlightCell(cell, Color.yellow, false);
            // single step
            if (isSingleStep && !unvisitedNode.equals(srcNode)) {
                display.updateStatus("Found " + unvisitedNode + ": Path: " + getPath(unvisitedNode) + " Cost: " + distancesMap.get(unvisitedNode));
                if (visitedList.size() < totalNodesSize) return;
            }
        }
        printSummaryTable();
        ended = true;
    }

    private void updateNeighbors(String visitNode){
            // get the visit node all neighbors edge
            Map<String, Integer> neighbors = network.getEdge(visitNode);
            if (neighbors != null) {
                for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                    // get neighbors edge and weight
                    String neighborsEdge = entry.getKey();
                    int weight = entry.getValue();
                    // if not visited yet, check the weight and update
                    if (!visitedList.contains(neighborsEdge)) {
                        int newDistence = distancesMap.get(visitNode) + weight;
                        if (newDistence < distancesMap.get(neighborsEdge)) {
                            distancesMap.put(neighborsEdge, newDistence);
                            prevNodes.put(neighborsEdge, visitNode);
                        }
                    }
                }
            }
    }

    // search the unvisited node
    private String getClosestUnvisitedNode() {
        String closestNode = null;
        int minDistance = Integer.MAX_VALUE;

        for (String node : distancesMap.keySet()) {
            if (!visitedList.contains(node) && distancesMap.get(node) < minDistance) {
                minDistance = distancesMap.get(node);
                closestNode = node;
            }
        }

        return closestNode;
    }

    // return the path from source to end node
    private String getPath(String targetNode) {
        List<String> path = new ArrayList<>();
        for (String currentNode = targetNode; currentNode != null; currentNode = prevNodes.get(currentNode)) {
            path.add(currentNode);
        }
        if (!path.contains(srcNode)) path.add(srcNode);

        Collections.reverse(path);
        return String.join(">", path);
    }

    // print path
    private void printSummaryTable() {
        StringBuilder builder = new StringBuilder("Source " + srcNode + ": \n ");
        List<String> nodes = new ArrayList<>(distancesMap.keySet());
        Collections.sort(nodes);

        for (String node : nodes) {
            if (node.equals(srcNode)) continue;
            String path = getPath(node);
            Integer cost = distancesMap.get(node);
           builder.append(node)
                   .append(": Path: ")
                   .append(path)
                   .append(" Cost: ")
                   .append(cost == Integer.MAX_VALUE ? "Unreachable" : cost)
                   .append('\n');
        }

        display.updateStatus(builder.toString());
    }
}