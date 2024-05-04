package pt.up.fe.comp2024.optimization.ollir;

import java.util.*;

public class GraphColoring {

    public GraphColoring() {}

    private Map<String, List<Integer>> getLiveInstructions(LivenessAnalysisResult livenessAnalysisResult) {
        Map<String, List<Integer>> liveRanges = new HashMap<>();
        for (int i = 0; i < livenessAnalysisResult.getLiveOutSize(); i++) {
            for (String variable : livenessAnalysisResult.getLiveOut(i)) {
                if (!liveRanges.containsKey(variable)) {    // initialize list if variable not in map
                    liveRanges.put(variable, new ArrayList<>());
                }
                liveRanges.get(variable).add(i);    // add instruction number to variable's list
            }
        }
        return liveRanges;
    }

    private Graph getInterferenceGraph(LivenessAnalysisResult livenessAnalysisResult) {
        // each node corresponds to a variable
        // two nodes are connected if the variables "interfere"
        // Interference (liveness overlaps): variables are alive at the same time: cannot be assigned to the same register

        // variable -> list of instruction numbers
        Map<String, List<Integer>> variablesLiveInstructions = this.getLiveInstructions(livenessAnalysisResult);
        Graph g = new Graph();
        for (String varName: variablesLiveInstructions.keySet()) {
            g.addNode(varName);
        }

        for (String first: variablesLiveInstructions.keySet()) {
            for (String second: variablesLiveInstructions.keySet()) {
                if (first.equals(second)
                        || Collections.disjoint(    // no interference
                                variablesLiveInstructions.get(first), variablesLiveInstructions.get(second))
                ) continue;

                // add edge between them, since they can be alive at the same time
                g.addBidirectionalEdge(first, second);
            }
        }
        return g;
    }
    private Map<String, Integer> colorGraph(Graph graph, int registerNumberLimit) {
        // heuristic algorithm to color the graph (https://docs.google.com/document/d/1OscYt8qOFkfc3xaxdPf-jOsozejf4JvXF9Z1BsOU2x8/edit?usp=sharing)

        // nodes: variables
        // edges: variable -> list of variables that interfere with it
        // registerNumberLimit: maximum number of registers available

        // greedy coloring algorithm
        // assign colors to nodes (variables) in order of degree (number of neighbors)
        // if a node has a neighbor with a color, it cannot have that color
        // if all colors are taken, add a new color

        // select node with less than registerNumberLimit edges
        // put on stack, remove from graph
        // repeat until no nodes in the graph
        // if there is no node with less than registerNumberLimit edges, then the algorithm fails (TODO: do the best coloring possible when -r = ...)

        Stack<String> stack = new Stack<>();
        while (!graph.getNodes().isEmpty()) {
            String node = null;
            for (String n: graph.getNodes()) {
                if (graph.getAdjacentNodes(n).size() < registerNumberLimit) {
                    node = n;
                    break;
                }
            }
            if (node == null) { // no node with less than registerNumberLimit edges
                return null;
            }

            stack.push(node);
            graph.removeNode(node);
        }

        Map<String, Integer> colors = new HashMap<>();
        while (!stack.isEmpty()) {
            String node = stack.pop();

            Set<Integer> adjacentNodeColors = new HashSet<>();  // list of colors of adjacent nodes
            for (String neighbor : graph.getAdjacentNodes(node)) {
                adjacentNodeColors.add(colors.get(neighbor));   // returns null for nodes without color
            }
            int color = 0;
            while (color < registerNumberLimit && adjacentNodeColors.contains(color)) {
                color++;
            }

            if (color == registerNumberLimit) { // no more colors available
                return null;
            }
            colors.put(node, color);
        }

        // each color will be register
        // start from lowest register number (avoiding registers for "this" and parameters)
        // reverse the removal of nodes from graph (stack)
        // for each node, assign the lowest register already not attributed to popped nodes
        return colors;
    }
    public Map<String, Integer> apply(LivenessAnalysisResult livenessAnalysisResult, int registerNumberLimit) {
        Graph graph = this.getInterferenceGraph(livenessAnalysisResult);
        return this.colorGraph(graph, registerNumberLimit);
    }

}
