package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import javax.lang.model.type.NullType;
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
        var fields = buildFields(classDecl, className, superName);
        var returnTypes = buildReturnTypes(classDecl, className, superName);
        var params = buildParams(classDecl, className, superName);
        var locals = buildLocals(classDecl, className, superName);

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
    private static Map<String, Type> buildReturnTypes(JmmNode classDecl, String className, String superName) {
        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(Kind.OTHER_METHOD)
                .forEach(method -> map.put(method.get("name"), getType(method.getObject("methodType", JmmNode.class), className, superName)));

        classDecl.getChildren(Kind.MAIN_METHOD)
                .forEach(method -> map.put(method.get("name"), new Type(TypeUtils.getVoidTypeName(), false)));

        return map;
    }
    private static List<Symbol> buildFields(JmmNode classDecl, String className, String superName) {
        return buildVarDecls(classDecl, className, superName);
    }
    private static List<Symbol> buildVarDecls(JmmNode node, String className, String superName) {
        return node.getChildren(Kind.VAR_DECL).stream()
                .map(varDecl -> new Symbol(getType(varDecl.getObject("varType", JmmNode.class), className, superName), varDecl.get("name")))
                .toList();
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
    private static List<Symbol> getParamsList(JmmNode method, String className, String superName) {
        return method.getChildren(Kind.PARAM).stream()
                .map(param -> new Symbol(getType(param.getObject("paramType", JmmNode.class), className, superName), param.get("name")))
                .toList();
    }
    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl, String className, String superName) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(Kind.OTHER_METHOD)
                .forEach(method -> map.put(method.get("name"), getParamsList(method, className, superName)));

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
    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl, String className, String superName) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(Kind.METHOD_DECL)
                .forEach(method -> map.put(method.get("name"), getLocalsList(method, className, superName)));

        return map;
    }
    private static List<Symbol> getLocalsList(JmmNode methodDecl, String className, String superName) {
        return buildVarDecls(methodDecl, className, superName);
    }
}
