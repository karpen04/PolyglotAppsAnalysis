package stats;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.*;
import java.io.FileWriter;
public class Statistics {

    private static Integer totalInvocations = 0;
    private static Map<String, Integer> totalInvocationsInClass = new HashMap<>();
    private static Map<String, Map<String, Double>> coveragePerClass = new HashMap<>();
    private static Map<String, Integer> totalCoveredInvocations = new HashMap<>();

    private List<String> satNames;

    public Statistics(String... satNames) {

        this.satNames = new ArrayList<>();

        for (String satName : satNames) {

            this.satNames.add(satName);
            totalCoveredInvocations.put(satName, 0);

        }

    }

    private Map<String, Double> calculateTotalCoverage() {

        Map<String, Double> result = new HashMap<>();

            for (Map.Entry<String, Integer> satTotalInvoc : totalCoveredInvocations.entrySet()) {

                double res = satTotalInvoc.getValue() / (double) totalInvocations;
                if(Double.isNaN(res))
                    res = 0;
                result.put(satTotalInvoc.getKey(), res);
            }

            return result;
    }

    public void incrementTotalInvocationsByClass(String className) {

        totalInvocationsInClass.put(className, totalInvocationsInClass.get(className) + 1);

    }

    public void incrementTotalInvocations() {

        totalInvocations++;

    }

    public void incrementTotalCoveredInvocationsBySat(String satName) {

        totalCoveredInvocations.put(satName, totalCoveredInvocations.get(satName) + 1);

    }

    public void incrementInvocationByClassBySat(String className, String satName) {

        coveragePerClass.get(className).put(satName, coveragePerClass.get(className).get(satName) + 1.0);

    }

    public void startNewClass(String className) {

        totalInvocationsInClass.put(className, 0);
        coveragePerClass.put(className, new HashMap<>());
        satNames.forEach(satName -> coveragePerClass.get(className).put(satName, 0.0));

    }

    private void calculateCoverage() {

        for (Map.Entry<String, Map<String, Double>> classEntry : coveragePerClass.entrySet()) {
            int totalClassInvocations = totalInvocationsInClass.get(classEntry.getKey());
            for (Map.Entry<String, Double> satEntry : classEntry.getValue().entrySet()) {
                double res = satEntry.getValue() / (double)totalClassInvocations;
                if (Double.isNaN(res))
                    res = 0;
                satEntry.setValue(res);
            }
        }
    }

    public void printAllStatistics() {

        calculateCoverage();

        System.out.println("\n=============STATS=============");

        System.out.println("Total coverage by SAT: ");
        for (Map.Entry<String, Double> satEntry : calculateTotalCoverage().entrySet()) {
            System.out.println("\t" + satEntry.getKey() + ": " +
                    BigDecimal.valueOf(satEntry.getValue() * 100).setScale(2, RoundingMode.HALF_UP)  + " %");
        }

        System.out.println("\nCoverage for each test case for each sat: ");
        for (Map.Entry<String, Map<String, Double>> classEntry : coveragePerClass.entrySet()) {
            System.out.println("Test Case Name: " + classEntry.getKey());
            for (Map.Entry<String, Double> satEntry : classEntry.getValue().entrySet()) {
                System.out.println("\t" + satEntry.getKey() + ": " +
                        BigDecimal.valueOf(satEntry.getValue() * 100).setScale(2, RoundingMode.HALF_UP) + " %");
            }
        }
    }

    public void writeTotalCoverageToFile(File filePath) {

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("Total coverage by SAT: " + System.lineSeparator());
            for (Map.Entry<String, Double> satEntry : calculateTotalCoverage().entrySet()) {
                writer.write("\t" + satEntry.getKey() + ": " +
                        BigDecimal.valueOf(satEntry.getValue() * 100).setScale(2, RoundingMode.HALF_UP)  + " %"
                + System.lineSeparator());
            }
            System.out.println("File written successfully: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeCoveragePerTestToCsv(File filePath) {

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Test Case,");
            for (Map.Entry<String, Map<String, Double>> classEntry : coveragePerClass.entrySet()) {
                int counter = 0;
                for (Map.Entry<String, Double> satEntry : classEntry.getValue().entrySet()) {
                        writer.append(satEntry.getKey());
                        counter++;
                        if (classEntry.getValue().size() == counter)
                            writer.append(System.lineSeparator());
                        else writer.append(",");

                }
                break;
            }
            for (Map.Entry<String, Map<String, Double>> classEntry : coveragePerClass.entrySet()) {
                writer.append(classEntry.getKey()).append(",");
                int counter = 0;
                for (Map.Entry<String, Double> satEntry : classEntry.getValue().entrySet()) {
                    writer.append(BigDecimal.valueOf(satEntry.getValue() * 100)
                                    .setScale(2, RoundingMode.HALF_UP).toString())
                            .append("%");
                    counter++;
                    if (counter == classEntry.getValue().entrySet().size())
                        writer.append(System.lineSeparator());
                    else writer.append(",");
                }

            }
            System.out.println("File written successfully: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
