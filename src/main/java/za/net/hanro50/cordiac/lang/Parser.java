package za.net.hanro50.cordiac.lang;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import za.net.hanro50.cordiac.App;
import za.net.hanro50.cordiac.Config;
import za.net.hanro50.cordiac.Util;

public class Parser {
    final static String manifest = "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    final static Gson gson = new GsonBuilder().create();
    final static Type mapToken = new TypeToken<Map<String, String>>() {
    }.getType();

    Map<String, String> mappings = new HashMap<String, String>();
    File conf;
    File cache;
    File base;
    LangInfo info;

    public String parse(String code, Object... args) {
        return String.format(mappings.getOrDefault(code, code), args);
    }

    public Parser()
            throws MalformedURLException, IOException {
        String pack = Config.language();
        this.base = new File(App.configDir, "lang");
        base.mkdir();
        String data = new String(Util.readAndClose(new URL(manifest).openStream()));
        ManifestJson manifests = gson.fromJson(data, ManifestJson.class);
        String assets = null;
        cache = new File(base, App.getInstance().getMinecraftVersion());
        cache.mkdir();
        File sFile = new File(base, App.getInstance().getMinecraftVersion() + "_manifest.json");
        if (!sFile.exists())
            for (Manifest manifest : manifests.versions) {
                App.getInstance().getLogger().info(manifest.id + "<===>" + App.getInstance().getMinecraftVersion());
                if (manifest.id.startsWith(App.getInstance().getMinecraftVersion())) {
                    System.out.println(gson.toJson(manifest));
                    String data2 = new String(Util.readAndClose(new URL(manifest.url).openStream()));
                    Version versionJson = gson.fromJson(data2, Version.class);
                    System.out.println(versionJson.assetIndex.url);
                    assets = new String(Util.readAndClose(new URL(versionJson.assetIndex.url).openStream()));
                    Util.write(sFile, assets);
                    break;
                }

            }
        else
            assets = Util.readFile(sFile);
        if (assets == null)
            return;

        Assets assetsObj = gson.fromJson(assets, Assets.class);
        List<String> availablePacks = new ArrayList<>();
        assetsObj.objects.forEach((k, v) -> {
            boolean langParser = k.endsWith(".lang");
            if (k.startsWith("minecraft/lang/") && (k.endsWith(".json")) || langParser) {

                String name = k.substring(k.lastIndexOf("/") + 1, k.length() - 5);
                availablePacks.add(name);
                if (langParser)
                    name = name.toLowerCase();
                File finalFile = new File(cache, name + ".json");
                if ((pack.equals(name)) || (name.equals("en_gb")))
                    if (finalFile.exists()) {
                        App.log.info("Pack already downloaded :" + name);
                    } else {
                        try {
                            App.log.info("Downloading language : " + name);
                            String d = new String(
                                    Util.readAndClose(new URL(
                                            "https://resources.download.minecraft.net/" + v.hash.substring(0, 2) + "/"
                                                    + v.hash)
                                            .openStream()));
                            // We need to convert the output to JSON
                            if (langParser) {
                                String done = "{";
                                for (String line : d.split("\n")) {
                                    if (line.contains("=") && !line.contains("\"")) {
                                        String[] items = line.split("=");
                                        App.log.info(items[0]);
                                        if (items[0].startsWith("entity.") && items[0].endsWith(".name")) {
                                            items[0] = "entity.minecraft."
                                                    + items[0].substring(7, items[0].length() - 5).toLowerCase();
                                        }
                                        done += "\"" + items[0] + "\":\"" + items[1] + "\",";
                                    }
                                }
                                d = done.substring(0, done.length() - 1) + "}";
                            }
                            Util.write(finalFile, d);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

            }
        });
        String packs = "Wiki:https://minecraft.fandom.com/wiki/Language\nAvailable languages:";
        for (String ap : availablePacks) {
            packs += "\n" + ap;
        }
        Util.write(new File(App.configDir, "Languages.txt"), packs);
        File finalFile = new File(cache, pack + ".json");
        if (finalFile.exists()) {
            mappings = gson.fromJson(Util.readFile(finalFile), mapToken);
        } else {
            App.log.warning("Invalid language code selected. Using fail-safe");
            File backup = new File(cache, "en_gb.json");
            mappings = gson.fromJson(Util.readFile(backup), mapToken);
        }

        // System.out.print(data);
    }

    public boolean has(String string) {
        return mappings.containsKey(string);
    }

}
