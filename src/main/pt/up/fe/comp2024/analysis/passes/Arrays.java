package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.comp2024.utils.ReportUtils;

public class Arrays extends AnalysisVisitor {
    private String currentMethod;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_INDEX_EXPR, this::visitArrayAccess);
        addVisit(Kind.MEMBER_ACCESS_EXPR, this::visitMemberAccess);
        addVisit(Kind.ARRAY_DECL_EXPR, this::visitArrayDecl);
        addVisit(Kind.ARRAY_ASSIGN_STMT, this::visitArrayIndexAssign);
    }
    private Void visitArrayIndexAssign(JmmNode node, SymbolTable table) {
        var arrayId = node.get("id");
        var index = node.getObject("index", JmmNode.class);
        var value = node.getObject("value", JmmNode.class);
        var arrayType = TypeUtils.getIdType(arrayId, node, table, currentMethod, this.getReports());
        var indexType = TypeUtils.getExprType(index, table, currentMethod, this.getReports());
        var valueType = TypeUtils.getExprType(value, table, currentMethod, this.getReports());

        if (arrayType != null && !arrayType.isArray()) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, node, "Array access on non-array type"));
        }
        if (indexType != null && !TypeUtils.getIntType().equals(indexType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, index, "Array index must be of type int"));
        }
        if (arrayType != null && valueType != null && (
                !arrayType.getName().equals(valueType.getName()) || valueType.isArray())
        ) {
            // should assign int to array element, but it's assigning either a different type or an array
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, value, "Array element assignment with different type"));
        }
        return null;
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArrayAccess(JmmNode method, SymbolTable table) {
        var array = method.getObject("name", JmmNode.class);
        var index = method.getObject("index", JmmNode.class);
        var arrayType = TypeUtils.getExprType(array, table, currentMethod, this.getReports());
        var indexType = TypeUtils.getExprType(index, table, currentMethod, this.getReports());
        if (!arrayType.isArray()) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, array, "Array access on non-array type"));
        }
        if (!TypeUtils.getIntType().equals(indexType)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, index, "Array index must be of type int"));
        }
        return null;
    }
    private Void visitMemberAccess(JmmNode method, SymbolTable table) {
        var leftExpr = method.getObject("object", JmmNode.class);
        String member = method.get("member");
        var leftExprType = TypeUtils.getExprType(leftExpr, table, currentMethod, this.getReports());

        if (member.equals("length")) {
            if (leftExprType != null && !leftExprType.isArray()) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, leftExpr, "Access length on non-array type"));
            }
            return null;
        }
        if (!table.getMethods().contains(member)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, method, "Accessing field or invalid member"));
            return null;
        }
        return null;
    }
    private Void visitArrayDecl(JmmNode method, SymbolTable table) {
        var elements = method.getChildren();
        var intType = TypeUtils.getIntType();
        for (var element : elements) {
            var elementType = TypeUtils.getExprType(element, table, currentMethod, this.getReports());
            if (!intType.equals(elementType)) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, element, "Array elements must be of type int"));
                break;
            }
        }
        return null;
    }
}
