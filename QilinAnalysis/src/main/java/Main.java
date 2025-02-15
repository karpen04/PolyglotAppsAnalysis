

import org.json.JSONObject;
import qilin.core.PTA;
import qilin.core.pag.ContextVarNode;
import qilin.core.pag.LocalVarNode;
import qilin.core.pag.VarNode;
import soot.Context;
import soot.SootMethod;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Main {

    public static File tempJsonFolder;
    public static File outputFile;
    public static String packageName = "";
    public static File pathToJar;
    public static File pathToCompiledClasses;
    public static File javaJRE;


    //Usage: -tj path_to_temp_json_folder -cc path_to_compiled_classes(without package) -p packageName -jr path_to_jar -jre path_to_java_jre_folder -o path_to_output_file
    public static void main(String[] args) {

        parseCmd(args);

        Main main = new Main();

        try {

            main.makeQilinAnalysis(pathToCompiledClasses.getAbsolutePath());

            mergeJsonFiles(tempJsonFolder.getAbsolutePath(), outputFile.getAbsolutePath());

        }catch (Exception exception){exception.printStackTrace();}


    }

    public static void parseCmd(String[] args) {

        for (int i = 0; i < args.length; i++) {

            if(args[i].equals("-tj")) {
                tempJsonFolder = new File(args[i+1]);
            }
            if(args[i].equals("-p")) {
                packageName = Paths.get(args[i+1]) + ".";
            }
            if(args[i].equals("-o")) {
                outputFile = new File(args[i+1]);
            }
            if(args[i].equals("-jr")) {
                pathToJar = new File(args[i+1]);
            }
            if(args[i].equals("-cc")) {
                pathToCompiledClasses = new File(args[i+1]);
            }
            if(args[i].equals("-jre")) {
                javaJRE = new File(args[i+1]);
            }
        }

    }

    public void makeQilinAnalysis(String pathToClasses) throws Exception {

        TestSuiteReader testSuiteReader = new TestSuiteReader();
        List<String> testSuitePackages = testSuiteReader.GetTestSuitePaths(pathToClasses);

        for (String packagePath : testSuitePackages) {
            List<String> classPaths = testSuiteReader.GetFullPathClasses(packagePath);
            for (String classPath : classPaths) {

                String className = Paths.get(classPath).getFileName().toString().
                        split(Pattern.quote("."))[0];

                System.out.println("\n==================== "
                        + className + " ====================\n");

                ProcessBuilder processBuilder = new ProcessBuilder(
                        "java", "-cp", System.getProperty("java.class.path"), "AnalysisEntrypoint",
                        packageName, className, pathToJar.getAbsolutePath(), javaJRE.getAbsolutePath(), tempJsonFolder.getAbsolutePath()
                );

                processBuilder.inheritIO();
                Process process = processBuilder.start();
                boolean finished = process.waitFor(6000, TimeUnit.SECONDS);
                if (!finished) {
                    System.err.println("Destroying process...");
                    process.destroy();
                }
            }
        }

        mergeJsonFiles(tempJsonFolder.getAbsolutePath(), outputFile.getAbsolutePath());
    }




    public static void mergeJsonFiles(String folderPath, String outputFilePath) {


        File folder = new File(folderPath);
        File[] jsonFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("No json files in the folder.");

            return;
        }

        JSONObject mergedObject = new JSONObject();

        for (File file : jsonFiles) {
            try {
                String data = new String(Files.readAllBytes(file.toPath())).trim();
                JSONObject jsonObject = new JSONObject(data);

                String className = jsonObject.keys().next();
                JSONObject classData = jsonObject.getJSONObject(className);

                mergedObject.put(className, classData);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        try (FileWriter fileWriter = new FileWriter(outputFilePath)) {
            fileWriter.write(mergedObject.toString(4));
            System.out.println("Merged json is written to: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Deleting temp json files...");
        for (File file : jsonFiles) {
            if (file.delete()) {
                System.out.println("Deleted: " + file.getName());
            } else {
                System.out.println("Failed to delete file: " + file.getName());
            }
        }
    }
}
