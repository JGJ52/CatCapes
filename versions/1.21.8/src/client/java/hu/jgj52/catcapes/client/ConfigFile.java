package hu.jgj52.catcapes.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ConfigFile {

    private static final Gson gson = new Gson();
    private static final File file = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("catcapes.json")
            .toFile();
    private static JsonObject json = new JsonObject();

    public static void load() {
        try {
            if (file.exists()) {
                json = gson.fromJson(new FileReader(file), JsonObject.class);
            } else {
                try (FileWriter writer = new FileWriter(file)) {
                    gson.toJson(json, writer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key, String def) {
        if (!json.has(key)) return def;
        return json.get(key).getAsString();
    }

    public static void set(String key, String value) {
        json.addProperty(key, value);
        save();
    }
}