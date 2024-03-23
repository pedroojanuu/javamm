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
    }
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        // currentMethod = method.get("name");
        var params = method.getChildren(Kind.PARAM);

        for (int i = 0; i < params.size() - 1; i++) {
            var param = params.get(i);
            var kind = param.getKind();
            if (kind.equals(Kind.VAR_ARGS.getNodeName())) {
                addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, param,"VarArgs parameter must always be the last parameter"));
            }
        }

        return null;
    }
}
