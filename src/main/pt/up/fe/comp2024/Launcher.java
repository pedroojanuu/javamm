package pt.up.fe.comp2024;

import org.antlr.v4.gui.TreeViewer;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2024.analysis.JmmAnalysisImpl;
import pt.up.fe.comp2024.backend.JasminBackendImpl;
import pt.up.fe.comp2024.optimization.JmmOptimizationImpl;
import pt.up.fe.comp2024.parser.JmmParserImpl;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Launcher {
    private static void testFile(String fileName) {
        var config = CompilerConfig.getDefault();
        if (!CompilerConfig.putFile(config, fileName)) {
            throw new RuntimeException("File name " + fileName + " is not a file");
        }
        var inputFile = CompilerConfig.getInputFile(config).orElseThrow();
        if (!inputFile.isFile()) {
            throw new RuntimeException("File name " + fileName + " is not a file");
        }
        String code = SpecsIo.read(inputFile);

        // Parsing stage
        JmmParserImpl parser = new JmmParserImpl();
        JmmParserResult parserResult = parser.parse(code, config);
        TestUtils.noErrors(parserResult.getReports());

        // Semantic Analysis stage
        JmmAnalysisImpl sema = new JmmAnalysisImpl();
        JmmSemanticsResult semanticsResult = sema.semanticAnalysis(parserResult);
        try {
            if (fileName.contains("error")) {
                TestUtils.mustFail(semanticsResult.getReports());
            } else {
                TestUtils.noErrors(semanticsResult.getReports());
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Error in file " + fileName);
            throw e;
        }

        // print the contents of the symbol table (e.g. imports, ...)
        var symbolTable = semanticsResult.getSymbolTable();
        System.out.println(symbolTable);

        /*
        // Optimization stage
        JmmOptimizationImpl ollirGen = new JmmOptimizationImpl();
        OllirResult ollirResult = ollirGen.toOllir(semanticsResult);
        TestUtils.noErrors(ollirResult.getReports());

        // Print OLLIR code
        //System.out.println(ollirResult.getOllirCode());

        // Code generation stage
        JasminBackendImpl jasminGen = new JasminBackendImpl();
        JasminResult jasminResult = jasminGen.toJasmin(ollirResult);
        TestUtils.noErrors(jasminResult.getReports());

        // Print Jasmin code
        //System.out.println(jasminResult.getJasminCode());
        */
    }
    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        Map<String, String> config = CompilerConfig.parseArgs(args);

        var inputFile = CompilerConfig.getInputFile(config).orElseThrow();
        if (!inputFile.isFile()) {
            throw new RuntimeException("Option '-i' expects a path to an existing input file, got '" + args[0] + "'.");
        }
        String code = SpecsIo.read(inputFile);

        // Parsing stage
        JmmParserImpl parser = new JmmParserImpl();
        JmmParserResult parserResult = parser.parse(code, config);
        TestUtils.noErrors(parserResult.getReports());

        // Print AST
        System.out.println(parserResult.getRootNode().toTree());

        // Semantic Analysis stage
        JmmAnalysisImpl sema = new JmmAnalysisImpl();
        JmmSemanticsResult semanticsResult = sema.semanticAnalysis(parserResult);
        System.out.println(semanticsResult.getReports());
        TestUtils.noErrors(semanticsResult.getReports());

        // print the contents of the symbol table (e.g. imports, ...)
        var symbolTable = semanticsResult.getSymbolTable();
        System.out.println(symbolTable);

        boolean testAll = true;
        if (testAll) {
        List<String> files = Arrays.asList("bool_exprs.java", "different_expressions.java", "import_method.java", "error_imported_class_does_not_extend_mine.java", "input.java", "input2.java", "input3.java", "method_call_from_import.java", "simple.java", "error_my_class_does_not_extend_import.java", "error_this_wrong.java", "error_unknown_field.java", "varargs_array_argument.java", "error_varargs_method_var_decl.java", "error_varargs_return.java", "import_complex.java", "error_varargs_field.java", "varargs_complex.java", "dijkstra.java", "merge_sort.java", "binary_search.java", "bubble_sort.java", "quicksort.java", "error_duplicated_fields.java", "error_duplicated_methods.java", "error_duplicated_imports.java", "error_duplicated_imported_classes.java", "inherited_method.java", "main_not_static.java", "error_field_in_static.java", "error_assignment_array.java", "error_if_array_index.java", "error_field_access.java", "ollir/chained_function_calls.java", "ollir/complex_arguments.java", "ollir/structure_fields.java", "ollir/this_return.java", "ollir/callhell.java", "ollir/return_this_and_chain.java", "ollir/another_call_hell.java", "ollir/method_calls_from_extends_type.java", "ollir/method_from_import_present_in_class.java", "error_access_length_on_this.java", "ollir/crazy_function_calls.java", "ollir/assign_fields.java");
            for (String file : files) {
                System.out.println(file);
                testFile("input/" + file);
            }
        }

        // Optimization stage
        JmmOptimizationImpl ollirGen = new JmmOptimizationImpl();
        OllirResult ollirResult = ollirGen.toOllir(semanticsResult);
        TestUtils.noErrors(ollirResult.getReports());

        // Print OLLIR code
        System.out.println(ollirResult.getOllirCode());

        // Code generation stage
//        JasminBackendImpl jasminGen = new JasminBackendImpl();
//        JasminResult jasminResult = jasminGen.toJasmin(ollirResult);
//        TestUtils.noErrors(jasminResult.getReports());
//
//        // Print Jasmin code
//        System.out.println(jasminResult.getJasminCode());
        // jasminResult.run();
    }

}
