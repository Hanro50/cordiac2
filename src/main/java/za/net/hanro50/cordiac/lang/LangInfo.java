package za.net.hanro50.cordiac.lang;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import za.net.hanro50.cordiac.Util;


public class LangInfo {
    public Map<String, String> info = new HashMap<>();
    public final String current;

    LangInfo(String current, File MainFile, Gson gson) {
        this.current = current;
        for (File file : MainFile.listFiles()) {
            if (file.getName().endsWith(".json")) {

                try {
                    Map<String, String> Tmp = Parser.gson.fromJson(Util.readFile(file), Parser.mapToken);
                    String name = Tmp.get("language.name");
                    String region = Tmp.get("language.region");

                    if (region != null)
                        name += " (" + region + ")";
                    info.put(file.getName().substring(0, file.getName().length() - 5), name);
                } catch (JsonSyntaxException | IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }
        

    }
}
