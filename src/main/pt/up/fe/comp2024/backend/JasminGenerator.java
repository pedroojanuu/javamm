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

//Notas falta - chamar metodos doutras classes, extends, constructors, True and false, operators, objRef&Arrays

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
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(Field.class, this::generateField);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
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
//        String superClass = classUnit.getSuperClass() == null ? "java/lang/Object" : classUnit.getSuperClass();
        String superClass = "java/lang/Object";
        code.append(".super ").append(superClass).append(NL).append(NL);

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

        // generate code for all other methodst/up/fe/comp/cp2/jasmin/
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
            return "L" + castType.getName() + ";";
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
        // Falta Boolean Assign
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
        // Falta Muitos Operadores
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            case SUB -> "isub";
            case DIV -> "idiv";
            case AND -> "iand";
            case OR -> "ior";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

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

        String argList = "";

        // generate code for loading arguments
        for (var arg : call.getArguments()) {
            code.append(generators.apply(arg));
            argList += transformToJasminType((arg.getType()));
        }

        if (call.getInvocationType() == CallType.NEW) {
            ClassType castType = (ClassType) call.getReturnType();
            return "new " + castType.getName() + NL +
                    "dup" + NL;
        }

        if(call.getMethodNameTry().isEmpty())
            throw new RuntimeException("Call Method doesn't exist");

        String callType = transformToJasminType(call.getReturnType());

        if(!call.getMethodName().isLiteral())
            throw new RuntimeException("Call Method Name is not a literal");
        LiteralElement callMethodName = (LiteralElement) call.getMethodName();
        if(callMethodName.getType().getTypeOfElement() != ElementType.STRING)
            throw new RuntimeException("Call Method Name must be a STRING");

        ClassType callerType = (ClassType) call.getCaller().getType();

        // generate code for calling method
        code.append(call.getInvocationType())
                .append(" ")
                .append(callerType.getName())
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
