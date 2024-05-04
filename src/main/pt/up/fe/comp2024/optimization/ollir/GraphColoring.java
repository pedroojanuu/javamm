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

    private Void getInterferenceGraph(LivenessAnalysisResult livenessAnalysisResult) {
        // each node corresponds to a variable
        // two nodes are connected if the variables "interfere"
        // Interference (liveness overlaps): variables are alive at the same time: cannot be assigned to the same register

        Map<String, List<Integer>> variablesLiveInstructions = this.getLiveInstructions(livenessAnalysisResult);  // variable -> list of instruction numbers

        List<String> nodes = new ArrayList<>(variablesLiveInstructions.keySet());
        Map<String, List<String>> edges = new HashMap<>();
        for (String node: nodes) {
            edges.put(node, new ArrayList<>());
        }

        for (String first: variablesLiveInstructions.keySet()) {
            for (String second: variablesLiveInstructions.keySet()) {
                if (first.equals(second)
                        || Collections.disjoint(    // no interference
                                variablesLiveInstructions.get(first), variablesLiveInstructions.get(second))
                ) continue;

                // add edge between them, since they can be alive at the same time
                edges.get(first).add(second);   // add edge between first and second
            }
        }

        return null;
    }
    public Void apply(LivenessAnalysisResult livenessAnalysisResult) {
        getInterferenceGraph(livenessAnalysisResult);
        return null;
    }

}
