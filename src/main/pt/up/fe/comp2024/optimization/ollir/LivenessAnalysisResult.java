package pt.up.fe.comp2024.optimization.ollir;

import java.util.List;
import java.util.Set;

public class LivenessAnalysisResult {
    private final List<Set<String>> in;
    private final List<Set<String>> out;

    public LivenessAnalysisResult(List<Set<String>> liveIn, List<Set<String>> liveOut) {
        this.in = liveIn;
        this.out = liveOut;
    }
    public Set<String> getLiveIn(int instrNr) {
        return in.get(instrNr);
    }
    public Set<String> getLiveOut(int instrNr) {
        return out.get(instrNr);
    }
    public int getLiveOutSize() {
        return out.size();
    }
    public int getLiveInSize() {
        return in.size();
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LiveIn:\n");
        for (int i = 0; i < in.size(); i++) {
            sb.append("\tInstruction ").append(i).append(": ").append(in.get(i)).append('\n');
        }
        sb.append("LiveOut:\n");
        for (int i = 0; i < out.size(); i++) {
            sb.append("\tInstruction ").append(i).append(": ").append(out.get(i)).append('\n');
        }
        return sb.toString();
    }
}
