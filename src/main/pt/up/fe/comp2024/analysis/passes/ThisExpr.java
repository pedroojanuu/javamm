package pt.up.fe.comp2024.analysis.passes;

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

public class ThisExpr extends AnalysisVisitor {
    private JmmNode currentMethod;
    @Override
    protected void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.THIS_EXPR, this::visitThisExpr);
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method;
        return null;
    }
    private Void visitThisExpr(JmmNode thisExpr, SymbolTable table) {
        if (currentMethod.getObject("isStatic", Boolean.class).equals(true)) {
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, thisExpr, "This expression in static method"));
        }
        return null;
    }
}
