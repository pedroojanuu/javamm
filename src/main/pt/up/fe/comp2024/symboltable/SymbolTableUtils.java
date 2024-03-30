package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

public class SymbolTableUtils {
    public static boolean hasImport(SymbolTable table, String importName) {
        return table.getImports().stream()
                .anyMatch(importDecl -> importDecl.equals(importName));
    }
}
