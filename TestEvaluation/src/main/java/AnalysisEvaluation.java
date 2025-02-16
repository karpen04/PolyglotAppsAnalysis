import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import stats.Statistics;

import java.io.*;
import java.nio.file.Files;
import java.util.*;


public class AnalysisEvaluation {

    private Statistics stats;

    private List<String> analysisLog;

    private List<String> satNames;

    public AnalysisEvaluation(String... satNames) {

        analysisLog = new ArrayList<>();
        this.stats = new Statistics(satNames);

    }

    // Load all JSON files and return them as a list of JSONObject
    public List<JSONObject> gatherOutputs(List<File> files) {
        List<JSONObject> jsonObjects = new ArrayList<>();
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                jsonObjects.add(new JSONObject(new JSONTokener(reader)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jsonObjects;
    }


    public List<String> readLookupMethods(File file) {
        try {

            return Files.readAllLines(file.toPath());

        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public JSONObject readTestCases(File testCasesJson) {
        JSONObject result = null;
        try (FileReader reader = new FileReader(testCasesJson)) {
            result = new JSONObject(new JSONTokener(reader));
        } catch (Exception e) { e.printStackTrace(); }

        return result;
    }

    // Analyze source code against lookup methods and JSON reports
    public void doAnalysis(List<JSONObject> jsons, List<String> lookupMethods, JSONObject testCases) {
        for (String className : testCases.keySet()) {
            System.out.println("\nClass name: " + className);
            stats.startNewClass(className);
            analysisLog.add("\nClass name: " + className);
            JSONObject methods = testCases.getJSONObject(className);
            for (String methodName : methods.keySet()) {
                System.out.println("\nMethod name: " + methodName);
                analysisLog.add("\nMethod name: " + methodName);
                JSONArray methodLines = methods.getJSONArray(methodName);
                for (int i = 0; i < methodLines.length(); i++) {
                    String line = methodLines.getString(i);
                    for (String lookupMethod : lookupMethods) {
                        if (line.contains(lookupMethod)) {
                            System.out.println("\nMethod invocation: " + lookupMethod);
                            analysisLog.add("\nMethod invocation: " + lookupMethod);
                            stats.incrementTotalInvocations();
                            stats.incrementTotalInvocationsByClass(className);
                            matchWithJsons(className, methodName, lookupMethod, jsons);
                        }
                    }
                }
            }
        }

        stats.printAllStatistics();
    }

    // Helper method to match source code lines with JSON structures
    private void matchWithJsons(String className, String methodName, String lookupMethod, List<JSONObject> jsons) {
        for (JSONObject json : jsons) {
            String satName = getSATName(json, jsons);
            System.out.println("=====" + satName + "=====");
            analysisLog.add("=====" + satName + "=====");
            if (json.has(className)) {
                JSONObject classJson = json.getJSONObject(className);
                if (classJson.has(methodName)) {
                    Object methodData = classJson.get(methodName);
                    boolean marked = false;
                    if (methodData instanceof JSONObject) {
                        for (String instruction :((JSONObject) methodData).keySet()) {
                            if (instruction.contains(lookupMethod)) {
                                System.out.println("Instruction: " + instruction
                                        + "\n" + ((JSONObject) methodData).get(instruction).toString());
                                analysisLog.add("Instruction: " + instruction
                                        + "\n" + ((JSONObject) methodData).get(instruction).toString());
                                if (!marked) {
                                    stats.incrementTotalCoveredInvocationsBySat(satName);
                                    stats.incrementInvocationByClassBySat(className, satName);
                                    marked = true;
                                }
                            }

                        }
                    } else if (methodData instanceof JSONArray) {
                        for (Object instruction :((JSONArray) methodData)) {
                            if (instruction.toString().contains(lookupMethod)) {
                                System.out.println(instruction);
                                analysisLog.add(instruction.toString());
                                if (!marked) {
                                    stats.incrementTotalCoveredInvocationsBySat(satName);
                                    stats.incrementInvocationByClassBySat(className, satName);
                                    marked = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String getSATName(JSONObject json, List<JSONObject> jsons) {

        String result = null;

        switch (jsons.indexOf(json)) {

            case 0:
                result = "Tai-E";
                break;
            case 1:
                result = "Qilin";
                break;
            case 2:
                result = "WALA";
                break;

        }

        return result;
    }

    public void writeAnalysisFile(File filePath) {

        try (FileWriter writer = new FileWriter(filePath)) {

            for (String line : analysisLog) {
                writer.write(line + System.lineSeparator());
            }
            System.out.println("File written successfully: " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Statistics getStats() {
        return stats;
    }
}
