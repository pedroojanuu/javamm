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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestMany {
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
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Error in file " + fileName);
            return;
        }

        // print the contents of the symbol table (e.g. imports, ...)
        var symbolTable = semanticsResult.getSymbolTable();
        System.out.println(symbolTable);

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

        // Print Jasmin code
//        System.out.println(jasminResult.getJasminCode());
    }

    public static void main(String[] args) {
        File[] fileList = new File("input/").listFiles();
        List<String> errors = new ArrayList<>();
        for (File file : fileList) {
            String fileName = file.getName();
            System.out.println("Testing file " + fileName);
            try{
                testFile("input/" + fileName);
            } catch (Exception e) {
                System.out.println("Error testing file " + fileName + ": " + e.getMessage());
                errors.add(fileName);
            }
            System.out.print("\n--------------------\n");
        }

        System.out.println("ERRORS:");
        for (String s : errors) System.out.println(s);
    }
}
