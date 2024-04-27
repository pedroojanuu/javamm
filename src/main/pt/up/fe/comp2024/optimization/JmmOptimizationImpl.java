package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.optimization.ast.ConstantFolding;
import pt.up.fe.comp2024.optimization.ast.ConstantPropagation;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {
    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        var constantFolding = new ConstantFolding();
        var constantPropagation = new ConstantPropagation();

        boolean iterationHasModifications;
        do {
            iterationHasModifications = constantFolding.visit(semanticsResult.getRootNode(), null);
            System.out.println("constantFolding has modifications: " + iterationHasModifications);
            iterationHasModifications |= constantPropagation.visit(semanticsResult.getRootNode(), null);
            System.out.println("constantPropagation has new modifications?: " + iterationHasModifications);
            System.out.println("Iteration has modifications: " + iterationHasModifications);
        } while (iterationHasModifications);
        System.out.println(semanticsResult.getRootNode().toTree());
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

        return ollirResult;
    }
}
