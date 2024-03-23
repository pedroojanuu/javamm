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
    public static Type getIntArrayType() {
        return new Type(INT_TYPE_NAME, true);
    }
    public static Type getBooleanType() {
        return new Type(BOOLEAN_TYPE_NAME, false);
    }
    public static Type getVoidType() {
        return new Type(VOID_TYPE_NAME, false);
    }
    public static Type getMyClassType(String name, String superName) {
        var type = new Type(name, false);
        type.putObject("super", superName);
        return type;
    }
    public static boolean typeInherits(Type subType, Type superType) {
        if (subType.equals(superType)) {
            return true;
        }
        var superName = subType.getOptionalObject("super").orElse(null);
        return superType.getName().equals(superName);
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
        var kind = Kind.fromString(expr.getKind());
        return switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case ID_LITERAL_EXPR -> getIdLiteralExprType(expr, table, currentMethod);
            case INT_LITERAL_EXPR, ARRAY_INDEX_EXPR -> getIntType();
            case MEMBER_ACCESS_EXPR -> getMemberAccessExprType(expr, table, currentMethod);
            case THIS_EXPR -> getMyClassType(table.getClassName(), table.getSuper());
            case NEW_OBJ_EXPR -> getNewObjExprType(expr, table);
            case METHOD_CALL_EXPR -> getMethodCallExprType(expr, table, currentMethod);
            case NEW_ARRAY_EXPR -> getIntArrayType();
            case BOOLEAN_LITERAL_EXPR -> getBooleanType();
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };
    }

    /**
     *
     * @param methodCallExpr
     * @param table
     * @param currentMethod
     * @return Message for the error report if the method call is invalid, null otherwise
     */
    public static String isValidMethodCall(JmmNode methodCallExpr, SymbolTable table, String currentMethod) {
        var defaultErrorMessage = "Unknown method call.";
        var invalidArgumentsMessage = "Invalid arguments for method call.";

        var object = methodCallExpr.getObject("object", JmmNode.class);
        var methodName = methodCallExpr.get("method");
        var objectType = getExprType(object, table, currentMethod);

        // e.g.: this.method(params); a = this, a.method(params);
        if (objectType.getName().equals(table.getClassName())) {
            boolean methodExists = table.getMethods().stream()
                    .anyMatch(m -> m.equals(methodName));
            if (!methodExists) {
                var superName = objectType.getOptionalObject("super").orElse("");
                if (superName.equals("")) {
                    return defaultErrorMessage;
                }
                return null;
            }
            var argList = methodCallExpr.getChild(methodCallExpr.getNumChildren() - 1);
            var paramsST = table.getParameters(methodName);
            int varArgsIdx = 0;
            for (int i = 0; i < paramsST.size(); i++) {
                var paramType = paramsST.get(i).getType();
                var argType = getExprType(argList.getChild(i), table, currentMethod);
                if (paramType.hasAttribute("varArgs")) {
                    varArgsIdx = i;
                    break;
                }
                if (!areTypesAssignable(argType, paramType)) {
                    return invalidArgumentsMessage;
                }
            }
            for (int i = varArgsIdx; i < argList.getNumChildren(); i++) {
                var argType = getExprType(argList.getChild(i), table, currentMethod);
                if (!areTypesAssignable(argType, getIntType())) {   // just compare to int
                    return invalidArgumentsMessage;
                }
            }
            return null;
        }

        // assume that methods from imports are valid
        if (table.getImports().stream()
                .anyMatch(imp -> imp.equals(objectType.getName()))) {
            return null;
        }
        return defaultErrorMessage;
    }
    public static Type getMethodCallExprType(JmmNode methodCallExpr, SymbolTable table, String currentMethod) {
        var methodName = methodCallExpr.get("method");
        var object = methodCallExpr.getObject("object", JmmNode.class);
        var objectType = getExprType(object, table, currentMethod);

        // e.g.: this.method(params); a = this, a.method(params);
        if (objectType.getName().equals(table.getClassName())) {
            var method = table.getMethods().stream()
                    .filter(m -> m.equals(methodName))
                    .findFirst()
                    .orElse(null);

            if (method == null) {
                throw new RuntimeException("Unknown method '" + methodName + "'");
            }

            return table.getReturnType(method);
        }
        // will probably have to deal with imports
        throw new RuntimeException("Unknown method '" + methodName + "'");
    }
    public static Type getNewObjExprType(JmmNode newObjExpr, SymbolTable table) {
        var className = newObjExpr.get("id");
        if (table.getClassName().equals(className)) {
            return getMyClassType(className, table.getSuper());
        }
        return getMyClassType(className, "");
    }
    public static Type getMemberAccessExprType(JmmNode memberAccessExpr, SymbolTable table, String currentMethod) {
        var member = memberAccessExpr.get("member");
        if (member.equals("length")) {
            return getIntType();
        }
        if (isField(member, table)) {
            return getField(member, table).getType();
        }

        // TODO: Might have to check imports
        throw new RuntimeException("Unknown member access '" + member + "'");
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

    public static Type getIdType(String id, SymbolTable table, String currentMethod) {
        if (isLocal(id, currentMethod, table)) {
            return getLocal(id, currentMethod, table).getType();
        }

        if (isParam(id, currentMethod, table)) {
            return getParam(id, currentMethod, table).getType();
        }

        if (isField(id, table)) {
            return getField(id, table).getType();
        }
        throw new RuntimeException("Unknown variable '" + id + "'");
    }
    public static Type getIdLiteralExprType(JmmNode idLiteralExpr, SymbolTable table, String currentMethod) {
        var name = idLiteralExpr.get("id");
        return getIdType(name, table, currentMethod);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: or they both come from imports
        return sourceType.getName().equals(destinationType.getName()) || typeInherits(sourceType, destinationType);
    }
}
