package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";
    private static final String VOID_TYPE_NAME = "void";
    private static final String STRING_TYPE_NAME = "String";
    public static Type getIntType() {
        return new Type(INT_TYPE_NAME, false);
    }
    public static Type getBooleanType() {
        return new Type(BOOLEAN_TYPE_NAME, false);
    }
    public static Type getVoidType() {
        return new Type(VOID_TYPE_NAME, false);
    }
    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }
    public static String getBooleanTypeName() {
        return BOOLEAN_TYPE_NAME;
    }
    public static String getVoidTypeName() {
        return VOID_TYPE_NAME;
    }
    public static String getStringTypeName() {
        return STRING_TYPE_NAME;
    }

    private static boolean isLocal(String name, String currentMethod, SymbolTable table) {
        return table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(name));
    }
    private static Symbol getLocal(String name, String currentMethod, SymbolTable table) {
        return table.getLocalVariables(currentMethod).stream()
                .filter(varDecl -> varDecl.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
    private static boolean isParam(String name, String currentMethod, SymbolTable table) {
        return table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(name));
    }
    private static Symbol getParam(String name, String currentMethod, SymbolTable table) {
        return table.getParameters(currentMethod).stream()
                .filter(param -> param.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private static boolean isField(String name, SymbolTable table) {
        return table.getFields().stream()
                .anyMatch(f -> f.getName().equals(name));
    }
    private static Symbol getField(String name, SymbolTable table) {
        return table.getFields().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table, String currentMethod) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());
        System.out.println(kind);
        return switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case ID_LITERAL_EXPR -> getIdLiteralExprType(expr, table, currentMethod);
            case INT_LITERAL_EXPR -> getIntType();
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "-", "/", "*" -> getIntType();
            case "<" -> getBooleanType();

            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getIdLiteralExprType(JmmNode idLiteralExpr, SymbolTable table, String currentMethod) {
        var name = idLiteralExpr.get("id");

        if (isLocal(name, currentMethod, table)) {
            return getLocal(name, currentMethod, table).getType();
        }

        if (isParam(name, currentMethod, table)) {
            return getParam(name, currentMethod, table).getType();
        }

        if (isField(name, table)) {
            return getField(name, table).getType();
        }
        throw new RuntimeException("Unknown variable '" + name + "'");
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
