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
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArrayAccess(JmmNode method, SymbolTable table) {
        var array = method.getObject("name", JmmNode.class);
        var index = method.getObject("index", JmmNode.class);
        var arrayType = TypeUtils.getExprType(array, table, currentMethod);
        var indexType = TypeUtils.getExprType(index, table, currentMethod);
        if (!arrayType.isArray()) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, array, "Array access on non-array type"));
        }
        if (!indexType.equals(TypeUtils.getIntType())) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, index, "Array index must be of type int"));
        }
        return null;
    }
    private Void visitMemberAccess(JmmNode method, SymbolTable table) {
        var leftExpr = method.getObject("object", JmmNode.class);
        String member = method.get("member");
        var leftExprType = TypeUtils.getExprType(leftExpr, table, currentMethod);

        if (member.equals("length") && !leftExprType.isArray()) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, leftExpr, "Access length on non-array type"));
        }
        return null;
    }
    private Void visitArrayDecl(JmmNode method, SymbolTable table) {
        var elements = method.getChildren();
        for (var element : elements) {
            var elementType = TypeUtils.getExprType(element, table, currentMethod);
            if (!elementType.equals(TypeUtils.getIntType())) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, element, "Array elements must be of type int"));
                break;
            }
        }
        return null;
    }
}
