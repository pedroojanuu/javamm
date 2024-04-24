package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.utils.ReportUtils;

public class VarArgs extends AnalysisVisitor {
    // private String currentMethod;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        var paramNodes = method.getChildren(Kind.PARAM);
        var paramSymbols = table.getParameters(method.get("name"));

        for (int i = 0; i < paramNodes.size() - 1; i++) {
            var paramSymbol = paramSymbols.get(i);
            var paramType = paramSymbol.getType();
            if (paramType != null && paramType.hasAttribute("varArgs")) {
                var paramNode = paramNodes.get(i);
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, paramNode,"VarArgs parameter must always be the last parameter"));
            }
        }
        var returnType = table.getReturnType(method.get("name"));
        if (returnType != null && returnType.hasAttribute("varArgs")) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, method,"Method return type cannot be VarArgs"));
        }
        return null;
    }
    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        var type = varDecl.getObject("varType", JmmNode.class);
        if (type != null && type.getKind().equals(Kind.VAR_ARGS.getNodeName())) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, varDecl,"VarArgs cannot be used in a variable declaration"));
        }
        return null;
    }
}
