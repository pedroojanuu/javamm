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

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;
    ClassUnit currentClass;
    HashMap<String, String> importTable;

    private final FunctionClassMap<TreeNode, String> generators;

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
            if(true) throw new RuntimeException(code);
        }

        return code;
    }

    private String generateClassUnit(ClassUnit classUnit) {

        currentClass = classUnit;
        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        // TODO: Hardcoded to Object, needs to be expanded
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

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var staticModifier = method.isStaticMethod() ? "static " : "";

        var methodName = method.getMethodName();

        var methodType = transformToJasminType(method.getReturnType());

        var methodParams = method.getParams();

        String paramList = "";

        if(methodParams.size() != 0) {
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

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        if(currentMethod == null)
            throw new RuntimeException("Method not set");
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        if(assign.getTypeOfAssign().getTypeOfElement() == ElementType.INT32 ||
                assign.getTypeOfAssign().getTypeOfElement() == ElementType.BOOLEAN)
            code.append("istore ").append(reg).append(NL);
        else
            code.append("astore ").append(reg).append(NL);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        if(operand.getType().getTypeOfElement() == ElementType.INT32 ||
                operand.getType().getTypeOfElement() == ElementType.BOOLEAN)
            return "iload " + reg + NL;
        else
            return "aload " + reg + NL;
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
            case LTH -> "isub" + NL + "iflt lessBranch" + NL + "iconst_0" + NL + "goto endLessBranch" + NL + "lessBranch:" + NL + "iconst_1" + NL + "endLessBranch:";
            case GTH -> "isub" + NL + "ifgt greaterBranch" + NL + "iconst_0" + NL + "goto endGreaterBranch" + NL + "greaterBranch:" + NL + "iconst_1" + NL + "endGreaterBranch:";
            case EQ -> "isub" + NL + "ifeq equalBranch" + NL + "iconst_0" + NL + "goto endEqualBranch" + NL + "equalBranch:" + NL + "iconst_1" + NL + "endEqualBranch:";
            case NEQ -> "isub" + NL + "ifne notEqualBranch" + NL + "iconst_0" + NL + "goto endNotEqualBranch" + NL + "notEqualBranch:" + NL + "iconst_1" + NL + "endNotEqualBranch:";
            case LTE -> "isub" + NL + "ifle lessEqualBranch" + NL + "iconst_0" + NL + "goto endLessEqualBranch" + NL + "lessEqualBranch:" + NL + "iconst_1" + NL + "endLessEqualBranch:";
            case GTE -> "isub" + NL + "ifge greaterEqualBranch" + NL + "iconst_0" + NL + "goto endGreaterEqualBranch" + NL + "greaterEqualBranch:" + NL + "iconst_1" + NL + "endGreaterEqualBranch:";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOp) {
        var code = new StringBuilder();
        if(unaryOp.getOperation().getOpType() == OperationType.NOT ||
                unaryOp.getOperation().getOpType() == OperationType.NOTB) {
            code.append(generators.apply(unaryOp.getOperand()));
            code.append("ineg").append(NL);
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
        } else {
            code.append(generators.apply(returnInst.getOperand()));
            code.append("areturn").append(NL);
        }

        return code.toString();
    }

    private String generateCall(CallInstruction call) {
        var code = new StringBuilder();

        if (call.getInvocationType() == CallType.NEW) {
            ClassType castType = (ClassType) call.getReturnType();
            return "new " + getImportedClass(castType.getName()) + NL +
                    "dup" + NL;
        }

        // Append code to load caller object
        if(call.getInvocationType() != CallType.invokestatic)
            code.append(generators.apply(call.getCaller()));

        String argList = "";

        // generate code for loading arguments
        for (var arg : call.getArguments()) {
            code.append(generators.apply(arg));
            argList += transformToJasminType((arg.getType()));
        }

        if(call.getMethodNameTry().isEmpty())
            throw new RuntimeException("Call Method doesn't exist");

        String callType = transformToJasminType(call.getReturnType());

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

        // generate code for calling method
        code.append(call.getInvocationType())
                .append(" ")
                .append(className)
                .append("/")
                .append(callMethodName.getLiteral().substring(1, callMethodName.getLiteral().length() - 1))
                .append("(" + argList + ")")
                .append(callType)
                .append(NL);

        return code.toString();
    }

    private String generateField(Field field) {
        String staticModifier = field.isStaticField() ? "static " : "";
        String finalModifier = field.isFinalField() ? "final " : "";
        String initialValue = field.isInitialized() ? " = " + field.getInitialValue() : "";
        return ".field " + field.getFieldName() + " " + staticModifier + finalModifier +
                transformToJasminType(field.getFieldType()) + initialValue + NL;
    }

    private String generatePutField(PutFieldInstruction putField) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(putField.getObject()));
        code.append(generators.apply(putField.getValue()));

        // store value in the stack in destination
        var field = putField.getField();

        generators.apply(putField.getValue());

        code.append("putfield ")
                .append(currentClass.getClassName())
                .append("/")
                .append(field.getName())
                .append(" " + transformToJasminType(field.getType()))
                .append(NL);

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getFieldInstruction) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(getFieldInstruction.getObject()));

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
}