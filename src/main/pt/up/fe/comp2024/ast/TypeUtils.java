package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.symboltable.SymbolTableUtils;
import pt.up.fe.comp2024.utils.ReportUtils;

import java.util.List;

public class TypeUtils {
    private static boolean mainIsStatic = false;
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
    public static void setMainIsStatic(boolean isStatic) {
        mainIsStatic = isStatic;
    }
    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table, String currentMethod, List<Report> reports) {
        var kind = Kind.fromString(expr.getKind());
        return switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr, table, currentMethod, reports);
            case ID_LITERAL_EXPR -> getIdLiteralExprType(expr, table, currentMethod, reports);
            case INT_LITERAL_EXPR, ARRAY_INDEX_EXPR -> getIntType();
            case MEMBER_ACCESS_EXPR -> getMemberAccessExprType(expr, table, currentMethod, reports);
            case THIS_EXPR -> getMyClassType(table.getClassName(), table.getSuper());
            case NEW_OBJ_EXPR -> getNewObjExprType(expr, table);
            case METHOD_CALL_EXPR -> getMethodCallExprType(expr, table, currentMethod, reports);
            case NEW_ARRAY_EXPR, ARRAY_DECL_EXPR -> getIntArrayType();
            case BOOLEAN_LITERAL_EXPR -> getBooleanType();
            case PAREN_EXPR -> getParenthesisExprType(expr, table, currentMethod, reports);
            case NOT_EXPR -> getNotExprType(expr, table, currentMethod, reports);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };
    }
    public static Type getNotExprType(JmmNode expr, SymbolTable table, String currentMethod, List<Report> reports) {
        var innerExpr = expr.getChild(0);
        var innerType = getExprType(innerExpr, table, currentMethod, reports);
        if (reports != null && !reports.isEmpty() && innerType != null && !getBooleanType().equals(innerType)) {
            reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, expr, "Invalid type for operator '!'"));
        }
        return getBooleanType();
    }
    public static Type getParenthesisExprType(JmmNode expr, SymbolTable table, String currentMethod, List<Report> reports) {
        return getExprType(expr.getChild(0), table, currentMethod, reports);
    }
    /**
     *
     * @param methodCallExpr
     * @param table
     * @param currentMethod
     * @return Message for the error report if the method call is invalid, null otherwise
     */
    public static String isValidMethodCall(JmmNode methodCallExpr, SymbolTable table, String currentMethod, List<Report> reports) {
        var defaultErrorMessage = "Unknown method call";
        var invalidArgumentsMessage = "Invalid arguments for method call.";

        var object = methodCallExpr.getObject("object", JmmNode.class);
        var methodName = methodCallExpr.get("method");
        var objectType = getExprType(object, table, currentMethod, reports);

        // e.g.: this.method(params); a = this, a.method(params);
        if (table.getClassName().equals(getName(objectType))) {
            boolean methodExists = table.getMethods().stream()
                    .anyMatch(m -> m.equals(methodName));
            if (!methodExists) {
                var superName = objectType.getOptionalObject("super").orElse("");
                if (superName.equals("")) {
                    return defaultErrorMessage + " '" + methodName + "' on class '" + table.getClassName() + "'";
                }
                return null;
            }
            var argList = methodCallExpr.getChild(methodCallExpr.getNumChildren() - 1);
            var paramsST = table.getParameters(methodName);
            int varArgsIdx = -1;
            // If the calling method accepts varargs, it can accept both a variable number of arguments of
            // the same type as an array, or directly an array
            for (int i = 0; i < paramsST.size(); i++) {
                var paramType = paramsST.get(i).getType();
                var argType = getExprType(argList.getChild(i), table, currentMethod, reports);
                if (paramType.hasAttribute("varArgs")) {
                    varArgsIdx = i;
                    break;
                }
                if (!areTypesAssignable(argType, paramType, table)) {
                    return invalidArgumentsMessage;
                }
            }
            if (varArgsIdx == -1) {
                if (argList.getNumChildren() != paramsST.size()) {
                    return invalidArgumentsMessage;
                }
                return null;
            }

            var argType = getExprType(argList.getChild(varArgsIdx), table, currentMethod, reports);
            if (getIntArrayType().equals(argType)) {
                return null;
            }
            for (int i = varArgsIdx; i < argList.getNumChildren(); i++) {
                argType = getExprType(argList.getChild(i), table, currentMethod, reports);
                if (!areTypesAssignable(argType, getIntType(), table)) {   // just compare to int
                    return invalidArgumentsMessage;
                }
            }
            return null;
        }

        // assume that methods from imports are valid
        var importId = object.getOptional("id").orElse(null);
        if (SymbolTableUtils.hasImport(table, importId) || SymbolTableUtils.hasImport(table, getName(objectType))) {
            return null;
        }
        return defaultErrorMessage;
    }

    private static String getName(Type type) {
        return type == null ? null : type.getName();
    }
    public static Type getMethodCallExprType(JmmNode methodCallExpr, SymbolTable table, String currentMethod, List<Report> reports) {
        var methodName = methodCallExpr.get("method");
        var object = methodCallExpr.getObject("object", JmmNode.class);
        var objectType = getExprType(object, table, currentMethod, reports);

        // e.g.: this.method(params); a = this, a.method(params);
        if (table.getClassName().equals(getName(objectType))) {
            var method = table.getMethods().stream()
                    .filter(m -> m.equals(methodName))
                    .findFirst()
                    .orElse(null);

            if (method == null && reports != null) {
                var superName = objectType.getOptionalObject("super").orElse("");
                if (superName.equals("")) {
                    reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, methodCallExpr, "Unknown method '" + methodName + "' for class '" + table.getClassName() + "'"));
                }
            }

            return table.getReturnType(method);
        }

        System.out.println(objectType);
        // assume that methods from imports are valid
        if (SymbolTableUtils.hasImport(table, getName(objectType)) || objectType == null) {
            return null;
        }
        if (reports != null) {
            reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, methodCallExpr, "Unknown method '" + methodName + "'"));
        }
        return null;
    }
    public static Type getNewObjExprType(JmmNode newObjExpr, SymbolTable table) {
        var className = newObjExpr.get("id");
        if (table.getClassName().equals(className)) {
            return getMyClassType(className, table.getSuper());
        }
        return getMyClassType(className, "");
    }
    public static Type getMemberAccessExprType(JmmNode memberAccessExpr, SymbolTable table, String currentMethod, List<Report> reports) {
        var member = memberAccessExpr.get("member");
        if (member.equals("length")) {
            return getIntType();
        }
        if (SymbolTableUtils.isField(member, table)) {
            return SymbolTableUtils.getField(member, table).getType();
        }

        reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, memberAccessExpr, "Unknown member access '" + member + "'"));
        return null;
    }
    private static Type getBinExprType(JmmNode binaryExpr, SymbolTable table, String currentMethod, List<Report> reports) {
        String operator = binaryExpr.get("op");
        JmmNode left = binaryExpr.getObject("left", JmmNode.class);
        JmmNode right = binaryExpr.getObject("right", JmmNode.class);
        Type leftType = getExprType(left, table, currentMethod, reports);
        Type rightType = getExprType(right, table, currentMethod, reports);
        switch (operator) {
            case "+", "-", "/", "*" -> {
                Type intType = getIntType();
                if (intType.equals(leftType) && intType.equals(rightType)) {
                    return getIntType();
                }
                if (reports != null) {
                    reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "Invalid types for operator '" + operator + "'"));
                }
                return null;
            }
            case "<" -> {
                Type intType = getIntType();
                if (intType.equals(leftType) && intType.equals(rightType)) {
                    return getBooleanType();
                }
                if (reports != null) {
                    reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "Invalid types for operator '" + operator + "'"));
                }
                return null;
            }
            case "&&" -> {
                Type booleanType = getBooleanType();
                if (booleanType.equals(leftType) && booleanType.equals(rightType)) {
                    return getBooleanType();
                }
                if (reports != null) {
                    reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, binaryExpr, "Invalid types for operator '" + operator + "'"));
                }
                return null;
            }

            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        }
    }

    public static Type getIdType(String id, JmmNode node, SymbolTable table, String currentMethod, List<Report> reports) {
        System.out.println("getIdType: " + id + " " + currentMethod + " " + table.getClassName() + " " + table.getFields());
        if (SymbolTableUtils.isLocal(id, currentMethod, table)) {
            return SymbolTableUtils.getLocal(id, currentMethod, table).getType();
        }

        if (SymbolTableUtils.isParam(id, currentMethod, table)) {
            return SymbolTableUtils.getParam(id, currentMethod, table).getType();
        }
        if (SymbolTableUtils.isField(id, table) && !(currentMethod.equals("main") && mainIsStatic)) {
            return SymbolTableUtils.getField(id, table).getType();
        }

        if (SymbolTableUtils.hasImport(table, id)) {
            return null;
        }
        reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, node, "Unknown variable '" + id + "'"));
        return null;
    }
    public static Type getIdLiteralExprType(JmmNode idLiteralExpr, SymbolTable table, String currentMethod, List<Report> reports) {
        var name = idLiteralExpr.get("id");
        return getIdType(name, idLiteralExpr, table, currentMethod, reports);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType, SymbolTable table) {
        System.out.println(sourceType);
        System.out.println(destinationType);
        if (sourceType == null || destinationType == null ||
                sourceType.equals(destinationType) || typeInherits(sourceType, destinationType)) {
            return true;
        }

        // both from imports: assume correct assignment
        return SymbolTableUtils.hasImport(table, sourceType.getName()) && SymbolTableUtils.hasImport(table, destinationType.getName());
    }
}
