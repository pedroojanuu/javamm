package pt.up.fe.comp2024.optimization.ollir;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.*;

public class RegisterOptimization {
    List<Report> reports;
    private final FunctionClassMap<TreeNode, String> handlers;
    OllirResult ollirResult;
    String code;

    // using sets to prevent duplicates (e.g.: b = a + a)
    private final List<Set<String>> usedVariables = new ArrayList<>();
    private final List<Set<String>> definedVariables = new ArrayList<>();

    private final HashMap<String, List<List<String>>> in = new HashMap<>();
    private final HashMap<String, List<List<String>>> out = new HashMap<>();

    Method currentMethod;

    int instr_nr;

    public RegisterOptimization(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        handlers = new FunctionClassMap<>();
        reports = new ArrayList<>();

        handlers.put(ClassUnit.class, this::handleClassUnit);
        handlers.put(Method.class, this::handleMethod);
        handlers.put(AssignInstruction.class, this::handleAssign);

        /*
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(Field.class, this::generateField);
         */
    }
    private String handleClassUnit(ClassUnit classUnit) {
        for (var method : classUnit.getMethods()) {
            handlers.apply(method);
        }
        return null;
    }
    private String handleMethod(Method method) {
        currentMethod = method;
        String methodName = method.getMethodName();
        List<Instruction> instructions = method.getInstructions();

        usedVariables.clear();
        definedVariables.clear();

        for (int i = 0; i < instructions.size(); i++) {
            usedVariables.add(new HashSet<>());
            definedVariables.add(new HashSet<>());
        }

        for (instr_nr = 0; instr_nr < instructions.size(); instr_nr++) {
            Instruction instruction = instructions.get(instr_nr);
            Set<String> used = usedVariables.get(instr_nr);
            Set<String> defined = definedVariables.get(instr_nr);
            handlers.apply(instruction);
        }

        return null;
    }
    private String handleAssign(AssignInstruction assignInstruction) {
        var currentMethodName = currentMethod.getMethodName();

        var lhs = assignInstruction.getDest();
        if (!(lhs instanceof Operand lhsOperand)) {
            throw new NotImplementedException(lhs.getClass());
        }
        // Set<String> used = usedVariables.get(instr_nr);
        Set<String> defined = definedVariables.get(instr_nr);

        defined.add(lhsOperand.getName());
        handlers.apply(assignInstruction.getRhs());

        return null;
    }
    private String handleBinaryOp(BinaryOpInstruction binaryOpInstruction) {
        Element left = binaryOpInstruction.getLeftOperand();
        Element right = binaryOpInstruction.getRightOperand();

        handlers.apply(left);   // TODO: change this?
        handlers.apply(right);
        return null;
    }
    private String defaultGenerator(TreeNode node) {

        return "";
    }
    public OllirResult apply() {
        String code = handlers.apply(ollirResult.getOllirClass());
        return new OllirResult(code, ollirResult.getConfig());
        // return new OllirResult(null, Collections.emptyMap());
    }
}
