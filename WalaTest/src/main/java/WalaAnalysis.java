import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.*;

import com.ibm.wala.core.util.config.AnalysisScopeReader;

import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;

import com.ibm.wala.ipa.cha.ClassHierarchyFactory;

import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;


import com.ibm.wala.util.WalaException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


import java.nio.file.Paths;
import java.util.*;

import java.util.regex.Pattern;



public class WalaAnalysis {

    public static Map<String, Map<String, Map<String, List<String>>>> analysisData = new HashMap<>();

    public static File outputFile;
    public static String pathToTests;
    public static File exclusionsFile;

    //usage: -t pathToTestsFolder(.class files) -e path_to_exclusions_file -o path_to_output_folder
    public static void main(String[] args) {
        //Paths.get("src/main/resources/TestCases").toString();

        parseCmd(args);

        WalaAnalysis myWalaAnalyzer = new WalaAnalysis();

        myWalaAnalyzer.MakeWalaAnalysis(pathToTests, exclusionsFile);

        writeJsonToFile(analysisData, outputFile.getAbsolutePath());

    }

    public static void parseCmd(String[] args) {

        for (int i = 0; i < args.length; i++) {

            if(args[i].equals("-t")) {
                pathToTests = Paths.get(args[i+1]).toString();
            }
            if(args[i].equals("-e")) {
                exclusionsFile = new File(args[i+1]);
            }
            if(args[i].equals("-o")) {
                outputFile = new File(args[i+1]);
            }
        }

    }

    public void MakeWalaAnalysis(String javaClassPath, File exclusionsFile) {
        try {

            TestSuiteReader testSuiteReader = new TestSuiteReader();
            List<String> testSuitePackages = testSuiteReader.GetTestSuitePaths(javaClassPath);

            for (String packagePath : testSuitePackages) {
                List<String> classPaths = testSuiteReader.GetFullPathClasses(packagePath);
                for (String classPath : classPaths) {

                    String className = Paths.get(classPath).getFileName().toString().split(Pattern.quote("."))[0];
                    System.out.println("\n==================== "
                            + className + " ====================\n");
                    analysisData.put(className, new HashMap<>());
                    //writeListToFile(new ArrayList<>(Collections.singletonList("TestClass: " + className)), outputFile.getAbsolutePath());

                    AnalysisScope scope1 = AnalysisScopeReader.instance.makeJavaBinaryAnalysisScope(classPath, exclusionsFile);
                    ClassHierarchy cha = ClassHierarchyFactory.make(scope1);


                    Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(cha);
                    AnalysisOptions options = new AnalysisOptions(scope1, entrypoints);

                    IAnalysisCacheView cacheView = new AnalysisCacheImpl();
                    CallGraphBuilder<?> builder = Util.makeZeroOneCFABuilder(Language.JAVA, options, cacheView, cha);
                    CallGraph cg = builder.makeCallGraph(options, null);

                    System.out.println("========================================IR========================================");
                    cg.forEach(node -> {
                        if (node.getMethod().toString().contains("Application")) {
                            IMethod method = node.getMethod();
                            analysisData.get(className).put(method.getName().toString(),new HashMap<>());
                            System.out.println("IR for method: " + method);
                            IR ir = node.getIR();
                            if (ir != null) {
                                System.out.println(ir);
                            } else {
                                System.out.println("No IR available for method.");
                            }
                            //Pointer analysis part
                            try {
                                makePointerAnalysis(builder, method, className);
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                    });

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makePointerAnalysis(CallGraphBuilder<?> builder, IMethod method, String className) throws WalaException {

        String methodName = method.getName().toString();
        PointerAnalysis<?> pa = builder.getPointerAnalysis();
        //retrieve HeapGraph
        HeapGraph<?> heapGraph = pa.getHeapGraph();

        for (Object node : heapGraph) {

            if (node instanceof LocalPointerKey) {

                LocalPointerKey key = (LocalPointerKey) node;

                if (key.getNode().getMethod().getName().toString().contains(methodName)) {

                    SymbolTable symbolTable = key.getNode().getIR().getSymbolTable();

                    System.out.println("Communication occurances: ");

                    key.getNode().getIR().iterateAllInstructions().forEachRemaining(instr -> {
                        if (instr == null || instr.iIndex() < 0) return;

                        int sourceLineNumber = key.getNode().getMethod().getLineNumber(instr.iIndex());

                        if (instr.toString().contains("Application")) {

                            //we want to filter everything which is connected to Value class
                            if (instr instanceof SSAInvokeInstruction invokeInstr) {
                                if (invokeInstr.getDeclaredTarget().getReturnType().getName().toString().contains("Value") ||
                                    invokeInstr.getDeclaredTarget().getDeclaringClass().getName().toString().contains("Value")) {
                                    String instruction = "  Instruction: " + instr + " (line: " + sourceLineNumber + ")";
                                    System.out.println(instruction);
                                    analysisData.get(className).get(methodName).put(instruction, new ArrayList<>());
                                    for (int i = 0; i < invokeInstr.getNumberOfUses(); i++) {
                                        int useVar = invokeInstr.getUse(i);
                                        if (symbolTable.isConstant(useVar)) {
                                            String constant = "    Use: Constant value: " + symbolTable.getConstantValue(useVar);
                                            System.out.println(constant);
                                            analysisData.get(className).get(methodName).get(instruction).add(constant);
                                        } else {
                                            PointerKey pointerKey = pa.getHeapModel().getPointerKeyForLocal(key.getNode(), useVar);
                                            if (pointerKey != null) {
                                                String ssa = "    Use: SSA Variable " + useVar + ", PointerKey: " + pointerKey;
                                                System.out.println(ssa);
                                                analysisData.get(className).get(methodName).get(instruction).add(ssa);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });

                    break;
                }

            }

        }

    }


    public static void writeJsonToFile(Map<String, Map<String, Map<String, List<String>>>> data, String filePath) {
        JSONObject jsonObject = new JSONObject(data);

        try (FileWriter file = new FileWriter(filePath)) {
            file.write(jsonObject.toString(4));
            System.out.println("JSON is written to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

