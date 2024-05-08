package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.optimization.ast.ConstantFolding;
import pt.up.fe.comp2024.optimization.ast.ConstantPropagation;
import pt.up.fe.comp2024.optimization.ollir.RegisterAllocation;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        var constantFolding = new ConstantFolding();
        var constantPropagation = new ConstantPropagation();

        var optimizationsEnabled = semanticsResult.getConfig().getOrDefault("optimize", "false");
        if (optimizationsEnabled.equals("false")) {
            return semanticsResult;
        }

        boolean iterationHasModifications;
        do {
            iterationHasModifications = constantPropagation.visit(semanticsResult.getRootNode(), semanticsResult.getSymbolTable());
            iterationHasModifications |= constantFolding.visit(semanticsResult.getRootNode(), null);
        } while (iterationHasModifications);

        return semanticsResult;
    }
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        int registerNumberOption = Integer.parseInt(ollirResult.getConfig().getOrDefault("registerAllocation", "-1"));

        if (registerNumberOption == -1) {
            return ollirResult;
        }

        // registerNumberOption == 0 -> as few local registers as possible
        // else limit local registers to registerNumberOption: this can result in an abort if the number of registers is not enough
        RegisterAllocation ra = new RegisterAllocation(ollirResult, registerNumberOption);
        return ra.apply();
    }
}
