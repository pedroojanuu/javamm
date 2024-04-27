package pt.up.fe.comp2024.optimization.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.Kind;

public class NodeHelper {
    public static JmmNode createNewIntLiteral(int value) {
        JmmNode result = new JmmNodeImpl(Kind.INT_LITERAL_EXPR.toString());
        result.put("value", String.valueOf(value));
        return result;
    }
    public static JmmNode createNewBooleanLiteral(boolean value) {
        JmmNode result = new JmmNodeImpl(Kind.BOOLEAN_LITERAL_EXPR.toString());
        result.put("value", value ? "true" : "false");
        return result;
    }
}
