import org.json.JSONObject;
import qilin.core.PTA;
import qilin.core.pag.ContextVarNode;
import qilin.core.pag.LocalVarNode;
import qilin.core.pag.VarNode;
import soot.Context;
import soot.SootMethod;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class AnalysisEntrypoint {

    public static String[] arguments = {
            "-apppath",
            "",
            "-mainclass",
            "",
            "-jre",
            "",
            "-pae",
            "-pe",
            "-se",
            "-clinit=APP",
            "-mh",
            "-lcs",
            "-pta=1c"
    };

    public static Map<String, Map<String, List<String>>> analysisData = new HashMap<>();

    public static void main(String[] args) {

        arguments[1] = args[2];
        arguments[3] = args[0] + args[1];
        arguments[5] = args[3];

        String tempJsonFolder = args[4];

        PTA pta = driver.Main.run(arguments);

        String className = args[1];

        analysisData.put(className, new HashMap<>());

        SootMethod currentMethod = null;
        //Context-sensitive variables
        Map<VarNode, Map<Context, ContextVarNode>> contextVarNode = pta.getPag().getContextVarNodeMap();

        for (Map.Entry<VarNode, Map<Context, ContextVarNode>> entry : contextVarNode.entrySet()) {

            if (entry.getKey().base() != null &&
                    entry.getKey().base().toString().contains("org.graalvm.polyglot.Value")) {

                if (entry.getKey().base() instanceof LocalVarNode) {
                    LocalVarNode localKey = (LocalVarNode) entry.getKey().base();
                    SootMethod newMethod = localKey.getMethod();

                    if (newMethod != null) {
                        if (currentMethod == null ||
                                !currentMethod.toString().equals(newMethod.toString())) {
                            currentMethod = newMethod;
                            System.out.println("\n" + currentMethod + "\n");
                            analysisData.get(className)
                                    .put(currentMethod.getName(), new ArrayList<>());
                        }

                    }
                }
                String polVal = "Polyglot Value: " + entry.getKey().base();
                System.out.println(polVal);
                analysisData.get(className).get(currentMethod.getName()).add(polVal);
                for (Map.Entry<Context, ContextVarNode> entry1 : entry.getValue().entrySet()) {
                    ContextVarNode varNode = entry1.getValue();
                    String varNodeStr = "  Variable Node: " + varNode;
                    System.out.println(varNodeStr);
                    analysisData.get(className).get(currentMethod.getName()).add(varNodeStr);
                }

            }
        }
        writeJsonToFile(analysisData, tempJsonFolder);

    }

    public static void writeJsonToFile(Map<String, Map<String, List<String>>> data, String filePath) {
        JSONObject jsonObject = new JSONObject(data);
        //make filename random
        String randomFileName;
        if (filePath.endsWith("/"))
            randomFileName = filePath + "out_" + UUID.randomUUID() + ".json";
        else randomFileName = filePath + "/out_" + UUID.randomUUID() + ".json";

        try (FileWriter file = new FileWriter(randomFileName)) {
            file.write(jsonObject.toString(4));
            System.out.println("JSON successfully written to " + randomFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
