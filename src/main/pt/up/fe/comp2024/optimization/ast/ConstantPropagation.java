package pt.up.fe.comp2024.optimization.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ConstantPropagation {
    public boolean visit(JmmNode node, SymbolTable table) {
        return false;
    }
}
