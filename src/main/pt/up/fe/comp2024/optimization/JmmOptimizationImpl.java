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
        //TODO: Do your OLLIR-based optimizations here
        int registerNumberOption = Integer.parseInt(ollirResult.getConfig().getOrDefault("registerAllocation", "-1"));

        if (registerNumberOption == -1) {
            return ollirResult;
        }

        RegisterAllocation ra = new RegisterAllocation(ollirResult, registerNumberOption);
        if (registerNumberOption == 0) {   // as few local registers as possible
            return ra.apply(); // TODO
        }
        // limit local registers to registerNumberOption: this can result in an abort if the number of registers is not enough
        // TODO


        /*
        generators
        apply generator to ollirResult.getClass()
        method -> do currentMethod = ...
        see variable assignment inside method -> do def[currentMethod].append(variable) ?
        see variable usage inside method -> do use[currentMethod].append(variable) ?
         */
        //ollirResult.getConfig().get("")
        // return registerOptimization.apply();
        return ra.apply();
    }
}
