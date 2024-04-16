package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.symboltable.SymbolTableUtils;
import pt.up.fe.comp2024.utils.ReportUtils;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ID_LITERAL_EXPR, this::visitVarRefExpr);
    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        if (currentMethod == null) {
            // should not be possible
            addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, varRefExpr, "There is a variable reference outside of a method."));
        }

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("id");

        // Var is a field, return
        if (SymbolTableUtils.isField(varRefName, table)) {
            return null;
        }

        // Var is a parameter, return
        if (SymbolTableUtils.isParam(varRefName, currentMethod, table)) {
            return null;
        }

        // Var is a declared variable, return
        if (SymbolTableUtils.isLocal(varRefName, currentMethod, table)) {
            return null;
        }

        if (SymbolTableUtils.hasImport(table, varRefName)) {
            return null;
        }
        // Create error report
        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(ReportUtils.buildErrorReport(Stage.SEMANTIC, varRefExpr, message));

        return null;
    }


}
