package pt.up.fe.comp2024.optimization.ollir;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.*;

public class LivenessAnalysis {
    List<Report> reports;
    private final FunctionClassMap<TreeNode, String> handlers;
    OllirResult ollirResult;

    // using sets to prevent duplicates (e.g.: b = a + a)
    private final List<Set<String>> usedVariables = new ArrayList<>();      // for each instruction, the variables used
    private final List<Set<String>> definedVariables = new ArrayList<>();   // for each instr., the vars defined
    private final List<Set<Integer>> successors = new ArrayList<>();        // for each instr., its successor instr.'s

    Method currentMethod;
    int instructionNumber;
    private Map<String, Descriptor> descriptors;

    public LivenessAnalysis(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        handlers = new FunctionClassMap<>();
        reports = new ArrayList<>();

        handlers.put(Method.class, this::handleMethod);
        handlers.put(AssignInstruction.class, this::handleAssign);
        handlers.put(SingleOpInstruction.class, this::handleSingleOp);
        handlers.put(LiteralElement.class, this::handleLiteral);
        handlers.put(ArrayOperand.class, this::handleArrayOperand);
        handlers.put(Operand.class, this::handleOperand);
        handlers.put(BinaryOpInstruction.class, this::handleBinaryOp);
        handlers.put(UnaryOpInstruction.class, this::handleUnaryOp);
        handlers.put(ReturnInstruction.class, this::handleReturn);
        handlers.put(CallInstruction.class, this::handleCall);
        handlers.put(PutFieldInstruction.class, this::handlePutField);
        handlers.put(GetFieldInstruction.class, this::handleGetField);

        handlers.put(GotoInstruction.class, this::handleGoto);

        // handlers.put(CondBranchInstruction.class, this::handleCondBranch);
            // handlers.put(SingleOpCondInstruction.class, this::handleSingleOp);
            // handlers.put(OpCondInstruction.class, this::handleOpCond);
    }
    private void addVariableToUse(String varName) {
        if (descriptors.get(varName) != null) {
            usedVariables.get(instructionNumber).add(varName);
        }
    }

    private String handleGoto(GotoInstruction gotoInstruction) {
        int successorIndex = currentMethod.getInstructions().indexOf(currentMethod.getLabels().get(gotoInstruction.getLabel()));
        successors.get(instructionNumber).add(successorIndex);
        return null;
    }
    private String handleCondBranch(CondBranchInstruction condBranchInstruction) {
        /*
        Element left = condBranchInstruction.getLeftOperand();
        Element right = condBranchInstruction.getRightOperand();

        handlers.apply(left);
        handlers.apply(right);
        */
        return null;
    }
    private String handleArrayOperand(ArrayOperand arrayOperand) {
        String arrayOperandName = arrayOperand.getName();
        addVariableToUse(arrayOperandName);
        List<Element> indexOperands = arrayOperand.getIndexOperands();
        indexOperands.forEach(handlers::apply);
        return null;
    }
    private String handleGetField(GetFieldInstruction getFieldInstruction) {
        Element object = getFieldInstruction.getObject();
        handlers.apply(object);
        return null;
    }
    private String handleCall(CallInstruction callInstruction) {
        handlers.apply(callInstruction.getCaller());
        for (var arg : callInstruction.getArguments()) {
            handlers.apply(arg);
        }
        return null;
    }
    private String handleUnaryOp(UnaryOpInstruction unaryOpInstruction) {
        Element operand = unaryOpInstruction.getOperand();
        handlers.apply(operand);
        return null;
    }
    private String handleOperand(Operand operand) {
        System.out.println("Operand name: " + operand.getName());
        String varName = operand.getName();
        addVariableToUse(varName);
        return null;
    }
    private String handleSingleOp(SingleOpInstruction singleOpInstruction) {
        Element operand = singleOpInstruction.getSingleOperand();
        handlers.apply(operand);
        return null;
    }
    private String handleLiteral(LiteralElement literalElement) {
        // no need to handle constants (e.g. 5 or true)
        return null;
    }
    private String handleMethod(Method method) {
        method.buildCFG();
        currentMethod = method;
        descriptors = method.getVarTable();

        List<Instruction> instructions = method.getInstructions();

        usedVariables.clear();
        definedVariables.clear();
        successors.clear();

        for (int i = 0; i < instructions.size(); i++) {
            usedVariables.add(new HashSet<>());
            definedVariables.add(new HashSet<>());
            successors.add(new HashSet<>());
        }

        for (instructionNumber = 0; instructionNumber < instructions.size(); instructionNumber++) {
            Instruction instruction = instructions.get(instructionNumber);

            if (instructionNumber < instructions.size() - 1) {  // not last instruction
                successors.get(instructionNumber).add(instructionNumber + 1);
            }

            handlers.apply(instruction);
        }

        return null;
    }
    private String handleAssign(AssignInstruction assignInstruction) {
        var lhs = assignInstruction.getDest();
        Set<String> defined = definedVariables.get(instructionNumber);
        if (!(lhs instanceof Operand lhsOperand)) {
            // this should be impossible
            throw new NotImplementedException("AssignInstruction with non-Operand lhs");
        }
        if (lhsOperand.isParameter()) { // parameters have their own registers
            return null;
        }

        defined.add(lhsOperand.getName());
        if (lhsOperand instanceof ArrayOperand arrayOperand) {   // handle array assignments
            // add array to defined variables (already done)
            // add others in lhs to used variables
            for (var indexOperand : arrayOperand.getIndexOperands()) {
                handlers.apply(indexOperand);
            }
        }

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
    private String handleReturn(ReturnInstruction returnInstruction) {
        Element returnOperand = returnInstruction.getOperand();
        if (returnOperand != null) {    // methods can have no return
            handlers.apply(returnOperand);
        }
        return null;
    }
    private String handlePutField(PutFieldInstruction putFieldInstruction) {
        // do fields count towards liveness and registers?
        /*
        Element object = putFieldInstruction.getObject();
        Element value = putFieldInstruction.getValue();
        handlers.apply(object);
        handlers.apply(value);
         */
        return null;
    }

    public LivenessAnalysisResult obtainResult(Method method) {
        handlers.apply(method);
        System.out.println("Method: " + method.getMethodName());
        System.out.println("Used: " + usedVariables);
        System.out.println("Defined: " + definedVariables);
        System.out.println("Successors: " + successors);

        List<Set<String>> liveIn = new ArrayList<>(), liveOut = new ArrayList<>();
        int nrInstructions = successors.size();
        for (int i = 0; i < nrInstructions; i++) {
            liveIn.add(new HashSet<>());
            liveOut.add(new HashSet<>());
        }
        List<Set<String>> prevLiveIn, prevLiveOut;
        do {
            prevLiveIn = new ArrayList<>();     // copy liveIn
            prevLiveOut = new ArrayList<>();    // copy of liveOut
            for (int i = 0; i < nrInstructions; i++) {
                prevLiveIn.add(new HashSet<>(liveIn.get(i)));
                prevLiveOut.add(new HashSet<>(liveOut.get(i)));
            }

            for (int i = 0; i < nrInstructions; i++) {
                Set<String> instrLiveIn = liveIn.get(i);
                Set<String> instrLiveOut = liveOut.get(i);
                Set<String> instrUsed = usedVariables.get(i);
                Set<String> instrDefined = definedVariables.get(i);
                Set<Integer> instrSuccessors = successors.get(i);

                // instrLiveIn = instrUsed U (instrLiveOut - instrDefined)
                instrLiveIn.clear();
                instrLiveIn.addAll(instrLiveOut);
                instrLiveIn.removeAll(instrDefined);
                instrLiveIn.addAll(instrUsed);

                // instrLiveOut = U { s in instrSuccessors } liveIn[s]
                for (Integer successor : instrSuccessors) {
                    instrLiveOut.addAll(liveIn.get(successor));
                }
            }
            System.out.println("live in: " + liveIn + " prev: " + prevLiveIn);
            System.out.println("live out: " + liveOut + " prev: " + prevLiveOut);
        } while(!liveIn.equals(prevLiveIn) || !liveOut.equals(prevLiveOut));    // repeat until liveIn and liveOut don't change

        return new LivenessAnalysisResult(liveIn, liveOut);
    }
}
