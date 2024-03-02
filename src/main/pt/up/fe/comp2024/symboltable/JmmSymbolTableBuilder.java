package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;


public class JmmSymbolTableBuilder {
    public JmmSymbolTableBuilder() {
        super();
    }
    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getObject("classDeclaration", JmmNode.class);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        String superName = buildExtends(classDecl);

        var imports = buildImports(root);
        var methods = buildMethods(classDecl);
        var fields = buildFields(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, superName, imports, methods, fields, returnTypes, params, locals);
    }
    private static List<String> buildImports(JmmNode root) {
        return root.getChildren(Kind.IMPORT_DECL).stream()
                .map(importDecl -> String.join(".", importDecl.getObjectAsList("id", String.class))).toList();
    }
    private static String buildExtends(JmmNode classDecl) {
        String annotation = "extended_name";
        if (classDecl.hasAttribute(annotation)) {
            return classDecl.get(annotation);
        }
        return "";
    }
    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(Kind.OTHER_METHOD)
                .forEach(method -> map.put(method.get("name"), getType(method.getObject("methodType", JmmNode.class))));

        classDecl.getChildren(Kind.MAIN_METHOD)
                .forEach(method -> map.put(method.get("name"), new Type(TypeUtils.getVoidTypeName(), false)));

        return map;
    }
    private static List<Symbol> buildFields(JmmNode classDecl) {
        return buildVarDecls(classDecl);
    }
    private static List<Symbol> buildVarDecls(JmmNode node) {
        return node.getChildren(Kind.VAR_DECL).stream()
                .map(varDecl -> new Symbol(getType(varDecl.getObject("varType", JmmNode.class)), varDecl.get("name")))
                .toList();
    }
    private static Type getType(JmmNode type) {
        switch (Kind.fromString(type.getKind())) {
            case INT_ARRAY, VAR_ARGS -> {
                return new Type(TypeUtils.getIntTypeName(), true);
            }
            case BOOLEAN -> {
                return new Type(TypeUtils.getBooleanTypeName(), false);
            }
            case INT -> {
                return new Type(TypeUtils.getIntTypeName(), false);
            }
            case OTHER_CLASSES -> {
                return new Type(type.get("name"), false);
            }
            default -> throw new RuntimeException("Unknown kind of varDecl: " + type.getKind());
        }
    }
    private static List<Symbol> getParamsList(JmmNode method) {
        return method.getChildren(Kind.PARAM).stream()
                .map(param -> new Symbol(getType(param.getObject("paramType", JmmNode.class)), param.get("name")))
                .toList();
    }
    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(Kind.OTHER_METHOD)
                .forEach(method -> map.put(method.get("name"), getParamsList(method)));

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
    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> mainMethod = classDecl.getChildren(Kind.MAIN_METHOD).stream()
                .map(method -> "main")
                .toList();
        List<String> methods = classDecl.getChildren(Kind.OTHER_METHOD).stream()
                .map(method -> method.get("name"))
                .toList();
        return new ArrayList<>(mainMethod) {{
            addAll(methods);
        }};
    }
    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(Kind.METHOD_DECL)
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }
    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        return buildVarDecls(methodDecl);
    }
}
