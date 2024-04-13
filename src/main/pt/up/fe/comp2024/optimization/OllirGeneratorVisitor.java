package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";
    private final String IMPORT = "import ";
    private final String FIELD = ".field ";
    private final String PUBLIC = "public ";
    private final String RET = "ret";

    private Set<JmmNode> varDeclTemp = new HashSet<>();

    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }

    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(CLASS_DECL, this::visitClassDecl);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(TYPE, this::visitType);
        addVisit(OTHER_CLASSES, this::visitOtherClasses);
        addVisit(PARAM, this::visitParam);
        addVisit(MAIN_METHOD, this::visitMainMethodDecl);
        addVisit(OTHER_METHOD, this::visitOtherMethodDecl);
        //addVisit(INT_ARRAY, this::visitIntArray);
        //addVisit(VAR_ARGS, this::visitVarArgs);
        //addVisit(IF_STMT, this::visitIfStmt);
        //addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        //addVisit(ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);
        addVisit(PAREN_EXPR, this::visitParenExpr);
        //addVisit(ARRAY_INDEX_EXPR, this::visitArrayIndexExpr);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCallExpr);
        addVisit(LEN_EXPR, this::visitLenExpr);
        addVisit(NOT_EXPR, this::visitNotExpr);
        //addVisit(NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(NEW_OBJ_EXPR, this::visitNewObjExpr);
        //addVisit(ARRAY_DECL_EXPR, this::visitArrayDeclExpr);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    private String visitImportDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append(IMPORT);

        var id = node.get("id");
        var contents = id.substring(1, id.length() - 1).split(", ");
        String result = String.join(".", contents);
        code.append(result);

        code.append(END_STMT);

        return code.toString();
    }

    private String visitClassDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(NL);

        code.append(table.getClassName());
        if (node.hasAttribute("extended_name"))
            code.append(" extends " + node.get("extended_name"));

        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String visitVarDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        if (fromString(node.getParent().getKind()) == CLASS_DECL)
            code.append(FIELD + PUBLIC);

        code.append(node.get("name"));

        JmmNode varType = node.getChild(0);
        code.append(visit(varType));

        if (fromString(node.getParent().getKind()) == MAIN_METHOD || fromString(node.getParent().getKind()) == OTHER_METHOD) {
            varDeclTemp.add(node);
            return "";
        }

        code.append(END_STMT);

        return code.toString();
    }

    private String visitType(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append(OptUtils.toOllirType(node));

        return code.toString();
    }

    private String visitOtherClasses(JmmNode node, Void unused) {
        return "." + node.get("name");
    }

    private String visitMainMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        code.append ("public static main");

        // param
        code.append("(");
        code.append(node.get("parameterName") + ".array");
        code.append(OptUtils.toOllirType(TypeUtils.getMyClassType(node.get("paramType"), "")));
        code.append(")");

        code.append(".V");

        code.append(L_BRACKET);

        // children stmts
        for (int i = 0; i < node.getNumChildren(); i++)
            code.append(visit(node.getJmmChild(i)));

        code.append(RET + ".V" + END_STMT);

        code.append(R_BRACKET);
        code.append(NL);

        varDeclTemp.clear();

        return code.toString();
    }

    private String visitOtherMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        code.append("(");
        var params = node.getChildren(PARAM);
        if (!params.isEmpty()) {
            int i;
            for (i = 0; i < params.size() - 1; i++) {
                var param = params.get(i);
                var paramCode = visit(param);
                code.append(paramCode + ", ");
            }
            var lastParam = params.get(i);
            code.append(visit(lastParam));
        }
        code.append(")");

        // type
        boolean isVoid = Objects.equals(node.get("methodType"), "void");
        if (!isVoid) {
            var retType = visit(node.getJmmChild(0));
            System.out.println(node.getJmmChild(0));
            code.append(retType);
            System.out.println(retType);
        } else code.append(OptUtils.toOllirType(TypeUtils.getVoidType()));

        code.append(L_BRACKET);

        // rest of its children stmts
        var afterParam = params.size() + (isVoid? 0 : 1);
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            if (!isVoid && i == node.getNumChildren()-1)
                code.append(visitReturnStmt(child, null));
            else code.append(visit(child));
        }

        if (isVoid) code.append(RET + ".V" + END_STMT);

        code.append(R_BRACKET);
        code.append(NL);

        varDeclTemp.clear();

        return code.toString();
    }

    private String visitParam(JmmNode node, Void unused) {

        var typeCode = visit(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var res = exprVisitor.visit(node.getJmmChild(0));

        code.append(res.getComputation());
        code.append(res.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        var id = node.get("id");
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type type = TypeUtils.getIdType(id, node, table, methodName, null);
        String ollirType = OptUtils.toOllirType(type);

        var rhs = exprVisitor.visit(node.getJmmChild(0));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table, methodName, null);
        String typeString = OptUtils.toOllirType(thisType);

        code.append(id + ollirType);
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();

        /*code.append(lhsCode);
        code.append(SPACE + ASSIGN);

        // code to compute self
        // statement has type of lhs
        String typeString = OptUtils.toOllirType(lhs.getChild(0));
        code.append(typeString + SPACE);

        code.append(exprVisitor.visit(node.getJmmChild(0)).getCode());

        code.append(END_STMT);

        return code.toString();*/
    }

    private String visitReturnStmt(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        expr = exprVisitor.visit(node);

        code.append(expr.getComputation());
        code.append(RET);
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitParenExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // code.append("(");

        JmmNode expr = node.getJmmChild(0);
        code.append(exprVisitor.visit(expr));

        // code.append(")");

        return code.toString();
    }

    private String visitMethodCallExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        /*List<JmmNode> args = null;

        if (node.getChildren().size() > 1)
            if (node.getJmmChild(1).getKind().equals("Arglist"))
                args = node.getJmmChild(1).getChildren();

        List<String> argsCode = new ArrayList<>();

        if (args != null)
            args.forEach(arg -> {
                var result = exprVisitor.visit(arg);
                code.append(result.getComputation());
                argsCode.add(result.getCode());
            });

        Type idType = TypeUtils.getIdLiteralExprType(node.getJmmChild(0), table, node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow());

        // id is an import
        if (Objects.isNull(idType)) code.append("invokestatic(");

        code.append(node.getJmmChild(0).get("id") + ", \"" + node.get("method") + "\"");

        if (!argsCode.isEmpty()) {
            code.append(", ");
            for (int i = 0; i < argsCode.size()-1; i++)
                code.append(argsCode.get(i) + ", ");
            code.append(argsCode.getLast());
            }

        code.append(")");

        // id is an import
        if (Objects.isNull(idType)) code.append(".V");*/

        var res = exprVisitor.visit(node);
        code.append(res.getComputation());
        code.append(res.getCode());

        return code.toString();
    }

    private String visitLenExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitNotExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitNewObjExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
