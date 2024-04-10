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
import pt.up.fe.comp2024.utils.ReportUtils;


public class BinExprTypes extends AnalysisVisitor  {
    private String currentMethod;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }
    private void checkArithmetic(JmmNode left, JmmNode right, Type leftType, Type rightType, SymbolTable table) {
        var intType = TypeUtils.getIntType();
        if (!intType.equals(leftType) || !intType.equals(rightType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, left, "Arithmetic operation with non-integer types"));
        }
    }
    private void checkComparison(JmmNode left, JmmNode right, Type leftType, Type rightType, SymbolTable table) {
        var intType = TypeUtils.getIntType();
        if (!intType.equals(leftType) || !intType.equals(rightType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, left, "Comparison operation with non-integer types"));
        }
    }
    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        var left = binaryExpr.getObject("left", JmmNode.class);
        var right = binaryExpr.getObject("right", JmmNode.class);
        var op = binaryExpr.get("op");

        var leftType = TypeUtils.getExprType(left, table, currentMethod, this.getReports());
        var rightType = TypeUtils.getExprType(right, table, currentMethod, this.getReports());

        if (leftType != null && !leftType.equals(rightType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "Binary expression with different types"));
        }
        else {
            if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
                checkArithmetic(left, right, leftType, rightType, table);
            } else if (op.equals("<")) {
                checkComparison(left, right, leftType, rightType, table);
            }
            else if (op.equals("&&") || op.equals("||")) {
                var booleanType = TypeUtils.getBooleanType();
                if (!booleanType.equals(leftType) || !booleanType.equals(rightType)) {
                    addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "Logical operation with non-boolean types"));
                }
            }
            else {
                addReport(Report.newError(Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        "Unknown operator",
                        null)
                );
            }
        }
        return null;
    }

}
