package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

//Notas falta - extends, constructors, True and false, operators, objRef&Arrays
//Imports em jasmin
//Erro no super class
//Diferenca entre ANDB e AND
//Duvidas o que operationSHRR seginifica
//Temos que implementar short-circuiting

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {
    private final boolean showCode = false;

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;
    int stackSize = 0;

    int maxStackSize = 0;

    Method currentMethod;
    ClassUnit currentClass;
    HashMap<String, String> importTable;

    private final FunctionClassMap<TreeNode, String> generators;

    private int numLessThan = 0;

    private Instruction nextInstructionIsIncrement;
    private boolean ignoreInst = false;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
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
        generators.put(CondBranchInstruction.class, this::generateCondBranch);
        generators.put(GotoInstruction.class, this::generateGoto);
    }

    public List<Report> getReports() {
        return reports;
    }

    // start of the import table, for each line in code, if it starts with import, add it to the import table
    // Exmple: "import org.Class1;" -> <"Class1","org/Class1">
    public void createImportTable(String code){
        importTable = new HashMap<>();
        String[] lines = code.split("\n");
        for (String line : lines) {
            if (line.startsWith("import")) {
                String[] parts = line.split(" ");
                String[] parts2 = parts[1].split(";");
                String[] parts3 = parts2[0].split("\\.");
                String className = parts3[parts3.length-1];
                String path = parts2[0].replace(".", "/");
                importTable.put(className, path);
            }
        }
    }

    public String getImportedClass(String className) {
        if(importTable.containsKey(className))
            return importTable.get(className);
        else
            return className;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            createImportTable(ollirResult.getOllirCode());
            code = generators.apply(ollirResult.getOllirClass());
            //  -------------------
            if(this.showCode) throw new RuntimeException(code);
            //  -------------------
        }

        return code;
    }

    private void increaseStackSize() {
        stackSize++;
        if (stackSize > maxStackSize) {
            maxStackSize = stackSize;
        }
    }

    private String generateClassUnit(ClassUnit classUnit) {

        currentClass = classUnit;
        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        if(classUnit.getSuperClass() != null) {
            String superClassName = getImportedClass(classUnit.getSuperClass());
            code.append(".super ").append(superClassName).append(NL).append(NL);
        } else {
            code.append(".super java/lang/Object").append(NL).append(NL);
        }

        // generate code for all other methodst/up/fe/comp/cp2/jasmin/
        for (var field : ollirResult.getOllirClass().getFields()) {
            code.append(generators.apply(field));
        }
        code.append(NL);

        if(classUnit.getSuperClass() == null) {
            // generate a single constructor method
            var defaultConstructor = """
                    ;default constructor
                    .method public <init>()V
                        aload_0
                        invokespecial java/lang/Object/<init>()V
                        return
                    .end method
                    """;
            code.append(defaultConstructor);
        }

        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        currentClass = null;

        return code.toString();
    }

    private String transformToJasminType(Type type) {
        if (type.getTypeOfElement() == ElementType.BOOLEAN) {
            return "Z";
        } else if (type.getTypeOfElement() == ElementType.INT32) {
            return "I";
        } else if (type.getTypeOfElement() == ElementType.VOID) {
            return "V";
        } else if (type.getTypeOfElement() == ElementType.STRING) {
            return "Ljava/lang/String;";
        } else if (type.getTypeOfElement() == ElementType.OBJECTREF) {
            ClassType castType = (ClassType) type;
            return "L" + getImportedClass(castType.getName()) + ";";
        } else if (type.getTypeOfElement() == ElementType.CLASS) {
            return "Ljava/lang/Class;";
        } else if(type.getTypeOfElement() == ElementType.ARRAYREF) {
            ArrayType castType = (ArrayType) type;
            return "[" + transformToJasminType(castType.getElementType());
        } else {
            throw new NotImplementedException(type.getTypeOfElement());
        }
    }

    private String generateMethod(Method method) {
        // set method
        currentMethod = method;
        maxStackSize = 0;

        var code = new StringBuilder();
        var codeTemp = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var staticModifier = method.isStaticMethod() ? "static " : "";

        var methodName = method.getMethodName();

        var methodType = transformToJasminType(method.getReturnType());

        var methodParams = method.getParams();

        String paramList = "";

        if(!methodParams.isEmpty()) {
            for (var param: methodParams) {
                paramList += transformToJasminType(param.getType());
            }
        }

        code.append("\n.method ")
                .append(modifier)
                .append(staticModifier)
                .append(methodName)
                .append("(" + paramList + ")")
                .append(methodType)
                .append(NL);

        HashMap<Instruction, List<String>> methodLabels = new HashMap<>();
        for (Map.Entry<String, Instruction> inst : method.getLabels().entrySet()){
            if(methodLabels.containsKey(inst.getValue())){
                methodLabels.get(inst.getValue()).add(inst.getKey());
            } else {
                List<String> labels = new ArrayList<>();
                labels.add(inst.getKey());
                methodLabels.put(inst.getValue(), labels);
            }
        }

        for (int i = 0; i < method.getInstructions().size(); i++) {

            var inst = method.getInstructions().get(i);

            if(i < method.getInstructions().size() - 1)
                nextInstructionIsIncrement = method.getInstructions().get(i + 1);
            else
                nextInstructionIsIncrement = null;

            if(ignoreInst){
                ignoreInst = false;
                continue;
            }

            if(methodLabels.containsKey(inst))
                for(String label : methodLabels.get(inst))
                    codeTemp.append(label).append(":").append(NL);
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            codeTemp.append(instCode);
//            codeTemp.append("------------------------------------").append(NL);
            for(int j = 0; j < stackSize; j++)
                codeTemp.append(TAB).append("pop").append(NL);
            stackSize = 0;

            // desativa a flag de possivel incremento
        }

        // Get max register number
        int maxReg = 0;
        for (var var : method.getVarTable().values()) {
            if (var.getVirtualReg() > maxReg) {
                maxReg = var.getVirtualReg();
            }
        }

        code.append(TAB).append(".limit stack ").append(maxStackSize).append(NL);
        code.append(TAB).append(".limit locals ").append(maxReg + 1).append(NL);

        code.append(codeTemp);

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        if(currentMethod == null)
            throw new RuntimeException("Method not set");
        var code = new StringBuilder();

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        String incrementOrDecrement = verifyIncrementOrDecrement(assign);
        if(incrementOrDecrement != null)
            return incrementOrDecrement;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        if(operand instanceof ArrayOperand) {
            increaseStackSize();
            if(reg <= 3 && reg >= 0)
                code.append("aload_").append(reg).append(NL);
            else
                code.append("aload ").append(reg).append(NL);
            code.append(generators.apply(((ArrayOperand) operand).getIndexOperands().get(0)));
        }

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        if(operand instanceof ArrayOperand) {
            stackSize -= 2;
            code.append("iastore").append(NL);
        } else if((assign.getTypeOfAssign().getTypeOfElement() == ElementType.INT32 ||
                   assign.getTypeOfAssign().getTypeOfElement() == ElementType.BOOLEAN) &&
                   !(operand.getType() instanceof ArrayType)) {
            if(reg <= 3 && reg >= 0)
                code.append("istore_").append(reg).append(NL);
            else
                code.append("istore ").append(reg).append(NL);
        } else{
            if(reg <= 3 && reg >= 0)
                code.append("astore_").append(reg).append(NL);
            else
                code.append("astore ").append(reg).append(NL);
        }

        stackSize--;
        return code.toString();
    }

    private String verifyIncrementOrDecrement(AssignInstruction assign) {
        if(assign.getRhs() instanceof BinaryOpInstruction){
            Operand dest = (Operand) assign.getDest();
            String ret = verifyIncrementOrDecrementAux(assign, dest);
            if(ret != null)
                return ret;

            if(nextInstructionIsIncrement != null && nextInstructionIsIncrement instanceof AssignInstruction nextAssign) {
                Operand originalDest = (Operand) assign.getDest();
                Operand possibleDest = (Operand) nextAssign.getDest();
                if(originalDest.getName().startsWith("tmp") &&
                        nextAssign.getRhs() instanceof SingleOpInstruction rhs &&
                        rhs.getSingleOperand() instanceof Operand op &&
                        op.getName().equals(originalDest.getName())){
                    ret = verifyIncrementOrDecrementAux(assign, possibleDest);
                    if(ret != null)
                        return ret;
                }
            }
        }

        // Espreita a instrucao da frente
        // Se inst atual for um assign, e proxima tambem, e o lado direito da proxima for igual ao laddo esquerdo do assign atual
        // entao ativa a flag de possivel incremento e otherLeftSide = lado esquerdo da proxima instrucao
        // Se flag ignoreInstruction estiver ativa, ignora a instrucao atual e desativa a flag

        // Se flag de possivel incremento estiver ativa, testa tambem com dest = otherLeftSide
        // Se isto for veradade, ativa a flag de ignorar instrucao

//            if(true) throw new RuntimeException(((Operand) dest).getName());
        //if(true) throw new NotImplementedException((dest.getName().equals(((Operand) left).getName())) ? "True" : "false");

        return null;

    }

    private String verifyIncrementOrDecrementAux(AssignInstruction assign, Operand dest) {
        BinaryOpInstruction rhs = (BinaryOpInstruction) assign.getRhs();
        Element left = (Element) rhs.getLeftOperand();
        Element right = (Element) rhs.getRightOperand();
        OperationType operation = rhs.getOperation().getOpType();

        int numberToInc;

        if(right.isLiteral() && left instanceof Operand && dest.getName().equals(((Operand) left).getName())) {
            //if(true) throw new NotImplementedException("Increment and Decrement not implemented");
            numberToInc = Integer.parseInt(((LiteralElement) right).getLiteral());
        }
        else if (left.isLiteral() && right instanceof Operand && dest.getName().equals(((Operand) right).getName()))
            numberToInc = Integer.parseInt(((LiteralElement) left).getLiteral());
        else
            return null;

        if(operation == OperationType.SUB)
            numberToInc = -numberToInc;
        else if(operation != OperationType.ADD)
            return null;

        int reg = currentMethod.getVarTable().get(dest.getName()).getVirtualReg();

        if(numberToInc >= -128 && numberToInc <= 127) {
            return "iinc " + reg + " " + Integer.toString(numberToInc) + NL;
        }
        return null;
    }


    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        increaseStackSize();
        int parsedInt;
        try {
            parsedInt = Integer.parseInt(literal.getLiteral());
        } catch (NumberFormatException e) {
            return "ldc " + literal.getLiteral() + NL;
        }

        if (parsedInt == -1)
            return "iconst_m1" + NL;
        else if (parsedInt >= 0 && parsedInt <= 5)
            return "iconst_" + literal.getLiteral() + NL;
        else if(parsedInt >= -128 && parsedInt <= 127)
            return "bipush " + literal.getLiteral() + NL;
        else if(parsedInt >= -32768 && parsedInt <= 32767)
            return "sipush " + literal.getLiteral() + NL;
        else
            return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        increaseStackSize();
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        if (operand instanceof ArrayOperand) {
            ArrayOperand arrayOperand = (ArrayOperand) operand;
            String code;
            if (reg <= 3 && reg >= 0)
                code = "aload_" + reg + NL;
            else
                code = "aload " + reg + NL;
            code += generators.apply(arrayOperand.getIndexOperands().get(0)) +
                    "iaload" + NL;
            increaseStackSize();
            stackSize -= 2;
            return code;
        } else if (operand.getType().getTypeOfElement() == ElementType.INT32 ||
                operand.getType().getTypeOfElement() == ElementType.BOOLEAN) {
            if (reg <= 3 && reg >= 0)
                return "iload_" + reg + NL;
            else
                return "iload " + reg + NL;
        } else {
            if (reg <= 3 && reg >= 0)
                return "aload_" + reg + NL;
            else
                return "aload " + reg + NL;
        }
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            case SUB -> "isub";
            case DIV -> "idiv";
            case AND -> "iand";
            case ANDB -> "iand";
            case OR -> "ior";
            case ORB -> "ior";
            case XOR -> "ixor";
            case SHL -> "ishl";
            case SHR -> "ishr";
            case SHRR -> "iushr";
            case LTH -> "isub" + NL +
                        "iflt lessBranch" + Integer.toString(++numLessThan) + NL +
                        "iconst_0" + NL +
                        "goto endLessBranch" + Integer.toString(numLessThan) + NL +
                        "lessBranch" + Integer.toString(numLessThan) + ":" + NL +
                        "iconst_1" + NL +
                        "endLessBranch" + Integer.toString(numLessThan) + ":";
            case GTH -> "isub" + NL +
                        "ifgt greaterBranch" + Integer.toString(++numLessThan) + NL +
                        "iconst_0" + NL +
                        "goto endGreaterBranch" + Integer.toString(numLessThan) + NL +
                        "greaterBranch" + Integer.toString(numLessThan) + ":" + NL +
                        "iconst_1" + NL +
                        "endGreaterBranch" + Integer.toString(numLessThan) + ":";
            case EQ -> "isub" + NL +
                       "ifeq equalBranch" + Integer.toString(++numLessThan) + NL +
                        "iconst_0" + NL +
                        "goto endEqualBranch" + Integer.toString(numLessThan) + NL +
                        "equalBranch" + Integer.toString(numLessThan) + ":" + NL +
                        "iconst_1" + NL +
                        "endEqualBranch" + Integer.toString(numLessThan) + ":";
            case NEQ -> "isub" + NL +
                        "ifne notEqualBranch" + Integer.toString(++numLessThan) + NL +
                        "iconst_0" + NL +
                        "goto endNotEqualBranch" + Integer.toString(numLessThan) + NL +
                        "notEqualBranch" + Integer.toString(numLessThan) + ":" + NL +
                       "iconst_1" + NL +
                        "endNotEqualBranch:";
            case LTE -> "isub" + NL +
                        "ifle lessEqualBranch" + Integer.toString(++numLessThan) + NL +
                        "iconst_0" + NL +
                        "goto endLessEqualBranch" + Integer.toString(numLessThan) + NL +
                        "lessEqualBranch" + Integer.toString(numLessThan) + ":" + NL +
                        "iconst_1" + NL +
                        "endLessEqualBranch" + Integer.toString(numLessThan) + ":";
            case GTE -> "isub" + NL +
                        "ifge greaterEqualBranch" + Integer.toString(++numLessThan) + NL +
                        "iconst_0" + NL +
                        "goto endGreaterEqualBranch" + Integer.toString(numLessThan) + NL +
                        "greaterEqualBranch" + Integer.toString(numLessThan) + ":" + NL +
                        "iconst_1" + NL +
                        "endGreaterEqualBranch" + Integer.toString(numLessThan) + ":";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        stackSize -= 2;
        increaseStackSize();

        return code.toString();
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOp) {
        var code = new StringBuilder();
        if(unaryOp.getOperation().getOpType() == OperationType.NOT ||
                unaryOp.getOperation().getOpType() == OperationType.NOTB) {
            code.append(generators.apply(unaryOp.getOperand()));
            code.append("iconst_1").append(NL);
            increaseStackSize();
            code.append("ixor").append(NL);
            stackSize--;
        }

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        if(returnInst.getOperand() == null)
            code.append("return").append(NL);
        else if(returnInst.getOperand().getType().getTypeOfElement() == ElementType.INT32 ||
                returnInst.getOperand().getType().getTypeOfElement() == ElementType.BOOLEAN) {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("ireturn").append(NL);
            stackSize--;
        } else {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("areturn").append(NL);
            stackSize--;
        }

        return code.toString();
    }

    private String generateCall(CallInstruction call) {
        var code = new StringBuilder();

        if (call.getInvocationType() == CallType.NEW) {
            if (call.getReturnType() instanceof ClassType) {
                ClassType castType = (ClassType) call.getReturnType();
                code.append("new ").append(getImportedClass(castType.getName()))
                        .append(NL);//.append("dup").append(NL);
                return code.toString();
            } else if (call.getReturnType() instanceof ArrayType) {
                code.append(generators.apply(call.getArguments().get(0)));
                code.append("newarray int").append(NL);
                stackSize--;
                return code.toString();
            } else
                new RuntimeException("Cannot instanciate new " + call.getReturnType().toString());
        }

        if(call.getInvocationType() == CallType.arraylength) {
            code.append(generators.apply(call.getCaller()));
            code.append("arraylength").append(NL);
            stackSize--;
            return code.toString();
        }

        int valToSubtractToStackSize = 0;
        // Append code to load caller object
        if(call.getInvocationType() != CallType.invokestatic) {
            code.append(generators.apply(call.getCaller()));
            valToSubtractToStackSize++;
        }

        String argList = "";

        // generate code for loading arguments
        for (var arg : call.getArguments()) {
            code.append(generators.apply(arg));
            argList += transformToJasminType((arg.getType()));
            valToSubtractToStackSize++;
        }

        if(call.getMethodNameTry().isEmpty())
            throw new RuntimeException("Call Method doesn't exist");

        if(!call.getMethodName().isLiteral())
            throw new RuntimeException("Call Method Name is not a literal");
        LiteralElement callMethodName = (LiteralElement) call.getMethodName();
        if(callMethodName.getType().getTypeOfElement() != ElementType.STRING)
            throw new RuntimeException("Call Method Name must be a STRING");

        String className;
        if(call.getInvocationType() == CallType.invokestatic)
            className = getImportedClass(((Operand) call.getCaller().toElement()).getName());
        else
            className = getImportedClass(((ClassType) call.getCaller().getType()).getName());

        String returnType = transformToJasminType(call.getReturnType());

        // generate code for calling method
        code.append(call.getInvocationType())
                .append(" ")
                .append(className)
                .append("/")
                .append(callMethodName.getLiteral().substring(1, callMethodName.getLiteral().length() - 1))
                .append("(" + argList + ")")
                .append(returnType)
                .append(NL);

        stackSize -= valToSubtractToStackSize;
        if (call.getReturnType().getTypeOfElement() != ElementType.VOID)
            increaseStackSize();

        return code.toString();
    }

    private String generateField(Field field) {
        String accessModifier = field.getFieldAccessModifier() != AccessModifier.DEFAULT ?
                field.getFieldAccessModifier().name().toLowerCase() + " " : " public ";
        String staticModifier = field.isStaticField() ? "static " : "";
        String finalModifier = field.isFinalField() ? "final " : "";
        String initialValue = field.isInitialized() ? " = " + field.getInitialValue() : "";
        return ".field " + accessModifier + staticModifier + finalModifier + field.getFieldName() + " " +
                transformToJasminType(field.getFieldType()) + initialValue + NL;
    }

    private String generatePutField(PutFieldInstruction putField) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(putField.getObject()));
        code.append(generators.apply(putField.getValue()));

        // store value in the stack in destination
        var field = putField.getField();

        code.append("putfield ")
                .append(currentClass.getClassName())
                .append("/")
                .append(field.getName())
                .append(" " + transformToJasminType(field.getType()))
                .append(NL);

        stackSize -= 2;

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(getFieldInstruction.getObject()));

        stackSize--;
        increaseStackSize();

        // store value in the stack in destination
        var field = getFieldInstruction.getField();

        code.append("getfield ")
                .append(currentClass.getClassName())
                .append("/")
                .append(field.getName())
                .append(" " + transformToJasminType(field.getType()))
                .append(NL);

        return code.toString();
    }

    private String generateCondBranch(CondBranchInstruction instruction) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(instruction.getCondition()));

        code.append("ifne ").append(instruction.getLabel()).append(NL);

        stackSize--;

        return code.toString();
    }

    private String generateGoto(GotoInstruction instruction) {
        return "goto " + instruction.getLabel() + NL;
    }
}
