package pt.up.fe.comp2024.optimization.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.List;
import java.util.function.BiFunction;

public class ConstantFolding extends PostorderJmmVisitor<Void, Boolean> {
    @Override
    protected BiFunction<Boolean, List<Boolean>, Boolean> getReduce() {
        return (a, b) -> a || b.stream().reduce(false, (x, y) -> x || y);
    }
    public ConstantFolding() {
        setDefaultValue(() -> false);
    }
    @Override
    protected void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.PAREN_EXPR, this::visitParenExpr);
        addVisit(Kind.NOT_EXPR, this::visitNotExpr);
    }
    private Boolean visitParenExpr(JmmNode parenExpr, Void unused) {
        JmmNode expr = parenExpr.getJmmChild(0);
        var exprKind = Kind.fromString(expr.getKind());
        if (exprKind == Kind.INT_LITERAL_EXPR || exprKind == Kind.BOOLEAN_LITERAL_EXPR) {
            expr.removeParent();
            parenExpr.replace(expr);
            return true;
        }
        return false;
    }

    private Boolean visitBinaryExpr(JmmNode binaryExpr, Void unused) {
        String operator = binaryExpr.get("op");
        JmmNode left = binaryExpr.getJmmChild(0);
        JmmNode right = binaryExpr.getJmmChild(1);
        //JmmNode left = binaryExpr.getObject("left", JmmNode.class);
        //JmmNode right = binaryExpr.getObject("right", JmmNode.class);

        var leftKind = Kind.fromString(left.getKind());
        var rightKind = Kind.fromString(right.getKind());

        if (leftKind == Kind.INT_LITERAL_EXPR && rightKind == Kind.INT_LITERAL_EXPR) {
            int leftValue = Integer.parseInt(left.get("value"));
            int rightValue = Integer.parseInt(right.get("value"));

            if (operator.equals("<")) {
                JmmNode newBooleanLiteral = NodeHelper.createNewBooleanLiteral(leftValue < rightValue);
                binaryExpr.replace(newBooleanLiteral);
                return true;
            }

            int result = switch (operator) {
                case "+" -> leftValue + rightValue;
                case "-" -> leftValue - rightValue;
                case "*" -> leftValue * rightValue;
                case "/" -> leftValue / rightValue;
                default -> throw new IllegalStateException("Unexpected value: " + operator);
            };
            binaryExpr.removeChild(left);
            binaryExpr.removeChild(right);

            JmmNode newIntLiteral = NodeHelper.createNewIntLiteral(result);
            binaryExpr.replace(newIntLiteral);

            return true;
        } else if (leftKind == Kind.BOOLEAN_LITERAL_EXPR && rightKind == Kind.BOOLEAN_LITERAL_EXPR) {
            boolean leftValue = left.get("value").equals("true");
            boolean rightValue = right.get("value").equals("true");

            boolean result = leftValue && rightValue;
            if (!operator.equals("&&")) {
                throw new IllegalStateException("Unexpected value: " + operator);
            }
            JmmNode newBooleanLiteral = NodeHelper.createNewBooleanLiteral(result);
            binaryExpr.replace(newBooleanLiteral);
            return true;
        }
        return false;
    }
    private Boolean visitNotExpr(JmmNode notExpr, Void unused) {
        JmmNode expr = notExpr.getJmmChild(0);

        var exprKind = Kind.fromString(expr.getKind());
        if (exprKind == Kind.BOOLEAN_LITERAL_EXPR) {
            boolean value = expr.get("value").equals("true");
            JmmNode newBooleanLiteral = NodeHelper.createNewBooleanLiteral(!value);
            notExpr.replace(newBooleanLiteral);

            return true;
        }
        return false;
    }
}
