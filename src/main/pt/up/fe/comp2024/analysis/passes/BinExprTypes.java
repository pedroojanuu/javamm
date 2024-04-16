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
    private void checkIntAuxiliar(JmmNode left, JmmNode right, Type leftType, Type rightType, SymbolTable table, String label) {
        var intType = TypeUtils.getIntType();
        if ((leftType != null && !intType.equals(leftType)) || (rightType != null && !intType.equals(rightType))) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, left, label + " operation with non-integer types"));
        }
    }
    private void checkArithmetic(JmmNode left, JmmNode right, Type leftType, Type rightType, SymbolTable table) {
        checkIntAuxiliar(left, right, leftType, rightType, table, "Arithmetic");
    }

    private void checkComparison(JmmNode left, JmmNode right, Type leftType, Type rightType, SymbolTable table) {
        checkIntAuxiliar(left, right, leftType, rightType, table, "Comparison");
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
            switch (op) {
                case "+", "-", "*", "/" -> checkArithmetic(left, right, leftType, rightType, table);
                case "<" -> checkComparison(left, right, leftType, rightType, table);
                case "&&", "||" -> {
                    var booleanType = TypeUtils.getBooleanType();
                    if ((leftType != null && !booleanType.equals(leftType)) || (rightType != null && !booleanType.equals(rightType))) {
                        addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "Logical operation with non-boolean types"));
                    }
                }
                default -> addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "Unknown operator"));
            }
        }
        return null;
    }

}
