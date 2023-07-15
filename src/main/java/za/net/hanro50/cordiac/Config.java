package za.net.hanro50.cordiac;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {
    static FileConfiguration config;

    static String getToken() {
        return config.getString("token");
    }

    public static boolean useWebHooks() {
        return config.getBoolean("useWebHooks", true);
    }

    public static boolean showJoinMessages() {
        return config.getBoolean("showJoinMessages", true);
    }

    public static boolean showLeaveMessages() {
        return config.getBoolean("showLeaveMessages", true);
    }

    public static boolean showDeathMessages() {
        return config.getBoolean("showDeathMessages", true);
    }

    public static boolean showAdvancementMessages() {
        return config.getBoolean("showAdvancementMessages", true);
    }

    public static boolean showAchievementMessages() {
        return config.getBoolean("showAchievementMessages", true);
    }

    public static boolean forceLink() {
        return config.getBoolean("forceLink", false);
    }

    public static String language() {
        return config.getString("language", "en_gb");
    }

    public static List<String> reports() {
        List<String> result = config.getStringList("report");
        if (result == null || result.size() < 1) {
            result = new ArrayList<>();
            result.add("grief");
            result.add("inappropriate_build");
            result.add("theft");
            result.add("dispute");
            result.add("xray");
        }

        return result;
    }

    public static String chatFormat() {
        return config.getString("format", "<%username%> %message%");
    }
}
