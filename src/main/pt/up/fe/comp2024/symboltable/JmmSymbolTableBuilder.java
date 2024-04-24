package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.comp2024.utils.ReportUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;


public class JmmSymbolTableBuilder {
    public JmmSymbolTableBuilder() {
        super();
    }
    public static JmmSymbolTable build(JmmNode root, List<Report> reports) {
        // reports are used to check for duplicate imports, methods, fields, and parameters

        var classDecl = root.getObject("classDeclaration", JmmNode.class);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        String superName = buildExtends(classDecl);

        var imports = buildImports(root, reports);
        var methods = buildMethods(classDecl, reports);
        var fields = buildFields(classDecl, className, superName, reports);
        var returnTypes = buildReturnTypes(classDecl, className, superName);
        var params = buildParams(classDecl, className, superName, reports);
        var locals = buildLocals(classDecl, className, superName, reports);

        return new JmmSymbolTable(className, superName, imports, methods, fields, returnTypes, params, locals);
    }
    private static List<String> buildImports(JmmNode root, List<Report> reports) {
        List<List<String>> importsList = root.getChildren(Kind.IMPORT_DECL).stream()
                .map(importDecl -> importDecl.getObjectAsList("id", String.class))
                .toList();
        List<String> imports = importsList.stream().map(importDeclList -> String.join(".", importDeclList)).toList();

        Set<String> importsSet = new HashSet<>(imports);   // checking for duplicate imports
        if (importsSet.size() < imports.size()) {
            reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, root, "Duplicate imports"));
        }
        Set<String> importClassesSet = new HashSet<>(importsList.stream().map(importDeclList -> importDeclList.get(importDeclList.size() - 1)).toList());   // checking for duplicate classes imported
        if (importClassesSet.size() < imports.size()) {
            reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, root, "Duplicate import classes"));
        }
        return imports;
    }
    private static String buildExtends(JmmNode classDecl) {
        String annotation = "extended_name";
        if (classDecl.hasAttribute(annotation)) {
            return classDecl.get(annotation);
        }
        return "";
    }
    private static Map<String, Type> buildReturnTypes(JmmNode classDecl, String className, String superName) {
        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(Kind.OTHER_METHOD)
                .forEach(method -> map.put(method.get("name"), getType(method.getObject("methodType", JmmNode.class), className, superName)));

        classDecl.getChildren(Kind.MAIN_METHOD)
                .forEach(method -> map.put(method.get("name"), new Type(TypeUtils.getVoidTypeName(), false)));

        return map;
    }
    private static List<Symbol> buildFields(JmmNode classDecl, String className, String superName, List<Report> reports) {
        return buildVarDecls(classDecl, className, superName, "fields", reports);
    }
    private static List<Symbol> buildVarDecls(JmmNode node, String className, String superName, String varDeclsType, List<Report> reports) {
        var varDecls = node.getChildren(Kind.VAR_DECL).stream()
                .map(varDecl -> new Symbol(getType(varDecl.getObject("varType", JmmNode.class), className, superName), varDecl.get("name")))
                .toList();
        HashSet<String> set = new HashSet<>(varDecls.stream().map(Symbol::getName).toList());
        if (set.size() < varDecls.size()) {
            reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, node, "Duplicate " + varDeclsType));
        }
        return varDecls;
    }
    private static Type getType(JmmNode node, String className, String superName) {
        Type type;
        switch (Kind.fromString(node.getKind())) {
            case INT_ARRAY -> {
                return new Type(TypeUtils.getIntTypeName(), true);
            }
            case VAR_ARGS -> {
                type = new Type(TypeUtils.getIntTypeName(), true);
                type.putObject("varArgs", true);
                return type;
            }
            case BOOLEAN -> {
                return new Type(TypeUtils.getBooleanTypeName(), false);
            }
            case INT -> {
                return new Type(TypeUtils.getIntTypeName(), false);
            }
            case OTHER_CLASSES -> {
                if (node.get("name").equals(className)) {
                    type = new Type(node.get("name"), false);
                    type.putObject("super", superName);
                    return type;
                }
                return new Type(node.get("name"), false);
            }
            default -> throw new RuntimeException("Unknown kind of varDecl: " + node.getKind());
        }
    }
    private static List<Symbol> getParamsList(JmmNode method, String className, String superName, List<Report> reports) {
        var params = method.getChildren(Kind.PARAM).stream()
                .map(param -> new Symbol(getType(param.getObject("paramType", JmmNode.class), className, superName), param.get("name")))
                .toList();
        HashSet<String> set = new HashSet<>(params.stream().map(Symbol::getName).toList());
        if (set.size() < params.size()) {
            reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, method, "Duplicate parameters"));
        }
        return params;
    }
    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl, String className, String superName, List<Report> reports) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(Kind.OTHER_METHOD)
                .forEach(method -> map.put(method.get("name"), getParamsList(method, className, superName, reports)));

        classDecl.getChildren(Kind.MAIN_METHOD)
                .forEach(method -> map.put(method.get("name"),
                        List.of(
                                new Symbol(
                                        new Type(TypeUtils.getStringTypeName(), true),
                                        method.get("parameterName")
                                )
                        )
                ));

        return map;
    }
    private static List<String> buildMethods(JmmNode classDecl, List<Report> reports) {
        TypeUtils.setMainIsStatic(false);

        List<JmmNode> mainMethodNodes = classDecl.getChildren(Kind.MAIN_METHOD);
        List<String> mainMethod = mainMethodNodes.stream()
                .map(method -> "main")
                .toList();
        if (!mainMethodNodes.isEmpty()) {
            JmmNode mainMethodNode = mainMethodNodes.get(0);
            TypeUtils.setMainIsStatic(mainMethodNode.getObject("isStatic", Boolean.class));
        }

        List<String> methods = classDecl.getChildren(Kind.OTHER_METHOD).stream()
                .map(method -> method.get("name"))
                .toList();
        if (mainMethod.size() > 1) {
            reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, classDecl, "Multiple main methods"));
        }
        HashSet<String> set = new HashSet<>(methods);
        if (set.size() < methods.size()) {
            reports.add(ReportUtils.buildErrorReport(Stage.SEMANTIC, classDecl, "Duplicate methods"));
        }

        return new ArrayList<>(mainMethod) {{
            addAll(methods);
        }};
    }
    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl, String className, String superName, List<Report> reports) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(Kind.METHOD_DECL)
                .forEach(method -> map.put(method.get("name"), getLocalsList(method, className, superName, reports)));

        return map;
    }
    private static List<Symbol> getLocalsList(JmmNode methodDecl, String className, String superName, List<Report> reports) {
        return buildVarDecls(methodDecl, className, superName, "local variables", reports);
    }
}
