package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

public class BoolExpr extends AnalysisVisitor {
    private String currentMethod;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);

        addVisit(Kind.WHILE_STMT, this::visitBoolExprStmt);
        addVisit(Kind.IF_STMT, this::visitBoolExprStmt);
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }
    private Void visitBoolExprStmt(JmmNode node, SymbolTable table) {
        var condition = node.getObject("condition", JmmNode.class);
        var conditionType = TypeUtils.getExprType(condition, table, currentMethod);

        if (!conditionType.equals(TypeUtils.getBooleanType())) {
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(condition), NodeUtils.getColumn(condition), "Condition must be of type boolean", null));
        }
        return null;
    }
}
