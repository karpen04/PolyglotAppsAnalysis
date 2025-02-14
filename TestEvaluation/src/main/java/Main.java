
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import java.util.List;


public class Main {

    public static List<File> jsonFiles = new ArrayList<>();
    public static String outputFolderPath = "";
    public static String testCasesJsonPath = "";
    public static String methodsLookupPath = "";

    //usage: -j json_path1,json_path2... -i test_cases_json_path -l lookup_methods_file_path -o output_folder_path
    public static void main(String[] args) {

        parseCmd(args);

        File methodsLookupFile = new File(methodsLookupPath);
        File testCasesJson = new File(testCasesJsonPath);
        File analysisOutputFile = new File(outputFolderPath + "/analysisOut.txt");
        File analysisTotalCoverageFile = new File(outputFolderPath + "/analysisCoverage.txt");
        File analysisCoveraegePerClassFile = new File(outputFolderPath + "/analysisPerClass.csv");


        AnalysisEvaluation analysisEvaluation = new AnalysisEvaluation("Tai-E", "Qilin", "WALA");

        JSONObject testCases = analysisEvaluation.readTestCases(testCasesJson);

        List<JSONObject> jsonObjects = analysisEvaluation.gatherOutputs(jsonFiles);

        List<String> lookupMethods = analysisEvaluation.readLookupMethods(methodsLookupFile);

        analysisEvaluation.doAnalysis(jsonObjects, lookupMethods, testCases);

        analysisEvaluation.writeAnalysisFile(analysisOutputFile);

        analysisEvaluation.getStats().writeTotalCoverageToFile(analysisTotalCoverageFile);

        analysisEvaluation.getStats().writeCoveragePerTestToCsv(analysisCoveraegePerClassFile);
    }

    public static void parseCmd(String[] args) {

        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-j")) {
                String[] jsonArr = args[i+1].trim()
                        .replaceAll("\\s","").split(",");
                for (int j = 0; j < jsonArr.length; j++) {
                    jsonFiles.add(new File(jsonArr[j]));
                }
            }

            if (args[i].equals("-o")) {
                outputFolderPath = args[i+1];
            }

            if (args[i].equals("-l")) {
                methodsLookupPath = args[i+1];
            }

            if (args[i].equals("-i")) {
                testCasesJsonPath = args[i+1];
            }
        }
    }
}
