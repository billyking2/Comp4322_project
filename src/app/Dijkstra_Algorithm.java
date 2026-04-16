package app;

import ui.LSRDisplay;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Dijkstra_Algorithm {

    private LSA_structure network;
    private String source_node;
    private Map<String, Integer> distances;
    private Map<String, String> previous_node;
    private Set<String> visited;
    private final LSRDisplay display;
    private boolean started = false, ended = false;
    private Lsa_file control;

    public Dijkstra_Algorithm(Lsa_file control, String source_node, final LSRDisplay display) {
        this.control = control;
        this.network = control.getNetwork();
        this.source_node = source_node;
        this.distances = new HashMap<>();
        this.previous_node = new HashMap<>();
        this.visited = new HashSet<>();
        this.display = display;

        for (String node : network.get_all_nodes()) {
            distances.put(node, Integer.MAX_VALUE);
            //distance={A inf,B inf ,C inf,D inf,E inf....}
        }
        distances.put(source_node, 0);
    }

    public void compute_all() {
        started = true;
        run_Dijkstra(false);
    }

    public void single_step() {
        started = true;
        run_Dijkstra(true);
    }

    public boolean isStarted() {return started;}
    public boolean isEnded() {return ended;}

    private void run_Dijkstra(boolean is_single_step) {
        if (ended) {
            display.updateStatus("The dijkstra algorithm has already ended!");
            return;
        }
        int total_nodes_size = network.get_all_nodes().size();
        // visit all the node
        while (visited.size() < total_nodes_size) {
            String visit_node = get_closest_unvisited_node();
            if (visit_node == null) break;

            visited.add(visit_node);

            update_neighbors(visit_node);
            Object cell = control.getNodeCell(visit_node);
            if (!visit_node.equals(source_node))
                display.highlightCell(cell, Color.yellow, false);
            // single step
            if (is_single_step && !visit_node.equals(source_node)) {
                display.updateStatus("Found " + visit_node + ": Path: " + get_path(visit_node) + " Cost: " + distances.get(visit_node));
                if (visited.size() < total_nodes_size) return;
            }
        }
        print_summary_table();
        ended = true;
    }

    private void update_neighbors(String visit_node){
            // get the visit node all neighbors edge
            Map<String, Integer> neighbors = network.get_edge(visit_node);
            if (neighbors != null) {
                for (Map.Entry<String, Integer> entry : neighbors.entrySet()) {
                    // get neighbors edge and weight
                    String neighbors_edge = entry.getKey();
                    int weight = entry.getValue();
                    // if not visited yet, check the weight and update
                    if (!visited.contains(neighbors_edge)) {
                        int new_distences = distances.get(visit_node) + weight;
                        if (new_distences < distances.get(neighbors_edge)) {
                            distances.put(neighbors_edge, new_distences);
                            previous_node.put(neighbors_edge, visit_node);
                        }
                    }
                }
            }
    }

    // search the unvisited node
    private String get_closest_unvisited_node() {
        String closest_node = null;
        int min_distance = Integer.MAX_VALUE;

        for (String node : distances.keySet()) {
            if (!visited.contains(node) && distances.get(node) < min_distance) {
                min_distance = distances.get(node);
                closest_node = node;
            }
        }

        return closest_node;
    }

    // return the path from source to end node
    private String get_path(String target_node) {
        List<String> path = new ArrayList<>();
        for (String current_node = target_node; current_node != null; current_node = previous_node.get(current_node)) {
            path.add(current_node);
        }
        if (!path.contains(source_node)) path.add(source_node);

        Collections.reverse(path);
        return String.join(">", path);
    }

    // print path
    private void print_summary_table() {
        StringBuilder builder = new StringBuilder("\nSource " + source_node + ": \n ");
        List<String> nodes = new ArrayList<>(distances.keySet());
        Collections.sort(nodes);

        for (String node : nodes) {
            if (node.equals(source_node)) continue;
            String path = get_path(node);
            Integer cost = distances.get(node);
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