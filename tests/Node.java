import java.util.ArrayList;

public class Node {
    public Node(String name) {
        this.name = name;
    }

    public String name;
    public final ArrayList<Node> children = new ArrayList<>();
    public final ArrayList<Node> parents = new ArrayList<>();
    public final ArrayList<Edge> edges = new ArrayList<>();

    public void addChild(Node child, final int cost) {
        child.parents.add(this);
        this.children.add(child);

        edges.add(new Edge(this, child, cost));
    }
}
