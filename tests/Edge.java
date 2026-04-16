public class Edge {
    private final Node a;
    private final Node b;
    private final int cost;

    public Edge(Node A, Node B, int cost) {
        a = A;
        b = B;
        this.cost = cost;
    }
}
