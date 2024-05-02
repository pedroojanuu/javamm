package pt.up.fe.comp2024.optimization.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

public class ConstantPropagation extends AJmmVisitor<SymbolTable, Boolean> {
    private HashMap<String, Integer> values = new HashMap<>();

    // when we get out of a loop or if statement, we remove the values that were modified inside it
    private final Stack<Set<String>> valuesToRemove = new Stack<>();
    private String currentMethod;
    private boolean insideIfStmt = false;

    /**
     * This is used to visit all the assignments inside a while loop.
     * We need this to remove them from the known values.
     */
    private boolean visitOnlyAssigns = false;   //

    private void addValueToRemove(String id) {
        if (!valuesToRemove.isEmpty()) {
            valuesToRemove.peek().add(id);
        }
    }

    /**
     * Removes the given ids from the value Hashmap.
     * Must not assume that each of the values in the values list is inside the values hashmap.
     * @param toRemove list of ids to remove from the values hashmap
     */
    private void removeValues(Set<String> toRemove) {
        for (String value : toRemove) {
            this.values.remove(value);  // Make the value unknown (just came out of a conditional statement)
        }
    }
    protected Boolean defaultVisitor(JmmNode node, SymbolTable table) {
        SpecsCheck.checkNotNull(node, () -> "Node should not be null");
        //BiFunction<JmmNode, SymbolTable, Boolean> visit = this.getVisit(node);
        //Boolean nodeResult = visit.apply(node, table);
        Boolean result = false;

        for (JmmNode child : node.getChildren()) {
            result |= this.visit(child, table);
        }

        //return reduceFunction.apply(nodeResult, childrenResults);
        return result;
    }
    public ConstantPropagation() {
        this.setDefaultVisit(this::defaultVisitor);
    }
    @Override
    protected void buildVisitor() {
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
        addVisit(Kind.IF_STMT, this::visitIfStmt);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.ID_LITERAL_EXPR, this::visitIdLiteral);
    }
    private Boolean visitAssignStmt(JmmNode assignStmt, SymbolTable unused) {
        String id = assignStmt.get("id");
        if (visitOnlyAssigns) {
            // remove before the while statement
            // remove after the while statement, since we cannot be sure of its value
            addValueToRemove(id);
            return false;       // no need to visit children of assignment
        }
        if (insideIfStmt) {
            addValueToRemove(id);   // remove the value after the if statement
        }

        JmmNode right = assignStmt.getJmmChild(0);
        Boolean res = visit(right, unused);


        Kind rightKind = Kind.fromString(right.getKind());
        if (rightKind.equals(Kind.INT_LITERAL_EXPR)) {
            values.put(id, Integer.parseInt(right.get("value")));
        }
        else if (rightKind.equals(Kind.BOOLEAN_LITERAL_EXPR)) {
            values.put(id, right.get("value").equals("true") ? 1 : 0);
        }
        else {
            values.remove(id);  // not sure about the value anymore, need to remove it from known values
        }
        return res;
    }
    private Boolean visitIdLiteral(JmmNode idLiteral, SymbolTable table) {
        if (visitOnlyAssigns) return false;

        String id = idLiteral.get("id");

        if (values.containsKey(id)) {
            var intType = TypeUtils.getIntType();
            var boolType = TypeUtils.getBooleanType();
            var idType = TypeUtils.getIdType(id, idLiteral, table, currentMethod, null);

            JmmNode newNode;
            if (intType.equals(idType)) {
                newNode = NodeHelper.createNewIntLiteral(values.get(id));
            }
            else if (boolType.equals(idType)) {
                newNode = NodeHelper.createNewBooleanLiteral(values.get(id) != 0);
            }
            else return false;

            idLiteral.replace(newNode);
            values.put(id, values.get(id));

            return true;
        }
        return false;
    }
    private Boolean visitMethodDecl(JmmNode methodDecl, SymbolTable unused) {
        values.clear();
        currentMethod = methodDecl.get("name");

        Boolean res = false;
        for (JmmNode child : methodDecl.getChildren()) {
            res |= visit(child, unused);
        }
        return res;
    }
    private Boolean visitWhileStmt(JmmNode whileNode, SymbolTable unused) {
        if (visitOnlyAssigns) {
            return defaultVisitor(whileNode, unused);
        }

        JmmNode condition = whileNode.getJmmChild(0);
        Kind conditionKind = Kind.fromString(condition.getKind());
        /*
        if (conditionKind.equals(Kind.BOOLEAN_LITERAL_EXPR) && condition.get("value").equals("false")) {
            whileNode.delete();
            return true;
        }
        else
         */
        // JmmNode body = whileNode.getJmmChild(1);
        Boolean res = false;

        // while (loop) statements need to be treated more carefully than ifs (because of statements before assignments)
        // e.g. x = 0; while (...) { a = x; x = 1; a = x; }
        // the first x cannot be replaced by an int literal, while the second x can be replaced by 1.

        // visit assign statements inside the loop and remove them from the values hashmap
        // they will be added later when going through the loop if they have known values

        visitOnlyAssigns = true;
        System.out.println("visiting while for assigns");
        valuesToRemove.push(new HashSet<>());
        for (JmmNode child : whileNode.getChildren()) {
            res |= visit(child, unused);
        }
        System.out.println("done visiting while for assigns: " + valuesToRemove.peek());
        Set<String> toRemove = valuesToRemove.pop();
        this.removeValues(toRemove);
        visitOnlyAssigns = false;
        System.out.println("values after while: " + values);

        for (JmmNode child : whileNode.getChildren()) {
            res |= visit(child, unused);
        }

        this.removeValues(toRemove);  // remove the values inside the while loop again after processing the loop
        return res;
    }
    private Boolean visitIfStmt(JmmNode ifNode, SymbolTable unused) {
        if (visitOnlyAssigns) {
            return defaultVisitor(ifNode, unused);
        }
        /*
        JmmNode condition = ifNode.getJmmChild(0);
        Kind conditionKind = Kind.fromString(condition.getKind());
        if (conditionKind.equals(Kind.BOOLEAN_LITERAL_EXPR)) {
            var conditionIsTrue = condition.get("value").equals("true");
            if (conditionIsTrue) {
                JmmNode ifBody = ifNode.getJmmChild(1);
                ifBody.removeParent();
                ifNode.replace(ifBody);
                visit(ifBody, unused);
            }  else {
                JmmNode elseBody = ifNode.getJmmChild(2);
                elseBody.removeParent();

                ifNode.replace(elseBody);
                visit(elseBody, unused);
            }
            return true;
        }
        else {
         */
        Boolean res = false;
        boolean previousInsideIfStmt = insideIfStmt;
        HashMap<String, Integer> previousValues = new HashMap<>(values);    // easier method to independently analyse if and else bodies

        insideIfStmt = true;
        valuesToRemove.push(new HashSet<>());
        res |= visit(ifNode.getJmmChild(0), unused);    // boolean

        res |= visit(ifNode.getJmmChild(1), unused);    // ifBody
        values = previousValues;
        res |= visit(ifNode.getJmmChild(2), unused);    // elseBody

        for (JmmNode child : ifNode.getChildren()) {
            res |= visit(child, unused);
        }
        Set<String> toRemove = valuesToRemove.pop();
        this.removeValues(toRemove);  // can remove from previousValues (or values coming from if or else)
        insideIfStmt = previousInsideIfStmt;
        return res;
    }
}
