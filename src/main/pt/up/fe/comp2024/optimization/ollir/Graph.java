package pt.up.fe.comp2024.optimization.ollir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Graph {
    private final Map<String, Set<String>> edges;
    public Graph() {
        this.edges = new HashMap<>();
    }
    public void addNode(String node) {
        this.edges.put(node, new HashSet<>());
    }

    /**
     * Add a bidirectional edge between two nodes. Does not check if nodes exist.
     */
    public void addBidirectionalEdge(String node1, String node2) {
        this.addNode(node1);
        this.addNode(node2);
        this.edges.get(node1).add(node2);
        this.edges.get(node2).add(node1);
    }
    public Set<String> getAdjacentNodes(String node) {
        return this.edges.get(node);
    }
    public Set<String> getNodes() {
        return this.edges.keySet();
    }
    public void removeNode(String node) {
        this.edges.remove(node);
        for (String key: this.edges.keySet()) {
            this.edges.get(key).remove(node);
        }
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String node: this.edges.keySet()) {
            sb.append(node).append("-> ").append(this.edges.get(node)).append('\n');
        }
        return sb.toString();
    }
}
