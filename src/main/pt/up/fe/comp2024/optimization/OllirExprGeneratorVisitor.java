package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.comp2024.symboltable.SymbolTableUtils;

import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(ID_LITERAL_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INT_LITERAL_EXPR, this::visitInteger);
        addVisit(BOOLEAN_LITERAL_EXPR, this::visitBooleanLiteral);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCallExpr);
        addVisit(THIS_EXPR, this::visitThisExpr);
        addVisit(NEW_OBJ_EXPR, this::visitNewObjExpr);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table, node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow(), null);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table, node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow(), null);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("id");
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type type = TypeUtils.getExprType(node, table, methodName, null);
        String ollirType = "";
        if (type != null) ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }


    private OllirExprResult visitBooleanLiteral(JmmNode node, Void unused) {
        var boolType = new Type(TypeUtils.getBooleanTypeName(), false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String code = OptUtils.toOllirBoolean(node.get("value")) + ollirBoolType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitMethodCallExpr(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        OllirExprResult objectVisit = visit(node.getJmmChild(0));
        computation.append(objectVisit.getComputation());
        String objectName = objectVisit.getCode();
        String methodName = node.get("method");

        String invoke = "";
        String type = "";

        if (table.getImports().contains(objectName))
            // since the "object" is a mere imported class, the method has to be static
            invoke = "invokestatic";
        else invoke = "invokevirtual";

        var assignAncestor = node.getAncestor(ASSIGN_STMT);
        if (assignAncestor.isPresent())
            // type will be that of the lhs of the expression
            type = OptUtils.toOllirType(TypeUtils.getIdType(assignAncestor.get().get("id"), node.getParent(), table, node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow(), null));
        else type = ".V";


        List<OllirExprResult> argsResult = new ArrayList<>();

        if (node.getChildren().size() > 1) {    // has args
            List<JmmNode> args = node.getJmmChild(1).getChildren();
            for (JmmNode arg : args)
                argsResult.add(visit(arg));
        }

        for (OllirExprResult argResult : argsResult)
            computation.append(argResult.getComputation());

        StringBuilder invocation = new StringBuilder();
        invocation.append(invoke + "(" + objectName + ", \"" + methodName + "\"");

        if (!argsResult.isEmpty()) {
            invocation.append(", ");
            int i;
            for (i = 0; i < argsResult.size() - 1; i++)
                invocation.append(argsResult.get(i).getCode() + ", ");
            invocation.append(argsResult.get(i).getCode());
        }

        invocation.append(")" + type);

        if (type.equals(".V")) code.append(invocation);
        else {
            String resultTemp = OptUtils.getTemp();
            computation.append(resultTemp);
            computation.append(type + " " + ASSIGN + type + " ");
            computation.append(invocation + END_STMT);
            code.append(resultTemp + type);
        }

        /*if (objectName.equals("this")) {
            resultTemp = OptUtils.getTemp();
            computation.append(resultTemp);
            computation.append(type + " " + ASSIGN + type + " ");
            computation.append("invokevirtual(this, \"" + methodName + "\"");
            if (!argsResult.isEmpty()) {
                computation.append(", ");
                int i;
                for (i = 0; i < argsResult.size() - 1; i++)
                    computation.append(argsResult.get(i).getCode() + ", ");
                computation.append(argsResult.get(i).getCode());
            }
            computation.append(")" + type + END_STMT);
        } else if (type.equals("") || TypeUtils.getExprType(node.getJmmChild(0), table, node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow(), null) != null) {
            // field or local variable || method from import that has a result to be assigned
            resultTemp = OptUtils.getTemp();
            computation.append(resultTemp);
            computation.append(type + " " + ASSIGN + type + " ");
            computation.append("invokevirtual(" + objectName + ", \"" + methodName + "\"");
            if (!argsResult.isEmpty()) {
                computation.append(", ");
                int i;
                for (i = 0; i < argsResult.size() - 1; i++)
                    computation.append(argsResult.get(i).getCode() + ", ");
                computation.append(argsResult.get(i).getCode());
            }
            computation.append(")" + type + END_STMT);
        } else {
            // void method from import
            code.append("invokestatic(" + objectName + ", \"" + methodName + "\"");
            if (!argsResult.isEmpty()) {
                code.append(", ");
                int i;
                for (i = 0; i < argsResult.size() - 1; i++)
                    code.append(argsResult.get(i).getCode() + ", ");
                code.append(argsResult.get(i).getCode());
            }
            code.append(")");
        }

        code.append(resultTemp + type);
        */

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitThisExpr(JmmNode node, Void unused) {
        return new OllirExprResult("this");
    }

    private OllirExprResult visitNewObjExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        String type = node.get("id");
        String tempId = OptUtils.getTemp();

        computation.append(tempId + "." + type + " " + ASSIGN + "." + type +
                " new(" + type + ")." + type + END_STMT);

        computation.append("invokespecial(" +
                tempId + "." + type + ",\"<init>\").V" + END_STMT);

        return new OllirExprResult(tempId+"."+type, computation);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
