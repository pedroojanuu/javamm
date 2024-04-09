package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

public class SymbolTableUtils {
    public static boolean hasImport(SymbolTable table, String importName) {
        var imports = table.getImports();
        for (var importDecl : imports) {
            var parts = importDecl.split("\\.");
            if (parts[parts.length - 1].equals(importName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLocal(String name, String currentMethod, SymbolTable table) {
        return table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(name));
    }
    public static Symbol getLocal(String name, String currentMethod, SymbolTable table) {
        return table.getLocalVariables(currentMethod).stream()
                .filter(varDecl -> varDecl.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
    public static boolean isParam(String name, String currentMethod, SymbolTable table) {
        return table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(name));
    }
    public static Symbol getParam(String name, String currentMethod, SymbolTable table) {
        return table.getParameters(currentMethod).stream()
                .filter(param -> param.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static boolean isField(String name, SymbolTable table) {
        return table.getFields().stream()
                .anyMatch(f -> f.getName().equals(name));
    }
    public static Symbol getField(String name, SymbolTable table) {
        return table.getFields().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
