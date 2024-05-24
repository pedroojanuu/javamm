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
    private final FunctionClassMap<TreeNode, Void> handlers;
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
        handlers.put(CondBranchInstruction.class, this::handleCondBranch);
    }
    private void addVariableToUse(String varName) {
        Descriptor d = descriptors.get(varName);
        if (d != null && d.getScope() != VarScope.PARAMETER) {
            usedVariables.get(instructionNumber).add(varName);
        }
    }

    private Void handleGoto(GotoInstruction gotoInstruction) {
        int successorIndex = currentMethod.getInstructions().indexOf(currentMethod.getLabels().get(gotoInstruction.getLabel()));
        successors.get(instructionNumber).add(successorIndex);
        return null;
    }
    private Void handleCondBranch(CondBranchInstruction condBranchInstruction) {
        handlers.apply(condBranchInstruction.getCondition());
        Instruction successor = currentMethod.getLabels().get(condBranchInstruction.getLabel());
        int successorNr = currentMethod.getInstructions().indexOf(successor);
        successors.get(instructionNumber).add(successorNr);
        return null;
    }
    private Void handleArrayOperand(ArrayOperand arrayOperand) {
        String arrayOperandName = arrayOperand.getName();
        addVariableToUse(arrayOperandName);
        List<Element> indexOperands = arrayOperand.getIndexOperands();
        indexOperands.forEach(handlers::apply);
        return null;
    }
    private Void handleGetField(GetFieldInstruction getFieldInstruction) {
        Element object = getFieldInstruction.getObject();
        handlers.apply(object);
        // this is probably not needed
        // field accesses are only allowed for "this" and "this" must always have a register
        return null;
    }
    private Void handleCall(CallInstruction callInstruction) {
        handlers.apply(callInstruction.getCaller());
        for (var arg : callInstruction.getArguments()) {
            handlers.apply(arg);
        }
        return null;
    }
    private Void handleUnaryOp(UnaryOpInstruction unaryOpInstruction) {
        Element operand = unaryOpInstruction.getOperand();
        handlers.apply(operand);
        return null;
    }
    private Void handleOperand(Operand operand) {
        String varName = operand.getName();
        addVariableToUse(varName);
        return null;
    }
    private Void handleSingleOp(SingleOpInstruction singleOpInstruction) {
        Element operand = singleOpInstruction.getSingleOperand();
        handlers.apply(operand);
        return null;
    }
    private Void handleLiteral(LiteralElement literalElement) {
        // no need to handle constants (e.g. 5 or true)
        return null;
    }
    private Void handleMethod(Method method) {
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
    private Void handleAssign(AssignInstruction assignInstruction) {
        var lhs = assignInstruction.getDest();
        Set<String> defined = definedVariables.get(instructionNumber);
        if (!(lhs instanceof Operand lhsOperand)) {
            // this should be impossible
            throw new NotImplementedException("AssignInstruction with non-Operand lhs");
        }
        if (lhsOperand.isParameter()) { // parameters have their own registers
            System.out.println("variable " + lhsOperand.getName() + " is a parameter");
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
    private Void handleBinaryOp(BinaryOpInstruction binaryOpInstruction) {
        Element left = binaryOpInstruction.getLeftOperand();
        Element right = binaryOpInstruction.getRightOperand();

        handlers.apply(left);   // TODO: change this? (I don't remember why this TODO is here...)
        handlers.apply(right);
        return null;
    }
    private Void handleReturn(ReturnInstruction returnInstruction) {
        Element returnOperand = returnInstruction.getOperand();
        if (returnOperand != null) {    // methods can have no return
            handlers.apply(returnOperand);
        }
        return null;
    }
    private Void handlePutField(PutFieldInstruction putFieldInstruction) {
        // fields do not count towards liveness or registers (because a register is always reserved for "this")
        // however, the value assigned to the field should be handled (added to used)

        Element value = putFieldInstruction.getValue();
        handlers.apply(value);
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
                instrLiveIn.addAll(instrLiveOut);       // first subtract
                instrLiveIn.removeAll(instrDefined);
                instrLiveIn.addAll(instrUsed);          // union

                // instrLiveOut = U { s in instrSuccessors } liveIn[s]
                for (Integer successor : instrSuccessors) {
                    instrLiveOut.addAll(liveIn.get(successor));
                }
            }
        } while(!liveIn.equals(prevLiveIn) || !liveOut.equals(prevLiveOut));    // repeat until liveIn and liveOut don't change

        return new LivenessAnalysisResult(liveIn, liveOut);
    }
}
