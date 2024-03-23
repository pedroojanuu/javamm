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

public class ArrayAccess extends AnalysisVisitor {
    private String currentMethod;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_INDEX_EXPR, this::visitArrayAccess);
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
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(array), NodeUtils.getColumn(array), "Array access on non-array type", null));
        }
        if (!indexType.equals(TypeUtils.getIntType())) {
            addReport(Report.newError(Stage.SEMANTIC, NodeUtils.getLine(index), NodeUtils.getColumn(index), "Array index must be of type int", null));
        }
        return null;
    }
}
