package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.comp2024.ast.Kind;

public class AssignType extends AnalysisVisitor {
    private String currentMethod;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssign);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }
    private Void visitAssign(JmmNode assign, SymbolTable table) {
        var id = assign.get("id");
        var expr = assign.getObject("value", JmmNode.class);

        var leftType = TypeUtils.getIdType(id, table, currentMethod);
        var rightType = TypeUtils.getExprType(expr, table, currentMethod);

        var rightSuperName = rightType.getOptionalObject("super").orElse(null);
        System.out.println("start");
        System.out.println("left type: " + leftType);
        System.out.println("right type: " + rightType);
        System.out.println("right super name:" + rightSuperName);
        System.out.println("end");
        if (!leftType.equals(rightType) &&
                !leftType.getName().equals(rightSuperName)) {
            addReportNoException(assign, "Assignment with different types");
        }
        return null;
    }
}
