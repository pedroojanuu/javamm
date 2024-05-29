package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

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
    private final String IF = "if";
    private final String GOTO = "goto";

    private final SymbolTable table;

    private List<JmmNode> importNodes = new ArrayList<>();
    private boolean visitingReturn = false;
    private Type returnType = null;
    private boolean visitingArgImported = false;
    private Type visitingArgImportedType = null;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    public void appendImportNode(JmmNode node) {
        importNodes.add(node);
    }

    public void setVisitingReturn(Type retType) {
        this.visitingReturn = true;
        this.returnType = retType;
    }

    public void unsetVisitingReturn() {
        this.visitingReturn = false;
        this.returnType = null;
    }

    @Override
    protected void buildVisitor() {

        addVisit(PAREN_EXPR, this::visitParenExpr);
        addVisit(ARRAY_INDEX_EXPR, this::visitArrayIndexExpr);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCallExpr);
        addVisit(MEMBER_ACCESS_EXPR, this::visitMemberAccessExpr);  // length
        addVisit(NOT_EXPR, this::visitNotExpr);
        addVisit(NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(NEW_OBJ_EXPR, this::visitNewObjExpr);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INT_LITERAL_EXPR, this::visitInteger);
        addVisit(BOOLEAN_LITERAL_EXPR, this::visitBooleanLiteral);
        addVisit(ID_LITERAL_EXPR, this::visitVarRef);
        addVisit(THIS_EXPR, this::visitThisExpr);
        addVisit(ARRAY_DECL_EXPR, this::visitArrayDeclExpr);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult andOptimization(JmmNode node) {
        StringBuilder computation = new StringBuilder();

        OllirExprResult lhs = visit(node.getJmmChild(0));
        OllirExprResult rhs = visit(node.getJmmChild(1));

        String booleanOllirType = OptUtils.toOllirType(TypeUtils.getBooleanType());

        computation.append(lhs.getComputation()).append(rhs.getComputation());

        String temp = OptUtils.getTemp() + booleanOllirType;
        String ifThen = OptUtils.getIfThen();
        String ifEnd = OptUtils.getIfEnd();

        computation.append(IF + SPACE + "(" + lhs.getCode() + ")" + SPACE + GOTO + SPACE + ifThen + END_STMT);
        computation.append(temp + SPACE + ASSIGN + booleanOllirType + SPACE + OptUtils.toOllirBoolean("false") + booleanOllirType + END_STMT);
        computation.append(GOTO + SPACE + ifEnd + END_STMT);

        computation.append(ifThen + ":\n");
        computation.append(temp + SPACE + ASSIGN + booleanOllirType + SPACE + rhs.getCode() + END_STMT);

        computation.append(ifEnd + ":\n");

        return new OllirExprResult(temp, computation);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        if (node.get("op").equals("&&")) return andOptimization(node);

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

        /*
            0 = import
            1 = field
            2 = param
            3 = local var
         */
        int varType = 0;

        List<Symbol> classFields = table.getFields();
        for (Symbol symbol : classFields) {
            if (symbol.getName().equals(id)) {
                varType = 1;
                break;
            }
        }

        Optional<List<Symbol>> methodParams = table.getParametersTry(methodName);
        if (methodParams.isPresent()) {
            List<Symbol> params = methodParams.get();
            for (Symbol symbol : params) {
                if (symbol.getName().equals(id)) {
                    varType = 2;
                    break;
                }
            }
        }

        Optional<List<Symbol>> methodLocals = table.getLocalVariablesTry(methodName);
        if (methodLocals.isPresent()) {
            List<Symbol> locals = methodLocals.get();
            for (Symbol symbol : locals) {
                if (symbol.getName().equals(id)) {
                    varType = 3;
                    break;
                }
            }
        }

        // TODO: repensar
//        if (isField) {
//            for (JmmNode imp : importNodes) {
//                if (imp.get("ID").equals(id)) {
//                    isField = false;
//                    break;
//                }
//            }
//        }

        if (varType == 1) return classFieldVar(node);

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

    private OllirExprResult visitCallExprDiscard(JmmNode node, String ollirType) {

        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        OllirExprResult objectVisit = visit(node.getJmmChild(0));
        computation.append(objectVisit.getComputation());

        String objectName;
        String methodName = node.get("method");

        String invoke = "invokestatic";

        if (node.getJmmChild(0).getKind().equals(THIS_EXPR.toString())) {
            objectName = "this";
            invoke = "invokevirtual";
        }
        else if (node.getJmmChild(0).getKind().equals(ID_LITERAL_EXPR.toString()))
            objectName = node.getJmmChild(0).get("id");
        else {
            objectName = "";
            invoke = "invokevirtual";
        }

        String caller = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();

        Optional<List<Symbol>> methodLocals = table.getLocalVariablesTry(caller);
        if (methodLocals.isPresent()) {
            List<Symbol> locals = methodLocals.get();
            for (Symbol symbol : locals) {
                System.out.println(symbol);
                System.out.println(objectName);
                if (symbol.getName().equals(objectName)) {
                    invoke = "invokevirtual";
                    break;
                }
            }
        }

        Optional<List<Symbol>> methodParams = table.getParametersTry(caller);

        if (invoke.equals("invokestatic") && methodParams.isPresent()) {
            List<Symbol> params = methodParams.get();
            for (Symbol symbol : params) {
                if (symbol.getName().equals(objectName)) {
                    invoke = "invokevirtual";
                    break;
                }
            }
        }

        List<Symbol> classFields = table.getFields();

        if (invoke.equals("invokestatic")) {
            for (Symbol symbol : classFields) {
                if (symbol.getName().equals(objectName)) {
                    invoke = "invokevirtual";
                    break;
                }
            }
        }

        List<OllirExprResult> argsResult = visitArgs(node, methodName);

        for (OllirExprResult argResult : argsResult)
            computation.append(argResult.getComputation());

        code.append(invoke + "(" + objectVisit.getCode() + ", \"" + methodName + "\"");

        if (!argsResult.isEmpty()) {
            code.append(", ");
            int i;
            for (i = 0; i < argsResult.size() - 1; i++)
                code.append(argsResult.get(i).getCode() + ", ");
            code.append(argsResult.get(i).getCode());
        }

        code.append(")" + ollirType);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitArrayIndexExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        String intOllirType = OptUtils.toOllirType(TypeUtils.getIntType());

        OllirExprResult lhs = visit(node.getJmmChild(0));
        computation.append(lhs.getComputation());

        OllirExprResult rhs = visit(node.getJmmChild(1));
        computation.append(rhs.getComputation());

        String code = OptUtils.getTemp() + intOllirType;

        computation.append(code + SPACE + ASSIGN
                + intOllirType + SPACE + lhs.getCode()
                + "[" + rhs.getCode() + "]" + intOllirType + END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitMethodCallExpr(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        OllirExprResult objectVisit = visit(node.getJmmChild(0));
        computation.append(objectVisit.getComputation());

        String objectName;

        String invoke = "invokestatic";
        String type = "";

        if (node.getJmmChild(0).getKind().equals(THIS_EXPR.toString())) {
            objectName = "this";
            invoke = "invokevirtual";
        }
        else if (node.getJmmChild(0).getKind().equals(ID_LITERAL_EXPR.toString()))
            objectName = node.getJmmChild(0).get("id");
        else {
            objectName = "";
            invoke = "invokevirtual";
        }

        String caller = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        String callee = node.get("method");

        Optional<JmmNode> assignAncestor = node.getAncestor(ASSIGN_STMT);
        if (!assignAncestor.isPresent()) assignAncestor = node.getAncestor(ARRAY_ASSIGN_STMT);
        Optional<JmmNode> invokeAncestor = node.getAncestor(METHOD_CALL_EXPR);  // to determine if result will be discarded
        Optional<JmmNode> notOptAncestor = node.getAncestor(NOT_EXPR);
        Optional<JmmNode> binOptAncestor = node.getAncestor(BINARY_EXPR);
        boolean whileParent = node.getParent().getKind().equals(IF_STMT.toString());
        boolean ifParent = node.getParent().getKind().equals(WHILE_STMT.toString());

        if (assignAncestor.isPresent() && !invokeAncestor.isPresent())
            // type will be that of the lhs of the assignment expression
            type = OptUtils.toOllirType(TypeUtils.getIdType(assignAncestor.get().get("id"), node.getParent(), table, node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow(), null));
        else if (notOptAncestor.isPresent())
            type = OptUtils.toOllirType(TypeUtils.getBooleanType());
        else if (binOptAncestor.isPresent()) {
            String op = binOptAncestor.get().get("op");
            if (op.equals("*") || op.equals("/") || op.equals("+") || op.equals("-"))
                type = OptUtils.toOllirType(TypeUtils.getIntType());
            else if (op.equals("<") || op.equals("&&"))
                type = OptUtils.toOllirType(TypeUtils.getBooleanType());
        } else if (whileParent || ifParent)
            type = OptUtils.toOllirType(TypeUtils.getBooleanType());
        else if (table.getMethods().contains(callee)) {
            type = OptUtils.toOllirType(table.getReturnType(callee));
            if (!visitingReturn && !assignAncestor.isPresent() && !invokeAncestor.isPresent()) return visitCallExprDiscard(node, type);
        } else if (this.visitingArgImported)
            type = OptUtils.toOllirType(this.visitingArgImportedType);
        else if (visitingReturn)
            type = OptUtils.toOllirType(returnType);
        else type = OptUtils.toOllirType(TypeUtils.getVoidType());


        Optional<List<Symbol>> methodLocals = table.getLocalVariablesTry(caller);
        if (methodLocals.isPresent()) {
            List<Symbol> locals = methodLocals.get();
            for (Symbol symbol : locals) {
                System.out.println(symbol);
                System.out.println(objectName);
                if (symbol.getName().equals(objectName)) {
                    invoke = "invokevirtual";
                    break;
                }
            }
        }

        Optional<List<Symbol>> methodParams = table.getParametersTry(caller);

        if (invoke.equals("invokestatic") && methodParams.isPresent()) {
            List<Symbol> params = methodParams.get();
            for (Symbol symbol : params) {
                if (symbol.getName().equals(objectName)) {
                    invoke = "invokevirtual";
                    break;
                }
            }
        }

        List<Symbol> classFields = table.getFields();

        if (invoke.equals("invokestatic")) {
            for (Symbol symbol : classFields) {
                if (symbol.getName().equals(objectName)) {
                    invoke = "invokevirtual";
                    break;
                }
            }
        }

        // pode ter nome igual a import!
//        for (JmmNode imp : importNodes)
//            if (imp.get("ID").equals(objectName)) {
//                invoke = "invokestatic";
//                break;
//            }

        List<OllirExprResult> argsResult = visitArgs(node, callee);

        for (OllirExprResult argResult : argsResult)
            computation.append(argResult.getComputation());

        StringBuilder invocation = new StringBuilder();
        invocation.append(invoke + "(" + objectVisit.getCode() + ", \"" + callee + "\"");

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
        return new OllirExprResult("this." + table.getClassName());
    }

    private OllirExprResult visitNewArrayExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        String intOllirType = OptUtils.toOllirType(TypeUtils.getIntType());
        String intArrayOllirType = OptUtils.toOllirType(TypeUtils.getIntArrayType());

        OllirExprResult size = visit(node.getJmmChild(0));
        String sizeTemp = OptUtils.getTemp() + intOllirType;
        computation.append(size.getComputation());
        computation.append(sizeTemp + SPACE + ASSIGN + intOllirType + SPACE + size.getCode() + END_STMT);

        String arrayTemp = OptUtils.getTemp() + intArrayOllirType;
        computation.append(arrayTemp + SPACE + ASSIGN + intArrayOllirType
        + " new(array, " + sizeTemp + ")" + intArrayOllirType + END_STMT);

        return new OllirExprResult(arrayTemp, computation);
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

    private OllirExprResult visitMemberAccessExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        String intOllirType = OptUtils.toOllirType(TypeUtils.getIntType());
        String intArrayOllirType = OptUtils.toOllirType(TypeUtils.getIntArrayType());

        String code = OptUtils.getTemp() + intOllirType;
        OllirExprResult id = visit(node.getJmmChild(0));
        computation.append(id.getComputation());
        computation.append(code + SPACE + ASSIGN + intOllirType
        + " arraylength(" + id.getCode() + intArrayOllirType
        + ")" + intOllirType + END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitNotExpr(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();

        OllirExprResult childVisit = visit(node.getJmmChild(0));

        computation.append(childVisit.getComputation());

        String temp = OptUtils.getTemp();
        String type = OptUtils.toOllirType(TypeUtils.getBooleanType());

        computation.append(temp + type + SPACE + ASSIGN + type + SPACE);
        computation.append("!" + OptUtils.toOllirType(TypeUtils.getBooleanType()) + SPACE + childVisit.getCode() + END_STMT);

        return new OllirExprResult(temp + type, computation);
    }

    private OllirExprResult visitArrayDeclExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        String intOllirType = OptUtils.toOllirType(TypeUtils.getIntType());
        String intArrayOllirType = OptUtils.toOllirType(TypeUtils.getIntArrayType());

        String size = String.valueOf(node.getNumChildren()) + intOllirType;
        String temp = OptUtils.getTemp() + intArrayOllirType;
        String varArgsArray = OptUtils.getVarArgsArray() + intArrayOllirType;

        computation.append(temp + SPACE + ASSIGN + intArrayOllirType
        + " new(array, " + size + ")" + intArrayOllirType + END_STMT);

        computation.append(varArgsArray + SPACE + ASSIGN + intArrayOllirType + SPACE + temp + END_STMT);

        for (int i = 0; i < node.getNumChildren(); i++) {
            OllirExprResult childVisit = visit(node.getChild(i));
            computation.append(childVisit.getComputation());

            computation.append(varArgsArray + "[" + i + intOllirType + "]"
            + intOllirType + SPACE + ASSIGN + intOllirType + SPACE + childVisit.getCode() + END_STMT);
        }

        return new OllirExprResult(varArgsArray, computation);
    }

    private List<OllirExprResult> visitArgs(JmmNode node, String calleeName) {
        List<OllirExprResult> argsResult = new ArrayList<>();

        if (node.getNumChildren() <= 1) return argsResult;

        List<JmmNode> args = node.getJmmChild(1).getChildren();
        List<JmmNode> params = new ArrayList<>();
        boolean hasVarargs = false;

        if (node.getAncestor(OTHER_METHOD).isPresent()) {
            List<JmmNode> classMethods = node.getAncestor(OTHER_METHOD).get().getParent().getChildren();
            for (JmmNode method : classMethods) {
                if (method.get("name").equals(calleeName)) {
                    for (JmmNode param : method.getChildren()) {
                        if (param.getKind().equals(PARAM.toString())) {
                            params.add(param);
                            if (param.get("paramType").equals(VAR_ARGS.toString())) {
                                hasVarargs = true;
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }

        int i;
        int thresh = !params.isEmpty() ? params.size() - 1 : args.size() - 1;

        for (i = 0; i < thresh; i++) {
            JmmNode arg = args.get(i);
            if (arg.getKind().equals(METHOD_CALL_EXPR.toString()) && !table.getMethods().contains(arg.get("method"))) {
                this.visitingArgImported = true;
                this.visitingArgImportedType = table.getParameters(calleeName).get(i).getType();
            }
            argsResult.add(visit(arg));
            this.visitingArgImported = false;
            this.visitingArgImportedType = null;
        }

        if (!hasVarargs) {
            JmmNode arg = args.get(args.size()-1);
            if (arg.getKind().equals(METHOD_CALL_EXPR.toString()) && !table.getMethods().contains(arg.get("method"))) {
                this.visitingArgImported = true;
                this.visitingArgImportedType = table.getParameters(calleeName).get(table.getParameters(calleeName).size()-1).getType();
            }
            argsResult.add(visit(arg));
            this.visitingArgImported = false;
            this.visitingArgImportedType = null;

            return argsResult;
        }

        StringBuilder computation = new StringBuilder();

        String intOllirType = OptUtils.toOllirType(TypeUtils.getIntType());
        String intArrayOllirType = OptUtils.toOllirType(TypeUtils.getIntArrayType());

        String size = args.size() - i + intOllirType;
        String temp = OptUtils.getTemp() + intArrayOllirType;
        String varArgsArray = OptUtils.getVarArgsArray() + intArrayOllirType;

        computation.append(temp + SPACE + ASSIGN + intArrayOllirType
                + " new(array, " + size + ")" + intArrayOllirType + END_STMT);

        computation.append(varArgsArray + SPACE + ASSIGN + intArrayOllirType + SPACE + temp + END_STMT);

        int idx = 0;

        for (i = i; i < args.size(); i++) {
            OllirExprResult childVisit = visit(args.get(i));
            computation.append(childVisit.getComputation());

            computation.append(varArgsArray + "[" + idx + intOllirType + "]"
                    + intOllirType + SPACE + ASSIGN + intOllirType + SPACE + childVisit.getCode() + END_STMT);

            idx++;
        }

        argsResult.add(new OllirExprResult(varArgsArray, computation));
        return argsResult;
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
