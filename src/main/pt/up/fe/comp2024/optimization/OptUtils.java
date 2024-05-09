package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;
    private static int ifThenNumber = -1;
    private static int ifEndNumber = -1;
    private static int whileCondNumber = -1;
    private static int whileBodyNumber = -1;
    private static int varArgsNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String getIfThen() {
        ifThenNumber++;
        return "if_then_" + ifThenNumber;
    }

    public static String getIfEnd() {
        ifEndNumber++;
        return "if_end_" + ifEndNumber;
    }

    public static String getWhileCond() {
        whileCondNumber++;
        return "while_cond_" + whileCondNumber;
    }

    public static String getWhileBody() {
        whileBodyNumber++;
        return "while_body_" + whileBodyNumber;
    }

    public static String getVarArgsArray() {
        varArgsNumber++;
        return "__varargs_array_" + varArgsNumber;
    }

    public static String toOllirType(JmmNode typeNode) {

        TYPE.checkOrThrow(typeNode);

        String typeName = typeNode.getKind();

        return toOllirType(typeName, typeName.contains("Array"));
    }

    public static String toOllirType(Type type) {
        return toOllirType(type.getName(), type.isArray());
    }

    public static String toOllirBoolean(String value) {
        return switch (value) {
            case "false" -> "0";
            case "true" -> "1";
            default -> throw new NotImplementedException(value);
        };
    }

    private static String toOllirType(String typeName, boolean isArray) {

        String type = (isArray? ".array." : ".")
                + switch (typeName) {
                    case "Int", "int", "IntArray" -> "i32";
                    case "Boolean", "boolean" -> "bool";
                    case "Void", "void" -> "V";
                    default -> typeName;
                };

        return type;
    }


}
