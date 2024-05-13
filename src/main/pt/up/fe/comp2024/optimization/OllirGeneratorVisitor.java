package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.comp2024.symboltable.SymbolTableUtils;

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
    private final String IF = "if";
    private final String GOTO = "goto";
    private final String END_LABEL = ":\n";

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
        addVisit(STMT_GROUP, this::visitStmtGroup);
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);

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

        // Compound imports come from the tree as a list of strings
        // i.e. "import a.b.c;" comes as ["a", "b", "c"]
        String id = node.get("id");
        String[] contents = id.substring(1, id.length() - 1).split(", ");
        String result = String.join(".", contents);
        code.append(result);

        code.append(END_STMT);

        exprVisitor.appendImportNode(node);

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
        Kind parentKind = fromString(node.getParent().getKind());
        if (parentKind == MAIN_METHOD || parentKind == OTHER_METHOD) return "";

        StringBuilder code = new StringBuilder();

        code.append(FIELD + PUBLIC);

        code.append(node.get("name"));

        JmmNode varType = node.getChild(0);
        code.append(visit(varType));

        code.append(END_STMT);

        return code.toString();
    }

    private String visitType(JmmNode node, Void unused) {
        return OptUtils.toOllirType(node);
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

        return code.toString();
    }

    private String visitOtherMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        if (NodeUtils.getBooleanAttribute(node, "isPublic", "false")) code.append("public ");

        // name
        code.append(node.get("name"));

        // params
        code.append("(");
        List<JmmNode> params = node.getChildren(PARAM);
        if (!params.isEmpty()) {
            for (int i = 0; i < params.size() - 1; i++) code.append(visit(params.get(i)) + ", ");
            code.append(visit(params.get(params.size() - 1)));
            // code.append(visit(params.getLast()));
        }
        code.append(")");

        // type
        code.append(visit(node.getJmmChild(0)));

        code.append(L_BRACKET);

        // rest of its children stmts
        int afterParams = params.size() + 1;
        for (int i = afterParams; i < node.getNumChildren() - 1; i++)
            code.append(visit(node.getJmmChild(i)));
        //code.append(returnStmt(node.getChildren().getLast()));
        code.append(returnStmt(node.getChildren().get(node.getNumChildren() - 1)));

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private String visitStmtGroup(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        for (JmmNode stmt : node.getChildren())
            code.append(visit(stmt));

        return code.toString();
    }

    private String visitIfStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        OllirExprResult condition = exprVisitor.visit(node.getJmmChild(0));
        code.append(condition.getComputation());

        String thenBody = visit(node.getJmmChild(1));
        String elseBody = visit(node.getJmmChild(2));

        String ifThen = OptUtils.getIfThen();
        String ifEnd = OptUtils.getIfEnd();

        code.append(IF + " (" + condition.getCode() + ") " + GOTO + SPACE + ifThen + END_STMT);
        code.append(elseBody);
        code.append(GOTO + SPACE + ifEnd + END_STMT);

        code.append(ifThen + END_LABEL);
        code.append(thenBody);

        code.append(ifEnd + END_LABEL);

        return code.toString();
    }

    private String visitParam(JmmNode node, Void unused) {
        return node.get("name") + visit(node.getJmmChild(0));
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        OllirExprResult condition = exprVisitor.visit(node.getJmmChild(0));

        String body = visit(node.getJmmChild(1));

        String whileCond = OptUtils.getWhileCond();
        String whileBody = OptUtils.getWhileBody();

        code.append(GOTO + SPACE + whileCond + END_STMT);

        code.append(whileBody + END_LABEL);
        code.append(body);

        code.append(whileCond + END_LABEL);
        code.append(condition.getComputation());
        code.append(IF + " (" + condition.getCode() + ") " + GOTO + SPACE + whileBody + END_STMT);

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        OllirExprResult res = exprVisitor.visit(node.getJmmChild(0));

        return res.getComputation() + res.getCode() + END_STMT;
    }

    private String classFieldAssign(JmmNode node) {
        StringBuilder code = new StringBuilder();

        String id = node.get("id");
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        String type = OptUtils.toOllirType(TypeUtils.getIdType(id, node, table, methodName, null));

        OllirExprResult lhs = exprVisitor.visit(node.getJmmChild(0));

        code.append(lhs.getComputation());

        code.append("putfield(this, " + id + type + ", ");
        code.append(lhs.getCode() + ").V" + END_STMT);

        return code.toString();
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        String id = node.get("id");
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();


        boolean isField = true;

        if (SymbolTableUtils.isLocal(id, methodName, table)) isField = false;
        else if (SymbolTableUtils.isParam(id, methodName, table)) isField = false;

        if (isField) return classFieldAssign(node);


        OllirExprResult rhs = exprVisitor.visit(node.getJmmChild(0));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(rhs.getComputation());

        String type = OptUtils.toOllirType(TypeUtils.getIdType(id, node, table, methodName, null));

        code.append(id + type);
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(type);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String returnStmt(JmmNode node) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        exprVisitor.setVisitingReturn(retType);

        OllirExprResult expr = exprVisitor.visit(node);

        exprVisitor.unsetVisitingReturn();

        code.append(expr.getComputation());
        code.append(RET);
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitArrayAssignStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        String intOllirType = OptUtils.toOllirType(TypeUtils.getIntType());

        OllirExprResult indexVisit = exprVisitor.visit(node.getJmmChild(0));
        OllirExprResult rhsVisit = exprVisitor.visit(node.getJmmChild(1));

        code.append(indexVisit.getComputation());
        code.append(rhsVisit.getComputation());

        code.append(node.get("id") + "[" + indexVisit.getCode() + "]");
        code.append(intOllirType + SPACE + ASSIGN + intOllirType + SPACE + rhsVisit.getCode() + END_STMT);

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

