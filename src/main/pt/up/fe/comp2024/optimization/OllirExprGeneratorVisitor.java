package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.comp2024.symboltable.SymbolTableUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    private List<JmmNode> importNodes = new ArrayList<>();

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    public void appendImportNode(JmmNode node) {
        importNodes.add(node);
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
        addVisit(PAREN_EXPR, this::visitParenExpr);
        addVisit(NOT_EXPR, this::visitNotExpr);

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

    private OllirExprResult classFieldVar(JmmNode node) {
        StringBuilder computation = new StringBuilder();

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type type = TypeUtils.getExprType(node, table, methodName, null);
        String ollirType = "";
        if (type != null) ollirType = OptUtils.toOllirType(type);

        String temp = OptUtils.getTemp();

        computation.append(temp + ollirType + " " + ASSIGN + ollirType);
        computation.append(" getfield(this, " + node.get("id") + ollirType);
        computation.append(")" + ollirType + END_STMT);

        return new OllirExprResult(temp + ollirType, computation);
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("id");
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();

        boolean isField = false;

        List<Symbol> classFields = table.getFields();

        if (!isField) {
            for (Symbol symbol : classFields) {
                if (symbol.getName().equals(id)) {
                    isField = true;
                    break;
                }
            }
        }

        Optional<List<Symbol>> methodParams = table.getParametersTry(methodName);

        if (isField && methodParams.isPresent()) {
            List<Symbol> params = methodParams.get();
            for (Symbol symbol : params) {
                if (symbol.getName().equals(id)) {
                    isField = false;
                    break;
                }
            }
        }

        Optional<List<Symbol>> methodLocals = table.getLocalVariablesTry(methodName);

        if (isField && methodLocals.isPresent()) {
            List<Symbol> locals = methodLocals.get();
            for (Symbol symbol : locals) {
                if (symbol.getName().equals(id)) {
                    isField = false;
                    break;
                }
            }
        }
        if (isField) {
            for (JmmNode imp : importNodes) {
                if (imp.get("ID").equals(id)) {
                    isField = false;
                    break;
                }
            }
        }

        if (isField) return classFieldVar(node);

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

        invoke = "invokevirtual";

        System.out.println(importNodes);

        for (JmmNode imp : importNodes)
            if (imp.get("ID").equals(objectName)) {
                invoke = "invokestatic";
                break;
            }

        var assignAncestor = node.getAncestor(ASSIGN_STMT);
        if (assignAncestor.isPresent())
            // type will be that of the lhs of the expression
            type = OptUtils.toOllirType(TypeUtils.getIdType(assignAncestor.get().get("id"), node.getParent(), table, node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow(), null));
        else if (table.getMethods().contains(methodName)) {
            type = OptUtils.toOllirType(table.getReturnType(methodName));
        }
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

    private OllirExprResult visitParenExpr(JmmNode node, Void unused) {
        return visit(node.getJmmChild(0));
    }

    private OllirExprResult visitNotExpr(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();

        OllirExprResult childVisit = visit(node.getJmmChild(0));

        computation.append(childVisit.getComputation());

        String temp = OptUtils.getTemp();
        String type = ".bool";

        computation.append(temp + type + " " + ASSIGN + type + " ");
        computation.append("!.bool " + childVisit.getCode() + END_STMT);

        return new OllirExprResult(temp + type, computation);
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
