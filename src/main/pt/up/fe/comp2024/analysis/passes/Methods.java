package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.comp2024.utils.ReportUtils;

public class Methods extends AnalysisVisitor {
    @Override
    protected void buildVisitor() {
        addVisit(Kind.OTHER_METHOD, this::visitNormalMethodDecl);
        addVisit(Kind.MAIN_METHOD, this::visitMainMethodDecl);
        //addVisit(Kind.METHOD_CALL_EXPR, this::visitMethodCall);
    }
    private Void visitNormalMethodDecl(JmmNode method, SymbolTable table) {
        var returnType = TypeUtils.getExprType(method.getObject("returnExpr", JmmNode.class), table, method.get("name"));
        var methodType = table.getReturnType(method.get("name"));
        if (!returnType.equals(methodType) &&
                !TypeUtils.typeInherits(returnType, methodType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, method, "Method return type does not match the declared type"));
        }
        return null;
    }
    private Void visitMainMethodDecl(JmmNode method, SymbolTable table) {
        var paramType = method.get("paramType");
        String generalMessage = " A void method must always be static and be called main.";
        if (!paramType.equals("String")) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, method, generalMessage + "It must have a String[] parameter"));
        }

        var methodName = method.get("name");
        if (!methodName.equals("main")) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, method, generalMessage));
        }
        return null;
    }
    // private Void visitMethodCall(JmmNode method, SymbolTable table) {}
}
