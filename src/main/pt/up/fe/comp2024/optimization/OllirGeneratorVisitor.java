package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

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
    private final String IMPORT = "import";
    private final String FIELD = ".field";
    private final String BOOL = ".bool";
    private final String INTEGER = ".i32";


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
        addVisit(MAIN_METHOD, this::visitMethodDecl);
        addVisit(OTHER_METHOD, this::visitMethodDecl);
        //addVisit(INT_ARRAY, this::visitIntArray);
        //addVisit(VAR_ARGS, this::visitVarArgs);
        addVisit(BOOLEAN, this::visitBoolean);
        addVisit(INT, this::visitInt);
        addVisit(OTHER_CLASSES, this::visitOtherClass);
        addVisit(PARAM, this::visitParam);
        addVisit(STMT_GROUP, this::visitStmtGroup);
        //addVisit(IF_STMT, this::visitIfStmt);
        //addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        //addVisit(ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);

        //addVisit(RETURN_STMT, this::visitReturnStmt);?????

        addVisit(PAREN_EXPR, this::visitParenExpr);
        //addVisit(ARRAY_INDEX_EXPR, this::visitArrayIndexExpr);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCallExpr);
        addVisit(LEN_EXPR, this::visitLenExpr);
        addVisit(NOT_EXPR, this::visitNotExpr);
        //addVisit(NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(NEW_OBJ_EXPR, this::visitNewObjExpr);
        addVisit(BINARY_EXPR, this::visitBinaryExpr);
        addVisit(INT_LITERAL_EXPR, this::visitIntLiteralExpr);
        addVisit(TRUE_LITERAL_EXPR, this::visitBoolLiteralExpr);
        addVisit(FALSE_LITERAL_EXPR, this::visitBoolLiteralExpr);
        addVisit(ID_LITERAL_EXPR, this::visitIdLiteralExpr);
        addVisit(THIS_EXPR, this::visitThisExpr);
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
        code.append(node.get("id"));
        code.append(END_STMT);

        return code.toString();
    }

    private String visitClassDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());
        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);
            // TODO: como vai visitar returnExpr se tal Kind não existe?

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
        // TODO
        StringBuilder code = new StringBuilder();

        switch (Kind.fromString(node.getParent().getKind())) {
            case CLASS_DECL -> code.append(FIELD);
            //case METHOD_DECL -> code.append()
        }

        code.append(SPACE);
        JmmNode varType = node.getChild(0);
        code.append(visit(varType));

        code.append(END_STMT);

        return code.toString();
    }

    private String visitType(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        switch (Kind.fromString(node.getKind())) {
            case BOOLEAN -> code.append(BOOL);
            case INT -> code.append(INTEGER);
            case OTHER_CLASSES -> {
                code.append(".");
                code.append(node.get("name"));
            }
        }

        return code.toString();
    }

    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // param
        var paramCode = visit(node.getJmmChild(1));
        code.append("(" + paramCode + ")");

        // type
        var retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);

        code.append(L_BRACKET);


        // rest of its children stmts
        var afterParam = 2;
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private String visitBoolean(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitInt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitOtherClass(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }

    private String visitStmtGroup(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitReturnStmt(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitParenExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitMethodCallExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

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

    private String visitBinaryExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitIntLiteralExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitBoolLiteralExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitIdLiteralExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        // TODO

        return code.toString();
    }

    private String visitThisExpr(JmmNode node, Void unused) {
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
