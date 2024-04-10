package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

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
        Type resType = TypeUtils.getExprType(node, table, node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow());
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table, "");
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("id");
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type type = TypeUtils.getExprType(node, table, methodName);
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
        var object = node.getJmmChild(0);
        OllirExprResult objectCode = null;
        String invokeType = null;
        if (object.getKind().equals("IdLiteralExpr")) { // object is import
            objectCode = new OllirExprResult(object.get("id"));
            invokeType = "static";
        } else objectCode = visit(object);

        List<OllirExprResult> argsResult = new ArrayList<>();

        if (node.getChildren().size() > 1) {    // has args
            List<JmmNode> args = node.getJmmChild(1).getChildren();
            for (JmmNode arg : args)
                argsResult.add(visit(arg));
        }

        StringBuilder computation = new StringBuilder();

        for (OllirExprResult argResult : argsResult)
            computation.append(argResult.getComputation());

        if (objectCode.getCode().equals("this")) {
            computation.append(OptUtils.getTemp());
        }

        /*// code to compute self
        Type resType = TypeUtils.getExprType(node, table, "");
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table, "");
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);*/

        return new OllirExprResult("", computation);
    }

    private OllirExprResult visitThisExpr(JmmNode node, Void unused) {
        return new OllirExprResult("this");
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
