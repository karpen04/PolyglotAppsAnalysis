package pascal.taie.analysis.custom;
import org.json.JSONObject;
import pascal.taie.Main;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.graph.icfg.ICFG;
import pascal.taie.analysis.graph.icfg.ICFGBuilder;
import pascal.taie.analysis.pta.PointerAnalysis;
import pascal.taie.analysis.pta.PointerAnalysisResultImpl;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class InterLangComAnalysis extends ProgramAnalysis<Set<Stmt>> {

    public static final String ID = "inter-lang-com-analysis";

    static Map<String, Map<String, Map<String, List<String>>>> analysisData = new HashMap<>();

    public InterLangComAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Set<Stmt> analyze() {

        PointerAnalysisResultImpl result = World.get().getResult(PointerAnalysis.ID);

        JClass testCaseClass = World.get().getMainMethod().getRef().getDeclaringClass();
        String className = World.get().getMainMethod().getRef().getDeclaringClass().getName()
                .split("\\.")[1];

        ICFG<Method,Stmt> icfg = World.get().getResult(ICFGBuilder.ID);

        analysisData.put(className,
                new HashMap<>());

        for(JMethod method : testCaseClass.getDeclaredMethods()) {

            analysisData.get(className).put(method.getName(), new HashMap<>());

            Map<Stmt,Var> ptaVars = new HashMap<>();
            Map<Stmt,Obj> ptaObjs = new HashMap<>();

            method.getIR().getStmts().forEach(stmt -> {
                 System.out.println("Stmt: " + stmt);
                 System.out.println("In edges: ");
                icfg.getInEdgesOf(stmt).forEach(System.out::println);
                 System.out.println("Out edges: ");
                icfg.getOutEdgesOf(stmt).forEach(System.out::println);

                for (Var var : result.getVars()) {
                    if (stmt.toString().contains(var.getName()) &&
                            var.getType().getName().contains("polyglot.Value")
                    && var.getInvokes().stream().anyMatch(el -> el.toString().contains(className))) {

                        if(!analysisData.get(className).get(method.getName()).containsKey(stmt.toString())) {
                            analysisData.get(className).get(method.getName()).put(stmt.toString(), new ArrayList<>());
                        }
                        analysisData.get(className).get(method.getName()).get(stmt.toString()).add(var.toString());
                        icfg.getInEdgesOf(stmt).forEach(inEdge ->
                                analysisData.get(className).get(method.getName()).get(stmt.toString()).add("In edge: " + inEdge));
                        icfg.getOutEdgesOf(stmt).forEach(outEdge ->
                                analysisData.get(className).get(method.getName()).get(stmt.toString()).add("Out edge: " + outEdge));

                        ptaVars.put(stmt, var);
                    }
                }

                for (Obj obj : result.getObjects()) {

                    if (stmt.toString().contains(obj.getAllocation().toString().split("=")[0])
                            && obj.getType().getName().contains("polyglot.Value")) {
                        if(!analysisData.get(className).get(method.getName()).containsKey(stmt.toString())) {
                            analysisData.get(className).get(method.getName()).put(stmt.toString(), new ArrayList<>());
                        }
                        analysisData.get(className).get(method.getName()).get(stmt.toString()).add(obj.toString());
                        ptaObjs.put(stmt, obj);
                    }
                }
            });

            System.out.println("=============Found Pta Vars and Objects=============");

            for (Map.Entry<Stmt, Var> stVar : ptaVars.entrySet()) {

                System.out.println("Stmt: " + stVar.getKey() +
                        "\nVar: " + stVar.getValue());

            }
            for (Map.Entry<Stmt, Obj> stObj : ptaObjs.entrySet()) {

                System.out.println("Stmt: " + stObj.getKey() +
                        "\nObj: " + stObj.getValue());

            }

        }
        writeJsonToFile(analysisData, Main.outputFile.getAbsolutePath());
        return Set.of();
    }

    public static void writeJsonToFile(Map<String, Map<String, Map<String, List<String>>>> data, String filePath) {
        JSONObject jsonObject = new JSONObject(data);
        //make filename random
        String randomFileName = filePath + "/" + UUID.randomUUID() + "_out.json";
        try (FileWriter file = new FileWriter(randomFileName)) {
            file.write(jsonObject.toString(4));
            System.out.println("JSON successfully written to " + randomFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
