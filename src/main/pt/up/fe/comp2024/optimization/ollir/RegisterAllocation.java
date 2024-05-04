package pt.up.fe.comp2024.optimization.ollir;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.utils.ReportUtils;

import java.util.Map;

public class RegisterAllocation {
    private final OllirResult ollirResult;
    private final LivenessAnalysis livenessAnalysis;
    private final GraphColoring graphColoring;
    private final int numReg;

    public RegisterAllocation(OllirResult ollirResult, int numReg) {
        this.ollirResult = ollirResult;
        this.livenessAnalysis = new LivenessAnalysis(ollirResult);
        this.graphColoring = new GraphColoring();
        this.numReg = numReg;
    }

    /**
     * Assigns registers to variables in the method.
     * Used to "report" the mapping of local variables to registers.
     */
    private void assignRegisters(Method method, Map<String, Integer> colors) {
        // avoid register 0 (reserved for "this")
        // avoid parameter registers
        // TODO: static vs non-static methods ? Should we not add 1 for static methods?
        int initialRegisterOffset = 1 + method.getParams().size();

        Map<String, Descriptor> methodVarTable = method.getVarTable();
        for (Map.Entry<String, Integer> entry: colors.entrySet()) {
            String varName = entry.getKey();
            int register = entry.getValue();   // the color of the variable is the register

            Descriptor d = methodVarTable.get(varName);
            d.setVirtualReg(initialRegisterOffset + register);
        }
    }
    public OllirResult apply() {
        for (Method method: this.ollirResult.getOllirClass().getMethods()) {
            LivenessAnalysisResult liveAnalysisResult = livenessAnalysis.obtainResult(method);
            Map<String, Integer> colors = graphColoring.obtainResult(liveAnalysisResult, this.numReg);
            System.out.println("Colors = " + colors);

            if (colors == null) {
                this.ollirResult.getReports().add(ReportUtils.buildErrorReport(
                        Stage.OPTIMIZATION,
                        null,
                        "Unable to allocate registers for method " + method.getMethodName() + " due to insufficient registers."
                ));
            }
            else {
                assignRegisters(method, colors);
            }
        }
        return null;
    }
}
